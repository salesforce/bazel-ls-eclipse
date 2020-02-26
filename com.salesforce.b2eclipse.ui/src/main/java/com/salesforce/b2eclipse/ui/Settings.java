package com.salesforce.b2eclipse.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Settings {
	private static Settings instance;
	
	private static String INIT_PARAM_FILE_NAME = "initParam.properties";
	
	private Map<String, String> settings = new HashMap<>();
	
	private Settings() {
		try (InputStream input = getClass().getClassLoader().getResourceAsStream(INIT_PARAM_FILE_NAME)) {

			if(input != null) {
				Properties prop = new Properties();
	            prop.load(input);
	            for (String key : prop.stringPropertyNames()) {
	                settings.put(key, prop.getProperty(key));
	            }
			}
		
        } catch (IOException ex) {
            ex.printStackTrace();
        }
	}
	
	public static Settings getSettings() {
		if(instance == null) {
			instance = new Settings();
		}
		return instance;
	}

}
