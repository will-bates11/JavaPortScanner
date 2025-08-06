package scanner.exception;

public class ScannerException extends RuntimeException {
    public ScannerException(String message) {
        super(message);
    }

    public ScannerException(String message, Throwable cause) {
        super(message, cause);
    }
}

class PortValidationException extends ScannerException {
    public PortValidationException(String message) {
        super(message);
    }
}

class ConnectionException extends ScannerException {
    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}

class ConfigurationException extends ScannerException {
    public ConfigurationException(String message) {
        super(message);
    }
}
