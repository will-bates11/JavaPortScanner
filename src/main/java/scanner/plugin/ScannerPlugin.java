package scanner.plugin;

import org.pf4j.ExtensionPoint;
import scanner.fingerprint.ServiceInfo;
import java.util.Map;

public interface ScannerPlugin extends ExtensionPoint {
    String getName();
    String getVersion();
    String getDescription();
    void initialize(Map<String, String> config);
    void beforeScan(String host, int port);
    void afterScan(String host, int port, ServiceInfo serviceInfo);
    void onScanComplete(String host, Map<Integer, ServiceInfo> results);
}
