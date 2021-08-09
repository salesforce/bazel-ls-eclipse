package com.salesforce.b2eclipse.util;

import static com.salesforce.b2eclipse.BazelJdtPlugin.getJavaCoreHelper;
import static com.salesforce.b2eclipse.BazelJdtPlugin.getResourceHelper;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

import com.salesforce.b2eclipse.BazelJdtPlugin;
import com.salesforce.b2eclipse.classpath.BazelClasspathContainer;
import com.salesforce.b2eclipse.command.BazelWorkspaceCommandRunner;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

public class ClasspathUtils {
    private static final String JAVA_DEBUG_DELEGATE_CMD_HANDLER = "JavaDebugDelegateCommandHandler";
    private static final String JUNIT_LAUNCH_CONFIGURATION_DELEGATE = "JUnitLaunchConfigurationDelegate";
    private static final String JDT_UTILS = "JdtUtils";
    private static final String JAVA_RUNTIME_CLASS = "JavaRuntime";
    private static final String JAVA_RUNTIME_METHOD = "resolveRuntimeClasspath";
    private static final String BAZEL_ECLIPSE_PROJECT_FACTORY = "BazelEclipseProjectFactory";
    private static final String ABSTRACT_JAVA_LAUNCH_CONFIGURATION_DELEGATE = "AbstractJavaLaunchConfigurationDelegate";
    private static final String METHOD_JDT_UTILS_SOURCE_CONTAINERS = "getSourceContainers";
    private static final String METHOD_GET_JAVA_LIBRARY_PATH = "getJavaLibraryPath";
    private static final String METHOD_GET_CLASSPATH_AND_MODULEPATH = "getClasspathAndModulepath";
    private static final String METHOD_GET_MODULE_CLI_OPTIONS = "getModuleCLIOptions";
    private static final String METHOD_COLLECT_EXECUTION_ARGUMENTS = "collectExecutionArguments";

    /**
     * Returns the IJavaProject in the current workspace that contains at least one of the specified sources.
     */
    public static IJavaProject getSourceProjectForSourcePaths(BazelWorkspaceCommandRunner bazelCommandRunner,
            List<String> sources) {
        for (String candidate : sources) {
            IJavaProject project = getSourceProjectForSourcePath(bazelCommandRunner, candidate);
            if (project != null) {
                return project;
            }
        }
        return null;
    }

    public static boolean isTopLevelCall(StackTraceElement[] stack) {
        CallSource callSource = getCallSource(stack);
        switch (callSource) {
            case JDT_UTILS:
                return isAcceptableJdtUtils(stack);
            case RUN_DEBUG:
                return isAcceptableRunDebugRange(stack);
            case JUNIT:
                return isAcceptableJunit(stack);
            case PROJECT_FACTORY:
                return false;
            default:
                return false;
        }
    }

    private static CallSource getCallSource(StackTraceElement[] stack) {
        for (StackTraceElement elem : stack) {
            String classname = elem.getClassName();
            if (classname.endsWith(JAVA_DEBUG_DELEGATE_CMD_HANDLER)) {
                return CallSource.RUN_DEBUG;
            }
            if (classname.endsWith(JUNIT_LAUNCH_CONFIGURATION_DELEGATE)) {
                return CallSource.JUNIT;
            }
            if (classname.endsWith(JDT_UTILS)) {
                return CallSource.JDT_UTILS;
            }
            if (classname.endsWith(BAZEL_ECLIPSE_PROJECT_FACTORY)) {
                return CallSource.PROJECT_FACTORY;
            }
        }
        return CallSource.UNDEFINED;
    }

