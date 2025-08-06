package scanner.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class ScanRequest {
    @JsonProperty
    private String host;
    
    @JsonProperty
    private Integer startPort;
    
    @JsonProperty
    private Integer endPort;
    
    @JsonProperty
    private String profile;
    
    @JsonProperty
    private Map<String, String> options;
    
    @JsonProperty
    private List<Integer> specificPorts;

    // Getters and setters
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public Integer getStartPort() { return startPort; }
    public void setStartPort(Integer startPort) { this.startPort = startPort; }
    public Integer getEndPort() { return endPort; }
    public void setEndPort(Integer endPort) { this.endPort = endPort; }
    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }
    public Map<String, String> getOptions() { return options; }
    public void setOptions(Map<String, String> options) { this.options = options; }
    public List<Integer> getSpecificPorts() { return specificPorts; }
    public void setSpecificPorts(List<Integer> specificPorts) { this.specificPorts = specificPorts; }
}
