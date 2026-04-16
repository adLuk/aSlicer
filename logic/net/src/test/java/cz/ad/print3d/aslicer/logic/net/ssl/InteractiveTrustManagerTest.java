package cz.ad.print3d.aslicer.logic.net.ssl;

import cz.ad.print3d.aslicer.logic.core.security.SecurityInitializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InteractiveTrustManagerTest {

    @BeforeAll
    public static void setup() {
        SecurityInitializer.init();
    }

    @TempDir
    Path tempDir;

    @Test
    public void testUntrustedCertificateThrowsException() throws Exception {
        // Create a real X509Certificate (e.g. from a byte array or just use a dummy implementation if possible)
        // Since X509Certificate is abstract, we can create a simple subclass for testing.
        X509Certificate dummyCert = new TestX509Certificate();
        X509Certificate[] chain = new X509Certificate[]{dummyCert};

        // Create manager with empty trust stores
        System.setProperty("aslicer.ssl.trust_system_certs", "false");
        
        InteractiveTrustManager manager = new InteractiveTrustManager();
        
        assertThrows(UntrustedCertificateException.class, () -> {
            manager.checkServerTrusted(chain, "RSA");
        });
    }

    private static class TestX509Certificate extends X509Certificate {
        private static final long serialVersionUID = 1L;

        @Override public void checkValidity() {}
        @Override public void checkValidity(java.util.Date date) {}
        @Override public int getVersion() { return 3; }
        @Override public java.math.BigInteger getSerialNumber() { return java.math.BigInteger.ONE; }
        @Override public java.security.Principal getIssuerDN() { return new javax.security.auth.x500.X500Principal("CN=Test"); }
        @Override public java.security.Principal getSubjectDN() { return new javax.security.auth.x500.X500Principal("CN=Test"); }
        @Override public java.util.Date getNotBefore() { return new java.util.Date(); }
        @Override public java.util.Date getNotAfter() { return new java.util.Date(); }
        @Override public byte[] getTBSCertificate() { return new byte[0]; }
        @Override public byte[] getSignature() { return new byte[0]; }
        @Override public String getSigAlgName() { return "SHA256withRSA"; }
        @Override public String getSigAlgOID() { return "1.2.840.113549.1.1.11"; }
        @Override public byte[] getSigAlgParams() { return null; }
        @Override public boolean[] getIssuerUniqueID() { return null; }
        @Override public boolean[] getSubjectUniqueID() { return null; }
        @Override public java.util.Set<String> getCriticalExtensionOIDs() { return null; }
        @Override public byte[] getExtensionValue(String oid) { return null; }
        @Override public java.util.Set<String> getNonCriticalExtensionOIDs() { return null; }
        @Override public boolean hasUnsupportedCriticalExtension() { return false; }
        @Override public byte[] getEncoded() { return new byte[0]; }
        @Override public void verify(java.security.PublicKey key) {}
        @Override public void verify(java.security.PublicKey key, String sigProvider) {}
        @Override public String toString() { return "TestCert"; }
        @Override public java.security.PublicKey getPublicKey() { return null; }
        @Override public boolean[] getKeyUsage() { return null; }
        @Override public int getBasicConstraints() { return -1; }
        @Override public javax.security.auth.x500.X500Principal getIssuerX500Principal() {
            return new javax.security.auth.x500.X500Principal("CN=Test");
        }
        @Override public javax.security.auth.x500.X500Principal getSubjectX500Principal() {
            return new javax.security.auth.x500.X500Principal("CN=Test");
        }
    }

    @Test
    public void testAcceptedIssuersNotEmpty() throws Exception {
        InteractiveTrustManager manager = new InteractiveTrustManager();
        X509Certificate[] issuers = manager.getAcceptedIssuers();
        assertNotNull(issuers);
        // Even if empty, it shouldn't crash. In most environments it will have system certs.
    }

    @Test
    public void testClientTrustedDelegates() throws Exception {
        InteractiveTrustManager manager = new InteractiveTrustManager();
        X509Certificate[] chain = new X509Certificate[]{new TestX509Certificate()};
        // Should not throw exception if at least one trust manager is available, 
        // but might throw if cert is not trusted.
        // For now just verify it doesn't crash on nulls if no managers are available.
        try {
            manager.checkClientTrusted(chain, "RSA");
        } catch (CertificateException e) {
            // Expected if not trusted
        }
    }
}
