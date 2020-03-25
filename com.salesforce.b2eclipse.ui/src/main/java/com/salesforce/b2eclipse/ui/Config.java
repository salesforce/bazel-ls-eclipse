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

package com.salesforce.b2eclipse.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class Config {
    private static final String CP_WORKING_DIR_ENV = "B2E_JDTLS_REPOSITORY";

    private static final String CP_SOCKET_PORT = "connectionProvider.socket.port";

    private static final String LS_BAZEL_ENABLED = "java.import.bazel.enabled";

    private static final String LS_BAZEL_SRC_PATH = "java.import.bazel.src.path";

    private static final String LS_BAZEL_TEST_PATH = "java.import.bazel.test.path";

    private static volatile Config instance;

    private int cpSocketPort;

    private String cpProcessWorkingDir;

    private LSInitOptions lsInitOptions;

    private Config() {
        Properties properties = readProperties();

        cpSocketPort = Integer.parseInt(properties.getProperty(CP_SOCKET_PORT));
        cpProcessWorkingDir = System.getenv(CP_WORKING_DIR_ENV);

        lsInitOptions = buildLSInitOptions(properties);
    }

    public static Config getInstance() {
        Config localInstance = instance;

        if (localInstance == null) {
            synchronized (Config.class) {
                localInstance = instance;

                if (localInstance == null) {
                    localInstance = new Config();
                    instance = localInstance;
                }
            }
        }

        return localInstance;
    }

    public int getConnectionProviderSocketPort() {
        return cpSocketPort;
    }

    public String getConnectionProviderProcessWorkingDir() {
        return cpProcessWorkingDir;
    }

    public LSInitOptions getLanguageServerInitOptions() {
        return lsInitOptions;
    }

    private Properties readProperties() {
        Properties properties = new Properties();

        try (InputStream propStream = Config.class.getClassLoader().getResourceAsStream("plugin.properties")) {
            properties.load(propStream);
        } catch (IOException e) {
            B2EPlugin.logError(e);
            throw new RuntimeException(e);
        }

        return properties;
    }

    private LSInitOptions buildLSInitOptions(Properties properties) {
        Map<String, String> settings = new HashMap<>();

        settings.put(LS_BAZEL_ENABLED, properties.getProperty(LS_BAZEL_ENABLED));
        settings.put(LS_BAZEL_SRC_PATH, properties.getProperty(LS_BAZEL_SRC_PATH));
        settings.put(LS_BAZEL_TEST_PATH, properties.getProperty(LS_BAZEL_TEST_PATH));

        return new LSInitOptions(settings);
    }

    public static class LSInitOptions {
        private Map<String, String> settings;

        public LSInitOptions(Map<String, String> settings) {
            this.settings = settings;
        }
    }
}
