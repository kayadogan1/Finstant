package com.dogankaya.FinanStream.helpers;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
/**
 * The {@code FinanStreamProperties} class holds configuration properties for the FinanStream application.
 * <p>
 * These properties include the list of handler class names to be loaded and detailed platform-specific
 * configuration information.
 * <p>
 * The class is configured via Spring Boot's {@code @ConfigurationProperties} mechanism.
 */
@Configuration
@ConfigurationProperties(prefix = "finanstream")
public class FinanStreamProperties {

    private List<String> handlerClassNames;
    private Map<String, PlatformProperties> platforms;
    private String ratesConfigPath;

    /**
     * Returns the list of handler class names.
     *
     * @return the list of handler class names.
     */
    public List<String> getHandlerClassNames() {
        return handlerClassNames;
    }
    /**
     * Sets the list of handler class names.
     *
     * @param handlerClassNames the list of handler class names to set.
     */
    public void setHandlerClassNames(List<String> handlerClassNames) {
        this.handlerClassNames = handlerClassNames;
    }
    /**
     * Returns the map of platform-specific properties.
     *
     * @return the platform properties map.
     */
    public Map<String, PlatformProperties> getPlatforms() {
        return platforms;
    }
    /**
     * Sets the platform-specific properties map.
     *
     * @param platforms the platform properties map to set.
     */
    public void setPlatforms(Map<String, PlatformProperties> platforms) {
        this.platforms = platforms;
    }

    public String getRatesConfigPath() {
        return ratesConfigPath;
    }

    public void setRatesConfigPath(String ratesConfigPath) {
        this.ratesConfigPath = ratesConfigPath;
    }

    /**
     * Retrieves the properties for a specific platform by name.
     *
     * @param platformName the name of the platform.
     * @return the platform properties, or {@code null} if the platform does not exist.
     */
    public PlatformProperties getPlatformProperties(String platformName) {
        if (platforms == null) return null;
        return platforms.get(platformName);
    }
    /**
     * Represents platform-specific configuration properties.
     */
    public static class PlatformProperties {
        private String host;
        private int port;
        private String name;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
