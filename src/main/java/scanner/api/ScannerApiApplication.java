package scanner.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScan(basePackages = "scanner")
@EnableAsync
public class ScannerApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScannerApiApplication.class, args);
    }
}
