package scanner.fingerprint;

import java.util.Map;
import java.util.HashMap;
import java.nio.charset.StandardCharsets;
import java.net.Socket;
import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceDetector {
    private static final Logger logger = LoggerFactory.getLogger(ServiceDetector.class);
    private static final int BANNER_TIMEOUT = 3000;
    private static final Map<Integer, String> COMMON_PROBES = new HashMap<>();
    
    static {
        // HTTP
        COMMON_PROBES.put(80, "GET / HTTP/1.0\r\n\r\n");
        COMMON_PROBES.put(443, "GET / HTTP/1.0\r\n\r\n");
        // SSH
        COMMON_PROBES.put(22, "SSH-2.0-JavaPortScanner\r\n");
        // SMTP
        COMMON_PROBES.put(25, "EHLO JavaPortScanner\r\n");
        // FTP
        COMMON_PROBES.put(21, "USER anonymous\r\n");
        // MySQL
        COMMON_PROBES.put(3306, "\n");
    }

    public ServiceInfo detectService(String host, int port) {
        ServiceInfo info = new ServiceInfo(port);
        
        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(BANNER_TIMEOUT);
            
            // Try to get banner without sending probe
            String banner = getBanner(socket);
            if (banner != null) {
                info.setBanner(banner);
                info.setServiceName(identifyService(banner));
                info.setVersion(extractVersion(banner));
                return info;
            }

            // If no banner, try with probe
            String probe = COMMON_PROBES.getOrDefault(port, "\r\n");
            socket.getOutputStream().write(probe.getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();

            banner = getBanner(socket);
            if (banner != null) {
                info.setBanner(banner);
                info.setServiceName(identifyService(banner));
                info.setVersion(extractVersion(banner));
            }
        } catch (IOException e) {
            logger.debug("Could not detect service on port {}: {}", port, e.getMessage());
        }

        return info;
    }

    private String getBanner(Socket socket) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder banner = new StringBuilder();
            char[] buffer = new char[1024];
            int read;

            while (reader.ready() && (read = reader.read(buffer)) != -1) {
                banner.append(buffer, 0, read);
                if (banner.length() > 4096) break; // Limit banner size
            }

            return banner.length() > 0 ? banner.toString() : null;
        } catch (IOException e) {
            return null;
        }
    }

    private String identifyService(String banner) {
        // Add pattern matching for common services
        if (banner.contains("SSH")) return "SSH";
        if (banner.contains("HTTP")) return "HTTP";
        if (banner.contains("220") && banner.contains("SMTP")) return "SMTP";
        if (banner.contains("220") && banner.contains("FTP")) return "FTP";
        if (banner.contains("MySQL")) return "MySQL";
        return "Unknown";
    }

    private String extractVersion(String banner) {
        // Add version extraction patterns
        // This is a simple example - would need more sophisticated regex patterns
        String[] parts = banner.split(" ");
        for (String part : parts) {
            if (part.matches("\\d+\\.\\d+(\\.\\d+)?")) {
                return part;
            }
        }
        return "Unknown";
    }
}
