package cz.ad.print3d.aslicer.logic.net.ssl;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages trusted SSL certificates for the application.
 *
 * <p>This manager maintains a local Java KeyStore (JKS) in the user's home directory
 * to store self-signed or otherwise untrusted certificates that the user has
 * explicitly decided to trust.</p>
 */
public class SslCertificateManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SslCertificateManager.class);
    private static final String KEYSTORE_DIR = System.getProperty("user.home") + "/.aslicer";
    private static final String KEYSTORE_PATH = KEYSTORE_DIR + "/aslicer.bcfks";
    private static final char[] PASSWORD = "aslicer-fips-pwd".toCharArray();
    private static final String KEYSTORE_TYPE = "BCFKS";

    /**
     * Loads the local KeyStore from disk.
     *
     * <p>If the KeyStore file does not exist, a new empty one is created.</p>
     *
     * @return the loaded {@link KeyStore}.
     * @throws GeneralSecurityException if there is a security-related error.
     * @throws IOException if there is an I/O error.
     */
    public static KeyStore loadKeyStore() throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE, BouncyCastleFipsProvider.PROVIDER_NAME);
        File file = new File(KEYSTORE_PATH);
        if (file.exists()) {
            try (InputStream is = new FileInputStream(file)) {
                ks.load(is, PASSWORD);
            }
        } else {
            File dir = new File(KEYSTORE_DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                LOGGER.error("Failed to create directory for KeyStore: {}", KEYSTORE_DIR);
            }
            ks.load(null, PASSWORD);
        }
        return ks;
    }

    /**
     * Adds a certificate to the local trusted KeyStore.
     *
     * @param cert the certificate to trust.
     * @param alias the alias to use for the certificate.
     * @throws GeneralSecurityException if there is a security-related error.
     * @throws IOException if there is an I/O error.
     */
    public static void trustCertificate(X509Certificate cert, String alias) throws GeneralSecurityException, IOException {
        KeyStore ks = loadKeyStore();
        ks.setCertificateEntry(alias, cert);
        try (OutputStream os = new FileOutputStream(KEYSTORE_PATH)) {
            ks.store(os, PASSWORD);
        }
        LOGGER.info("Certificate with alias '{}' added to trusted KeyStore", alias);
    }

    /**
     * Checks if a certificate is already present in the trusted KeyStore.
     *
     * @param cert the certificate to check.
     * @return true if the certificate is trusted, false otherwise.
     */
    public static boolean isCertificateTrusted(X509Certificate cert) {
        try {
            KeyStore ks = loadKeyStore();
            String alias = ks.getCertificateAlias(cert);
            return alias != null;
        } catch (GeneralSecurityException | IOException e) {
            LOGGER.error("Error checking certificate trust status", e);
            return false;
        }
    }
}
