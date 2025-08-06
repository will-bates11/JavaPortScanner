package scanner.security;

import scanner.fingerprint.ServiceInfo;
import java.util.*;

public class ComplianceChecker {
    private final Map<String, List<ComplianceRule>> complianceRules;

    public ComplianceChecker() {
        this.complianceRules = initializeRules();
    }

    public List<ComplianceIssue> checkCompliance(ServiceInfo service) {
        List<ComplianceIssue> issues = new ArrayList<>();

        // Check general rules that apply to all services
        checkGeneralCompliance(service, issues);

        // Check service-specific rules
        List<ComplianceRule> serviceRules = complianceRules.getOrDefault(
            service.getServiceName().toLowerCase(),
            Collections.emptyList()
        );

        for (ComplianceRule rule : serviceRules) {
            if (!rule.isCompliant(service)) {
                issues.add(new ComplianceIssue(
                    rule.getName(),
                    rule.getDescription(),
                    rule.getSeverity(),
                    service.getServiceName()
                ));
            }
        }

        return issues;
    }

    private void checkGeneralCompliance(ServiceInfo service, List<ComplianceIssue> issues) {
        // Check for insecure protocols
        if (service.getProtocol() != null && service.getProtocol().equalsIgnoreCase("HTTP")) {
            issues.add(new ComplianceIssue(
                "Insecure Protocol",
                "HTTP is being used instead of HTTPS",
                ComplianceIssue.Severity.HIGH,
                service.getServiceName()
            ));
        }

        // Check for well-known vulnerable ports
        checkVulnerablePorts(service, issues);

        // Check for unnecessary exposed services
        checkUnnecessaryServices(service, issues);
    }

    private void checkVulnerablePorts(ServiceInfo service, List<ComplianceIssue> issues) {
        Map<Integer, String> vulnerablePorts = Map.of(
            23, "Telnet - Insecure cleartext protocol",
            25, "SMTP without TLS",
            69, "TFTP - Insecure file transfer",
            135, "MSRPC - Potential security risk",
            137, "NetBIOS - Legacy protocol security risk",
            445, "SMB - Ensure latest version and proper configuration"
        );

        if (vulnerablePorts.containsKey(service.getPort())) {
            issues.add(new ComplianceIssue(
                "Vulnerable Port",
                vulnerablePorts.get(service.getPort()),
                ComplianceIssue.Severity.HIGH,
                service.getServiceName()
            ));
        }
    }

    private void checkUnnecessaryServices(ServiceInfo service, List<ComplianceIssue> issues) {
        Set<String> unnecessaryServices = Set.of(
            "telnet",
            "ftp",
            "rsh",
            "rlogin"
        );

        if (unnecessaryServices.contains(service.getServiceName().toLowerCase())) {
            issues.add(new ComplianceIssue(
                "Unnecessary Service",
                "Running potentially unnecessary and insecure service: " + service.getServiceName(),
                ComplianceIssue.Severity.MEDIUM,
                service.getServiceName()
            ));
        }
    }

    private Map<String, List<ComplianceRule>> initializeRules() {
        Map<String, List<ComplianceRule>> rules = new HashMap<>();

        // SSH Rules
        rules.put("ssh", Arrays.asList(
            new ComplianceRule("SSH Version", "SSH version should be >= 2.0", ComplianceIssue.Severity.CRITICAL) {
                @Override
                public boolean isCompliant(ServiceInfo service) {
                    return service.getBanner() != null && 
                           service.getBanner().contains("SSH-2.0");
                }
            }
        ));

        // HTTP/HTTPS Rules
        rules.put("http", Arrays.asList(
            new ComplianceRule("TLS Version", "Should use HTTPS instead of HTTP", ComplianceIssue.Severity.HIGH) {
                @Override
                public boolean isCompliant(ServiceInfo service) {
                    return false; // HTTP is always non-compliant
                }
            }
        ));

        rules.put("https", Arrays.asList(
            new ComplianceRule("TLS Version", "Should use TLS 1.2 or higher", ComplianceIssue.Severity.HIGH) {
                @Override
                public boolean isCompliant(ServiceInfo service) {
                    if (service.getBanner() == null) return false;
                    return service.getBanner().contains("TLSv1.2") || 
                           service.getBanner().contains("TLSv1.3");
                }
            }
        ));

        return rules;
    }
}

abstract class ComplianceRule {
    private final String name;
    private final String description;
    private final ComplianceIssue.Severity severity;

    public ComplianceRule(String name, String description, ComplianceIssue.Severity severity) {
        this.name = name;
        this.description = description;
        this.severity = severity;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ComplianceIssue.Severity getSeverity() {
        return severity;
    }

    public abstract boolean isCompliant(ServiceInfo service);
}
