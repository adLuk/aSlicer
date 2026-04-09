package cz.ad.print3d.aslicer.logic.net;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Abstract base class for printer clients.
 */
public abstract class AbstractPrinterClient implements PrinterClient {
    protected final String ipAddress;
    protected CertificateValidationCallback certificateValidationCallback;

    /**
     * Constructs an AbstractPrinterClient with the given IP address.
     *
     * @param ipAddress the IP address of the printer.
     */
    protected AbstractPrinterClient(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public void setCertificateValidationCallback(CertificateValidationCallback callback) {
        this.certificateValidationCallback = callback;
    }

    /**
     * Creates an {@link HttpClient} configured to handle self-signed certificates.
     * 
     * @return the configured HTTP client.
     */
    protected HttpClient createHttpClient() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    if (certificateValidationCallback != null) {
                        X509Certificate cert = chain[0];
                        String details = "Subject: " + cert.getSubjectX500Principal().getName() + "\n" +
                                         "Issuer: " + cert.getIssuerX500Principal().getName();
                        
                        Boolean accepted = certificateValidationCallback.onUntrustedCertificate(details).join();
                        if (!accepted) {
                            throw new CertificateException("User rejected the certificate");
                        }
                    }
                    // For self-signed we might want to skip standard verification if user accepted
                }
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, new java.security.SecureRandom());
            return HttpClient.newBuilder().sslContext(sslContext).build();
        } catch (Exception e) {
            return HttpClient.newBuilder().build();
        }
    }
}
