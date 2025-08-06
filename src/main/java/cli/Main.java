package cli;

import scanner.PortScanner;
import org.apache.commons.cli.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("h", "host", true, "Target host (default: localhost)");
        options.addOption("s", "start", true, "Start port (default: 1)");
        options.addOption("e", "end", true, "End port (default: 65535)");
        options.addOption("t", "timeout", true, "Timeout in ms (default: 1000)");
        options.addOption("n", "threads", true, "Number of threads (default: 100)");
        options.addOption("f", "format", true, "Output format: console, json, csv (default: console)");
        options.addOption(null, "help", false, "Show help");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        String host = "localhost";
        int startPort = 1, endPort = 65535, timeout = 1000, threads = 100;
        String format = "console";

        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("help")) {
                formatter.printHelp("JavaPortScanner", options);
                return;
            }
            if (cmd.hasOption("host")) host = cmd.getOptionValue("host");
            if (cmd.hasOption("start")) startPort = Integer.parseInt(cmd.getOptionValue("start"));
            if (cmd.hasOption("end")) endPort = Integer.parseInt(cmd.getOptionValue("end"));
            if (cmd.hasOption("timeout")) timeout = Integer.parseInt(cmd.getOptionValue("timeout"));
            if (cmd.hasOption("threads")) threads = Integer.parseInt(cmd.getOptionValue("threads"));
            if (cmd.hasOption("format")) format = cmd.getOptionValue("format");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            formatter.printHelp("JavaPortScanner", options);
            return;
        }

        if (startPort < 1 || endPort > 65535 || startPort > endPort) {
            System.out.println("Invalid port range. Ports must be between 1 and 65535.");
            return;
        }
        if (timeout < 1) {
            System.out.println("Timeout must be positive.");
            return;
        }
        if (threads < 1) {
            System.out.println("Thread count must be positive.");
            return;
        }

        PortScanner scanner = new PortScanner(host, startPort, endPort, timeout, threads);
        List<Integer> openPorts = scanner.scan();

        switch (format.toLowerCase()) {
            case "json":
                System.out.println(toJson(openPorts));
                break;
            case "csv":
                System.out.println(toCsv(openPorts));
                break;
            default:
                printConsole(openPorts);
        }
    }

    private static void printConsole(List<Integer> openPorts) {
        System.out.println("\nScan complete.");
        if (openPorts.isEmpty()) {
            System.out.println("No open ports found.");
        } else {
            System.out.println("Open ports:");
            for (int port : openPorts) {
                System.out.println("  " + port);
            }
        }
    }

    private static String toJson(List<Integer> openPorts) {
        return "{\"openPorts\": " + openPorts.toString() + "}";
    }

    private static String toCsv(List<Integer> openPorts) {
        StringBuilder sb = new StringBuilder("port\n");
        for (int port : openPorts) sb.append(port).append("\n");
        return sb.toString();
    }
}
