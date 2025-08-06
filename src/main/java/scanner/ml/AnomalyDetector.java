package scanner.ml;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.time.Instant;

public class AnomalyDetector {
    private static final Logger logger = LoggerFactory.getLogger(AnomalyDetector.class);
    
    private final Map<Integer, List<ScanMetric>> portHistory = new HashMap<>();
    private final double anomalyThreshold = 2.0; // Standard deviations
    private final int historyWindow = 1000; // Number of scans to keep in history

    public void recordScan(int port, long responseTime, boolean isOpen, String banner) {
        ScanMetric metric = new ScanMetric(responseTime, isOpen, banner);
        portHistory.computeIfAbsent(port, k -> new ArrayList<>()).add(metric);
        
        // Trim history if too long
        List<ScanMetric> history = portHistory.get(port);
        if (history.size() > historyWindow) {
            history.subList(0, history.size() - historyWindow).clear();
        }
    }

    public boolean isAnomaly(int port, long responseTime, boolean isOpen, String banner) {
        List<ScanMetric> history = portHistory.get(port);
        if (history == null || history.size() < 10) {
            return false; // Not enough history to detect anomalies
        }

        // Check response time anomaly
        double meanTime = history.stream()
            .mapToLong(m -> m.responseTime)
            .average()
            .orElse(0);
        
        double stdDev = calculateStdDev(history, meanTime);
        
        // Check if current response time is anomalous
        if (Math.abs(responseTime - meanTime) > anomalyThreshold * stdDev) {
            logger.warn("Anomalous response time detected for port {}: {} ms (mean: {} ms, stddev: {} ms)",
                port, responseTime, meanTime, stdDev);
            return true;
        }

        // Check state change anomaly
        boolean usuallyOpen = history.stream()
            .filter(m -> m.isOpen)
            .count() > history.size() / 2;
        
        if (usuallyOpen != isOpen) {
            logger.warn("Anomalous state detected for port {}: {} (usually {})",
                port, isOpen ? "open" : "closed", usuallyOpen ? "open" : "closed");
            return true;
        }

        // Check banner anomaly if present
        if (banner != null && !banner.isEmpty()) {
            String usualbanner = getMostCommonBanner(history);
            if (!banner.equals(usualbanner)) {
                logger.warn("Anomalous banner detected for port {}", port);
                return true;
            }
        }

        return false;
    }

    private double calculateStdDev(List<ScanMetric> metrics, double mean) {
        return Math.sqrt(metrics.stream()
            .mapToDouble(m -> Math.pow(m.responseTime - mean, 2))
            .average()
            .orElse(0));
    }

    private String getMostCommonBanner(List<ScanMetric> metrics) {
        return metrics.stream()
            .filter(m -> m.banner != null)
            .collect(HashMap<String, Long>::new,
                (map, metric) -> map.merge(metric.banner, 1L, Long::sum),
                HashMap::putAll)
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("");
    }

    private static class ScanMetric {
        final long timestamp;
        final long responseTime;
        final boolean isOpen;
        final String banner;

        ScanMetric(long responseTime, boolean isOpen, String banner) {
            this.timestamp = Instant.now().toEpochMilli();
            this.responseTime = responseTime;
            this.isOpen = isOpen;
            this.banner = banner;
        }
    }
}
