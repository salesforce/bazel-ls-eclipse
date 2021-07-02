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
package com.salesforce.b2eclipse.classpath;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.salesforce.b2eclipse.BazelJdtPlugin;
import com.salesforce.b2eclipse.BazelNature;
import com.salesforce.b2eclipse.abstractions.WorkProgressMonitor;
import com.salesforce.b2eclipse.command.BazelCommandLineToolConfigurationException;
import com.salesforce.b2eclipse.command.BazelCommandManager;
import com.salesforce.b2eclipse.command.BazelWorkspaceCommandRunner;
import com.salesforce.b2eclipse.config.BazelEclipseProjectFactory;
import com.salesforce.b2eclipse.config.BazelEclipseProjectSupport;
import com.salesforce.b2eclipse.model.AspectOutputJars;
import com.salesforce.b2eclipse.model.AspectPackageInfo;
import com.salesforce.b2eclipse.model.BazelMarkerDetails;
import com.salesforce.b2eclipse.runtime.api.ResourceHelper;
import com.salesforce.b2eclipse.runtime.impl.EclipseWorkProgressMonitor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Computes the classpath for a Bazel package and provides it to the JDT tooling in Eclipse.
 */
@SuppressWarnings("restriction")
public class BazelClasspathContainer implements IClasspathContainer {
    public static final String CONTAINER_NAME = "com.salesforce.b2eclipse.BAZEL_CONTAINER";

    // TODO make classpath cache timeout configurable
    private static final long CLASSPATH_CACHE_TIMEOUT_MS = 30000;

    private final IPath eclipseProjectPath;
    private final IProject eclipseProject;
    private final IJavaProject eclipseJavaProject;
    private final String eclipseProjectName;

    private IClasspathEntry[] cachedEntries;
    private long cachePutTimeMillis = 0;

    private static List<BazelClasspathContainer> instances = new ArrayList<>();

    public BazelClasspathContainer(IProject eclipseProject, IJavaProject eclipseJavaProject)
            throws IOException, InterruptedException, BackingStoreException, JavaModelException,
            BazelCommandLineToolConfigurationException {
        this.eclipseProject = eclipseProject;
        this.eclipseJavaProject = eclipseJavaProject;
        eclipseProjectName = eclipseProject.getName();
        eclipseProjectPath = eclipseProject.getLocation();

        instances.add(this);
    }

    public static void clean() {
        for (BazelClasspathContainer instance : instances) {
            instance.cachedEntries = null;
            instance.cachePutTimeMillis = 0;
        }
    }

    private Set<IClasspathEntry> calculateBazelClasspath(Collection<AspectPackageInfo> aspectPackageInfos,
            WorkProgressMonitor progressMonitor) {
        BazelWorkspaceCommandRunner bazelWorkspaceCmdRunner = BazelJdtPlugin.getBazelCommandManager()
                .getWorkspaceCommandRunner(BazelJdtPlugin.getBazelWorkspaceRootDirectory());
        Set<IClasspathEntry> classpathEntries = aspectPackageInfos.stream().map(AspectPackageInfo::getJars)
                .flatMap(List::stream)
                .map((AspectOutputJars jars) -> jarsToClasspathEntry(bazelWorkspaceCmdRunner, progressMonitor, jars))
                .filter(Objects::nonNull).collect(Collectors.toSet());
        aspectPackageInfos.stream().map(AspectPackageInfo::getGeneratedJars).flatMap(List::stream)
        .map((AspectOutputJars jars) -> jarsToClasspathEntry(bazelWorkspaceCmdRunner, progressMonitor, jars))
        .filter(Objects::nonNull).forEachOrdered(classpathEntries::add);

        return classpathEntries;
    }

