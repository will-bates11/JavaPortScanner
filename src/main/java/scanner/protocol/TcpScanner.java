package scanner.protocol;

import scanner.fingerprint.ServiceInfo;
import java.net.*;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;

public class TcpScanner implements ProtocolScanner {
    private static final Logger logger = LoggerFactory.getLogger(TcpScanner.class);

    @Override
    public ServiceInfo scan(InetAddress address, int port, int timeout) {
        ServiceInfo info = new ServiceInfo(port);
        info.setProtocol("TCP");

        try (Socket socket = new Socket()) {
            socket.setSoTimeout(timeout);
            long startTime = System.nanoTime();
            socket.connect(new InetSocketAddress(address, port), timeout);
            long endTime = System.nanoTime();
            
            info.setResponseTime((endTime - startTime) / 1_000_000); // Convert to milliseconds
            info.setState("OPEN");
            
            // Try to get banner
            if (socket.isConnected()) {
                try {
                    String banner = getBanner(socket);
                    if (banner != null) {
                        info.setBanner(banner);
                    }
                } catch (Exception e) {
                    logger.debug("Could not get banner for {}:{}", address.getHostAddress(), port);
                }
            }
        } catch (SocketTimeoutException e) {
            info.setState("FILTERED");
        } catch (ConnectException e) {
            info.setState("CLOSED");
        } catch (IOException e) {
            info.setState("ERROR");
            info.setError(e.getMessage());
        }

        return info;
    }

    private String getBanner(Socket socket) throws IOException {
        socket.setSoTimeout(2000); // Short timeout for banner grab
        byte[] buffer = new byte[2048];
        int read = socket.getInputStream().read(buffer);
        return read > 0 ? new String(buffer, 0, read).trim() : null;
    }

    @Override
    public String getProtocolName() {
        return "TCP";
    }

    @Override
    public boolean supportsPort(int port) {
        return port > 0 && port < 65536;
    }

    @Override
    public Map<String, String> getDefaultProbes() {
        Map<String, String> probes = new HashMap<>();
        probes.put("HTTP", "GET / HTTP/1.0\r\n\r\n");
        probes.put("FTP", "USER anonymous\r\n");
        probes.put("SMTP", "HELO scanner.local\r\n");
        probes.put("SSH", "SSH-2.0-Scanner\r\n");
        return probes;
    }
}
