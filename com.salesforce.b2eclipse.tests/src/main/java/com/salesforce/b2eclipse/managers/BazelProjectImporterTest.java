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

package com.salesforce.b2eclipse.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.salesforce.b2eclipse.config.BazelEclipseProjectFactory;
import com.salesforce.b2eclipse.importer.BazelProjectImportScanner;

@SuppressWarnings("restriction")
public class BazelProjectImporterTest {

    private static final String IMPORT_BAZEL_ENABLED = "java.import.bazel.enabled";

    private static final String BAZEL_SRC_PATH_KEY = "java.import.bazel.src.path";

    private static final String BAZEL_SRC_PATH_VALUE = "/java/src";

    private BazelProjectImporter importer;

    private B2EPreferncesManager preferencesManager;

    private final Map<String, Object> settings = new HashMap<>();

    @Before
    public void setup() {
        importer = new BazelProjectImporter();

        preferencesManager = B2EPreferncesManager.getInstance();

        settings.put(IMPORT_BAZEL_ENABLED, true);
        settings.put(BAZEL_SRC_PATH_KEY, BAZEL_SRC_PATH_VALUE);
        preferencesManager.setConfiguration(settings);
    }

    @After
    public void deleteImportedProjects() throws CoreException {
        for (IProject project : getWorkspaceRoot().getProjects()) {
            project.delete(true, null);
        }
    }

