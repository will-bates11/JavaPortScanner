import java.net.*;

public class App {

    public static void main(String[] args) {
        String host = "localhost";
        int startPort = 1;
        int endPort = 65535;

        // Create socket and scan each port within the range
        for (int i = startPort; i <= endPort; i++) {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(host, i), 1000);
                System.out.println("Port " + i + " is open");
                socket.close();
            } catch (Exception ex) {
                // Ignore exception, port is closed
            }
        }
    }
}