    @Override
    public IClasspathEntry[] getClasspathEntries() {
        // sanity check
        if (!BazelJdtPlugin.hasBazelWorkspaceRootDirectory()) {
            throw new IllegalStateException(
                    "Attempt to retrieve the classpath of a Bazel Java project prior to setting up the Bazel workspace.");
        }

        boolean foundCachedEntries = false;
        boolean isImport = false;

        /**
         * Observed behavior of Eclipse is that this method can get called multiple times before the first invocation
         * completes, therefore the cache is not as effective as it could be. Synchronize on this instance such that the
         * first invocation completes and populates the cache before the subsequent calls are allowed to proceed.
         */
        synchronized (this) {

            if (cachedEntries != null) {
                long now = System.currentTimeMillis();
                if ((now - cachePutTimeMillis) > CLASSPATH_CACHE_TIMEOUT_MS) {
                    cachedEntries = null;
                } else {
                    foundCachedEntries = true;
                    if (!BazelEclipseProjectFactory.getImportInProgress().get()) {
                        BazelJdtPlugin.logInfo("  Using cached classpath for project " + eclipseProjectName);
                        return cachedEntries;
                    }
                    // classpath computation is iterative right now during import, each project's classpath is computed many times.
                    // earlier in the import process, project refs might be brought in as jars because the associated project
                    //   may not have been imported yet.
                    // by not caching during import, the classpath is continually recomputed and eventually arrives in the right state
                    BazelJdtPlugin.logInfo("  Recomputing classpath for project " + eclipseProjectName
                        + " because we are in an import operation.");
                    isImport = true;
                }
            }

            WorkProgressMonitor progressMonitor = new EclipseWorkProgressMonitor(null);

            if (eclipseJavaProject.getElementName().startsWith(BazelNature.BAZELWORKSPACE_PROJECT_BASENAME)) {
                // this project is the artificial container to hold Bazel workspace scoped assets (e.g. the WORKSPACE file)
                return new IClasspathEntry[] {};
            }

            BazelJdtPlugin.logInfo("Computing classpath for project " + eclipseProjectName + " (cached entries: "
                    + foundCachedEntries + ", is import: " + isImport + ")");

            List<IClasspathEntry> classpathEntries = new ArrayList<>();
            Set<IPath> projectsAddedToClasspath = new HashSet<>();

            BazelCommandManager commandFacade = BazelJdtPlugin.getBazelCommandManager();
            BazelWorkspaceCommandRunner bazelWorkspaceCmdRunner =
                    commandFacade.getWorkspaceCommandRunner(BazelJdtPlugin.getBazelWorkspaceRootDirectory());

            try {
                IProject eclipseIProject = eclipseProject.getProject();
                List<String> bazelTargetsForProject =
                        BazelEclipseProjectSupport.getBazelTargetsForEclipseProject(eclipseIProject, false);

                Map<String, AspectPackageInfo> packageInfos = bazelWorkspaceCmdRunner.getAspectPackageInfos(
                    eclipseIProject.getName(), bazelTargetsForProject, progressMonitor, "getClasspathEntries");

                Set<IProject> projectDependencies = new HashSet<IProject>();
                packageInfos.values().forEach((AspectPackageInfo packageInfo) -> {
                    packageInfo.getGeneratedJars();
                });

                Set<IClasspathEntry> additionalClasspath =
                        calculateBazelClasspath(packageInfos.values(), progressMonitor);

                for (AspectPackageInfo packageInfo : packageInfos.values()) {
                    IJavaProject otherProject =
                            getSourceProjectForSourcePaths(bazelWorkspaceCmdRunner, packageInfo.getSources());

                    if (otherProject != null && eclipseProject.getProject().getFullPath().equals(otherProject.getProject().getFullPath())) {
                        BazelJdtPlugin.logInfo(
                                "the project referenced is actually the the current project that this classpath container is for - nothing to do");
                    } else if (otherProject != null) {
                        // otherProject != null
                        // add the referenced project to the classpath, directly as a project classpath entry
                        IPath projectFullPath = otherProject.getProject().getFullPath();
                        if (!projectsAddedToClasspath.contains(projectFullPath)) {
                            classpathEntries.add(BazelJdtPlugin.getJavaCoreHelper().newProjectEntry(projectFullPath));
                        }
                        projectsAddedToClasspath.add(projectFullPath);

                        // now make a project reference between this project and the other project; this allows for features like
                        // code refactoring across projects to work correctly
                        projectDependencies.add(otherProject.getProject());
                    }
                }
                classpathEntries.addAll(additionalClasspath);
                addProjectReferences(eclipseIProject, projectDependencies);
            } catch (IOException | InterruptedException e) {
                BazelJdtPlugin.logException(
                    "Unable to compute classpath containers entries for project " + eclipseProjectName, e);
                return new IClasspathEntry[] {};
            } catch (BazelCommandLineToolConfigurationException e) {
                BazelJdtPlugin.logError("Bazel not found: " + e.getMessage());
                return new IClasspathEntry[] {};
            }

            // cache the entries
            cachePutTimeMillis = System.currentTimeMillis();
            cachedEntries = classpathEntries.toArray(new IClasspathEntry[] {});
            BazelJdtPlugin.logInfo("Cached the classpath for project " + eclipseProjectName);
        }
        return cachedEntries;
    }

    @Override
    public String getDescription() {
        return "Bazel Classpath Container";
    }

    @Override
    public int getKind() {
        return K_APPLICATION;
    }

    @Override
    public IPath getPath() {
        return eclipseProjectPath;
    }

