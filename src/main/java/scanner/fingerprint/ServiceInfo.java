package scanner.fingerprint;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceInfo {
    @JsonProperty
    private final int port;
    
    @JsonProperty
    private String serviceName = "Unknown";
    
    @JsonProperty
    private String version = "Unknown";
    
    @JsonProperty
    private String banner;
    
    @JsonProperty
    private String protocol = "TCP";
    
    @JsonProperty
    private boolean vulnerable = false;
    
    @JsonProperty
    private String vulnerabilityDetails;

    public ServiceInfo(int port) {
        this.port = port;
    }

    // Getters and setters
    public int getPort() { return port; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getBanner() { return banner; }
    public void setBanner(String banner) { this.banner = banner; }
    
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    
    public boolean isVulnerable() { return vulnerable; }
    public void setVulnerable(boolean vulnerable) { this.vulnerable = vulnerable; }
    
    public String getVulnerabilityDetails() { return vulnerabilityDetails; }
    public void setVulnerabilityDetails(String details) { this.vulnerabilityDetails = details; }
}
