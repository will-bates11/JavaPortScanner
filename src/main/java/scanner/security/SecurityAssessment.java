package scanner.security;

import scanner.fingerprint.ServiceInfo;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityAssessment {
    private static final Logger logger = LoggerFactory.getLogger(SecurityAssessment.class);
    private final CveDatabase cveDatabase;
    private final ComplianceChecker complianceChecker;

    public SecurityAssessment() {
        this.cveDatabase = new CveDatabase();
        this.complianceChecker = new ComplianceChecker();
    }

    public SecurityReport assess(List<ServiceInfo> services) {
        SecurityReport report = new SecurityReport();
        Map<String, List<Vulnerability>> vulnerabilities = new HashMap<>();
        Map<String, List<ComplianceIssue>> complianceIssues = new HashMap<>();

        for (ServiceInfo service : services) {
            try {
                // Vulnerability assessment
                List<Vulnerability> serviceVulns = assessService(service);
                if (!serviceVulns.isEmpty()) {
                    vulnerabilities.put(service.toString(), serviceVulns);
                }

                // Compliance checking
                List<ComplianceIssue> serviceCompliance = complianceChecker.checkCompliance(service);
                if (!serviceCompliance.isEmpty()) {
                    complianceIssues.put(service.toString(), serviceCompliance);
                }

            } catch (Exception e) {
                logger.error("Error assessing service {}: {}", service, e.getMessage());
            }
        }

        report.setVulnerabilities(vulnerabilities);
        report.setComplianceIssues(complianceIssues);
        report.setGeneratedDate(new Date());
        report.calculateRiskScore();

        return report;
    }

    private List<Vulnerability> assessService(ServiceInfo service) {
        List<Vulnerability> vulnerabilities = new ArrayList<>();
        
        if (service.getBanner() != null && !service.getBanner().isEmpty()) {
            // Extract version information from banner if available
            String version = extractVersion(service.getBanner());
            if (version != null) {
                vulnerabilities.addAll(cveDatabase.findVulnerabilities(service.getServiceName(), version));
            }
        }

        // Check for common vulnerabilities based on port and protocol
        vulnerabilities.addAll(checkCommonVulnerabilities(service));

        return vulnerabilities;
    }

    private String extractVersion(String banner) {
        // Common version patterns
        String[] patterns = {
            "\\b\\d+\\.\\d+\\.\\d+\\b",  // Matches x.y.z
            "\\b\\d+\\.\\d+\\b",         // Matches x.y
            "version\\s+(\\d[\\d\\.]+)", // Matches "version X.Y.Z"
            "\\b(\\d+\\.\\d+[\\d\\.]*[a-z]?)\\b" // Matches versions with optional suffix
        };

        for (String pattern : patterns) {
            var matcher = java.util.regex.Pattern.compile(pattern).matcher(banner.toLowerCase());
            if (matcher.find()) {
                return matcher.group();
            }
        }

        return null;
    }

    private List<Vulnerability> checkCommonVulnerabilities(ServiceInfo service) {
        List<Vulnerability> vulnerabilities = new ArrayList<>();
        
        // Check for well-known vulnerable port/protocol combinations
        if (service.getPort() == 21 && "FTP".equalsIgnoreCase(service.getServiceName())) {
            vulnerabilities.addAll(cveDatabase.findVulnerabilities("FTP", ""));
        }
        
        // Add checks for other common vulnerable services
        if (service.getPort() == 23) {
            Vulnerability v = new Vulnerability();
            v.setCveId("TELNET-UNSAFE");
            v.setDescription("Telnet sends data in cleartext and is considered unsafe");
            v.setSeverity("HIGH");
            v.setCvssScore(8.0);
            vulnerabilities.add(v);
        }

        return vulnerabilities;
    }
}