    private static boolean isAcceptableJunit(StackTraceElement[] stack) {
        /*
         * case getJavaLibraryPath:
         * 
         * BazelClasspathContainer.getClasspathEntries() line: 175  
         * JavaRuntime.processJavaLibraryPathEntries(IJavaProject, boolean, IClasspathEntry[], List<String>) line: 3085    
         * JavaRuntime.gatherJavaLibraryPathEntries(IJavaProject, boolean, Set<IJavaProject>, List<String>) line: 3036 
         * JavaRuntime.computeJavaLibraryPath(IJavaProject, boolean) line: 2994    
         * JUnitLaunchConfigurationDelegate(AbstractJavaLaunchConfigurationDelegate).getJavaLibraryPath(ILaunchConfiguration) line: 1088
         * 
         * case getClasspathAndModulepath:
         * 
         * BazelClasspathContainer.getClasspathEntries() line: 175  
         * JavaRuntime.computeDefaultContainerEntries(IRuntimeClasspathEntry, IJavaProject, boolean) line: 1560    
         * JavaRuntime.computeDefaultContainerEntries(IRuntimeClasspathEntry, ILaunchConfiguration, boolean) line: 1533    
         * JavaRuntime.resolveRuntimeClasspathEntry(IRuntimeClasspathEntry, ILaunchConfiguration) line: 1260   
         * StandardClasspathProvider.resolveClasspath(IRuntimeClasspathEntry[], ILaunchConfiguration) line: 95 
         * JavaRuntime.resolveRuntimeClasspath(IRuntimeClasspathEntry[], ILaunchConfiguration) line: 1662  
         * JUnitLaunchConfigurationDelegate(AbstractJavaLaunchConfigurationDelegate).getClasspathAndModulepath(ILaunchConfiguration) line: 464 
         * 
         * case getModuleCLIOptions:
         * BazelClasspathContainer.getClasspathEntries() line: 175  
         * JavaRuntime.computeDefaultContainerEntries(IRuntimeClasspathEntry, IJavaProject, boolean) line: 1560    
         * JavaRuntime.computeDefaultContainerEntries(IRuntimeClasspathEntry, ILaunchConfiguration, boolean) line: 1533    
         * JavaRuntime.resolveRuntimeClasspathEntry(IRuntimeClasspathEntry, ILaunchConfiguration) line: 1260   
         * StandardClasspathProvider.resolveClasspath(IRuntimeClasspathEntry[], ILaunchConfiguration) line: 95 
         * JavaRuntime.resolveRuntimeClasspath(IRuntimeClasspathEntry[], ILaunchConfiguration) line: 1662  
         * JUnitLaunchConfigurationDelegate(AbstractJavaLaunchConfigurationDelegate).getModuleCLIOptions(ILaunchConfiguration) line: 1156
         * 
         */
        int idx = 0;
        for (; idx < stack.length; idx++) {
            String classname = stack[idx].getClassName();
            if (classname.endsWith(BazelClasspathContainer.class.getName())) {
                break;
            }
        }
        for (int i = idx; i < stack.length; i++) {
            String classname = stack[i].getClassName();
            String methodname = stack[i].getMethodName();
            if (classname.endsWith(ABSTRACT_JAVA_LAUNCH_CONFIGURATION_DELEGATE)) {
                if (METHOD_GET_JAVA_LIBRARY_PATH.equals(methodname)) {
                    return i - idx < 6;
                }
                if (METHOD_GET_CLASSPATH_AND_MODULEPATH.equals(methodname)) {
                    return i - idx < 8;
                }
                if (METHOD_GET_MODULE_CLI_OPTIONS.equals(methodname)) {
                    return i - idx < 8;
                }
            }
            if (classname.endsWith(JUNIT_LAUNCH_CONFIGURATION_DELEGATE)) {
                if (METHOD_COLLECT_EXECUTION_ARGUMENTS.equals(methodname)) {
                    return i - idx < 9;
                }
            }
        }

        return false;
    }

    private static boolean isAcceptableJdtUtils(StackTraceElement[] stack) {
        /*
         * Case source code lookup.
         * 
         * BazelClasspathContainer.getClasspathEntries() line: 271 
         * JavaRuntime.computeDefaultContainerEntries(IRuntimeClasspathEntry, IJavaProject, boolean) line: 1560    
         * JavaRuntime.resolveRuntimeClasspathEntry(IRuntimeClasspathEntry, IJavaProject, boolean) line: 1503  
         * DefaultEntryResolver.resolveRuntimeClasspathEntry(IRuntimeClasspathEntry, IJavaProject, boolean) line: 70   
         * JavaRuntime.resolveRuntimeClasspathEntry(IRuntimeClasspathEntry, IJavaProject, boolean) line: 1508  
         * JavaRuntime.resolveRuntimeClasspathEntry(IRuntimeClasspathEntry, IJavaProject) line: 1444
         * JdtUtils.getSourceContainers(IJavaProject, Set<IRuntimeClasspathEntry>) line: 234
         */
        int idx = 0;
        for (; idx < stack.length; idx++) {
            String classname = stack[idx].getClassName();
            if (classname.endsWith(BazelClasspathContainer.class.getName())) {
                break;
            }
        }
        int limitIdx = idx + 7;
        for (; idx < limitIdx && idx < stack.length; idx++) {
            String classname = stack[idx].getClassName();
            String methodname = stack[idx].getMethodName();
            if (classname.endsWith(JDT_UTILS) && METHOD_JDT_UTILS_SOURCE_CONTAINERS.equals(methodname)) {
                return true;
            }

        }
        return false;
    }

