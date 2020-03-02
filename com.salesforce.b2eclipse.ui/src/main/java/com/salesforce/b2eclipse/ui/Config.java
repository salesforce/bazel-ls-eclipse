package com.salesforce.b2eclipse.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Config {
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
					instance = localInstance = new Config();
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
