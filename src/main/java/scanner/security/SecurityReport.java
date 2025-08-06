package scanner.security;

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonFormat;

public class SecurityReport {
    private Map<String, List<Vulnerability>> vulnerabilities;
    private Map<String, List<ComplianceIssue>> complianceIssues;
    private double riskScore;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date generatedDate;

    public Map<String, List<Vulnerability>> getVulnerabilities() {
        return vulnerabilities;
    }

    public void setVulnerabilities(Map<String, List<Vulnerability>> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }

    public Map<String, List<ComplianceIssue>> getComplianceIssues() {
        return complianceIssues;
    }

    public void setComplianceIssues(Map<String, List<ComplianceIssue>> complianceIssues) {
        this.complianceIssues = complianceIssues;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public Date getGeneratedDate() {
        return generatedDate;
    }

    public void setGeneratedDate(Date generatedDate) {
        this.generatedDate = generatedDate;
    }

    public void calculateRiskScore() {
        double vulnScore = calculateVulnerabilityScore();
        double complianceScore = calculateComplianceScore();
        
        // Weight the scores (70% vulnerabilities, 30% compliance)
        this.riskScore = (vulnScore * 0.7) + (complianceScore * 0.3);
    }

    private double calculateVulnerabilityScore() {
        if (vulnerabilities == null || vulnerabilities.isEmpty()) {
            return 0.0;
        }

        double totalScore = 0.0;
        int count = 0;

        for (List<Vulnerability> vulns : vulnerabilities.values()) {
            for (Vulnerability vuln : vulns) {
                totalScore += vuln.getCvssScore();
                count++;
            }
        }

        return count > 0 ? (totalScore / count) : 0.0;
    }

    private double calculateComplianceScore() {
        if (complianceIssues == null || complianceIssues.isEmpty()) {
            return 0.0;
        }

        int totalIssues = 0;
        int criticalIssues = 0;

        for (List<ComplianceIssue> issues : complianceIssues.values()) {
            for (ComplianceIssue issue : issues) {
                totalIssues++;
                if (issue.getSeverity() == ComplianceIssue.Severity.CRITICAL) {
                    criticalIssues++;
                }
            }
        }

        // Score based on ratio of critical issues to total issues
        return totalIssues > 0 ? (criticalIssues * 10.0) / totalIssues : 0.0;
    }

    public String generateSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Security Assessment Report\n");
        summary.append("========================\n");
        summary.append(String.format("Generated: %s\n", generatedDate));
        summary.append(String.format("Overall Risk Score: %.2f/10\n\n", riskScore));

        // Vulnerability Summary
        summary.append("Vulnerability Summary:\n");
        summary.append("--------------------\n");
        vulnerabilities.forEach((service, vulns) -> {
            summary.append(String.format("Service: %s\n", service));
            vulns.forEach(v -> summary.append(String.format("- %s\n", v)));
            summary.append("\n");
        });

        // Compliance Summary
        summary.append("Compliance Summary:\n");
        summary.append("-----------------\n");
        complianceIssues.forEach((service, issues) -> {
            summary.append(String.format("Service: %s\n", service));
            issues.forEach(i -> summary.append(String.format("- %s\n", i)));
            summary.append("\n");
        });

        return summary.toString();
    }
}
