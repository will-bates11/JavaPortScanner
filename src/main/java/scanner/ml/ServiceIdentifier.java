package scanner.ml;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scanner.fingerprint.ServiceInfo;
import java.util.*;

public class ServiceIdentifier {
    private static final Logger logger = LoggerFactory.getLogger(ServiceIdentifier.class);
    private MultiLayerNetwork network;
    private final Map<String, Integer> serviceToIndex = new HashMap<>();
    private final List<String> indexToService = new ArrayList<>();
    private static final int FEATURE_SIZE = 256; // Size of the feature vector

    public ServiceIdentifier() {
        initializeNetwork();
        initializeServiceMap();
    }

    private void initializeNetwork() {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(123)
            .list()
            .layer(0, new DenseLayer.Builder()
                .nIn(FEATURE_SIZE)
                .nOut(128)
                .build())
            .layer(1, new DenseLayer.Builder()
                .nOut(64)
                .build())
            .layer(2, new OutputLayer.Builder()
                .nOut(indexToService.size())
                .build())
            .build();

        network = new MultiLayerNetwork(conf);
        network.init();
    }

    private void initializeServiceMap() {
        // Initialize common services
        String[] commonServices = {
            "HTTP", "HTTPS", "FTP", "SSH", "TELNET", "SMTP", "DNS", "MySQL",
            "PostgreSQL", "MongoDB", "Redis", "Memcached", "RDP", "VNC"
        };
        
        for (String service : commonServices) {
            addService(service);
        }
    }

    private void addService(String service) {
        if (!serviceToIndex.containsKey(service)) {
            serviceToIndex.put(service, indexToService.size());
            indexToService.add(service);
        }
    }

    public String identifyService(byte[] banner) {
        INDArray features = extractFeatures(banner);
        INDArray output = network.output(features);
        int predictedIndex = maxIndex(output);
        return indexToService.get(predictedIndex);
    }

    private INDArray extractFeatures(byte[] banner) {
        // Convert banner to feature vector
        double[] features = new double[FEATURE_SIZE];
        
        // Basic feature extraction: byte frequency analysis
        int[] frequency = new int[256];
        for (byte b : banner) {
            frequency[b & 0xFF]++; // Convert byte to unsigned
        }
        
        // Normalize frequencies
        if (banner.length > 0) {
            for (int i = 0; i < 256; i++) {
                features[i] = (double) frequency[i] / banner.length;
            }
        }
        
        return Nd4j.create(features);
    }

    private int maxIndex(INDArray array) {
        int maxIdx = 0;
        double maxVal = array.getDouble(0);
        for (int i = 1; i < array.length(); i++) {
            double val = array.getDouble(i);
            if (val > maxVal) {
                maxVal = val;
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    public void train(ServiceInfo serviceInfo) {
        if (serviceInfo.getBanner() == null || serviceInfo.getServiceName() == null) {
            return;
        }

        // Add new service if not seen before
        addService(serviceInfo.getServiceName());

        // Create training data
        INDArray features = extractFeatures(serviceInfo.getBanner().getBytes());
        INDArray labels = Nd4j.zeros(1, indexToService.size());
        labels.putScalar(serviceToIndex.get(serviceInfo.getServiceName()), 1.0);

        // Train the network
        network.fit(features, labels);
    }
}
