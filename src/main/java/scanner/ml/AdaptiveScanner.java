package scanner.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AdaptiveScanner {
    private static final Logger logger = LoggerFactory.getLogger(AdaptiveScanner.class);
    
    private final Map<String, NetworkProfile> networkProfiles = new HashMap<>();
    private final Map<Integer, PortStats> portStats = new HashMap<>();
    private int defaultTimeout = 1000;
    private int defaultThreads = 100;

    public class ScanStrategy {
        public final int timeout;
        public final int threads;
        public final int retries;
        public final long delayBetweenScans;

        public ScanStrategy(int timeout, int threads, int retries, long delayBetweenScans) {
            this.timeout = timeout;
            this.threads = threads;
            this.retries = retries;
            this.delayBetweenScans = delayBetweenScans;
        }
    }

    public ScanStrategy getOptimalStrategy(String host, List<Integer> ports) {
        NetworkProfile profile = networkProfiles.computeIfAbsent(host, h -> new NetworkProfile());
        
        // Calculate optimal timeout
        int optimalTimeout = calculateOptimalTimeout(profile);
        
        // Calculate optimal thread count
        int optimalThreads = calculateOptimalThreads(profile);
        
        // Calculate optimal retries based on error rate
        int optimalRetries = calculateOptimalRetries(profile);
        
        // Calculate delay between scans based on network congestion
        long optimalDelay = calculateOptimalDelay(profile);
        
        logger.debug("Optimal strategy for {}: timeout={}, threads={}, retries={}, delay={}",
            host, optimalTimeout, optimalThreads, optimalRetries, optimalDelay);
        
        return new ScanStrategy(optimalTimeout, optimalThreads, optimalRetries, optimalDelay);
    }

    private int calculateOptimalTimeout(NetworkProfile profile) {
        if (profile.avgResponseTime == 0) {
            return defaultTimeout;
        }
        
        // Base timeout on average response time plus standard deviation
        return (int) Math.min(5000, Math.max(100, 
            profile.avgResponseTime + (2 * Math.sqrt(profile.responseTimeVariance))));
    }

    private int calculateOptimalThreads(NetworkProfile profile) {
        if (profile.successRate == 0) {
            return defaultThreads;
        }
        
        // Adjust thread count based on success rate and network latency
        int baseThreads = (int) (defaultThreads * profile.successRate);
        
        // Reduce threads if high latency
        if (profile.avgResponseTime > 1000) {
            baseThreads = Math.max(10, baseThreads / 2);
        }
        
        return Math.min(200, Math.max(10, baseThreads));
    }

    private int calculateOptimalRetries(NetworkProfile profile) {
        // More retries for unreliable networks
        if (profile.errorRate > 0.5) return 5;
        if (profile.errorRate > 0.2) return 3;
        return 2;
    }

    private long calculateOptimalDelay(NetworkProfile profile) {
        // Add delay if network shows signs of congestion
        if (profile.congestionLevel > 0.8) return 500;
        if (profile.congestionLevel > 0.5) return 200;
        if (profile.congestionLevel > 0.3) return 100;
        return 0;
    }

    public void updateStats(String host, int port, long responseTime, boolean success, boolean timeout) {
        // Update network profile
        NetworkProfile profile = networkProfiles.computeIfAbsent(host, h -> new NetworkProfile());
        profile.updateStats(responseTime, success, timeout);
        
        // Update port statistics
        PortStats stats = portStats.computeIfAbsent(port, p -> new PortStats());
        stats.updateStats(success);
    }

    private static class NetworkProfile {
        double avgResponseTime = 0;
        double responseTimeVariance = 0;
        double successRate = 1.0;
        double errorRate = 0.0;
        double congestionLevel = 0.0;
        int totalScans = 0;
        
        void updateStats(long responseTime, boolean success, boolean timeout) {
            totalScans++;
            
            // Update response time statistics using Welford's online algorithm
            double delta = responseTime - avgResponseTime;
            avgResponseTime += delta / totalScans;
            responseTimeVariance += delta * (responseTime - avgResponseTime);
            
            // Update success and error rates
            successRate = (successRate * (totalScans - 1) + (success ? 1 : 0)) / totalScans;
            errorRate = (errorRate * (totalScans - 1) + (timeout ? 1 : 0)) / totalScans;
            
            // Update congestion level based on response times and timeouts
            congestionLevel = (responseTime > avgResponseTime * 2 || timeout) ? 
                Math.min(1.0, congestionLevel + 0.1) : 
                Math.max(0.0, congestionLevel - 0.05);
        }
    }

    private static class PortStats {
        int totalAttempts = 0;
        int successfulAttempts = 0;
        
        void updateStats(boolean success) {
            totalAttempts++;
            if (success) successfulAttempts++;
        }
        
        double getSuccessRate() {
            return totalAttempts == 0 ? 0 : (double) successfulAttempts / totalAttempts;
        }
    }
}
