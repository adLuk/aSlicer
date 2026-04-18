package cz.ad.print3d.aslicer.logic.net.bambu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hivemq.client.mqtt.MqttClientSslConfig;
import com.hivemq.client.mqtt.MqttClientSslConfigBuilder;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAckReturnCode;
import cz.ad.print3d.aslicer.logic.net.ssl.InteractiveTrustManagerFactory;
import cz.ad.print3d.aslicer.logic.net.ssl.SslDetailsUtils;
import cz.ad.print3d.aslicer.logic.net.ssl.UntrustedCertificateException;
import cz.ad.print3d.aslicer.logic.printer.system.net.BambuPrinterNetConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
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
    private volatile String serial;
    private final String accessCode;
    private final BambuMqttMapper mapper;
    private volatile Mqtt3AsyncClient client;
    private volatile Consumer<Map<String, Object>> telemetryConsumer;
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
        if (serial != null && !serial.isEmpty() && !isPlaceholderSerial(serial)) {
            serialDiscoveryFuture.complete(serial);
        }
        return serialDiscoveryFuture.orTimeout(10, TimeUnit.SECONDS);
    }

    private boolean isPlaceholderSerial(String s) {
        return s == null || s.isEmpty() || s.equalsIgnoreCase("Bambu Printer") || s.equalsIgnoreCase("Bambu Lab Printer") || s.equalsIgnoreCase("Unknown");
    }

    /**
     * @return true if the MQTT client is connected.
     */
    public boolean isConnected() {
        return client != null && client.getState().isConnected();
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
     * @return a {@link CompletableFuture} that completes when the connection is established and subscribed.
     */
    public CompletableFuture<Void> connect() {
        String clientId = (serial != null && !serial.isEmpty()) ? serial : "aS" + (System.currentTimeMillis() % 1000000000L);
        
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

        InteractiveTrustManagerFactory tmf;
        try {
            tmf = new InteractiveTrustManagerFactory();
            sslBuilder.trustManagerFactory(tmf);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize SSL configuration", e);
            restoreProviders(originalProviders);
            return CompletableFuture.failedFuture(e);
        }

        InteractiveTrustManagerFactory finalTmf = tmf;

        client = Mqtt3Client.builder()
                .identifier(clientId)
                .serverHost(host)
                .serverPort(port)
                .sslConfig(sslBuilder.build())
                .automaticReconnectWithDefaultConfig()
                .addConnectedListener(context -> {
                    LOGGER.info("MQTT Client ({}) connected to {}", clientId, host);
                    // Resubscribe on reconnect if serial is known
                    if (serial != null && !serial.isEmpty()) {
                        subscribeToTelemetry();
                        // Also try to get version to refresh state
                        sendGetVersion();
                    }
                })
                .addDisconnectedListener(context -> {
                    Throwable cause = context.getCause();
                    LOGGER.info("MQTT Client ({}) disconnected from {}: {}", clientId, host, (cause != null ? cause.getMessage() : "No cause provided"));
                })
                .buildAsync();

        return client.connectWith()
                .simpleAuth()
                .username("bblp")
                .password(accessCode.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .cleanSession(true)
                .keepAlive(60)
                .send()
                .handle((ack, throwable) -> {
                    // Try to extract serial number from SSL certificate even if connection failed or succeeded
                    X509Certificate cert = finalTmf.getTrustManager().getLastHandshakeCertificate();
                    if (cert != null) {
                        String cn = SslDetailsUtils.getCommonName(cert);
                        if (cn != null && !cn.isEmpty() && (this.serial == null || isPlaceholderSerial(this.serial))) {
                            LOGGER.info("Extracted serial number from SSL certificate: {} for host {}", cn, host);
                            this.serial = cn;
                            serialDiscoveryFuture.complete(cn);
                        }
                    }

                    if (throwable != null) {
                        // Restore security providers immediately on failure
                        restoreProviders(originalProviders);
                        
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
                    
                    if (ack.getReturnCode() != Mqtt3ConnAckReturnCode.SUCCESS) {
                        restoreProviders(originalProviders);
                        return CompletableFuture.<Void>failedFuture(new RuntimeException("MQTT Connection rejected with code: " + ack.getReturnCode()));
                    }
                    
                    // After successful connection and serial discovery from certificate,
                    // we might need to update the client if it was created with a temporary ID.
                    // But for now, we just continue to subscription.
                    
                    return CompletableFuture.<Void>completedFuture(null);
                })
                .thenCompose(f -> f)
                .thenCompose(v -> {
                    // Connected successfully, now subscribe
                    CompletableFuture<Void> subscribeFuture;
                    if (serial == null || serial.isEmpty()) {
                        subscribeFuture = subscribeToDiscovery();
                    } else {
                        subscribeFuture = subscribeToTelemetry();
                    }
                    return subscribeFuture.handle((subAck, subEx) -> {
                        // Restore security providers after subscription (success or failure)
                        restoreProviders(originalProviders);
                        if (subEx != null) {
                            LOGGER.warn("Initial subscription failed for {}, but connection is active. Will try later if serial is discovered.", host, subEx);
                            // Do not fail the whole connect if subscription failed but we are connected.
                            // The printer might have rejected wildcard # but we might get serial later (e.g. mDNS)
                            // Or we already got it from SSL cert but subscription failed for some other reason.
                            if (this.serial != null && !this.serial.isEmpty()) {
                                return sendGetVersion().handle((v5, ex) -> {
                                    return subscribeToTelemetry();
                                }).thenCompose(f -> f);
                            }
                            serialDiscoveryFuture.thenAccept(s -> sendGetVersion());
                            return CompletableFuture.<Void>completedFuture(null);
                        }
                        
                        CompletableFuture<Void> initialRequestFuture;
                        if (this.serial != null && !this.serial.isEmpty()) {
                            // Delay slightly before sending first request to ensure subscription is active
                            initialRequestFuture = CompletableFuture.runAsync(() -> {
                                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                            }).thenCompose(v2 -> sendGetVersion());
                        } else {
                            initialRequestFuture = serialDiscoveryFuture.thenCompose(s -> 
                                CompletableFuture.runAsync(() -> {
                                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                                }).thenCompose(v3 -> sendGetVersion())
                            );
                        }
                        
                        return initialRequestFuture.handle((v4, ex) -> {
                            if (ex != null) {
                                LOGGER.warn("Initial version request failed for host {}: {}", host, ex.getMessage());
                            }
                            return (Void) null;
                        });
                    }).thenCompose(sf -> sf);
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
     * Subscribes to the wildcard topic to receive reports from any Bambu printer.
     *
     * <p>Once a message is received on a topic matching {@code device/+/report},
     * the serial number is extracted and discovery is completed.</p>
     *
     * @return a {@link CompletableFuture} that completes when the subscription is successful.
     */
    private CompletableFuture<Void> subscribeToDiscovery() {
        // Since wildcard device/+/report is not supported in LAN mode,
        // we subscribe to all topics (#) temporarily to find the serial if it's not known yet.
        // Some reports say even # might be rejected, but others say it works.
        // If it's rejected, we must rely on SSL cert CN or mDNS.
        String filter = "#";
        LOGGER.info("Subscribing to discovery wildcard topic ({}) for host {}", filter, host);
        return client.subscribeWith()
                .topicFilter(filter)
                .callback(mqtt3Publish -> {
                    String topic = mqtt3Publish.getTopic().toString();
                    LOGGER.trace("Discovery message received on topic: {}", topic);
                    String[] parts = topic.split("/");
                    if (parts.length >= 2) {
                        String discoveredSerial = parts[1];
                        if (discoveredSerial != null && !discoveredSerial.isEmpty()) {
                            if (this.serial == null || !this.serial.equals(discoveredSerial)) {
                                LOGGER.info("Discovered serial number: {} for host {}", discoveredSerial, host);
                                this.serial = discoveredSerial;
                                serialDiscoveryFuture.complete(discoveredSerial);
                                // If we were in discovery mode, now we can subscribe to the real telemetry topic
                                subscribeToTelemetry();
                            }
                        }
                    }
                    byte[] payload = mqtt3Publish.getPayloadAsBytes();
                    handleTelemetry(new String(payload, StandardCharsets.UTF_8));
                })
                .send()
                .handle((subAck, ex) -> {
                    if (ex != null) {
                        LOGGER.error("Failed to subscribe to discovery topic ({}) for host {}: {}", filter, host, ex.getMessage());
                        serialDiscoveryFuture.completeExceptionally(ex);
                        return CompletableFuture.<Void>failedFuture(ex);
                    } else {
                        LOGGER.info("Successfully subscribed to discovery topic ({}) for host {}", filter, host);
                        return CompletableFuture.<Void>completedFuture(null);
                    }
                }).thenCompose(f -> f);
    }

    /**
     * Subscribes to the printer's telemetry topic.
     *
     * <p>Incoming messages are handled by the callback which forwards the payload
     * to the {@link #handleTelemetry(String)} method.</p>
     *
     * @return a {@link CompletableFuture} that completes when the subscription is successful.
     */
    CompletableFuture<Void> subscribeToTelemetry() {
        String topicReport = BambuTopics.telemetry(serial);
        String topicNotify = BambuTopics.notify(serial);
        LOGGER.info("Subscribing to telemetry topics ({}, {}) for host {}", topicReport, topicNotify, host);
        
        CompletableFuture<?> sub1 = client.subscribeWith()
                .topicFilter(topicReport)
                .callback(mqtt3Publish -> {
                    byte[] payload = mqtt3Publish.getPayloadAsBytes();
                    handleTelemetry(new String(payload, StandardCharsets.UTF_8));
                })
                .send();
                
        // Optional notify topic - some printers might not support it
        client.subscribeWith()
                .topicFilter(topicNotify)
                .callback(mqtt3Publish -> {
                    byte[] payload = mqtt3Publish.getPayloadAsBytes();
                    handleTelemetry(new String(payload, StandardCharsets.UTF_8));
                })
                .send()
                .handle((ack, ex) -> {
                    if (ex != null) {
                        LOGGER.warn("Failed to subscribe to notify topic for host {}: {}", host, ex.getMessage());
                    }
                    return null;
                });

        return sub1.handle((v, ex) -> {
            if (ex != null) {
                LOGGER.error("Failed to subscribe to telemetry topic for host {}: {}", host, ex.getMessage());
                return CompletableFuture.<Void>failedFuture(ex);
            } else {
                LOGGER.info("Successfully subscribed to telemetry topic for host {}", host);
                return CompletableFuture.<Void>completedFuture(null);
            }
        }).thenCompose(f -> f);
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
            LOGGER.info("Disconnecting MQTT client for host {}", host);
            client.disconnect()
                    .whenComplete((v, ex) -> {
                        if (ex != null) {
                            LOGGER.warn("Error during MQTT disconnect for {}: {}", host, ex.getMessage());
                        } else {
                            LOGGER.debug("MQTT client for {} disconnected successfully", host);
                        }
                    });
        }
    }

    /**
     * Sends a {@code system: get_version} message to the printer to retrieve hardware and software
     * version information for all modules.
     *
     * <p>This should be called as soon as the printer's serial number is known, as it is
     * required to construct the request topic.</p>
     *
     * @return a {@link CompletableFuture} that completes when the message is sent.
     */
    public CompletableFuture<Void> sendGetVersion() {
        return sendGetVersion(3);
    }

    /**
     * Internal version of sendGetVersion with retry logic.
     * 
     * @param retries number of retries left
     * @return a {@link CompletableFuture} that completes when the message is sent.
     */
    private CompletableFuture<Void> sendGetVersion(int retries) {
        if (client == null) {
            LOGGER.error("Failed to send get_version to {}: MQTT client is not initialized", host);
            return CompletableFuture.failedFuture(new RuntimeException("MQTT client is not initialized"));
        }

        if (!client.getState().isConnected()) {
            if (retries > 0) {
                LOGGER.info("MQTT client for {} is not connected (state: {}). Attempting to reconnect... ({} retries left)", 
                    host, client.getState(), retries);
                
                CompletableFuture<Void> connFuture;
                if (client.getState() == com.hivemq.client.mqtt.MqttClientState.DISCONNECTED) {
                    connFuture = this.connect();
                } else {
                    // Already connecting or other state, wait a bit
                    connFuture = CompletableFuture.runAsync(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    });
                }

                return connFuture.thenCompose(v -> sendGetVersion(retries - 1));
            }
            
            String state = client.getState().toString();
            LOGGER.error("Failed to send get_version to {}: MQTT client is not connected. Current state: {}", host, state);
            return CompletableFuture.failedFuture(new RuntimeException("MQTT client is not connected (state: " + state + ")"));
        }
        
        String topic = getRequestTopic();
        if (topic == null || isPlaceholderSerial(serial)) {
            LOGGER.info("Request topic unknown or placeholder for host {}, waiting for serial discovery...", host);
            return getSerialDiscoveryFuture()
                    .handle((s, ex) -> {
                        if (ex != null) {
                            LOGGER.warn("Serial discovery timed out for {}, cannot send get_version", host);
                            return CompletableFuture.<Void>completedFuture(null);
                        }
                        return sendGetVersion(retries);
                    }).thenCompose(f -> f);
        }
        // Use robust payload with command: get_version
        String payload = "{\"system\": {\"sequence_id\": \"0\", \"command\": \"get_version\"}, \"user_id\": \"0\", \"sequence_id\": \"0\"}";
        LOGGER.info("Sending get_version request to topic {} for host {}", topic, host);
        return client.publishWith()
                .topic(topic)
                .qos(com.hivemq.client.mqtt.datatypes.MqttQos.AT_LEAST_ONCE)
                .payload(payload.getBytes(StandardCharsets.UTF_8))
                .send()
                .handle((pubAck, ex) -> {
                    if (ex != null) {
                        if (retries > 0) {
                             LOGGER.warn("Failed to publish get_version to {}, retrying...: {}", host, ex.getMessage());
                             return sendGetVersion(retries - 1);
                        }
                        LOGGER.error("Failed to send get_version to {}: {}", host, ex.getMessage());
                        return CompletableFuture.<Void>failedFuture(ex);
                    }
                    LOGGER.info("Successfully sent get_version to {} on topic {}", host, topic);
                    return CompletableFuture.<Void>completedFuture(null);
                }).thenCompose(f -> f);
    }

    /**
     * Gets the telemetry subscription topic for the current printer serial.
     *
     * @return the MQTT telemetry topic string.
     */
    public String getTelemetryTopic() {
        if (serial == null || serial.isEmpty()) return "device/+/report";
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
