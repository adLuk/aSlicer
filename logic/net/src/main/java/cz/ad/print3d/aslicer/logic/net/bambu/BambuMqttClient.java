package cz.ad.print3d.aslicer.logic.net.bambu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hivemq.client.mqtt.MqttClientSslConfig;
import com.hivemq.client.mqtt.MqttClientSslConfigBuilder;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    private static final int DEFAULT_PORT = 8883;
    private static final int DISCOVERY_TIMEOUT_SEC = 10;
    private static final int KEEP_ALIVE_SEC = 60;
    private static final long INITIAL_REQUEST_DELAY_MS = 1000;

    private final String host;
    private final int port;
    private final String accessCode;
    private final BambuMqttMapper mapper;
    private final CompletableFuture<String> serialDiscoveryFuture = new CompletableFuture<>();

    private final AtomicInteger connectionCounter = new AtomicInteger(0);
    private final AtomicBoolean isManualDisconnect = new AtomicBoolean(false);
    private volatile String serial;
    private volatile Mqtt3AsyncClient client;
    private volatile CompletableFuture<Void> connectFuture;
    private volatile Consumer<Map<String, Object>> telemetryConsumer;

    /**
     * Constructs a new BambuMqttClient with the provided connection details.
     *
     * @param connection the printer connection details containing URL, serial, and access code.
     */
    public BambuMqttClient(BambuPrinterNetConnection connection) {
        this.host = connection.getPrinterUrl().getHost();
        this.port = connection.getPrinterUrl().getPort() == -1 ? DEFAULT_PORT : connection.getPrinterUrl().getPort();
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
        if (!isPlaceholderSerial(serial)) {
            serialDiscoveryFuture.complete(serial);
        }
        return serialDiscoveryFuture.orTimeout(DISCOVERY_TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    /**
     * Checks if the provided serial number is a placeholder.
     *
     * @param s the serial number to check.
     * @return true if it is a placeholder or null/empty.
     */
    boolean isPlaceholderSerial(String s) {
        return s == null || s.isEmpty() || s.equalsIgnoreCase("Bambu Printer") || 
               s.equalsIgnoreCase("Bambu Lab Printer") || s.equalsIgnoreCase("Unknown");
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
     * <p>If already connected, this method returns a completed future.</p>
     *
     * @return a {@link CompletableFuture} that completes when the connection is established and initial subscriptions are made.
     */
    public synchronized CompletableFuture<Void> connect() {
        isManualDisconnect.set(false);
        if (isConnected()) {
            LOGGER.debug("MQTT client for {} is already connected", host);
            return CompletableFuture.completedFuture(null);
        }

        if (connectFuture != null && !connectFuture.isDone()) {
            LOGGER.debug("MQTT connection for {} already in progress", host);
            return connectFuture;
        }

        List<Provider> originalProviders = prioritizeSunProviders();
        InteractiveTrustManagerFactory tmf;
        try {
            tmf = new InteractiveTrustManagerFactory();
        } catch (Exception e) {
            LOGGER.error("Failed to initialize SSL configuration", e);
            restoreProviders(originalProviders);
            return CompletableFuture.failedFuture(e);
        }

        this.client = buildMqttClient(tmf);

        connectFuture = client.connectWith()
                .simpleAuth()
                .username("bblp")
                .password(accessCode.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .cleanSession(true)
                .keepAlive(KEEP_ALIVE_SEC)
                .send()
                .handle((ack, throwable) -> handleConnectResult(ack, throwable, tmf, originalProviders))
                .thenCompose(f -> f)
                .thenCompose(v -> performInitialSubscriptions(originalProviders));
        
        return connectFuture;
    }

    /**
     * Temporarily prioritizes Sun security providers to ensure compatibility with XDH algorithms.
     *
     * @return the list of original providers in their original order.
     */
    private List<Provider> prioritizeSunProviders() {
        String[] sunProviders = {"SunJSSE", "SunJCE", "SunEC"};
        List<Provider> originalProviders = new ArrayList<>();
        for (String name : sunProviders) {
            Provider p = Security.getProvider(name);
            if (p != null) {
                originalProviders.add(p);
                Security.removeProvider(name);
                Security.insertProviderAt(p, 1);
            }
        }
        LOGGER.debug("Temporarily prioritized Sun providers for Bambu MQTT connection");
        return originalProviders;
    }

    /**
     * Restores the original security provider ordering.
     *
     * @param providers the list of providers to restore.
     */
    private void restoreProviders(List<Provider> providers) {
        for (int i = providers.size() - 1; i >= 0; i--) {
            Provider p = providers.get(i);
            Security.removeProvider(p.getName());
            Security.addProvider(p);
        }
        LOGGER.debug("Restored original security provider ordering");
    }

    /**
     * Builds the HiveMQ MQTT client instance.
     *
     * @param tmf the trust manager factory for SSL.
     * @return a new {@link Mqtt3AsyncClient} instance.
     */
    private Mqtt3AsyncClient buildMqttClient(InteractiveTrustManagerFactory tmf) {
        String clientId = (!isPlaceholderSerial(serial)) ? serial : "aS" + (System.currentTimeMillis() % 1000000000L);
        
        MqttClientSslConfig sslConfig = MqttClientSslConfig.builder()
                .hostnameVerifier((hostname, session) -> true)
                .trustManagerFactory(tmf)
                .build();

        return Mqtt3Client.builder()
                .identifier(clientId)
                .serverHost(host)
                .serverPort(port)
                .transportConfig()
                    .mqttConnectTimeout(30, TimeUnit.SECONDS)
                    .applyTransportConfig()
                .sslConfig(sslConfig)
                .automaticReconnectWithDefaultConfig()
                .addConnectedListener(context -> {
                    int count = connectionCounter.incrementAndGet();
                    LOGGER.info("MQTT Client ({}) connected to {} (attempt #{})", clientId, host, count);
                    
                    // On reconnect (count > 1), we need to restore subscriptions because we use cleanSession(true)
                    if (count > 1) {
                        LOGGER.info("Restoring subscriptions after reconnect for host {}", host);
                        if (!isPlaceholderSerial(serial)) {
                            subscribeToTelemetry().exceptionally(ex -> {
                                LOGGER.error("Failed to restore telemetry subscriptions for {}: {}", host, ex.getMessage());
                                return null;
                            });
                        } else {
                            subscribeToDiscovery().exceptionally(ex -> {
                                LOGGER.error("Failed to restore discovery subscription for {}: {}", host, ex.getMessage());
                                return null;
                            });
                        }
                    }
                })
                .addDisconnectedListener(context -> {
                    Throwable cause = context.getCause();
                    if (isManualDisconnect.get()) {
                        LOGGER.info("MQTT Client ({}) disconnected from {} (manual disconnect)", clientId, host);
                    } else {
                        LOGGER.warn("MQTT Client ({}) disconnected unexpectedly from {}: {}", clientId, host,
                                (cause != null ? cause.getMessage() : "No cause provided"));
                    }
                })
                .buildAsync();
    }

    /**
     * Handles the result of the MQTT connection attempt.
     */
    private CompletableFuture<Void> handleConnectResult(Mqtt3ConnAck ack, Throwable throwable, 
                                                         InteractiveTrustManagerFactory tmf, 
                                                         List<Provider> originalProviders) {
        extractSerialFromCertificate(tmf);

        if (throwable != null) {
            restoreProviders(originalProviders);
            return CompletableFuture.failedFuture(unwrapConnectionException(throwable));
        }

        if (ack.getReturnCode() != Mqtt3ConnAckReturnCode.SUCCESS) {
            restoreProviders(originalProviders);
            return CompletableFuture.failedFuture(new RuntimeException("MQTT Connection rejected with code: " + ack.getReturnCode()));
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Extracts the serial number from the printer's SSL certificate if available.
     */
    private void extractSerialFromCertificate(InteractiveTrustManagerFactory tmf) {
        X509Certificate cert = tmf.getTrustManager().getLastHandshakeCertificate();
        if (cert != null) {
            String cn = SslDetailsUtils.getCommonName(cert);
            if (cn != null && !cn.isEmpty() && isPlaceholderSerial(this.serial)) {
                LOGGER.info("Extracted serial number from SSL certificate: {} for host {}", cn, host);
                this.serial = cn;
                serialDiscoveryFuture.complete(cn);
            }
        }
    }

    /**
     * Unwraps the HiveMQ connection exception to find the root cause.
     */
    private Throwable unwrapConnectionException(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause != cause.getCause()) {
            if (cause instanceof UntrustedCertificateException) {
                break;
            }
            cause = cause.getCause();
        }
        return cause;
    }

    /**
     * Performs initial subscriptions and version requests after a successful connection.
     */
    private CompletableFuture<Void> performInitialSubscriptions(List<Provider> originalProviders) {
        CompletableFuture<Void> subscribeFuture = isPlaceholderSerial(serial) ? 
                subscribeToDiscovery() : subscribeToTelemetry();

        return subscribeFuture.handle((subAck, subEx) -> {
            restoreProviders(originalProviders);
            if (subEx != null) {
                LOGGER.error("Initial subscription failed for host {}: {}", host, subEx.getMessage());
                return CompletableFuture.<Void>failedFuture(subEx);
            }

            return triggerInitialVersionRequest();
        }).thenCompose(sf -> sf);
    }

    /**
     * Triggers the initial get_version request, potentially waiting for serial discovery.
     */
    private CompletableFuture<Void> triggerInitialVersionRequest() {
        if (!isPlaceholderSerial(this.serial)) {
            return delayedSendGetVersion();
        } else {
            return serialDiscoveryFuture.thenCompose(s -> delayedSendGetVersion());
        }
    }

    /**
     * Sends the get_version request after a short delay to ensure subscriptions are active.
     */
    private CompletableFuture<Void> delayedSendGetVersion() {
        return CompletableFuture.runAsync(() -> {
            try { Thread.sleep(INITIAL_REQUEST_DELAY_MS); } catch (InterruptedException ignored) {}
        }).thenCompose(v -> sendGetVersion());
    }

    /**
     * Subscribes to the wildcard topic to receive reports from any Bambu printer.
     *
     * @return a {@link CompletableFuture} that completes when the subscription is successful.
     */
    private CompletableFuture<Void> subscribeToDiscovery() {
        Mqtt3AsyncClient currentClient = this.client;
        if (currentClient == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("MQTT client is null"));
        }
        String filter = "#";
        LOGGER.info("Subscribing to discovery wildcard topic ({}) for host {}", filter, host);
        return currentClient.subscribeWith()
                .topicFilter(filter)
                .callback(mqtt3Publish -> {
                    String topic = mqtt3Publish.getTopic().toString();
                    String[] parts = topic.split("/");
                    if (parts.length >= 2) {
                        String discoveredSerial = parts[1];
                        if (discoveredSerial != null && !discoveredSerial.isEmpty() && 
                            (this.serial == null || !this.serial.equals(discoveredSerial))) {
                            LOGGER.info("Discovered serial number: {} for host {}", discoveredSerial, host);
                            this.serial = discoveredSerial;
                            serialDiscoveryFuture.complete(discoveredSerial);
                            subscribeToTelemetry();
                        }
                    }
                    handleTelemetry(new String(mqtt3Publish.getPayloadAsBytes(), StandardCharsets.UTF_8));
                })
                .send()
                .handle((subAck, ex) -> {
                    if (ex != null) {
                        LOGGER.error("Failed to subscribe to discovery topic ({}) for host {}: {}", filter, host, ex.getMessage());
                        serialDiscoveryFuture.completeExceptionally(ex);
                        return CompletableFuture.<Void>failedFuture(ex);
                    }
                    LOGGER.info("Successfully subscribed to discovery topic ({}) for host {}", filter, host);
                    return CompletableFuture.<Void>completedFuture(null);
                }).thenCompose(f -> f);
    }

    /**
     * Subscribes to the printer's telemetry and notify topics.
     *
     * @return a {@link CompletableFuture} that completes when the primary subscription is successful.
     */
    CompletableFuture<Void> subscribeToTelemetry() {
        Mqtt3AsyncClient currentClient = this.client;
        if (currentClient == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("MQTT client is null"));
        }
        String topicReport = BambuTopics.telemetry(serial);
        String topicNotify = BambuTopics.notify(serial);
        LOGGER.info("Subscribing to telemetry topics ({}, {}) for host {}", topicReport, topicNotify, host);
        
        CompletableFuture<?> sub1 = currentClient.subscribeWith()
                .topicFilter(topicReport)
                .callback(mqtt3Publish -> handleTelemetry(new String(mqtt3Publish.getPayloadAsBytes(), StandardCharsets.UTF_8)))
                .send();
                
        currentClient.subscribeWith()
                .topicFilter(topicNotify)
                .callback(mqtt3Publish -> handleTelemetry(new String(mqtt3Publish.getPayloadAsBytes(), StandardCharsets.UTF_8)))
                .send()
                .handle((ack, ex) -> {
                    if (ex != null) LOGGER.warn("Failed to subscribe to notify topic for host {}: {}", host, ex.getMessage());
                    return null;
                });

        return sub1.handle((v, ex) -> {
            if (ex != null) {
                LOGGER.error("Failed to subscribe to telemetry topic for host {}: {}", host, ex.getMessage());
                return CompletableFuture.<Void>failedFuture(ex);
            }
            LOGGER.info("Successfully subscribed to telemetry topic for host {}", host);
            return CompletableFuture.<Void>completedFuture(null);
        }).thenCompose(f -> f);
    }

    /**
     * Parses the incoming telemetry payload and dispatches it to the telemetry consumer.
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
                LOGGER.trace("Failed to parse telemetry payload: {}", e.getMessage());
            }
        }
    }

    /**
     * Disconnects the MQTT client from the printer's broker.
     *
     * @return a {@link CompletableFuture} that completes when the disconnection is finished.
     */
    public synchronized CompletableFuture<Void> disconnect() {
        if (client != null) {
            isManualDisconnect.set(true);
            LOGGER.info("Disconnecting MQTT client for host {}", host);
            CompletableFuture<Void> disconnectFuture = client.disconnect()
                    .whenComplete((v, ex) -> {
                        if (ex != null) {
                            LOGGER.warn("Error during MQTT disconnect for {}: {}", host, ex.getMessage());
                        } else {
                            LOGGER.debug("MQTT client for {} disconnected successfully", host, (Object) null);
                        }
                    });
            client = null;
            connectFuture = null;
            return disconnectFuture;
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Sends a {@code system: get_version} message to the printer.
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
        if (connectFuture != null && !connectFuture.isDone()) {
            LOGGER.info("Connection in progress for {}, waiting before sending get_version...", host);
            return connectFuture.handle((v, ex) -> sendGetVersion(retries)).thenCompose(f -> f);
        }

        Mqtt3AsyncClient currentClient = this.client;
        if (currentClient == null) {
            LOGGER.error("Failed to send get_version to {}: MQTT client is null (disconnected)", host);
            return CompletableFuture.failedFuture(new RuntimeException("MQTT client is not initialized"));
        }
        
        if (!currentClient.getState().isConnected()) {
            LOGGER.warn("Attempting to send get_version to {} while client is in state {}", host, currentClient.getState());
            if (retries > 0) {
                LOGGER.info("Retrying get_version for {} in 1s... (retries left: {})", host, retries);
                return CompletableFuture.runAsync(() -> {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }).thenCompose(v -> sendGetVersion(retries - 1));
            }
            return CompletableFuture.failedFuture(new RuntimeException("MQTT client is not connected (state: " + currentClient.getState() + ")"));
        }
        
        String topic = getRequestTopic();
        if (topic == null) {
            LOGGER.info("Request topic unknown for host {}, waiting for serial discovery...", host);
            return getSerialDiscoveryFuture()
                    .handle((s, ex) -> ex != null ? CompletableFuture.<Void>completedFuture(null) : sendGetVersion(retries))
                    .thenCompose(f -> f);
        }

        String payload = "{\"system\": {\"sequence_id\": \"0\", \"command\": \"get_version\"}, \"user_id\": \"0\", \"sequence_id\": \"0\"}";
        LOGGER.info("Sending get_version request to topic {} for host {}", topic, host);
        return client.publishWith()
                .topic(topic)
                .qos(com.hivemq.client.mqtt.datatypes.MqttQos.AT_LEAST_ONCE)
                .payload(payload.getBytes(StandardCharsets.UTF_8))
                .send()
                .handle((pubAck, ex) -> {
                    if (ex != null && retries > 0) {
                        LOGGER.warn("Failed to publish get_version to {}, retrying...: {}", host, ex.getMessage());
                        return sendGetVersion(retries - 1);
                    } else if (ex != null) {
                        LOGGER.error("Failed to send get_version to {}: {}", host, ex.getMessage());
                        return CompletableFuture.<Void>failedFuture(ex);
                    }
                    LOGGER.info("Successfully sent get_version to {} on topic {}", host, topic);
                    return CompletableFuture.<Void>completedFuture(null);
                }).thenCompose(f -> f);
    }

    /**
     * Gets the request topic for sending commands to the printer.
     *
     * @return the MQTT request topic string, or null if serial is unknown.
     */
    public String getRequestTopic() {
        return isPlaceholderSerial(serial) ? null : BambuTopics.request(serial);
    }

    /**
     * Gets the telemetry subscription topic for the current printer serial.
     *
     * @return the MQTT telemetry topic string.
     */
    public String getTelemetryTopic() {
        if (isPlaceholderSerial(serial)) return "device/+/report";
        return BambuTopics.telemetry(serial);
    }
}
