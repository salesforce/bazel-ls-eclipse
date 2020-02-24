/**
 * Copyright (c) 2019, Salesforce.com, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of Salesforce.com nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */
package com.salesforce.b2eclipse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import com.google.common.base.Throwables;
import com.salesforce.b2eclipse.abstractions.BazelAspectLocation;
import com.salesforce.b2eclipse.command.BazelCommandManager;
import com.salesforce.b2eclipse.command.BazelWorkspaceCommandRunner;
import com.salesforce.b2eclipse.command.CommandBuilder;
import com.salesforce.b2eclipse.command.shell.ShellCommandBuilder;
import com.salesforce.b2eclipse.config.BazelAspectLocationImpl;
import com.salesforce.b2eclipse.runtime.api.JavaCoreHelper;
import com.salesforce.b2eclipse.runtime.api.ResourceHelper;
import com.salesforce.b2eclipse.runtime.impl.EclipseJavaCoreHelper;
import com.salesforce.b2eclipse.runtime.impl.EclipseResourceHelper;

/**
 * The activator class controls the Bazel Eclipse plugin life cycle
 */
public class BazelJdtPlugin extends Plugin {
	private static BundleContext context;
	
    // The plug-in ID
    public static final String PLUGIN_ID = "com.salesforce.b2eclipse.jdt.ls"; //$NON-NLS-1$

    // The preference key for the bazel workspace root path
    public static final String BAZEL_WORKSPACE_PATH_PREF_NAME = "bazel.workspace.root";

    // GLOBAL COLLABORATORS
    // TODO move the collaborators to some other place, perhaps a dedicated static context object

    /**
     * The location on disk that stores the Bazel workspace associated with the Eclipse workspace.
     * Currently, we only support one Bazel workspace in an Eclipse workspace so this is a static singleton.
     */
    private static File bazelWorkspaceRootDirectory = null;

    /**
     * Facade that enables the plugin to execute the bazel command line tool outside of a workspace
     */
    private static BazelCommandManager bazelCommandManager;

    /**
     * Runs bazel commands in the loaded workspace.
     */
    private static BazelWorkspaceCommandRunner bazelWorkspaceCommandRunner;

    /**
     * ResourceHelper is a useful singleton for looking up workspace/projects from the Eclipse environment
     */
    private static ResourceHelper resourceHelper;

    /**
     * JavaCoreHelper is a useful singleton for working with Java projects in the Eclipse workspace
     */
    private static JavaCoreHelper javaCoreHelper;
    
    // Command to find bazel path on windows
    public static final String WIN_BAZEL_FINDE_COMMAND = "where bazel";
    
    // Command to find bazel path on linux or mac
    public static final String LINUX_BAZEL_FINDE_COMMAND = "which bazel";
    
    public static final String BAZEL_EXECUTABLE_ENV_VAR = "BAZEL_EXECUTABLE";
    
    public static final String BAZEL_EXECUTABLE_DEFAULT_PATH = "/usr/local/bin/bazel";

    // LIFECYCLE

    /**
     * The constructor
     */
    public BazelJdtPlugin() {}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		
		BazelJdtPlugin.context = bundleContext;
		
        BazelAspectLocation aspectLocation = new BazelAspectLocationImpl();
		CommandBuilder commandBuilder = new ShellCommandBuilder();
        ResourceHelper eclipseResourceHelper = new EclipseResourceHelper();
        JavaCoreHelper eclipseJavaCoreHelper = new EclipseJavaCoreHelper();

