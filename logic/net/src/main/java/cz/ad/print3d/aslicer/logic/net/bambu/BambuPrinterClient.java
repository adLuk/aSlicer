package cz.ad.print3d.aslicer.logic.net.bambu;

import cz.ad.print3d.aslicer.logic.net.AbstractPrinterClient;
import cz.ad.print3d.aslicer.logic.printer.dto.Printer3DDto;
import cz.ad.print3d.aslicer.logic.printer.dto.PrinterSystemDto;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Client for communicating with Bambu Lab printers.
 */
public class BambuPrinterClient extends AbstractPrinterClient {
    private final String serial;
    private final String accessCode;
    private BambuMqttClient mqttClient;

    /**
     * Constructs a BambuPrinterClient.
     *
     * @param ipAddress  the IP address of the printer.
     * @param serial     the serial number of the printer.
     * @param accessCode the access code for authentication.
     */
    public BambuPrinterClient(String ipAddress, String serial, String accessCode) {
        super(ipAddress);
        this.serial = serial;
        this.accessCode = accessCode;
    }

    @Override
    public CompletableFuture<Void> connect() {
        try {
            URL url = URI.create("mqtts://" + ipAddress + ":8883").toURL();
            BambuMqttPrinterNetConnection connection = new BambuMqttPrinterNetConnection(url, serial, accessCode);
            mqttClient = new BambuMqttClient(connection);
            return mqttClient.connect();
        } catch (MalformedURLException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<Printer3DDto> getDetails() {
        Printer3DDto dto = new Printer3DDto();
        PrinterSystemDto system = new PrinterSystemDto();
        system.setPrinterManufacturer("Bambu Lab");
        system.setPrinterName("Bambu Lab " + serial);
        system.setPrinterModel("Unknown Bambu Model");
        dto.setPrinterSystem(system);
        return CompletableFuture.completedFuture(dto);
    }

    @Override
    public void disconnect() {
        if (mqttClient != null) {
            mqttClient.disconnect();
        }
    }
}
