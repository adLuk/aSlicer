package cz.ad.print3d.aslicer.logic.printer.system.net;

/**
 * Enum representing the available types of printer network connections.
 */
public enum PrinterNetConnectionType {

    /**
     * Connection established via a USB interface.
     */
    USB,

    /**
     * Connection established via a network protocol (e.g., MQTT, HTTP over Wi-Fi/Ethernet).
     */
    NETWORK,

    /**
     * Custom or specialized connection type.
     */
    CUSTOM
}
