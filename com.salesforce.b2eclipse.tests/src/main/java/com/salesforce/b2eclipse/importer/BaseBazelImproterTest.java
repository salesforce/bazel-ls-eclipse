/**
 * Copyright (c) 2020, Salesforce.com, Inc. All rights reserved.
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
 */

package com.salesforce.b2eclipse.importer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;
import org.eclipse.jdt.ls.core.internal.preferences.Preferences;

import com.salesforce.b2eclipse.config.BazelEclipseProjectFactory;
import com.salesforce.b2eclipse.model.BazelPackageInfo;
import com.salesforce.b2eclipse.runtime.impl.EclipseWorkProgressMonitor;

/**
 * An abstract class for importing a Bazel project.
 */
@SuppressWarnings("restriction")
public abstract class BaseBazelImproterTest {

    public static final String BAZEL_SRC_PATH = "java.import.bazel.src.path";

    public static final String BAZEL_SRC_PATH_VALUE = "/java/src";

    public static final String BAZEL_SRC_PATH_VALUE_FOR_BUILD_WITH_CLASS_TEST = "/";

    private BazelPackageInfo workspaceRootPackage;
    private IWorkspaceRoot workspaceRoot;
    private BazelProjectImportScanner scanner;

    public BaseBazelImproterTest() {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        this.workspaceRoot = workspace.getRoot();
        scanner = new BazelProjectImportScanner();

        PreferenceManager manager = new PreferenceManager();
        Map<String, Object> settings = new HashMap<>();
        settings.put(BAZEL_SRC_PATH, BAZEL_SRC_PATH_VALUE);
        Preferences pref = Preferences.createFrom(settings);
        manager.update(pref);
        JavaLanguageServerPlugin.setPreferencesManager(manager);
    }

    protected void importProject() {
        List<BazelPackageInfo> bazelPackagesToImport =
                workspaceRootPackage.getChildPackageInfos().stream().collect(Collectors.toList());

        BazelEclipseProjectFactory.importWorkspace(workspaceRootPackage, bazelPackagesToImport,
            new EclipseWorkProgressMonitor(), new NullProgressMonitor());
    }

    public BazelPackageInfo getWorkspaceRootPackage() {
        return workspaceRootPackage;
    }

    public void setWorkspaceRootPackage(BazelPackageInfo workspaceRootPackage) {
        this.workspaceRootPackage = workspaceRootPackage;
    }

    public IWorkspaceRoot getWorkspaceRoot() {
        return workspaceRoot;
    }

    public void setWorkspaceRoot(IWorkspaceRoot workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    public BazelProjectImportScanner getScanner() {
        return scanner;
    }

    public void setScanner(BazelProjectImportScanner scanner) {
        this.scanner = scanner;
    }

}
