package scanner.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import scanner.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigurationManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    private static final String DEFAULT_CONFIG_FILE = "scanner-config.yml";
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private ScannerConfig config;

    public ConfigurationManager() {
        this(DEFAULT_CONFIG_FILE);
    }

    public ConfigurationManager(String configFile) {
        try {
            loadConfig(configFile);
        } catch (IOException e) {
            logger.warn("Could not load configuration file: {}. Using defaults.", configFile);
            config = new ScannerConfig();
        }
    }

    private void loadConfig(String configFile) throws IOException {
        Path configPath = findConfigFile(configFile);
        if (configPath != null) {
            config = mapper.readValue(configPath.toFile(), ScannerConfig.class);
            logger.info("Loaded configuration from: {}", configPath);
        } else {
            createDefaultConfig(configFile);
        }
    }

    private Path findConfigFile(String configFile) {
        // Look in multiple locations
        Path[] searchPaths = {
            Paths.get(configFile),
            Paths.get(System.getProperty("user.home"), ".portscanner", configFile),
            Paths.get("/etc/portscanner", configFile)
        };

        for (Path path : searchPaths) {
            if (Files.exists(path)) {
                return path;
            }
        }
        return null;
    }

    private void createDefaultConfig(String configFile) throws IOException {
        config = new ScannerConfig();
        
        // Add default profiles
        ScannerConfig.Profile quickScan = new ScannerConfig.Profile();
        quickScan.setName("quick");
        quickScan.setDescription("Quick scan of common ports");
        quickScan.setStartPort(1);
        quickScan.setEndPort(1024);
        quickScan.setTimeout(500);
        quickScan.setThreads(50);
        
        ScannerConfig.Profile fullScan = new ScannerConfig.Profile();
        fullScan.setName("full");
        fullScan.setDescription("Full port range scan");
        fullScan.setStartPort(1);
        fullScan.setEndPort(65535);
        fullScan.setTimeout(1000);
        fullScan.setThreads(100);

        config.getProfiles().put("quick", quickScan);
        config.getProfiles().put("full", fullScan);
        config.setDefaultProfile("quick");

        // Save the default configuration
        Path configDir = Paths.get(System.getProperty("user.home"), ".portscanner");
        Files.createDirectories(configDir);
        Path configPath = configDir.resolve(configFile);
        mapper.writeValue(configPath.toFile(), config);
        logger.info("Created default configuration at: {}", configPath);
    }

    public ScannerConfig.Profile getProfile(String profileName) {
        if (profileName == null) {
            profileName = config.getDefaultProfile();
        }
        
        ScannerConfig.Profile profile = config.getProfiles().get(profileName);
        if (profile == null) {
            throw new ConfigurationException("Profile not found: " + profileName);
        }
        
        // Apply global defaults if not set in profile
        if (profile.getTimeout() == 0) {
            profile.setTimeout(config.getGlobal().getDefaultTimeout());
        }
        if (profile.getThreads() == 0) {
            profile.setThreads(config.getGlobal().getDefaultThreads());
        }
        
        return profile;
    }

    public ScannerConfig.GlobalConfig getGlobalConfig() {
        return config.getGlobal();
    }

    public void saveConfig(String configFile) throws IOException {
        Path configPath = Paths.get(configFile);
        mapper.writeValue(configPath.toFile(), config);
        logger.info("Saved configuration to: {}", configPath);
    }
}
