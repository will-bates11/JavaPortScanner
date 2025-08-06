package scanner.progress;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScanProgress {
    private static final Logger logger = LoggerFactory.getLogger(ScanProgress.class);
    
    private final int totalPorts;
    private final AtomicInteger scannedPorts;
    private final AtomicInteger openPorts;
    private final Instant startTime;
    private final AtomicLong lastUpdateTime;
    private final AtomicInteger lastScannedCount;
    private final int updateIntervalMs;

    public ScanProgress(int totalPorts) {
        this.totalPorts = totalPorts;
        this.scannedPorts = new AtomicInteger(0);
        this.openPorts = new AtomicInteger(0);
        this.startTime = Instant.now();
        this.lastUpdateTime = new AtomicLong(System.currentTimeMillis());
        this.lastScannedCount = new AtomicInteger(0);
        this.updateIntervalMs = 1000; // Update every second
    }

    public void incrementScanned() {
        scannedPorts.incrementAndGet();
        updateProgress();
    }

    public void incrementOpen() {
        openPorts.incrementAndGet();
    }

    private void updateProgress() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastUpdate = currentTime - lastUpdateTime.get();

        if (timeSinceLastUpdate >= updateIntervalMs) {
            synchronized (this) {
                // Check again in case another thread updated
                if (currentTime - lastUpdateTime.get() >= updateIntervalMs) {
                    printProgress();
                    lastUpdateTime.set(currentTime);
                    lastScannedCount.set(scannedPorts.get());
                }
            }
        }
    }

    private void printProgress() {
        int scanned = scannedPorts.get();
        double percentage = (double) scanned / totalPorts * 100;
        int portsPerSecond = calculateScanRate(scanned);
        Duration estimatedRemaining = calculateTimeRemaining(scanned, portsPerSecond);
        Duration elapsed = Duration.between(startTime, Instant.now());

        StringBuilder progress = new StringBuilder("\r");
        progress.append(String.format("Progress: %.1f%% ", percentage))
               .append(String.format("(%d/%d) | ", scanned, totalPorts))
               .append(String.format("Open ports: %d | ", openPorts.get()))
               .append(String.format("Rate: %d ports/sec | ", portsPerSecond))
               .append(String.format("Elapsed: %02d:%02d | ", elapsed.toMinutes(), elapsed.toSecondsPart()))
               .append(String.format("ETA: %02d:%02d", 
                     estimatedRemaining.toMinutes(), estimatedRemaining.toSecondsPart()));

        // Use carriage return to update the same line
        System.out.print(progress);
        
        // Log to file without carriage return
        if (percentage % 10 == 0 || percentage == 100) { // Log every 10%
            logger.info(progress.toString().replace("\r", ""));
        }

        if (scanned == totalPorts) {
            System.out.println(); // Move to next line when complete
        }
    }

    private int calculateScanRate(int currentScanned) {
        long timeDiff = System.currentTimeMillis() - lastUpdateTime.get();
        int portDiff = currentScanned - lastScannedCount.get();
        if (timeDiff == 0) return 0;
        return (int) (portDiff * 1000L / timeDiff);
    }

    private Duration calculateTimeRemaining(int scanned, int scanRate) {
        if (scanRate <= 0) return Duration.ofHours(999); // Default for unknown
        int remainingPorts = totalPorts - scanned;
        long remainingSeconds = remainingPorts / Math.max(1, scanRate);
        return Duration.ofSeconds(remainingSeconds);
    }

    public void printFinalSummary() {
        Duration totalTime = Duration.between(startTime, Instant.now());
        double averageRate = (double) totalPorts / Math.max(1, totalTime.getSeconds());
        
        System.out.println("\n=== Scan Summary ===");
        System.out.printf("Total ports scanned: %d%n", totalPorts);
        System.out.printf("Open ports found: %d%n", openPorts.get());
        System.out.printf("Total time: %02d:%02d%n", 
                totalTime.toMinutes(), totalTime.toSecondsPart());
        System.out.printf("Average scan rate: %.1f ports/sec%n", averageRate);
        
        logger.info("Scan completed - Total: {}, Open: {}, Time: {}:{}, Rate: {}/sec",
                totalPorts, openPorts.get(), totalTime.toMinutes(), 
                totalTime.toSecondsPart(), String.format("%.1f", averageRate));
    }
}
