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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for importing a general Bazel test project.
 */
public class BazelImportTest extends BaseBazelImproterTest {

    public BazelImportTest() {
        super("projects/bazel-ls-demo-project");
    }

    @Before
    public void setup() {
        setSettings(BAZEL_SRC_PATH_VALUE);
        importProject();
    }

    @Test
    public void testImport() throws CoreException {
        IProject module1Proj = getWorkspaceRoot().getProject("module1");
        IProject module2Proj = getWorkspaceRoot().getProject("module2");
        IProject module3Proj = getWorkspaceRoot().getProject("module3");

        IProject[] referencedProjects = module1Proj.getReferencedProjects();

        assertEquals(2, referencedProjects.length);

        assertTrue("Didn't find module2 in the referenced projects list",
            Arrays.stream(referencedProjects).anyMatch(proj -> proj.equals(module2Proj)));

        assertTrue("Didn't find module3 in the referenced projects list",
            Arrays.stream(referencedProjects).anyMatch(proj -> proj.equals(module3Proj)));
    }
}
