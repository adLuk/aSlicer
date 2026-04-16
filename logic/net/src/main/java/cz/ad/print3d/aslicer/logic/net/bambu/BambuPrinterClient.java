package cz.ad.print3d.aslicer.logic.net.bambu;

import cz.ad.print3d.aslicer.logic.net.AbstractPrinterClient;
import cz.ad.print3d.aslicer.logic.net.ssl.SslCertificateManager;
import cz.ad.print3d.aslicer.logic.net.ssl.SslDetailsUtils;
import cz.ad.print3d.aslicer.logic.net.ssl.UntrustedCertificateException;
import cz.ad.print3d.aslicer.logic.printer.dto.Printer3DDto;
import cz.ad.print3d.aslicer.logic.printer.dto.PrinterSystemDto;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Client for communicating with Bambu Lab printers.
 */
public class BambuPrinterClient extends AbstractPrinterClient {
    private final String serial;
    private final String accessCode;
    private BambuMqttClient mqttClient;
    private long lastDetailsUpdate = 0;
    private static final long UPDATE_THROTTLE_MS = 1000;
    private String lastDiscoveredSerial = null;

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
            URL url = URI.create("https://" + ipAddress + ":8883").toURL();
            BambuMqttPrinterNetConnection connection = new BambuMqttPrinterNetConnection(url, serial, accessCode);
            mqttClient = new BambuMqttClient(connection);
            mqttClient.setTelemetryConsumer(this::handleTelemetry);
            return attemptConnect().orTimeout(20, TimeUnit.SECONDS);
        } catch (MalformedURLException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private void handleTelemetry(java.util.Map<String, Object> telemetry) {
        if (detailsUpdateCallback != null) {
            String currentSerial = (mqttClient != null) ? mqttClient.getSerial() : null;
            long now = System.currentTimeMillis();
            
            // Update immediately if serial was just discovered, otherwise throttle
            boolean serialChanged = (currentSerial != null && !currentSerial.equals(lastDiscoveredSerial));
            
            if (serialChanged || (now - lastDetailsUpdate >= UPDATE_THROTTLE_MS)) {
                lastDetailsUpdate = now;
                lastDiscoveredSerial = currentSerial;
                getDetails().thenAccept(details -> {
                    detailsUpdateCallback.accept(details);
                });
            }
        }
    }

    /**
     * Attempts to connect to the printer and handles certificate validation.
     *
     * @return a {@link CompletableFuture} for the connection process.
     */
    private CompletableFuture<Void> attemptConnect() {
        return mqttClient.connect().handle((v, ex) -> {
            if (ex != null) {
                return handleConnectionException(ex);
            }
            return CompletableFuture.completedFuture(v);
        }).thenCompose(f -> f);
    }

    private CompletableFuture<Void> handleConnectionException(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause != cause.getCause()) {
            if (cause instanceof UntrustedCertificateException) {
                break;
            }
            cause = cause.getCause();
        }

        if (cause instanceof UntrustedCertificateException && certificateValidationCallback != null) {
            UntrustedCertificateException untrustedEx = (UntrustedCertificateException) cause;
            X509Certificate cert = untrustedEx.getCertificate();
            String details = SslDetailsUtils.formatCertificateDetails(cert);
            return certificateValidationCallback.onUntrustedCertificate(details).thenCompose(accepted -> {
                if (accepted) {
                    try {
                        SslCertificateManager.trustCertificate(cert, ipAddress);
                        // Refresh the MQTT client to use new trust store
                        mqttClient.disconnect();
                        return attemptConnect();
                    } catch (Exception e) {
                        return CompletableFuture.failedFuture(e);
                    }
                } else {
                    return CompletableFuture.failedFuture(new CertificateException("User rejected the certificate"));
                }
            });
        }
        return CompletableFuture.failedFuture(ex);
    }

    @Override
    public CompletableFuture<Printer3DDto> getDetails() {
        if (mqttClient == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Client not connected"));
        }

        Printer3DDto dto = new Printer3DDto();
        PrinterSystemDto system = new PrinterSystemDto();
        system.setPrinterManufacturer("Bambu Lab");
        
        String currentSerial = mqttClient.getSerial();
        if (currentSerial == null || currentSerial.isEmpty()) {
            currentSerial = (serial != null && !serial.isEmpty()) ? serial : ipAddress;
            system.setPrinterName("Bambu Lab " + currentSerial);
            system.setPrinterModel("Bambu Lab Printer (" + currentSerial + ")");
            system.setSerialNumber(null); // Explicitly null to indicate not yet discovered
        } else {
            system.setPrinterName("Bambu Lab " + currentSerial);
            system.setPrinterModel("Bambu Lab Printer (" + currentSerial + ")");
            system.setSerialNumber(currentSerial);
        }
        
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
