package cz.ad.print3d.aslicer.logic.net.bambu;

/**
 * Utility class for building Bambu Lab MQTT topics.
 *
 * <p>Bambu Lab printers use a structured set of MQTT topics for reporting telemetry
 * and receiving requests. The topics follow the pattern: {@code device/<serial>/<type>}.</p>
 */
public final class BambuTopics {

    private BambuTopics() {}

    /**
     * Builds the telemetry reporting topic for a specific printer serial number.
     *
     * <p>This topic is used by the printer to publish its status and telemetry data.</p>
     *
     * @param serial the printer serial number.
     * @return the MQTT topic string for telemetry reporting.
     */
    public static String telemetry(String serial) {
        return String.format("device/%s/report", serial);
    }

    /**
     * Builds the request topic for a specific printer serial number.
     *
     * <p>This topic is used by external clients to send commands or requests to the printer.</p>
     *
     * @param serial the printer serial number.
     * @return the MQTT topic string for requests.
     */
    public static String request(String serial) {
        return String.format("device/%s/request", serial);
    }

    /**
     * Builds the notification topic for a specific printer serial number.
     *
     * <p>This topic may be used by the printer to publish specific notifications
     * separate from regular telemetry.</p>
     *
     * @param serial the printer serial number.
     * @return the MQTT topic string for notifications.
     */
    public static String notify(String serial) {
        return String.format("device/%s/notify", serial);
    }
}
