package scanner.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CveDatabase {
    private static final Logger logger = LoggerFactory.getLogger(CveDatabase.class);
    private static final String NVD_API_BASE_URL = "https://services.nvd.nist.gov/rest/json/cves/2.0";
    private static final String API_KEY_ENV_VAR = "NVD_API_KEY";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, List<Vulnerability>> serviceVulnerabilities;
    private final String apiKey;

    public CveDatabase() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.serviceVulnerabilities = new ConcurrentHashMap<>();
        this.apiKey = System.getenv(API_KEY_ENV_VAR);
    }

    public List<Vulnerability> findVulnerabilities(String service, String version) {
        String cacheKey = service.toLowerCase() + "@" + version;
        
        return serviceVulnerabilities.computeIfAbsent(cacheKey, k -> {
            try {
                return fetchVulnerabilities(service, version);
            } catch (IOException | InterruptedException e) {
                logger.error("Failed to fetch vulnerabilities for {}: {}", service, e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    private List<Vulnerability> fetchVulnerabilities(String service, String version) throws IOException, InterruptedException {
        String query = String.format("?keywordSearch=%s AND %s", service, version);
        URI uri = URI.create(NVD_API_BASE_URL + query);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json");

        if (apiKey != null && !apiKey.isEmpty()) {
            requestBuilder.header("apiKey", apiKey);
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return parseVulnerabilities(response.body());
        } else {
            logger.error("Failed to fetch vulnerabilities. Status code: {}", response.statusCode());
            return new ArrayList<>();
        }
    }

    private List<Vulnerability> parseVulnerabilities(String json) throws IOException {
        List<Vulnerability> vulnerabilities = new ArrayList<>();
        var root = objectMapper.readTree(json);
        var vulnArray = root.path("vulnerabilities");

        for (var vuln : vulnArray) {
            var cve = vuln.path("cve");
            var vulnerability = new Vulnerability();
            
            vulnerability.setCveId(cve.path("id").asText());
            vulnerability.setDescription(cve.path("descriptions").path(0).path("value").asText());
            vulnerability.setSeverity(parseSeverity(cve));
            vulnerability.setPublishedDate(cve.path("published").asText());
            vulnerability.setLastModifiedDate(cve.path("lastModified").asText());
            
            // Parse CVSS scores
            var metrics = cve.path("metrics");
            if (metrics.has("cvssMetricV31")) {
                var cvss = metrics.path("cvssMetricV31").path(0);
                vulnerability.setCvssScore(cvss.path("cvssData").path("baseScore").asDouble());
                vulnerability.setCvssVector(cvss.path("cvssData").path("vectorString").asText());
            }

            vulnerabilities.add(vulnerability);
        }

        return vulnerabilities;
    }

    private String parseSeverity(var cve) {
        var metrics = cve.path("metrics");
        if (metrics.has("cvssMetricV31")) {
            return metrics.path("cvssMetricV31")
                    .path(0)
                    .path("cvssData")
                    .path("baseSeverity")
                    .asText();
        }
        return "UNKNOWN";
    }

    public void clearCache() {
        serviceVulnerabilities.clear();
    }
}
