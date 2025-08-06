package scanner;

import scanner.exception.*;
import scanner.config.ConfigurationManager;
import scanner.config.ScannerConfig;
import scanner.progress.ScanProgress;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortScanner {
    private static final Logger logger = LoggerFactory.getLogger(PortScanner.class);
    private final String host;
    private final int startPort;
    private final int endPort;
    private final int timeout;
    private final int threads;
    private final List<Integer> openPorts = Collections.synchronizedList(new ArrayList<>());
    private final int maxRetries;
    private final Duration retryDelay;
    private final List<Integer> excludedPorts;
    private final Map<String, String> environment;

    public PortScanner(String host, int startPort, int endPort, int timeout, int threads) {
        // Validate input parameters
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        }
        if (startPort < 1 || startPort > 65535) {
            throw new IllegalArgumentException("Start port must be between 1 and 65535");
        }
        if (endPort < 1 || endPort > 65535) {
            throw new IllegalArgumentException("End port must be between 1 and 65535");
        }
        if (startPort > endPort) {
            throw new IllegalArgumentException("Start port cannot be greater than end port");
        }
        if (timeout < 1) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        if (threads < 1) {
            throw new IllegalArgumentException("Thread count must be positive");
        }

        this.host = host;
        this.startPort = startPort;
        this.endPort = endPort;
        this.timeout = timeout;
        this.threads = threads;
    }

    public List<Integer> scan() {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();
        for (int port = startPort; port <= endPort; port++) {
            final int p = port;
            futures.add(executor.submit(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, p), timeout);
                    logger.info("Port {} is open", p);
                    openPorts.add(p);
                } catch (Exception ignored) {
                    // Port is closed or unreachable
                }
            }));
        }
        for (Future<?> f : futures) {
            try { f.get(); } catch (Exception e) { }
        }
        executor.shutdown();
        return openPorts;
    }
}
