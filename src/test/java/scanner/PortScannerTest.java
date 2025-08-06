package scanner;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.List;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PortScannerTest {
    
    private PortScanner scanner;
    
    @Mock
    private Socket mockSocket;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testValidPortRange() {
        scanner = new PortScanner("localhost", 1, 100, 1000, 10);
        List<Integer> results = scanner.scan();
        assertNotNull("Scan results should not be null", results);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidStartPort() {
        new PortScanner("localhost", 0, 100, 1000, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidEndPort() {
        new PortScanner("localhost", 1, 65536, 1000, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPortRange() {
        new PortScanner("localhost", 100, 50, 1000, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidTimeout() {
        new PortScanner("localhost", 1, 100, 0, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidThreadCount() {
        new PortScanner("localhost", 1, 100, 1000, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullHostname() {
        new PortScanner(null, 1, 100, 1000, 10);
    }

    @Test
    public void testThreadPoolSize() {
        scanner = new PortScanner("localhost", 1, 1000, 1000, 5);
        long startTime = System.currentTimeMillis();
        scanner.scan();
        long duration = System.currentTimeMillis() - startTime;
        
        // With 5 threads scanning 1000 ports, should take longer than scanning with 100 threads
        scanner = new PortScanner("localhost", 1, 1000, 1000, 100);
        long startTime2 = System.currentTimeMillis();
        scanner.scan();
        long duration2 = System.currentTimeMillis() - startTime2;
        
        assertTrue("More threads should complete faster", duration > duration2);
    }
}
