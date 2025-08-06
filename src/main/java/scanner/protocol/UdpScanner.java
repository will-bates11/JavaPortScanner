package scanner.protocol;

import scanner.fingerprint.ServiceInfo;
import java.net.*;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;
import java.nio.ByteBuffer;

public class UdpScanner implements ProtocolScanner {
    private static final Logger logger = LoggerFactory.getLogger(UdpScanner.class);
    private static final int RECEIVE_BUFFER_SIZE = 1024;

    @Override
    public ServiceInfo scan(InetAddress address, int port, int timeout) {
        ServiceInfo info = new ServiceInfo(port);
        info.setProtocol("UDP");

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(timeout);
            
            // Send probe packet
            byte[] probeData = getProbeForPort(port);
            DatagramPacket probe = new DatagramPacket(probeData, probeData.length, address, port);
            
            long startTime = System.nanoTime();
            socket.send(probe);

            // Try to receive response
            byte[] receiveBuffer = new byte[RECEIVE_BUFFER_SIZE];
            DatagramPacket response = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            
            try {
                socket.receive(response);
                long endTime = System.nanoTime();
                info.setResponseTime((endTime - startTime) / 1_000_000);
                info.setState("OPEN");
                
                // Process response data
                String responseData = new String(response.getData(), 0, response.getLength());
                info.setBanner(responseData);
                
            } catch (SocketTimeoutException e) {
                // No response could mean filtered or closed
                info.setState("OPEN|FILTERED");
            }
        } catch (PortUnreachableException e) {
            info.setState("CLOSED");
        } catch (IOException e) {
            info.setState("ERROR");
            info.setError(e.getMessage());
        }

        return info;
    }

    private byte[] getProbeForPort(int port) {
        // Common UDP service probes
        switch (port) {
            case 53:  // DNS
                return createDnsQuery();
            case 161: // SNMP
                return createSnmpQuery();
            case 137: // NetBIOS
                return createNetBiosQuery();
            default:
                return new byte[]{0x0}; // Generic probe
        }
    }

    private byte[] createDnsQuery() {
        // Simple DNS query for "www.example.com"
        return new byte[] {
            0x00, 0x01, // Transaction ID
            0x01, 0x00, // Flags (standard query)
            0x00, 0x01, // Questions
            0x00, 0x00, // Answer RRs
            0x00, 0x00, // Authority RRs
            0x00, 0x00, // Additional RRs
            0x03, 'w', 'w', 'w',
            0x07, 'e', 'x', 'a', 'm', 'p', 'l', 'e',
            0x03, 'c', 'o', 'm',
            0x00,       // Root domain
            0x00, 0x01, // Type A
            0x00, 0x01  // Class IN
        };
    }

    private byte[] createSnmpQuery() {
        // Simple SNMP GET request
        return new byte[] {
            0x30, 0x26, // Sequence
            0x02, 0x01, 0x00, // Version
            0x04, 0x06, 'p', 'u', 'b', 'l', 'i', 'c', // Community
            0xa0, 0x19, // GetRequest
            0x02, 0x01, 0x00, // Request ID
            0x02, 0x01, 0x00, // Error Status
            0x02, 0x01, 0x00, // Error Index
            0x30, 0x0e, // Variable Bindings
            0x30, 0x0c, // Sequence
            0x06, 0x08, 0x2b, 0x06, 0x01, 0x02, 0x01, 0x01, 0x01, 0x00 // sysDescr.0
        };
    }

    private byte[] createNetBiosQuery() {
        // NetBIOS name query
        return new byte[] {
            0x00, 0x00, // Transaction ID
            0x01, 0x10, // Flags
            0x00, 0x01, // Questions
            0x00, 0x00, // Answer RRs
            0x00, 0x00, // Authority RRs
            0x00, 0x00, // Additional RRs
            0x20, 0x43, 0x4b, // Name
            0x41, 0x41, 0x41,
            0x41, 0x41, 0x41,
            0x41, 0x41, 0x41,
            0x41, 0x41, 0x41,
            0x41, 0x41, 0x41,
            0x41, 0x41, 0x41,
            0x41, 0x41, 0x41,
            0x41, 0x41, 0x41,
            0x41, 0x41, 0x41,
            0x41, 0x41, 0x41,
            0x00,       // End of name
            0x00, 0x20, // Type NB
            0x00, 0x01  // Class IN
        };
    }

    @Override
    public String getProtocolName() {
        return "UDP";
    }

    @Override
    public boolean supportsPort(int port) {
        return port > 0 && port < 65536;
    }

    @Override
    public Map<String, String> getDefaultProbes() {
        Map<String, String> probes = new HashMap<>();
        probes.put("DNS", "DNS query");
        probes.put("SNMP", "SNMP GET request");
        probes.put("NTP", "NTP request");
        probes.put("DHCP", "DHCP discover");
        return probes;
    }
}