    public boolean isValid() throws BackingStoreException, IOException, InterruptedException,
    BazelCommandLineToolConfigurationException {
        File bazelWorkspaceRootDirectory = BazelJdtPlugin.getBazelWorkspaceRootDirectory();
        if (bazelWorkspaceRootDirectory == null) {
            return false;
        }
        BazelCommandManager bazelCommandManager = BazelJdtPlugin.getBazelCommandManager();
        BazelWorkspaceCommandRunner bazelWorkspaceCmdRunner =
                bazelCommandManager.getWorkspaceCommandRunner(bazelWorkspaceRootDirectory);

        if (bazelWorkspaceCmdRunner != null) {
            if (eclipseProject.getName().startsWith(BazelNature.BAZELWORKSPACE_PROJECT_BASENAME)) {
                return true;
            }
            List<String> targets =
                    BazelEclipseProjectSupport.getBazelTargetsForEclipseProject(eclipseProject.getProject(), false);
            List<BazelMarkerDetails> details =
                    bazelWorkspaceCmdRunner.runBazelBuild(targets, null, Collections.emptyList());
            return details.isEmpty();

        }
        return false;
    }

    // INTERNAL

    /**
     * Returns the IJavaProject in the current workspace that contains at least one of the specified sources.
     */
    private IJavaProject getSourceProjectForSourcePaths(BazelWorkspaceCommandRunner bazelCommandRunner,
            List<String> sources) {
        for (String candidate : sources) {
            IJavaProject project = getSourceProjectForSourcePath(bazelCommandRunner, candidate);
            if (project != null) {
                return project;
            }
        }
        return null;
    }

