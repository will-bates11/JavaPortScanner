# Java Port Scanner

This is a simple Java-based port scanner that scans a specified range of ports on a given host to determine which ports are open.

## Features

- Scans ports in the range from 1 to 65535.
- Checks if ports are open on a specified host.
- Configurable timeout for port checking.

## Requirements

- Java Development Kit (JDK) 8 or later.

## Usage

1. Clone the repository:

    ```bash
    git clone https://github.com/will-bates11/java-port-scanner.git
    cd java-port-scanner
    ```

2. Compile the Java code:

    ```bash
    javac App.java
    ```

3. Run the application:

    ```bash
    java App
    ```

   By default, the application will scan the `localhost` for open ports in the range of 1 to 65535.

## Customization

To customize the host and port range, modify the following variables in the `App.java` file:

```java
String host = "localhost"; // Replace with the target host
int startPort = 1; // Replace with the starting port number
int endPort = 65535; // Replace with the ending port number
