package com.salesforce.b2eclipse.managers;

import java.util.Map;

import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.handlers.MapFlattener;

@SuppressWarnings("restriction")
public final class B2EPreferncesManager {

    /**
     * Preference key to enable/disable bazel importer.
     */
    public static final String IMPORT_BAZEL_ENABLED = "java.import.bazel.enabled";

    /**
     * Preference key to change java classes src path for bazel importer.
     */
    public static final String BAZEL_SRC_PATH = "java.import.bazel.src.path";

    /**
     * Preference key to change java classes test path for bazel importer.
     */
    public static final String BAZEL_TEST_PATH = "java.import.bazel.test.path";

    /**
     * Default java class src path for bazel importer.
     */
    public static final String BAZEL_DEFAULT_SRC_PATH = "/src/main/java";

    /**
     * Default java class test path for bazel importer.
     */
    public static final String BAZEL_DEFAULT_TEST_PATH = "/src/test/java";

    private static volatile B2EPreferncesManager instance;

    private boolean importBazelEnabled;
    private String importBazelSrcPath;
    private String importBazelTestPath;

    private B2EPreferncesManager() {
        Map<String, Object> configuration =
                JavaLanguageServerPlugin.getInstance().getPreferencesManager().getPreferences().asMap();
        importBazelEnabled = MapFlattener.getBoolean(configuration, IMPORT_BAZEL_ENABLED, false);
        importBazelSrcPath = MapFlattener.getString(configuration, BAZEL_SRC_PATH, BAZEL_DEFAULT_SRC_PATH);
        importBazelTestPath = MapFlattener.getString(configuration, BAZEL_TEST_PATH, BAZEL_DEFAULT_TEST_PATH);
    }

    public static B2EPreferncesManager getInstance() {
        B2EPreferncesManager localInstance = instance;

        if (localInstance == null) {
            synchronized (B2EPreferncesManager.class) {
                localInstance = instance;

                if (localInstance == null) {
                    localInstance = new B2EPreferncesManager();
                    instance = localInstance;
                }
            }
        }

        return localInstance;
    }

    public boolean isImportBazelEnabled() {
        return importBazelEnabled;
    }

    public String getImportBazelSrcPath() {
        return importBazelSrcPath;
    }

    public String getImportBazelTestPath() {
        return importBazelTestPath;
    }

}
