package cz.ad.print3d.aslicer.logic.net.bambu;

import cz.ad.print3d.aslicer.logic.printer.system.net.BambuPrinterNetConnection;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BambuMqttClientTest {

    @Test
    public void testTopicBuilders() throws Exception {
        BambuPrinterNetConnection conn = new BambuMqttPrinterNetConnection(URI.create("https://192.168.0.10:8883").toURL(), "SN999", "code");
        BambuMqttClient client = new BambuMqttClient(conn);
        assertEquals("device/SN999/report", client.getTelemetryTopic());
        assertEquals("device/SN999/request", client.getRequestTopic());
    }

    @Test
    public void testDiscoveryTopics() throws Exception {
        BambuPrinterNetConnection conn = new BambuMqttPrinterNetConnection(URI.create("https://192.168.0.10:8883").toURL(), null, "code");
        BambuMqttClient client = new BambuMqttClient(conn);
        // Default when serial is null is wildcard #
        assertEquals("device/+/report", client.getTelemetryTopic());
        assertNull(client.getRequestTopic());
    }

    @Test
    public void testSslCertDiscovery() throws Exception {
        // Create a real X509Certificate stub instead of mocking it to avoid Mockito issues with Java 25
        java.security.cert.X509Certificate cert = new java.security.cert.X509Certificate() {
            @Override public void checkValidity() {}
            @Override public void checkValidity(java.util.Date date) {}
            @Override public int getVersion() { return 3; }
            @Override public java.math.BigInteger getSerialNumber() { return java.math.BigInteger.ONE; }
            @Override public java.security.Principal getIssuerDN() { return new javax.security.auth.x500.X500Principal("CN=Bambu"); }
            @Override public java.security.Principal getSubjectDN() { return new javax.security.auth.x500.X500Principal("CN=01S123456789012, O=Bambu Lab"); }
            @Override public java.util.Date getNotBefore() { return new java.util.Date(); }
            @Override public java.util.Date getNotAfter() { return new java.util.Date(); }
            @Override public byte[] getTBSCertificate() { return new byte[0]; }
            @Override public byte[] getSignature() { return new byte[0]; }
            @Override public String getSigAlgName() { return "SHA256withRSA"; }
            @Override public String getSigAlgOID() { return "1.2.840.113549.1.1.11"; }
            @Override public byte[] getSigAlgParams() { return null; }
            @Override public boolean[] getIssuerUniqueID() { return null; }
            @Override public boolean[] getSubjectUniqueID() { return null; }
            @Override public boolean[] getKeyUsage() { return null; }
            @Override public int getBasicConstraints() { return -1; }
            @Override public byte[] getEncoded() { return new byte[0]; }
            @Override public void verify(java.security.PublicKey key) {}
            @Override public void verify(java.security.PublicKey key, String sigProvider) {}
            @Override public String toString() { return ""; }
            @Override public java.security.PublicKey getPublicKey() { return null; }
            @Override public boolean hasUnsupportedCriticalExtension() { return false; }
            @Override public java.util.Set<String> getCriticalExtensionOIDs() { return null; }
            @Override public java.util.Set<String> getNonCriticalExtensionOIDs() { return null; }
            @Override public byte[] getExtensionValue(String oid) { return null; }
            @Override public javax.security.auth.x500.X500Principal getSubjectX500Principal() {
                return new javax.security.auth.x500.X500Principal("CN=01S123456789012, O=Bambu Lab");
            }
        };
        
        String cn = cz.ad.print3d.aslicer.logic.net.ssl.SslDetailsUtils.getCommonName(cert);
        assertEquals("01S123456789012", cn);
    }
}
