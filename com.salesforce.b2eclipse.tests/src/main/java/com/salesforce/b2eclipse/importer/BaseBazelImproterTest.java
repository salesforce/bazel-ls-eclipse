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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import com.salesforce.b2eclipse.managers.B2EPreferncesManager;
import com.salesforce.b2eclipse.managers.BazelProjectImporter;

/**
 * An abstract class for importing a Bazel project.
 */
public abstract class BaseBazelImproterTest {

    public static final String IMPORT_BAZEL_ENABLED = "java.import.bazel.enabled";

    public static final String BAZEL_SRC_PATH = "java.import.bazel.src.path";

    public static final String BAZEL_SRC_PATH_VALUE = "/java/src";

    public static final String BAZEL_SRC_PATH_VALUE_FOR_BUILD_WITH_CLASS_TEST = "/";

    private BazelProjectImporter importer = new BazelProjectImporter();

    @SuppressWarnings("restriction")
    public BaseBazelImproterTest(String rootFolder) {
        importer.initialize(new File(rootFolder));
    }

    protected void setSettings(String path) {
        Map<String, Object> settings = new HashMap<>();
        settings.put(IMPORT_BAZEL_ENABLED, true);
        settings.put(BAZEL_SRC_PATH, path);
        B2EPreferncesManager preferencesManager = B2EPreferncesManager.getInstance();
        preferencesManager.setConfiguration(settings);
    }

    protected void importProject() {
        try {
            importer.importToWorkspace(new NullProgressMonitor());
        } catch (OperationCanceledException | CoreException e) {
            e.printStackTrace();
        }
    }

    public IWorkspaceRoot getWorkspaceRoot() {
        return ResourcesPlugin.getWorkspace().getRoot();
    }

}
