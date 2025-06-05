package com.dogankaya.FinanStream.helpers;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "finanstream")
public class FinanStreamProperties {

    private List<String> handlerClassNames;

    private Map<String, PlatformProperties> platforms;

    private String ratesConfigPath;

    public List<String> getHandlerClassNames() {
        return handlerClassNames;
    }

    public void setHandlerClassNames(List<String> handlerClassNames) {
        this.handlerClassNames = handlerClassNames;
    }

    public Map<String, PlatformProperties> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(Map<String, PlatformProperties> platforms) {
        this.platforms = platforms;
    }

    public String getRatesConfigPath() {
        return ratesConfigPath;
    }

    public void setRatesConfigPath(String ratesConfigPath) {
        this.ratesConfigPath = ratesConfigPath;
    }

    public PlatformProperties getPlatformProperties(String platformName) {
        if (platforms == null) return null;
        return platforms.get(platformName);
    }

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
