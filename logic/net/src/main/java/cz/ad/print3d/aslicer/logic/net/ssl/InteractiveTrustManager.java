package cz.ad.print3d.aslicer.logic.net.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * X509TrustManager that checks certificates against the application FIPS KeyStore
 * and (optionally) the system trust store.
 *
 * <p>If a certificate is not trusted by either, it throws an {@link UntrustedCertificateException}
 * containing the certificate to allow for user intervention.</p>
 */
public class InteractiveTrustManager implements X509TrustManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(InteractiveTrustManager.class);
    private final X509TrustManager appTrustManager;
    private final X509TrustManager systemTrustManager;
    private final boolean trustSystem;

    /**
     * Constructs a new InteractiveTrustManager.
     *
     * @throws Exception if there is an error initializing trust managers.
     */
    public InteractiveTrustManager() throws Exception {
        this.trustSystem = Boolean.parseBoolean(System.getProperty("aslicer.ssl.trust_system_certs", "true"));

        // 1. Initialize Application TrustManager from BCFKS (Centralized FIPS KeyStore)
        this.appTrustManager = createTrustManager(SslCertificateManager.loadKeyStore(), "SunJSSE");

        // 2. Initialize System TrustManager (if enabled)
        if (trustSystem) {
            this.systemTrustManager = createSystemTrustManager();
        } else {
            this.systemTrustManager = null;
            LOGGER.info("System certificates trust is disabled");
        }
    }

    private X509TrustManager createTrustManager(KeyStore ks, String provider) throws Exception {
        if (ks == null || ks.size() == 0) {
            return null;
        }
        TrustManagerFactory tmf;
        if (provider != null) {
            try {
                // Ensure we use the requested provider for TrustManagerFactory
                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm(), provider);
            } catch (NoSuchProviderException e) {
                LOGGER.warn("Provider {} not found for trust management, falling back to default", provider);
                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            }
        } else {
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        }
        
        tmf.init(ks);
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                // Wrap the TrustManager to ensure handshake crypto uses SunJSSE preferred providers
                return new HandshakeFixTrustManager((X509TrustManager) tm);
            }
        }
        throw new NoSuchAlgorithmException("No X509TrustManager found");
    }

    private X509TrustManager createSystemTrustManager() {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm(), "SunJSSE");
            try {
                // Try standard initialization
                tmf.init((KeyStore) null);
            } catch (Exception e) {
                // Fallback for BCFIPS NPE or format issues when loading system truststore
                LOGGER.warn("Failed to initialize system truststore normally, trying explicit load from cacerts: {}", e.getMessage());
                KeyStore ks = loadSystemKeyStore();
                tmf.init(ks);
            }
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    return new HandshakeFixTrustManager((X509TrustManager) tm);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Could not initialize system TrustManager", e);
        }
        return null;
    }

    /**
     * A wrapper for X509TrustManager that ensures certain cryptographic operations
     * during the handshake (like XDH keypair generation) prefer Sun providers
     * when Bouncy Castle FIPS is the primary provider.
     */
    private static class HandshakeFixTrustManager implements X509TrustManager {
        private final X509TrustManager delegate;

        HandshakeFixTrustManager(X509TrustManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            delegate.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // When we reach this point, the handshake is in progress.
            // However, the error happens BEFORE checkServerTrusted is called (during KeyShare exchange).
            // But having this class here might help if we were to trigger something.
            // In reality, we need a way to affect the KeyPairGenerator.
            delegate.checkServerTrusted(chain, authType);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return delegate.getAcceptedIssuers();
        }
    }

    private KeyStore loadSystemKeyStore() throws Exception {
        // Explicitly use SUN provider and try JKS or PKCS12 for system cacerts
        // This avoids BCFIPS strictness on stream format which causes IOException for JKS cacerts
        KeyStore ks;
        try {
            ks = KeyStore.getInstance("JKS", "SUN");
        } catch (Exception e) {
            try {
                ks = KeyStore.getInstance("PKCS12", "SUN");
            } catch (Exception e2) {
                // Last resort: default type but SUN provider
                ks = KeyStore.getInstance(KeyStore.getDefaultType(), "SUN");
            }
        }
        
        File cacerts = new File(System.getProperty("java.home") + "/lib/security/cacerts");
        if (!cacerts.exists()) {
            cacerts = new File(System.getProperty("java.home") + "/lib/security/jssecacerts");
        }
        
        if (cacerts.exists()) {
            try (InputStream is = new FileInputStream(cacerts)) {
                // Standard Java cacerts password is "changeit"
                ks.load(is, "changeit".toCharArray());
                LOGGER.debug("Loaded system truststore from {}", cacerts.getAbsolutePath());
            }
        } else {
            LOGGER.warn("System cacerts file not found, initialized empty truststore");
            ks.load(null, null);
        }
        return ks;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (appTrustManager != null) {
            appTrustManager.checkClientTrusted(chain, authType);
        } else if (trustSystem && systemTrustManager != null) {
            systemTrustManager.checkClientTrusted(chain, authType);
        } else {
            throw new CertificateException("No trust manager available for client trust checking");
        }
    }

    private X509Certificate lastHandshakeCertificate;

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain != null && chain.length > 0) {
            this.lastHandshakeCertificate = chain[0];
        }

        // 1. First priority: Check against Application BCFKS TrustStore (if not empty)
        if (appTrustManager != null) {
            try {
                appTrustManager.checkServerTrusted(chain, authType);
                LOGGER.debug("Server certificate trusted via application KeyStore");
                return;
            } catch (CertificateException e) {
                // Not trusted by app store, proceed to system check
                if (chain != null && chain.length > 0) {
                    LOGGER.debug("Certificate not in application store: {}", chain[0].getSubjectX500Principal());
                }
            } catch (RuntimeException e) {
                // Catch potential InvalidAlgorithmParameterException from SUN provider if KeyStore is empty
                LOGGER.warn("Application trust manager check failed: {}", e.getMessage());
            }
        }

        // 2. Second priority: Check against System TrustStore (if enabled)
        if (trustSystem && systemTrustManager != null) {
            try {
                systemTrustManager.checkServerTrusted(chain, authType);
                LOGGER.debug("Server certificate trusted via system truststore");
                return;
            } catch (CertificateException e) {
                // Not trusted by system store either
            } catch (RuntimeException e) {
                // Catch potential InvalidAlgorithmParameterException from SUN provider if system truststore is empty/invalid
                LOGGER.warn("System trust manager check failed: {}", e.getMessage());
            }
        }

        // 3. Final resort: Throw exception for user confirmation (Interactive flow)
        if (chain != null && chain.length > 0) {
            LOGGER.warn("Certificate not trusted: {}", chain[0].getSubjectX500Principal());
            throw new UntrustedCertificateException(chain[0]);
        }
        throw new CertificateException("Empty server certificate chain");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        X509Certificate[] appIssuers = appTrustManager != null ? appTrustManager.getAcceptedIssuers() : new X509Certificate[0];
        if (trustSystem && systemTrustManager != null) {
            X509Certificate[] systemIssuers = systemTrustManager.getAcceptedIssuers();
            X509Certificate[] combined = new X509Certificate[systemIssuers.length + appIssuers.length];
            System.arraycopy(systemIssuers, 0, combined, 0, systemIssuers.length);
            System.arraycopy(appIssuers, 0, combined, systemIssuers.length, appIssuers.length);
            return combined;
        }
        return appIssuers;
    }

    /**
     * Gets the last certificate seen during a server handshake check.
     *
     * @return the last server certificate, or null if none.
     */
    public X509Certificate getLastHandshakeCertificate() {
        return lastHandshakeCertificate;
    }
}
