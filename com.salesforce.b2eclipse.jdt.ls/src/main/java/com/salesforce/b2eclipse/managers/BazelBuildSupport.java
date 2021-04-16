package com.salesforce.b2eclipse.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ls.core.internal.ExtensionsExtractor;
import org.eclipse.jdt.ls.core.internal.IConstants;
import org.eclipse.jdt.ls.core.internal.IProjectImporter;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.ResourceUtils;
import org.eclipse.jdt.ls.core.internal.managers.IBuildSupport;
import org.eclipse.jdt.ls.core.internal.managers.ProjectsManager.CHANGE_TYPE;

import com.salesforce.b2eclipse.BazelJdtPlugin;
import com.salesforce.b2eclipse.BazelNature;

@SuppressWarnings("restriction")
public class BazelBuildSupport implements IBuildSupport {
    private static final String BUILD_FILE = "BUILD";
    private static final String WORKSPACE_FILE = "WORKSPACE";
    private static final String BAZEL_PROJECT_FILE_SUFIX = ".bazelproject";
    private static final List<String> WATCH_FILE_PATTERNS =
            Arrays.asList("**/BUILD", "**/WORKSPACE", "**/*.bazelproject");

    @Override
    public boolean applies(IProject project) {
        try {
            return project != null && project.hasNature(BazelNature.BAZEL_NATURE_ID);

        } catch (CoreException e) {
            return false;
        }
    }

    @Override
    public void update(IProject project, boolean force, IProgressMonitor monitor) throws CoreException {
        try {
            updateInternal(project, force, monitor);

        } catch (CoreException ex) {
            throw ex;

        } catch (AssertionFailedException ex) {
            // Bazel can't work with the provided project.
            // Just skip it.

        } catch (NoSuchElementException ex) {
            // No bazel importers found. This is definitely illegal situation
            // which is not clear how to solve right now.
            // Just skip it.
        }

    }

    protected void updateInternal(IProject project, boolean force, IProgressMonitor monitor) throws CoreException {

        Assert.isTrue(applies(project));

        final IProjectImporter importer = obtainBazelImporter().get();

        //TODO is false becasue rootFolder is NULL
        Assert.isTrue(importer.applies(monitor));

        importer.importToWorkspace(monitor);
    }

    @Override
    public boolean isBuildFile(IResource resource) {
        return resource != null && resource.getProject() != null && resource.getType() == IResource.FILE
                && (resource.getName().endsWith(BAZEL_PROJECT_FILE_SUFIX) || resource.getName().equals(BUILD_FILE)
                        || resource.getName().equals(BUILD_FILE));
    }

    @Override
    public boolean isBuildLikeFileName(String fileName) {
        return fileName.endsWith(BAZEL_PROJECT_FILE_SUFIX) || fileName.equals(BUILD_FILE)
                || fileName.equals(BUILD_FILE);
    }

    @Override
    public List<String> getWatchPatterns() {
        return WATCH_FILE_PATTERNS;
    }

    @Override
    public boolean fileChanged(IResource resource, CHANGE_TYPE changeType, IProgressMonitor monitor)
            throws CoreException {
        if (resource == null || !applies(resource.getProject())) {
            return false;
        }
        return IBuildSupport.super.fileChanged(resource, changeType, monitor) || isBuildFile(resource);
    }

    ///////

    private Optional<IProjectImporter> obtainBazelImporter() {
        return ExtensionsExtractor.<IProjectImporter>extractOrderedExtensions(IConstants.PLUGIN_ID, "importers")
                .stream().filter(importer -> importer instanceof BazelProjectImporter).findFirst();
    }

    @Override
    public String buildToolName() {
        return "Bazel";
    }

    @Override
    public boolean hasSpecificDeleteProjectLogic() {
        return true;
    }

    @Override
    public void deleteInvalidProjects(Collection<IPath> rootPaths, ArrayList<IProject> deleteProjectCandates,
            IProgressMonitor monitor) {
        for (IProject project : deleteProjectCandates) {
            if (applies(project)) {
                IPath projectLocation = getProjectLocation(project);

                if (ResourceUtils.isContainedIn(projectLocation, rootPaths)) {
                    // NOP - if the project is contained in the root path, it's a valid project
                    BazelJdtPlugin.logInfo("NOP - if the project is contained in the root path, it's a valid project");
                } else {
                    try {
                        project.delete(false, true, monitor);
                    } catch (CoreException e1) {
                        JavaLanguageServerPlugin.logException(e1.getMessage(), e1);
                    }
                }
            }
        }
    }

    private IPath getProjectLocation(IProject project) {
        IPath projectPath = project.getLocation();

        if (project.getFile("WORKSPACE").isAccessible()) {
            projectPath = project.getFile("WORKSPACE").getLocation();
        }

        if (project.getFile("BUILD").isAccessible()) {
            projectPath = project.getFile("BUILD").getLocation();
        }

        return projectPath;
    }
}
