package scanner.security;

public class ComplianceIssue {
    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    private final String name;
    private final String description;
    private final Severity severity;
    private final String service;

    public ComplianceIssue(String name, String description, Severity severity, String service) {
        this.name = name;
        this.description = description;
        this.severity = severity;
        this.service = service;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getService() {
        return service;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s (Service: %s)", 
            severity, name, description, service);
    }
}
