package scanner.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import scanner.api.model.ScanRequest;
import scanner.api.service.ScannerService;
import scanner.config.ScannerConfig;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ScannerController {

    @Autowired
    private ScannerService scannerService;

    @PostMapping("/scan")
    public Map<String, String> startScan(@RequestBody ScanRequest request) {
        String scanId = UUID.randomUUID().toString();
        scannerService.startScan(scanId, request);
        return Map.of("scanId", scanId);
    }

    @GetMapping("/scan/{scanId}")
    public Map<String, Object> getScanStatus(@PathVariable String scanId) {
        return scannerService.getScanStatus(scanId);
    }

    @GetMapping(value = "/scan/{scanId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamScanResults(@PathVariable String scanId) {
        return scannerService.streamScanResults(scanId);
    }

    @DeleteMapping("/scan/{scanId}")
    public void stopScan(@PathVariable String scanId) {
        scannerService.stopScan(scanId);
    }

    @GetMapping("/profiles")
    public List<ScannerConfig.Profile> getProfiles() {
        return scannerService.getAvailableProfiles();
    }

    @PostMapping("/profiles")
    public void saveProfile(@RequestBody ScannerConfig.Profile profile) {
        scannerService.saveProfile(profile);
    }

    @GetMapping("/reports/{scanId}")
    public Map<String, Object> getReport(@PathVariable String scanId, 
                                       @RequestParam(defaultValue = "summary") String type) {
        return scannerService.generateReport(scanId, type);
    }

    @PostMapping("/reports/{scanId}/export")
    public Map<String, String> exportReport(@PathVariable String scanId,
                                          @RequestParam String format) {
        String exportId = scannerService.exportReport(scanId, format);
        return Map.of("exportId", exportId);
    }

    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        return scannerService.getMetrics();
    }
}
