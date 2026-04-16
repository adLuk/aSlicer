package cz.ad.print3d.aslicer.logic.net.bambu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hivemq.client.mqtt.MqttClientSslConfig;
import com.hivemq.client.mqtt.MqttClientSslConfigBuilder;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import cz.ad.print3d.aslicer.logic.net.ssl.InteractiveTrustManagerFactory;
import cz.ad.print3d.aslicer.logic.net.ssl.UntrustedCertificateException;
import cz.ad.print3d.aslicer.logic.printer.system.net.BambuPrinterNetConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.security.Security;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Client for communicating with Bambu Lab 3D printers over MQTT.
 *
 * <p>This client handles the connection to the printer's MQTT broker, authentication
 * using the serial number and access code, and parsing of incoming telemetry data.</p>
 *
 * <p>It uses the HiveMQ MQTT client for asynchronous communication.</p>
 */
public class BambuMqttClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(BambuMqttClient.class);
    private final String host;
    private final int port;
    private String serial;
    private final String accessCode;
    private final BambuMqttMapper mapper;
    private Mqtt3AsyncClient client;
    private Consumer<Map<String, Object>> telemetryConsumer;
    private final CompletableFuture<String> serialDiscoveryFuture = new CompletableFuture<>();

    /**
     * Constructs a new BambuMqttClient with the provided connection details.
     *
     * @param connection the printer connection details containing URL, serial, and access code.
     */
    public BambuMqttClient(BambuPrinterNetConnection connection) {
        this.host = connection.getPrinterUrl().getHost();
        this.port = connection.getPrinterUrl().getPort() == -1 ? 8883 : connection.getPrinterUrl().getPort();
        this.serial = connection.getSerial();
        this.accessCode = connection.getAccessCode();
        this.mapper = new BambuMqttMapper();
    }

    /**
     * Gets a future that will be completed with the discovered serial number.
     *
     * <p>If the serial number was already provided during construction, this future
     * will be completed immediately.</p>
     * 
     * <p>The future has a default timeout of 10 seconds for discovery.</p>
     *
     * @return a {@link CompletableFuture} for the discovered serial number.
     */
    public CompletableFuture<String> getSerialDiscoveryFuture() {
        if (serial != null && !serial.isEmpty()) {
            serialDiscoveryFuture.complete(serial);
        }
        return serialDiscoveryFuture.orTimeout(10, TimeUnit.SECONDS);
    }

    /**
     * @return the discovered serial number, or null if not yet discovered.
     */
    public String getSerial() {
        return serial;
    }

    /**
     * Sets the consumer that will receive the parsed telemetry data.
     *
     * @param consumer the consumer function to handle telemetry maps.
     */
    public void setTelemetryConsumer(Consumer<Map<String, Object>> consumer) {
        this.telemetryConsumer = consumer;
    }

    /**
     * Connects to the printer's MQTT broker and completes the pairing/authentication process.
     *
     * <p>Once connected, it automatically subscribes to the telemetry topic.</p>
     *
     * <p>This method configures a custom SSL context that prioritizes the SunJSSE provider
     * to ensure compatibility with XDH algorithms (Ed25519/X25519) while maintaining
     * Bouncy Castle FIPS as the primary global provider.</p>
     *
     * @return a {@link CompletableFuture} that completes when the connection is established.
     */
    public CompletableFuture<Void> connect() {
        String clientId = (serial != null && !serial.isEmpty()) ? serial : "aSlicer_discovery_" + System.currentTimeMillis();
        
        MqttClientSslConfigBuilder sslBuilder = MqttClientSslConfig.builder()
                .hostnameVerifier((hostname, session) -> true);

        // To solve the "IllegalAccessError" and "Could not generate XDH keypair" issues when BCFIPS is the primary provider:
        // We temporarily reorder providers to put Sun providers before BCFIPS for the duration of the connect call.
        // This allows SunJSSE to use its internal classes (like TlsMasterSecretParameterSpec) and algorithms (XDH).
        String[] sunProviders = {"SunJSSE", "SunJCE", "SunEC"};
        java.util.List<Provider> originalProviders = new java.util.ArrayList<>();
        for (String name : sunProviders) {
            Provider p = Security.getProvider(name);
            if (p != null) {
                originalProviders.add(p);
                Security.removeProvider(name);
                Security.insertProviderAt(p, 1);
            }
        }
        LOGGER.debug("Temporarily prioritized Sun providers for Bambu MQTT connection");

        try {
            InteractiveTrustManagerFactory tmf = new InteractiveTrustManagerFactory();
            sslBuilder.trustManagerFactory(tmf);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize SSL configuration", e);
            restoreProviders(originalProviders);
            return CompletableFuture.failedFuture(e);
        }

        client = Mqtt3Client.builder()
                .identifier(clientId)
                .serverHost(host)
                .serverPort(port)
                .sslConfig(sslBuilder.build())
                .buildAsync();

        return client.connectWith()
                .simpleAuth()
                .username("bblp")
                .password(accessCode.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .send()
                .handle((ack, throwable) -> {
                    restoreProviders(originalProviders);
                    
                    if (throwable != null) {
                        // Extract original cause from HiveMQ exception
                        Throwable cause = throwable;
                        while (cause.getCause() != null && cause != cause.getCause()) {
                            if (cause instanceof UntrustedCertificateException) {
                                break;
                            }
                            cause = cause.getCause();
                        }
                        return CompletableFuture.<Void>failedFuture(cause);
                    }
                    return CompletableFuture.<Void>completedFuture(null);
                })
                .thenCompose(f -> f)
                .thenAccept(v -> {
                    // Connected successfully
                    if (serial == null || serial.isEmpty()) {
                        subscribeToDiscovery();
                    } else {
                        subscribeToTelemetry();
                    }
                });
    }

    /**
     * Restores the original provider ordering by moving specified providers to the end.
     * 
     * @param providers list of providers to move back.
     */
    private void restoreProviders(java.util.List<Provider> providers) {
        for (int i = providers.size() - 1; i >= 0; i--) {
            Provider p = providers.get(i);
            Security.removeProvider(p.getName());
            Security.addProvider(p);
        }
        LOGGER.debug("Restored original security provider ordering");
    }

    /**
     * Subscribes to the wildcard topic to discover the printer's serial number.
     *
     * <p>Once a message is received on a topic matching {@code device/+/report},
     * the serial number is extracted and discovery is completed.</p>
     */
    private void subscribeToDiscovery() {
        LOGGER.info("Subscribing to discovery wildcard topic (#) for host {}", host);
        client.subscribeWith()
                .topicFilter("#")
                .callback(mqtt3Publish -> {
                    String topic = mqtt3Publish.getTopic().toString();
                    LOGGER.trace("Discovery message received on topic: {}", topic);
                    if (topic.startsWith("device/") && topic.endsWith("/report")) {
                        String discoveredSerial = topic.split("/")[1];
                        if (discoveredSerial != null && !discoveredSerial.isEmpty()) {
                            LOGGER.info("Discovered serial number: {} for host {}", discoveredSerial, host);
                            this.serial = discoveredSerial;
                            serialDiscoveryFuture.complete(discoveredSerial);
                            // Resubscribe to specific telemetry and unsubscribe from discovery if needed
                            client.unsubscribeWith().topicFilter("#").send().thenRun(this::subscribeToTelemetry);
                        }
                    }
                    byte[] payload = mqtt3Publish.getPayloadAsBytes();
                    handleTelemetry(new String(payload, StandardCharsets.UTF_8));
                })
                .send()
                .whenComplete((subAck, ex) -> {
                    if (ex != null) {
                        LOGGER.error("Failed to subscribe to discovery topic (#) for host {}: {}", host, ex.getMessage());
                        serialDiscoveryFuture.completeExceptionally(ex);
                    } else {
                        LOGGER.info("Successfully subscribed to discovery topic (#) for host {}", host);
                    }
                });
    }

    /**
     * Subscribes to the printer's telemetry topic.
     *
     * <p>Incoming messages are handled by the callback which forwards the payload
     * to the {@link #handleTelemetry(String)} method.</p>
     */
    void subscribeToTelemetry() {
        String topic = BambuTopics.telemetry(serial);
        client.subscribeWith()
                .topicFilter(topic)
                .callback(mqtt3Publish -> {
                    byte[] payload = mqtt3Publish.getPayloadAsBytes();
                    handleTelemetry(new String(payload, StandardCharsets.UTF_8));
                })
                .send();
    }

    /**
     * Parses the incoming telemetry payload and dispatches it to the telemetry consumer.
     *
     * <p>The payload is expected to be a JSON string that is mapped by the {@link BambuMqttMapper}.</p>
     *
     * @param payload the JSON payload received from the MQTT broker.
     */
    void handleTelemetry(String payload) {
        if (telemetryConsumer != null) {
            try {
                Map<String, Object> telemetry = mapper.parse(payload);
                if (!telemetry.isEmpty()) {
                    telemetryConsumer.accept(telemetry);
                }
            } catch (JsonProcessingException e) {
                // Log error or handle appropriately
            }
        }
    }

    /**
     * Disconnects the MQTT client from the printer's broker.
     */
    public void disconnect() {
        if (client != null) {
            client.disconnect();
        }
    }

    /**
     * Gets the telemetry subscription topic for the current printer serial.
     *
     * @return the MQTT telemetry topic string.
     */
    public String getTelemetryTopic() {
        if (serial == null || serial.isEmpty()) return "#";
        return BambuTopics.telemetry(serial);
    }

    /**
     * Gets the request topic for sending commands to the printer.
     *
     * @return the MQTT request topic string.
     */
    public String getRequestTopic() {
        if (serial == null || serial.isEmpty()) return null;
        return BambuTopics.request(serial);
    }
}
