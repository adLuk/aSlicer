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
import java.util.concurrent.CompletionException;

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
            URL url = URI.create("https://" + ipAddress + ":8883").toURL();
            BambuMqttPrinterNetConnection connection = new BambuMqttPrinterNetConnection(url, serial, accessCode);
            mqttClient = new BambuMqttClient(connection);
            return attemptConnect();
        } catch (MalformedURLException e) {
            return CompletableFuture.failedFuture(e);
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
        if (ex instanceof CompletionException && ex.getCause() != null) {
            cause = ex.getCause();
        }

        if (cause instanceof UntrustedCertificateException && certificateValidationCallback != null) {
            X509Certificate cert = ((UntrustedCertificateException) cause).getCertificate();
            String details = SslDetailsUtils.formatCertificateDetails(cert);
            return certificateValidationCallback.onUntrustedCertificate(details).thenCompose(accepted -> {
                if (accepted) {
                    try {
                        SslCertificateManager.trustCertificate(cert, ipAddress);
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

        return mqttClient.getSerialDiscoveryFuture().thenApply(discoveredSerial -> {
            Printer3DDto dto = new Printer3DDto();
            PrinterSystemDto system = new PrinterSystemDto();
            system.setPrinterManufacturer("Bambu Lab");
            system.setPrinterName("Bambu Lab " + discoveredSerial);
            system.setPrinterModel("Bambu Lab Printer (" + discoveredSerial + ")");
            dto.setPrinterSystem(system);
            return dto;
        });
    }

    @Override
    public void disconnect() {
        if (mqttClient != null) {
            mqttClient.disconnect();
        }
    }
}