    @Test
    public void basic() throws CoreException {
        importer.initialize(new File("projects/bazel-ls-demo-project"));
        importer.importToWorkspace(new NullProgressMonitor());

        IProject module1Proj = getWorkspaceRoot().getProject("module1");
        JavaCore.create(module1Proj);
        List<ClasspathEntryMeta> module1CpEntriesMeta = new ArrayList<>(getVMContainerCPEntriesMeta(module1Proj));
        module1CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_SOURCE, BAZEL_SRC_PATH_VALUE));
        module1CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_PROJECT, "module2"));
        module1CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_PROJECT, "module3"));
        module1CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_LIBRARY, "guava"));
        module1CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_LIBRARY, "mybuilder_sources"));
        assertHasDependencies(module1Proj, Arrays.asList("module2", "module3"), module1CpEntriesMeta);

        IProject module2Proj = getWorkspaceRoot().getProject("module2");
        List<ClasspathEntryMeta> module2CpEntriesMeta = new ArrayList<>(getVMContainerCPEntriesMeta(module2Proj));
        module2CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_SOURCE, BAZEL_SRC_PATH_VALUE));
        module2CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_PROJECT, "module3"));
        module2CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_LIBRARY, "junit"));
        module2CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_LIBRARY, "module2-test"));
        assertHasDependencies(module2Proj, Arrays.asList("module3"), module2CpEntriesMeta);

        IProject module3Proj = getWorkspaceRoot().getProject("module3");
        List<ClasspathEntryMeta> module3CpEntriesMeta = new ArrayList<>(getVMContainerCPEntriesMeta(module3Proj));
        module3CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_SOURCE, BAZEL_SRC_PATH_VALUE));
        assertHasDependencies(module3Proj, Arrays.asList(), module3CpEntriesMeta);
    }

    @Test
    public void withClass() throws CoreException {
        importer.initialize(new File("projects/build-with-class"));
        updateSrcPath("/");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            importer.importToWorkspace(new NullProgressMonitor());
        });

        assertEquals("did not expect src code path to be equals to the bazel package path", exception.getMessage());
    }

    @Test
    public void withSubpackage() throws CoreException {
        importer.initialize(new File("projects/build-with-subpackage"));
        importer.importToWorkspace(new NullProgressMonitor());

        IProject moduleProj = getWorkspaceRoot().getProject("module");
        IProject subModuleProj = getWorkspaceRoot().getProject("submodule");
        IProject[] referencedProjects = moduleProj.getReferencedProjects();

        assertEquals(0, referencedProjects.length);

        assertFalse("Find submodule in the referenced projects list",
                Arrays.stream(referencedProjects).anyMatch(proj -> proj.equals(subModuleProj)));
    }

    @Test
    public void withEmptyTargetFile() throws CoreException, IOException {
        File targetFile = new File("projects/bazel-ls-demo-project", BazelProjectImportScanner.BAZELTARGETSFILENAME);

        FileUtils.writeStringToFile(targetFile, "", Charset.defaultCharset());
        basic();

        FileUtils.forceDelete(targetFile);
    }

    @Test
    public void withQueryInTargetFile() throws CoreException, IOException {
        File projectFile = new File("projects/bazel-ls-demo-project");
        File targetFile = new File(projectFile, BazelBuildSupport.BAZELPROJECT_FILE_NAME_SUFIX);

        FileUtils.writeLines(targetFile, Arrays.asList("directories:", "  module1", "  module2"));

        importer.initialize(projectFile);
        importer.importToWorkspace(new NullProgressMonitor());

        FileUtils.forceDelete(targetFile);

        IProject module1Proj = getWorkspaceRoot().getProject("module1");
        List<ClasspathEntryMeta> module1CpEntriesMeta = new ArrayList<>(getVMContainerCPEntriesMeta(module1Proj));
        module1CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_SOURCE, BAZEL_SRC_PATH_VALUE));
        module1CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_PROJECT, "module2"));
        module1CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_LIBRARY, "module3"));
        module1CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_LIBRARY, "guava"));
        module1CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_LIBRARY, "mybuilder_sources"));
        assertHasDependencies(module1Proj, Arrays.asList("module2"), module1CpEntriesMeta);

        IProject module2Proj = getWorkspaceRoot().getProject("module2");
        List<ClasspathEntryMeta> module2CpEntriesMeta = new ArrayList<>(getVMContainerCPEntriesMeta(module2Proj));
        module2CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_SOURCE, BAZEL_SRC_PATH_VALUE));
        module2CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_LIBRARY, "module3"));
        module2CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_LIBRARY, "junit"));
        module2CpEntriesMeta.add(new ClasspathEntryMeta(IClasspathEntry.CPE_LIBRARY, "module2-test"));
        assertHasDependencies(module2Proj, Arrays.asList(), module2CpEntriesMeta);
    }

    private IWorkspaceRoot getWorkspaceRoot() {
        return ResourcesPlugin.getWorkspace().getRoot();
    }

    private void updateSrcPath(String path) {
        settings.put(BAZEL_SRC_PATH_KEY, path);
        preferencesManager.setConfiguration(settings);
    }

    private List<ClasspathEntryMeta> getVMContainerCPEntriesMeta(IProject project) throws CoreException {
        IJavaProject javaProject = JavaCore.create(project);

        IClasspathEntry[] vmContainerCpEntries = JavaCore
                .getClasspathContainer(new Path(BazelEclipseProjectFactory.STANDARD_VM_CONTAINER_PREFIX), javaProject)
                .getClasspathEntries();

        return Arrays.stream(vmContainerCpEntries)
                .map(cpEntry -> new ClasspathEntryMeta(cpEntry.getEntryKind(), cpEntry.getPath().toString()))
                .collect(Collectors.toList());
    }

    private void assertHasDependencies(IProject proj, List<String> expectedRefProjectNames,
            List<ClasspathEntryMeta> expectedCPEntries) throws CoreException {
        List<IProject> referencedProjects = Arrays.asList(proj.getReferencedProjects());

        assertEquals(expectedRefProjectNames.size(), referencedProjects.size());

        expectedRefProjectNames.forEach(expectedRefProjectName -> {
            assertTrue(
                    String.format("Didn't find %s in the referenced projects list of %s", expectedRefProjectName,
                            proj.getName()),
                    referencedProjects.stream().anyMatch(refProj -> expectedRefProjectName.equals(refProj.getName())));
        });

        IJavaProject javaProj = JavaCore.create(proj);
        List<IClasspathEntry> projCpEntries = Arrays.asList(javaProj.getResolvedClasspath(false));

        assertEquals(expectedCPEntries.size(), projCpEntries.size());

        expectedCPEntries.forEach(cpeMeta -> {
            int cpeKind = cpeMeta.getKind();
            String cpePathSubstring = cpeMeta.getPathSubstring();

            assertTrue(
                    String.format("Didn't find (%s, %s) in the classpath of %s", cpeKind, cpePathSubstring,
                            proj.getName()),
                    projCpEntries.stream().anyMatch(projCpEntry -> projCpEntry.getEntryKind() == cpeKind
                    && projCpEntry.getPath().toString().contains(cpePathSubstring)));
        });
    }

    private static final class ClasspathEntryMeta {

        private final int kind;

        private final String pathSubstring;

        ClasspathEntryMeta(int kind, String pathSubstring) {
            this.kind = kind;
            this.pathSubstring = pathSubstring;
        }

        public int getKind() {
            return kind;
        }

        public String getPathSubstring() {
            return pathSubstring;
        }
    }

}
