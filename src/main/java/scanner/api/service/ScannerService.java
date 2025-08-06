package scanner.api.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import scanner.PortScanner;
import scanner.api.model.ScanRequest;
import scanner.config.ConfigurationManager;
import scanner.config.ScannerConfig;
import scanner.fingerprint.ServiceInfo;

import java.util.*;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ScannerService {
    private static final Logger logger = LoggerFactory.getLogger(ScannerService.class);
    
    private final Map<String, ScanContext> activeScanContexts = new ConcurrentHashMap<>();
    private final ConfigurationManager configManager = new ConfigurationManager();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, CompletableFuture<Void>> scanTasks = new ConcurrentHashMap<>();

    public void startScan(String scanId, ScanRequest request) {
        ScanContext context = new ScanContext(request);
        activeScanContexts.put(scanId, context);

        CompletableFuture<Void> scanTask = CompletableFuture.runAsync(() -> {
            try {
                performScan(scanId, context);
            } catch (Exception e) {
                logger.error("Scan {} failed: {}", scanId, e.getMessage());
                context.setError(e.getMessage());
            }
        }, executorService);

        scanTasks.put(scanId, scanTask);
    }

    private void performScan(String scanId, ScanContext context) {
        ScanRequest request = context.getRequest();
        PortScanner scanner = createScanner(request);
        
        context.setStatus("RUNNING");
        context.setStartTime(System.currentTimeMillis());

        List<Integer> portsToScan = determinePortsToScan(request);
        int totalPorts = portsToScan.size();
        AtomicInteger scannedPorts = new AtomicInteger(0);

        for (Integer port : portsToScan) {
            if (context.isStopped()) {
                context.setStatus("STOPPED");
                return;
            }

            try {
                ServiceInfo result = scanner.scanPort(request.getHost(), port);
                context.addResult(port, result);
                
                // Update progress
                int progress = (int) ((scannedPorts.incrementAndGet() / (double) totalPorts) * 100);
                context.setProgress(progress);
                
                // Notify listeners
                context.getEmitters().forEach(emitter -> {
                    try {
                        emitter.send(SseEmitter.event()
                            .name("progress")
                            .data(Map.of(
                                "progress", progress,
                                "port", port,
                                "result", result
                            )));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                });
            } catch (Exception e) {
                logger.error("Error scanning port {}: {}", port, e.getMessage());
            }
        }

        context.setStatus("COMPLETED");
        context.setEndTime(System.currentTimeMillis());
        
        // Complete all emitters
        context.getEmitters().forEach(SseEmitter::complete);
    }

    public Map<String, Object> getScanStatus(String scanId) {
        ScanContext context = activeScanContexts.get(scanId);
        if (context == null) {
            throw new IllegalArgumentException("Scan not found: " + scanId);
        }

        return Map.of(
            "status", context.getStatus(),
            "progress", context.getProgress(),
            "startTime", context.getStartTime(),
            "endTime", context.getEndTime(),
            "error", context.getError() != null ? context.getError() : "",
            "openPorts", context.getResults().size()
        );
    }

    public SseEmitter streamScanResults(String scanId) {
        ScanContext context = activeScanContexts.get(scanId);
        if (context == null) {
            throw new IllegalArgumentException("Scan not found: " + scanId);
        }

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        context.addEmitter(emitter);
        
        // Remove emitter when client disconnects
        emitter.onCompletion(() -> context.removeEmitter(emitter));
        emitter.onTimeout(() -> context.removeEmitter(emitter));
        
        return emitter;
    }

    public void stopScan(String scanId) {
        ScanContext context = activeScanContexts.get(scanId);
        if (context != null) {
            context.stop();
            CompletableFuture<Void> task = scanTasks.get(scanId);
            if (task != null) {
                task.cancel(true);
            }
        }
    }

    public List<ScannerConfig.Profile> getAvailableProfiles() {
        return new ArrayList<>(configManager.getProfiles().values());
    }

    public void saveProfile(ScannerConfig.Profile profile) {
        configManager.saveProfile(profile);
    }

    public Map<String, Object> generateReport(String scanId, String type) {
        ScanContext context = activeScanContexts.get(scanId);
        if (context == null) {
            throw new IllegalArgumentException("Scan not found: " + scanId);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("scanId", scanId);
        report.put("host", context.getRequest().getHost());
        report.put("startTime", context.getStartTime());
        report.put("endTime", context.getEndTime());
        report.put("duration", context.getEndTime() - context.getStartTime());
        report.put("totalPorts", context.getResults().size());

        if ("detailed".equals(type)) {
            report.put("results", context.getResults());
            report.put("statistics", generateStatistics(context));
        } else {
            report.put("openPorts", context.getResults().keySet());
        }

        return report;
    }

    public String exportReport(String scanId, String format) {
        Map<String, Object> report = generateReport(scanId, "detailed");
        String exportId = UUID.randomUUID().toString();
        
        // Async export process
        CompletableFuture.runAsync(() -> {
            try {
                switch (format.toLowerCase()) {
                    case "pdf":
                        exportToPdf(report, exportId);
                        break;
                    case "csv":
                        exportToCsv(report, exportId);
                        break;
                    case "json":
                        exportToJson(report, exportId);
                        break;
                }
            } catch (Exception e) {
                logger.error("Export failed: {}", e.getMessage());
            }
        });

        return exportId;
    }

    private Map<String, Object> generateStatistics(ScanContext context) {
        // Generate detailed statistics about the scan
        return Map.of(
            "openPortsCount", context.getResults().size(),
            "scanDuration", context.getEndTime() - context.getStartTime(),
            "averageResponseTime", context.getAverageResponseTime(),
            "mostCommonServices", context.getMostCommonServices()
        );
    }

    private void exportToPdf(Map<String, Object> report, String exportId) {
        // Implement PDF export
    }

    private void exportToCsv(Map<String, Object> report, String exportId) {
        // Implement CSV export
    }

    private void exportToJson(Map<String, Object> report, String exportId) {
        // Implement JSON export
    }

    private PortScanner createScanner(ScanRequest request) {
        ScannerConfig.Profile profile = request.getProfile() != null ?
            configManager.getProfile(request.getProfile()) :
            configManager.getDefaultProfile();
            
        return new PortScanner(
            request.getHost(),
            request.getStartPort() != null ? request.getStartPort() : profile.getStartPort(),
            request.getEndPort() != null ? request.getEndPort() : profile.getEndPort(),
            profile.getTimeout(),
            profile.getThreads()
        );
    }

    private List<Integer> determinePortsToScan(ScanRequest request) {
        if (request.getSpecificPorts() != null && !request.getSpecificPorts().isEmpty()) {
            return request.getSpecificPorts();
        }

        List<Integer> ports = new ArrayList<>();
        int start = request.getStartPort() != null ? request.getStartPort() : 1;
        int end = request.getEndPort() != null ? request.getEndPort() : 65535;
        
        for (int port = start; port <= end; port++) {
            ports.add(port);
        }
        
        return ports;
    }

    private static class ScanContext {
        private final ScanRequest request;
        private final Map<Integer, ServiceInfo> results = new ConcurrentHashMap<>();
        private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
        private String status = "PENDING";
        private int progress = 0;
        private long startTime;
        private long endTime;
        private String error;
        private volatile boolean stopped = false;

        public ScanContext(ScanRequest request) {
            this.request = request;
        }

        // Getters and setters
        public ScanRequest getRequest() { return request; }
        public Map<Integer, ServiceInfo> getResults() { return results; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public boolean isStopped() { return stopped; }
        public void stop() { this.stopped = true; }
        public Set<SseEmitter> getEmitters() { return emitters; }
        
        public void addEmitter(SseEmitter emitter) {
            emitters.add(emitter);
        }
        
        public void removeEmitter(SseEmitter emitter) {
            emitters.remove(emitter);
        }
        
        public void addResult(int port, ServiceInfo info) {
            results.put(port, info);
        }

        public double getAverageResponseTime() {
            return results.values().stream()
                .mapToLong(ServiceInfo::getResponseTime)
                .average()
                .orElse(0.0);
        }

        public Map<String, Integer> getMostCommonServices() {
            Map<String, Integer> serviceCounts = new HashMap<>();
            results.values().forEach(info -> 
                serviceCounts.merge(info.getServiceName(), 1, Integer::sum));
            return serviceCounts;
        }
    }
}
