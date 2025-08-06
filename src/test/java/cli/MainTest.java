package cli;

import org.junit.Test;
import org.junit.Before;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.Assert.*;

public class MainTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @Test
    public void testHelpCommand() {
        Main.main(new String[]{"--help"});
        String output = outContent.toString();
        assertTrue("Help output should contain usage information", 
            output.contains("Usage:") && output.contains("Options:"));
    }

    @Test
    public void testValidHostArgument() {
        Main.main(new String[]{"-h", "localhost", "-s", "80", "-e", "81"});
        String output = outContent.toString();
        assertTrue("Output should mention localhost", 
            output.contains("localhost"));
    }

    @Test
    public void testInvalidPortRange() {
        Main.main(new String[]{"-s", "100", "-e", "50"});
        String output = outContent.toString();
        assertTrue("Should show port range error", 
            output.contains("Invalid port range"));
    }

    @Test
    public void testInvalidTimeout() {
        Main.main(new String[]{"-t", "0"});
        String output = outContent.toString();
        assertTrue("Should show timeout error", 
            output.contains("Timeout must be positive"));
    }

    @Test
    public void testInvalidThreadCount() {
        Main.main(new String[]{"-n", "-1"});
        String output = outContent.toString();
        assertTrue("Should show thread count error", 
            output.contains("Thread count must be positive"));
    }

    @Test
    public void testJsonOutput() {
        Main.main(new String[]{"-h", "localhost", "-s", "80", "-e", "81", "-f", "json"});
        String output = outContent.toString();
        assertTrue("Output should be in JSON format", 
            output.contains("{\"openPorts\":"));
    }

    @Test
    public void testCsvOutput() {
        Main.main(new String[]{"-h", "localhost", "-s", "80", "-e", "81", "-f", "csv"});
        String output = outContent.toString();
        assertTrue("Output should be in CSV format", 
            output.contains("port\n"));
    }

    @Test
    public void testUnknownArgument() {
        Main.main(new String[]{"--unknown"});
        String output = outContent.toString();
        assertTrue("Should show unknown argument error", 
            output.contains("Unknown argument"));
    }
}
