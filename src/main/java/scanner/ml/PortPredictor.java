package scanner.ml;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.nio.file.*;
import scanner.fingerprint.ServiceInfo;

public class PortPredictor {
    private static final Logger logger = LoggerFactory.getLogger(PortPredictor.class);
    private MultiLayerNetwork network;
    private final String modelPath = "models/port_predictor.zip";
    private final Map<Integer, Double> portProbabilities = new HashMap<>();
    private final int inputFeatures = 10; // Number of features we use for prediction

    public PortPredictor() {
        initializeNetwork();
        loadOrCreateModel();
    }

    private void initializeNetwork() {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(123)
            .weightInit(WeightInit.XAVIER)
            .updater(new Adam(0.01))
            .list()
            .layer(0, new DenseLayer.Builder()
                .nIn(inputFeatures)
                .nOut(32)
                .activation(Activation.RELU)
                .build())
            .layer(1, new DenseLayer.Builder()
                .nOut(64)
                .activation(Activation.RELU)
                .build())
            .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .nOut(2) // Binary classification: port likely open or closed
                .activation(Activation.SOFTMAX)
                .build())
            .build();

        network = new MultiLayerNetwork(conf);
        network.init();
    }

    private void loadOrCreateModel() {
        try {
            File modelFile = new File(modelPath);
            if (modelFile.exists()) {
                network = MultiLayerNetwork.load(modelFile, true);
                logger.info("Loaded existing model from: {}", modelPath);
            } else {
                logger.info("No existing model found. Starting with fresh model.");
                // Save the initial model
                Files.createDirectories(Paths.get(modelFile.getParent()));
                network.save(modelFile);
            }
        } catch (IOException e) {
            logger.warn("Could not load/save model: {}", e.getMessage());
        }
    }

    public List<Integer> predictLikelyPorts(String host, Map<String, String> context) {
        INDArray features = extractFeatures(host, context);
        INDArray predictions = network.output(features);
        
        // Convert predictions to port probabilities
        List<Integer> likelyPorts = new ArrayList<>();
        for (int port = 1; port <= 65535; port++) {
            if (shouldScanPort(port, predictions)) {
                likelyPorts.add(port);
            }
        }
        
        return likelyPorts;
    }

    private INDArray extractFeatures(String host, Map<String, String> context) {
        // Create feature vector based on host and context
        double[] features = new double[inputFeatures];
        
        // Feature 1: Is this a local network?
        features[0] = host.startsWith("192.168.") || host.startsWith("10.") ? 1.0 : 0.0;
        
        // Feature 2: Time of day (normalized to 0-1)
        Calendar cal = Calendar.getInstance();
        features[1] = cal.get(Calendar.HOUR_OF_DAY) / 24.0;
        
        // Feature 3-10: Additional context-based features
        // Add more sophisticated feature extraction here
        
        return Nd4j.create(features);
    }

    private boolean shouldScanPort(int port, INDArray predictions) {
        // Combine ML predictions with historical data and known patterns
        double probability = predictions.getDouble(0);
        double historicalSuccess = portProbabilities.getOrDefault(port, 0.5);
        
        // Weight the decision based on multiple factors
        return (probability * 0.7 + historicalSuccess * 0.3) > 0.5;
    }

    public void updateModel(String host, int port, ServiceInfo result) {
        // Update port probabilities based on scan results
        double currentProb = portProbabilities.getOrDefault(port, 0.5);
        double newProb = result.getServiceName().equals("Unknown") ? 
            Math.max(0.1, currentProb * 0.9) : // Decrease probability if port was closed
            Math.min(0.9, currentProb * 1.1);  // Increase probability if service was found
        
        portProbabilities.put(port, newProb);
        
        // TODO: Periodically retrain the model with new data
    }

    public void saveModel() {
        try {
            network.save(new File(modelPath));
            logger.info("Saved model to: {}", modelPath);
        } catch (IOException e) {
            logger.error("Failed to save model: {}", e.getMessage());
        }
    }
}
