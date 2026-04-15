package cz.ad.print3d.aslicer.logic.net.bambu;

import cz.ad.print3d.aslicer.logic.printer.system.net.BambuPrinterNetConnection;
import java.net.URL;

/**
 * Implementation of {@link BambuPrinterNetConnection} for Bambu Lab 3D printers over MQTT.
 *
 * <p>This class stores the necessary details for establishing a network connection
 * to a Bambu Lab printer via the MQTT protocol, including the URL, serial number,
 * and access code.</p>
 */
public class BambuMqttPrinterNetConnection implements BambuPrinterNetConnection {

    private final URL printerUrl;
    private final String serial;
    private final String accessCode;

    /**
     * Constructs a new BambuMqttPrinterNetConnection.
     *
     * @param printerUrl the printer URL (e.g., {@code https://192.168.1.10:8883})
     * @param serial     the printer serial number
     * @param accessCode the printer access code (pairing code)
     */
    public BambuMqttPrinterNetConnection(URL printerUrl, String serial, String accessCode) {
        this.printerUrl = printerUrl;
        this.serial = serial;
        this.accessCode = accessCode;
    }

    /**
     * @return the {@link URL} of the printer's MQTT broker.
     */
    @Override
    public URL getPrinterUrl() {
        return printerUrl;
    }

    /**
     * @return the access code used for pairing with the printer.
     */
    @Override
    public String getPairingCode() {
        return accessCode;
    }

    /**
     * @return the serial number of the printer.
     */
    @Override
    public String getSerial() {
        return serial;
    }

    /**
     * @return the access code used for authentication.
     */
    @Override
    public String getAccessCode() {
        return accessCode;
    }
}
