package scanner.protocol;

import scanner.fingerprint.ServiceInfo;
import java.net.InetAddress;
import java.util.Map;

public interface ProtocolScanner {
    ServiceInfo scan(InetAddress address, int port, int timeout);
    String getProtocolName();
    boolean supportsPort(int port);
    Map<String, String> getDefaultProbes();
}
