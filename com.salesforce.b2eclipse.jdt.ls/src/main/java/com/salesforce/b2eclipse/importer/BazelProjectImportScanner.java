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
package com.salesforce.b2eclipse.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;

import com.google.common.collect.Lists;
import com.salesforce.b2eclipse.BazelJdtPlugin;
import com.salesforce.b2eclipse.command.BazelCommandManager;
import com.salesforce.b2eclipse.command.BazelWorkspaceCommandRunner;
import com.salesforce.b2eclipse.model.BazelPackageInfo;
import com.salesforce.b2eclipse.runtime.impl.EclipseWorkProgressMonitor;

/**
 * Scans a Bazel workspace looking for Java packages (BUILD files that have java_binary or java_library targets). It is
 * assumed that the user will provide the root workspace directory (where the WORKSPACE file is) and we will scan the
 * subtree below that.
 * <p>
 * TODO the current Import UI approach is not scalable beyond a few hundred Bazel Java packages.
 * <p>
 * It will be difficult for a user to select the projects they want if there are hundreds/thousands of boxes in the tree
 * view. Since we stole this Import UI design from m2e, we are stuck with their approach for now. In Maven it is
 * unlikely that you have a single parent module with hundreds of submodules (ha ha, I know of one case of that) so they
 * didn't optimize import for huge numbers of modules.
 * <p>
 * In the future, we should allow the user to provide the workspace root (but not load anything). Then there would be
 * two modes:
 * <p>
 * 1. the existing design where we scan the entire workspace and suggest to add all packages
 * <p>
 * 2. do not scan the workspace and populate the tree control, but wait for the user to interact with a Search Box
 * control. They can selectively search for what they want and add projects (e.g. 'basic-rest-se' and then click the
 * basic-rest-service) and then optionally also import upstream and/or downstream deps.
 *
 * Or said another way, approach number 1 is a simple approach that does not consider the Bazel dependency tree. The
 * second approach would make use of Bazel Query to intelligently suggest other projects to import based on a small
 * selection picked by the user.
 * 
 * @author plaird
 * @author siarhei_tsitou@epam.com
 */
public class BazelProjectImportScanner {

    private static final String BAZELTARGETSFILENAME = ".bazeltargets";
    
    private BazelWorkspaceCommandRunner bazelWorkspaceCmdRunner;
    
    private File rootDirectoryFile;
    
    public BazelProjectImportScanner(BazelCommandManager bazelCommandManager, File rootDirectoryFile) {
        this.rootDirectoryFile = rootDirectoryFile;
        this.bazelWorkspaceCmdRunner = bazelCommandManager.getWorkspaceCommandRunner(rootDirectoryFile);
    }

    /**
     * Get a list of candidate Bazel packages to import. This list is provided to the user in the form of a tree
     * control.
     * <p>
     * Currently, the list returned will always be of size 1. It represents the root node of the scanned Bazel
     * workspace. The root node has child node references, and the tree expands from there.
     * <p>
     * TODO support scanning at an arbitrary location inside of a Bazel workspace (e.g. //projects/libs) and have the
     * scanner crawl up to the WORKSPACE root from there.
     * 
     * @param rootDirectory
     *            the directory to scan, which must be the root node of a Bazel workspace
     * @return the workspace root BazelPackageInfo
     */
    public BazelPackageInfo getProjects(IProgressMonitor monitor) {
        if (rootDirectoryFile == null || !rootDirectoryFile.exists() || !rootDirectoryFile.isDirectory()) {
            // this is the initialization state of the wizard
            return null;
        }

        // TODO the correct way to do this is put the configurator on another thread, and allow it to update the progress monitor.
        // Do it on-thread for now as it is easiest.

        List<String> modules = findAllModules(monitor);
        
        List<String> modulesToLoad = getConfiguredModules(monitor);
        
        if (modulesToLoad != null) {
            modules = modules.stream()
                .filter(modulesToLoad::contains)
                .collect(Collectors.toList());
        }

        BazelPackageInfo workspace = new BazelPackageInfo(rootDirectoryFile);

        for (String project : modules) {
            new BazelPackageInfo(workspace, project);
        }

        return workspace;
    }

    /**
     * From a given {@link File}, detects which directories can/should be imported as projects into the workspace and
     * configured by this scanner. This first set of directories is then presented to the user as import proposals.
     *
     * <p>
     * This method must be stateless.
     * </p>
     *
     * @param root
     *            the root directory on which to start the discovery
     * @return the children (at any depth) that this configurator suggests to import as project
     */
    private List<String> findAllModules(IProgressMonitor monitor) {
        return bazelWorkspaceCmdRunner.getJavaPackages(rootDirectoryFile, new EclipseWorkProgressMonitor(monitor));
    }
    
    private List<String> getConfiguredModules(IProgressMonitor monitor) {
        File targetsFile = new File(rootDirectoryFile, BAZELTARGETSFILENAME);
        
        if (!targetsFile.exists()) {
            BazelJdtPlugin.logInfo(BAZELTARGETSFILENAME + " file is missing. Importing all discovered modules.");
            return null;
        }

        List<String> targets = Lists.newArrayList();

        try (FileReader fr = new FileReader(targetsFile)) {
            BufferedReader br = new BufferedReader(fr);
            String line;

            while ((line = br.readLine()) != null) {
                //  TODO: ignore #comments
                targets.add(line);
            }

        return bazelWorkspaceCmdRunner.getPackagesForTargets(rootDirectoryFile, targets, new EclipseWorkProgressMonitor(monitor));
            
        } catch (IOException e) {
            BazelJdtPlugin.logError("ERROR reading " + BAZELTARGETSFILENAME + " file:" + e.getMessage());
            return null;
        }
    }

}