    private static boolean isAcceptableRunDebugRange(StackTraceElement[] stack) {
        /*
         * ### case 1 (distance - 6) :
         * BazelClasspathContainer.getClasspathEntries() line: 271  
         * JavaRuntime.computeDefaultContainerEntries(IRuntimeClasspathEntry, IJavaProject, boolean) line: 1560    
         * JavaRuntime.computeDefaultContainerEntries(IRuntimeClasspathEntry, ILaunchConfiguration, boolean) line: 1533    
         * JavaRuntime.resolveRuntimeClasspathEntry(IRuntimeClasspathEntry, ILaunchConfiguration) line: 1260   
         * StandardClasspathProvider.resolveClasspath(IRuntimeClasspathEntry[], ILaunchConfiguration) line: 95 
         * JavaRuntime.resolveRuntimeClasspath(IRuntimeClasspathEntry[], ILaunchConfiguration) line: 1662
         * 
         * ### case 2 (distance - 10. not top level):
         * BazelClasspathContainer.getClasspathEntries() line: 271  
         * JavaRuntime.computeDefaultContainerEntries(IRuntimeClasspathEntry, IJavaProject, boolean) line: 1560    
         * JavaRuntime.resolveRuntimeClasspathEntry(IRuntimeClasspathEntry, IJavaProject, boolean) line: 1503  
         * DefaultEntryResolver.resolveRuntimeClasspathEntry(IRuntimeClasspathEntry, IJavaProject, boolean) line: 70   
         * JavaRuntime.resolveRuntimeClasspathEntry(IRuntimeClasspathEntry, IJavaProject, boolean) line: 1508  
         * JavaRuntime.computeDefaultContainerEntries(IRuntimeClasspathEntry, IJavaProject, boolean) line: 1604    
         * JavaRuntime.computeDefaultContainerEntries(IRuntimeClasspathEntry, ILaunchConfiguration, boolean) line: 1533    
         * JavaRuntime.resolveRuntimeClasspathEntry(IRuntimeClasspathEntry, ILaunchConfiguration) line: 1260 
         * StandardClasspathProvider.resolveClasspath(IRuntimeClasspathEntry[], ILaunchConfiguration) line: 95 
         * JavaRuntime.resolveRuntimeClasspath(IRuntimeClasspathEntry[], ILaunchConfiguration) line: 1662    
         */
        int idx = 0;
        for (; idx < stack.length; idx++) {
            String classname = stack[idx].getClassName();
            if (classname.endsWith(BazelClasspathContainer.class.getName())) {
                break;
            }
        }
        int limitIdx = idx + 6;
        for (; idx < limitIdx && idx < stack.length; idx++) {
            String classname = stack[idx].getClassName();
            String methodname = stack[idx].getMethodName();
            if (classname.endsWith(JAVA_RUNTIME_CLASS) && JAVA_RUNTIME_METHOD.equals(methodname)) {
                return true;
            }

        }
        return false;
    }


    private static IJavaProject getSourceProjectForSourcePath(BazelWorkspaceCommandRunner bazelCommandRunner,
            String sourcePath) {

        // TODO this code is messy, why get workspace root two different ways, and is there a better way to handle source paths?
        IWorkspaceRoot eclipseWorkspaceRoot = getResourceHelper().getEclipseWorkspaceRoot();
        IWorkspace eclipseWorkspace = getResourceHelper().getEclipseWorkspace();
        IWorkspaceRoot rootResource = eclipseWorkspace.getRoot();
        IProject[] projects = rootResource.getProjects();

        String absoluteSourcePathString = File.separator + sourcePath.replace("\"", "");
        Path absoluteSourcePath = new File(absoluteSourcePathString).toPath();

        for (IProject project : projects) {
            IJavaProject jProject = getJavaCoreHelper().getJavaProjectForProject(project);
            IClasspathEntry[] classpathEntries = getJavaCoreHelper().getRawClasspath(jProject);
            if (classpathEntries == null) {
                BazelJdtPlugin.logError("No classpath entries found for project [" + jProject.getElementName() + "]");
                continue;
            }
            for (IClasspathEntry entry : classpathEntries) {
                if (entry.getEntryKind() != IClasspathEntry.CPE_SOURCE) {
                    continue;
                }
                IResource res = getResourceHelper().findMemberInWorkspace(eclipseWorkspaceRoot, entry.getPath());
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
    private static boolean matchPatterns(Path path, IPath[] patterns) {
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
}
