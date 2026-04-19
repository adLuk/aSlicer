package cz.ad.print3d.aslicer.logic.net;

import cz.ad.print3d.aslicer.logic.printer.dto.Printer3DDto;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for communicating with a 3D printer.
 *
 * <p>Provides methods for connecting, authenticating, and fetching detailed
 * information about the printer.</p>
 */
public interface PrinterClient {

    /**
     * Attempts to establish a connection and perform a handshake with the printer.
     *
     * @return a {@link CompletableFuture} that completes when the connection is established.
     */
    CompletableFuture<Void> connect();

    /**
     * Fetches detailed information about the printer.
     *
     * @return a {@link CompletableFuture} that completes with the printer details.
     */
    CompletableFuture<Printer3DDto> getDetails();

    /**
     * @return true if the client is currently connected.
     */
    boolean isConnected();

    /**
     * Sets a callback that will be notified when printer details are updated
     * (e.g., when a report is received after an initial connection).
     *
     * @param callback a consumer that will receive the updated printer details.
     */
    void setDetailsUpdateCallback(java.util.function.Consumer<Printer3DDto> callback);

    /**
     * Closes the connection to the printer.
     */
    void disconnect();

    /**
     * Sets a callback for handling untrusted (self-signed) certificates.
     *
     * @param callback a function that returns true if the certificate should be accepted.
     */
    void setCertificateValidationCallback(CertificateValidationCallback callback);

    /**
     * @return a map of credentials used by this client.
     */
    java.util.Map<String, String> getCredentials();

    /**
     * Callback interface for handling untrusted certificates.
     */
    interface CertificateValidationCallback {
        /**
         * Called when an untrusted certificate is encountered.
         *
         * @param certificateDetails details of the untrusted certificate.
         * @return a {@link CompletableFuture} that completes with true if the certificate should be accepted.
         */
        CompletableFuture<Boolean> onUntrustedCertificate(String certificateDetails);
    }
}
