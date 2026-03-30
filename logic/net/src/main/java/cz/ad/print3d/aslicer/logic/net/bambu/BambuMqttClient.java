package cz.ad.print3d.aslicer.logic.net.bambu;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import cz.ad.print3d.aslicer.logic.printer.system.net.BambuPrinterNetConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Client for communicating with Bambu Lab 3D printers over MQTT.
 *
 * <p>This client handles the connection to the printer's MQTT broker, authentication
 * using the serial number and access code, and parsing of incoming telemetry data.</p>
 *
 * <p>It uses the HiveMQ MQTT client for asynchronous communication.</p>
 */
public class BambuMqttClient {

    private final String host;
    private final int port;
    private final String serial;
    private final String accessCode;
    private final BambuMqttMapper mapper;
    private Mqtt5AsyncClient client;
    private Consumer<Map<String, Object>> telemetryConsumer;

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
     * @return a {@link CompletableFuture} that completes when the connection is established.
     */
    public CompletableFuture<Void> connect() {
        client = Mqtt5Client.builder()
                .identifier(serial)
                .serverHost(host)
                .serverPort(port)
                .sslWithDefaultConfig()
                .buildAsync();

        return client.connectWith()
                .simpleAuth()
                .username("bblp")
                .password(accessCode.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .send()
                .thenAccept(mqtt5ConnAck -> {
                    // Connected successfully
                    subscribeToTelemetry();
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
                .callback(mqtt5Publish -> {
                    byte[] payload = mqtt5Publish.getPayloadAsBytes();
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
        return BambuTopics.telemetry(serial);
    }

    /**
     * Gets the request topic for sending commands to the printer.
     *
     * @return the MQTT request topic string.
     */
    public String getRequestTopic() {
        return BambuTopics.request(serial);
    }
}
