package cz.ad.print3d.aslicer.logic.net.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Exception thrown when a server certificate is not trusted.
 *
 * <p>This exception carries the untrusted certificate to allow the UI to display
 * its details and ask the user if they want to trust it.</p>
 */
public class UntrustedCertificateException extends CertificateException {
    private static final long serialVersionUID = 1L;

    private final X509Certificate certificate;

    /**
     * Constructs a new UntrustedCertificateException with the given certificate.
     *
     * @param certificate the untrusted certificate.
     */
    public UntrustedCertificateException(X509Certificate certificate) {
        super("Server certificate is not trusted");
        this.certificate = certificate;
    }

    /**
     * @return the untrusted certificate.
     */
    public X509Certificate getCertificate() {
        return certificate;
    }
}
