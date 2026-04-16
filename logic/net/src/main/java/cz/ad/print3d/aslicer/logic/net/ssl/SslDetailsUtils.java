package cz.ad.print3d.aslicer.logic.net.ssl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility class for extracting details from SSL certificates for display.
 */
public class SslDetailsUtils {

    /**
     * Formats X509Certificate details into a human-readable string.
     *
     * @param cert the certificate to format.
     * @return a formatted string with certificate details.
     */
    public static String formatCertificateDetails(X509Certificate cert) {
        StringBuilder sb = new StringBuilder();
        sb.append("Subject: ").append(cert.getSubjectX500Principal().getName()).append("\n");
        sb.append("Issuer: ").append(cert.getIssuerX500Principal().getName()).append("\n");
        sb.append("Serial Number: ").append(cert.getSerialNumber().toString(16).toUpperCase()).append("\n");
        sb.append("Valid From: ").append(cert.getNotBefore()).append("\n");
        sb.append("Valid To: ").append(cert.getNotAfter()).append("\n");
        sb.append("Fingerprint (SHA-256): ").append(getFingerprint(cert)).append("\n");
        return sb.toString();
    }

    /**
     * Computes the SHA-256 fingerprint of the certificate.
     *
     * @param cert the certificate.
     * @return the hexadecimal fingerprint string.
     */
    private static String getFingerprint(X509Certificate cert) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] der = cert.getEncoded();
            md.update(der);
            byte[] digest = md.digest();
            return IntStream.range(0, digest.length)
                    .mapToObj(i -> String.format("%02X", digest[i]))
                    .collect(Collectors.joining(":"));
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            return "Unable to compute fingerprint";
        }
    }
    /**
     * Extracts the Common Name (CN) from the certificate's subject.
     *
     * @param cert the certificate.
     * @return the Common Name, or null if not found.
     */
    public static String getCommonName(X509Certificate cert) {
        String name = cert.getSubjectX500Principal().getName();
        for (String part : name.split(",")) {
            part = part.trim();
            if (part.startsWith("CN=")) {
                String cn = part.substring(3);
                if (cn.startsWith("SN:")) {
                    return cn.substring(3);
                }
                return cn;
            }
        }
        return null;
    }
}
