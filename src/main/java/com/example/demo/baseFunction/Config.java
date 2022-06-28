package com.example.demo.baseFunction;

import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

@Log4j2
public class Config {

    private static final String APPLICATION_PROPERTIES = "application.properties";

    static public void fillConfig(String propertiesName, Properties properties) {
        try {
            ClassPathResource resource = new ClassPathResource(propertiesName);
            PropertiesLoaderUtils.fillProperties(properties, resource);
        } catch (IOException e) {
            log.error("config reader fillConfig", e);
            e.printStackTrace();
        }
    }

    static public void fillConfig(Properties properties) {
        fillConfig(APPLICATION_PROPERTIES, properties);
    }

    static public String getConfig(String key, String defaultValue) {
        try {
            ClassPathResource resource = new ClassPathResource(APPLICATION_PROPERTIES);
            Properties properties = PropertiesLoaderUtils.loadProperties(resource);
            return properties.getProperty(key, defaultValue);
        } catch (Exception e) {
            log.error("config reader getConfig", e);
            e.printStackTrace();
        }
        return null;
    }

    static public String getConfig(String key) {
        try {
            ClassPathResource resource = new ClassPathResource(APPLICATION_PROPERTIES);
            Properties properties = PropertiesLoaderUtils.loadProperties(resource);
            return properties.getProperty(key);
        } catch (Exception e) {
            log.error("config reader getConfig", e);
            e.printStackTrace();
        }
        return null;
    }
}