		startInternal(aspectLocation, commandBuilder, eclipseResourceHelper, eclipseJavaCoreHelper);
		
	}

    /**
     * This is the inner entrypoint where the initialization really begins. Both the real activation entrypoint
     * (when running in Eclipse, seen above) and the mocking framework call in here. When running for real,
     * the passed collaborators are all the real ones, when running mock tests the collaborators are mocks.
     */
     public static void startInternal(BazelAspectLocation aspectLocation, CommandBuilder commandBuilder, ResourceHelper rh, JavaCoreHelper javac) {
        // global collaborators
        resourceHelper = rh;
        javaCoreHelper = javac;
        
        File bazelPathFile = new File(getBazelPath());
		bazelCommandManager = new BazelCommandManager(aspectLocation, commandBuilder, bazelPathFile);

	}
     
    public static String getBazelPath() {
        String path = getEnvBazelPath();
        
        if (path == null) {
            path = getOSBazelPath();
        }
        
        if (path == null) {
            path = BAZEL_EXECUTABLE_DEFAULT_PATH;
            BazelJdtPlugin.logError("BazelJDTPlugin could not find Bazel path, was used standart path " + path);
        }
        
        return path;
    }
     
    public static String getEnvBazelPath() {
        return System.getenv(BAZEL_EXECUTABLE_ENV_VAR);
    }
    
    public static String getOSBazelPath() {
        String path = null;
        String command = null;
        if (Platform.getOS().equals(Platform.OS_WIN32)) {
            command = WIN_BAZEL_FINDE_COMMAND;
        }
        if (Platform.getOS().equals(Platform.OS_LINUX) || Platform.getOS().equals(Platform.OS_MACOSX)) {
            command = LINUX_BAZEL_FINDE_COMMAND;
        }
                
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(command).getInputStream()))){
            path = reader.lines().findFirst().get();
        } catch (IOException | NoSuchElementException e) {
            path = null;
        }
        
        return path;
    }
	
	

    // COLLABORATORS
    // TODO move the collaborators to some other place, perhaps a dedicated static context object

    /**
     * Has the Bazel workspace location been imported/loaded? This is a good sanity check before doing any operation
     * related to Bazel or Bazel Java projects.
     */
    public static boolean hasBazelWorkspaceRootDirectory() {
        return bazelWorkspaceRootDirectory != null;
    }

    /**
     * Returns the location on disk where the Bazel workspace is located. There must be a WORKSPACE file
     * in this location. Prior to importing/opening a Bazel workspace, this location will be null
     */
    public static File getBazelWorkspaceRootDirectory() {
        return bazelWorkspaceRootDirectory;
    }

    /**
     * Returns the location on disk where the Bazel workspace is located. There must be a WORKSPACE file
     * in this location. Prior to importing/opening a Bazel workspace, this location will be null
     */
    public static String getBazelWorkspaceRootDirectoryPath() {
        if (bazelWorkspaceRootDirectory == null) {
            new Throwable().printStackTrace();
            BazelJdtPlugin.logError("BazelPluginActivator was asked for the Bazel workspace root directory before it is determined.");
            return null;
        }
        return bazelWorkspaceRootDirectory.getAbsolutePath();
    }

    /**
     * Sets the location on disk where the Bazel workspace is located. There must be a WORKSPACE file
     * in this location. Changing this location is a big deal, so use this method only during setup/import.
     */
	static public void setBazelWorkspaceRootDirectory(File dir) {
        File workspaceFile = new File(dir, "WORKSPACE");
        if (!workspaceFile.exists()) {
            new Throwable().printStackTrace();
            BazelJdtPlugin.logError("BazelPluginActivator could not set the Bazel workspace directory as there is no WORKSPACE file here: " + dir.getAbsolutePath());
            return;
        }
        bazelWorkspaceRootDirectory = dir;

        // write it to the preferences file
    }


    /**
     * Returns the unique instance of {@link BazelCommandManager}, the facade enables the plugin to execute the bazel
     * command line tool.
     */
    public static BazelCommandManager getBazelCommandManager() {
        return bazelCommandManager;
    }

    /**
     * Once the workspace is set, the workspace command runner is available. Otherwise returns null
     */
	public static BazelWorkspaceCommandRunner getWorkspaceCommandRunner() {
        if (bazelWorkspaceCommandRunner == null) {
            if (bazelWorkspaceRootDirectory != null) {
                bazelWorkspaceCommandRunner = bazelCommandManager.getWorkspaceCommandRunner(bazelWorkspaceRootDirectory);
            }
        }
        return bazelWorkspaceCommandRunner;
    }

    /**
     * Returns the unique instance of {@link ResourceHelper}, this helper helps retrieve workspace and project
     * objects from the environment
     */
    public static ResourceHelper getResourceHelper() {
        return resourceHelper;
    }

    /**
     * Returns the unique instance of {@link JavaCoreHelper}, this helper helps manipulate the Java configuration
     * of a Java project
     */
    public static JavaCoreHelper getJavaCoreHelper() {
        return javaCoreHelper;
    }

	public static void log(IStatus status) {
		if (context != null) {
			Platform.getLog(BazelJdtPlugin.context.getBundle()).log(status);
		}
	}

	public static void log(CoreException e) {
		log(e.getStatus());
	}

	public static void logError(String message) {
		if (context != null) {
			log(new Status(IStatus.ERROR, context.getBundle().getSymbolicName(), message));
		}
	}

	public static void logInfo(String message) {
		if (context != null) {
			log(new Status(IStatus.INFO, context.getBundle().getSymbolicName(), message));
		}
	}

	public static void logException(Throwable ex) {
		if (context != null) {
			String message = ex.getMessage();
			if (message == null) {
				message = Throwables.getStackTraceAsString(ex);
			}
			logException(message, ex);
		}
	}

	public static void logException(String message, Throwable ex) {
		if (context != null) {
			log(new Status(IStatus.ERROR, context.getBundle().getSymbolicName(), message, ex));
		}
	}
}
