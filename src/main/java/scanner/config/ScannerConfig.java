package scanner.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class ScannerConfig {
    @JsonProperty
    private Map<String, Profile> profiles = new HashMap<>();
    
    @JsonProperty
    private String defaultProfile;
    
    @JsonProperty
    private GlobalConfig global = new GlobalConfig();

    public static class GlobalConfig {
        @JsonProperty
        private int defaultTimeout = 1000;
        
        @JsonProperty
        private int defaultThreads = 100;
        
        @JsonProperty
        private int maxRetries = 3;
        
        @JsonProperty
        private int retryDelay = 1000;
        
        @JsonProperty
        private List<Integer> excludedPorts = new ArrayList<>();

        // Getters and setters
        public int getDefaultTimeout() { return defaultTimeout; }
        public void setDefaultTimeout(int defaultTimeout) { this.defaultTimeout = defaultTimeout; }
        public int getDefaultThreads() { return defaultThreads; }
        public void setDefaultThreads(int defaultThreads) { this.defaultThreads = defaultThreads; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        public int getRetryDelay() { return retryDelay; }
        public void setRetryDelay(int retryDelay) { this.retryDelay = retryDelay; }
        public List<Integer> getExcludedPorts() { return excludedPorts; }
        public void setExcludedPorts(List<Integer> excludedPorts) { this.excludedPorts = excludedPorts; }
    }

    public static class Profile {
        @JsonProperty
        private String name;
        
        @JsonProperty
        private String description;
        
        @JsonProperty
        private String host = "localhost";
        
        @JsonProperty
        private int startPort = 1;
        
        @JsonProperty
        private int endPort = 65535;
        
        @JsonProperty
        private int timeout;
        
        @JsonProperty
        private int threads;
        
        @JsonProperty
        private List<Integer> excludedPorts = new ArrayList<>();
        
        @JsonProperty
        private Map<String, String> environment = new HashMap<>();

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getStartPort() { return startPort; }
        public void setStartPort(int startPort) { this.startPort = startPort; }
        public int getEndPort() { return endPort; }
        public void setEndPort(int endPort) { this.endPort = endPort; }
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
        public int getThreads() { return threads; }
        public void setThreads(int threads) { this.threads = threads; }
        public List<Integer> getExcludedPorts() { return excludedPorts; }
        public void setExcludedPorts(List<Integer> excludedPorts) { this.excludedPorts = excludedPorts; }
        public Map<String, String> getEnvironment() { return environment; }
        public void setEnvironment(Map<String, String> environment) { this.environment = environment; }
    }

    // Getters and setters
    public Map<String, Profile> getProfiles() { return profiles; }
    public void setProfiles(Map<String, Profile> profiles) { this.profiles = profiles; }
    public String getDefaultProfile() { return defaultProfile; }
    public void setDefaultProfile(String defaultProfile) { this.defaultProfile = defaultProfile; }
    public GlobalConfig getGlobal() { return global; }
    public void setGlobal(GlobalConfig global) { this.global = global; }
}