    private IJavaProject getSourceProjectForSourcePath(BazelWorkspaceCommandRunner bazelCommandRunner,
            String sourcePath) {

        // TODO this code is messy, why get workspace root two different ways, and is there a better way to handle source paths?
        ResourceHelper resourceHelper = BazelJdtPlugin.getResourceHelper();
        IWorkspaceRoot eclipseWorkspaceRoot = resourceHelper.getEclipseWorkspaceRoot();
        IWorkspace eclipseWorkspace = resourceHelper.getEclipseWorkspace();
        IWorkspaceRoot rootResource = eclipseWorkspace.getRoot();
        IProject[] projects = rootResource.getProjects();

        String absoluteSourcePathString = File.separator + sourcePath.replace("\"", "");
        Path absoluteSourcePath = new File(absoluteSourcePathString).toPath();

        for (IProject project : projects) {
            IJavaProject jProject = BazelJdtPlugin.getJavaCoreHelper().getJavaProjectForProject(project);
            IClasspathEntry[] classpathEntries = BazelJdtPlugin.getJavaCoreHelper().getRawClasspath(jProject);
            if (classpathEntries == null) {
                BazelJdtPlugin.logError("No classpath entries found for project [" + jProject.getElementName() + "]");
                continue;
            }
            for (IClasspathEntry entry : classpathEntries) {
                if (entry.getEntryKind() != IClasspathEntry.CPE_SOURCE) {
                    continue;
                }
                IResource res =
                        BazelJdtPlugin.getResourceHelper().findMemberInWorkspace(eclipseWorkspaceRoot, entry.getPath());
                if (res == null) {
                    continue;
                }
                IPath projectLocation = res.getLocation();
                String absProjectRoot = projectLocation.toOSString();
                absProjectRoot = entry.getPath().toString();
                if ((absProjectRoot != null && !absProjectRoot.isEmpty())
                        && absoluteSourcePath.startsWith(absProjectRoot)) {
                    IPath[] inclusionPatterns = entry.getInclusionPatterns();
                    IPath[] exclusionPatterns = entry.getExclusionPatterns();
                    if (!matchPatterns(absoluteSourcePath, exclusionPatterns) && (inclusionPatterns == null
                            || inclusionPatterns.length == 0 || matchPatterns(absoluteSourcePath, inclusionPatterns))) {
                        return jProject;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Globby match of file system patterns for a given path. If the path matches any of the patterns, this method
     * returns true.
     */
    private boolean matchPatterns(Path path, IPath[] patterns) {
        if (patterns != null) {
            for (IPath p : patterns) {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + p.toOSString());
                if (matcher.matches(path)) {
                    return true;
                }
            }
        }
        return false;
    }

    private IClasspathEntry jarsToClasspathEntry(BazelWorkspaceCommandRunner bazelCommandRunner,
            WorkProgressMonitor progressMonitor, AspectOutputJars jarSet) {
        IClasspathEntry cpEntry = null;
        File bazelOutputBase = bazelCommandRunner.getBazelWorkspaceOutputBase(progressMonitor);
        File bazelExecRoot = bazelCommandRunner.getBazelWorkspaceExecRoot(progressMonitor);
        IPath jarPath = getJarPathOnDisk(bazelOutputBase, bazelExecRoot, jarSet.getJar());
        if (jarPath != null) {
            IPath srcJarPath = getJarPathOnDisk(bazelOutputBase, bazelExecRoot, jarSet.getSrcJar());
            IPath srcJarRootPath = null;
            cpEntry = BazelJdtPlugin.getJavaCoreHelper().newLibraryEntry(jarPath, srcJarPath, srcJarRootPath);
        }
        return cpEntry;
    }

    @SuppressWarnings("unused")
    private IClasspathEntry[] jarsToClasspathEntries(BazelWorkspaceCommandRunner bazelCommandRunner,
            WorkProgressMonitor progressMonitor, Set<AspectOutputJars> jars) {
        IClasspathEntry[] entries = new IClasspathEntry[jars.size()];
        int i = 0;
        File bazelOutputBase = bazelCommandRunner.getBazelWorkspaceOutputBase(progressMonitor);
        File bazelExecRoot = bazelCommandRunner.getBazelWorkspaceExecRoot(progressMonitor);
        for (AspectOutputJars j : jars) {
            IPath jarPath = getJarPathOnDisk(bazelOutputBase, bazelExecRoot, j.getJar());
            if (jarPath != null) {
                IPath srcJarPath = getJarPathOnDisk(bazelOutputBase, bazelExecRoot, j.getSrcJar());
                IPath srcJarRootPath = null;
                entries[i] = BazelJdtPlugin.getJavaCoreHelper().newLibraryEntry(jarPath, srcJarPath, srcJarRootPath);
                i++;
            }
        }
        return entries;
    }

    private IPath getJarPathOnDisk(File bazelOutputBase, File bazelExecRoot, String file) {
        if (file == null) {
            return null;
        }
        Path path = null;
        if (file.startsWith("external")) {
            path = Paths.get(bazelOutputBase.toString(), file);
        } else {
            path = Paths.get(bazelExecRoot.toString(), file);
        }

        // We have had issues with Eclipse complaining about symlinks in the Bazel output directories not being real,
        // so we resolve them before handing them back to Eclipse.
        if (Files.isSymbolicLink(path)) {
            try {
                // resolving the link will fail if the symlink does not a point to a real file
                path = Files.readSymbolicLink(path);
            } catch (IOException ex) {
                BazelJdtPlugin.logError("Problem adding jar to project [" + eclipseProjectName
                    + "] because it does not exist on the filesystem: " + path);
                printDirectoryDiagnostics(path.toFile().getParentFile().getParentFile(), " ");
            }
        } else // it is a normal path, check for existence
            if (!Files.exists(path)) {
                BazelJdtPlugin.logError("Problem adding jar to project [" + eclipseProjectName
                    + "] because it does not exist on the filesystem: " + path);
                printDirectoryDiagnostics(path.toFile().getParentFile().getParentFile(), " ");
            }

        return org.eclipse.core.runtime.Path.fromOSString(path.toString());
    }

    /**
     * Creates a project references between this project and that project. The direction of reference goes from
     * this->that References are used by Eclipse code refactoring among other things.
     */
    private void addProjectReferences(IProject thisProject, Set<IProject> dependencies) {
        if (dependencies != null && dependencies.size() > 0) {
            ResourceHelper resourceHelper = BazelJdtPlugin.getResourceHelper();
            IProjectDescription projectDescription = resourceHelper.getProjectDescription(thisProject);
            IProject[] existingDependencies = projectDescription.getReferencedProjects();
            int oldSize = existingDependencies.length;
            Collections.addAll(dependencies, existingDependencies);
            if (dependencies.size() > oldSize) {
                IProject[] dependenciesArray = dependencies.toArray(IProject[]::new);
                projectDescription.setReferencedProjects(dependenciesArray);

                WorkspaceJob job = new WorkspaceJob("set project dependencies for " + thisProject.getName()) {
                    @Override
                    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
                        try {
                            thisProject.setDescription(projectDescription, null);
                        } catch (CoreException ce) {
                            BazelJdtPlugin.logException("set project dependencies for " + thisProject.getName(), ce);
                        }
                        return Status.OK_STATUS;
                    }
                };
                job.schedule();
            }

        }
    }

    private static void printDirectoryDiagnostics(File path, String indent) {
        File[] children = path.listFiles();
        System.out.println(indent + path.getAbsolutePath());
        if (children != null) {
            for (File child : children) {
                System.out.println(indent + child.getName());
                if (child.isDirectory()) {
                    printDirectoryDiagnostics(child, "   " + indent);
                }
            }
        }

    }
}
