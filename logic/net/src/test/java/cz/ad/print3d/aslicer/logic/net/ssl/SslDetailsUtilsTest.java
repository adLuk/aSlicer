package cz.ad.print3d.aslicer.logic.net.ssl;

import org.junit.jupiter.api.Test;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SslDetailsUtilsTest {

    private X509Certificate createCertStub(String dn) {
        return new X509Certificate() {
            @Override public void checkValidity() {}
            @Override public void checkValidity(java.util.Date date) {}
            @Override public int getVersion() { return 3; }
            @Override public java.math.BigInteger getSerialNumber() { return java.math.BigInteger.ONE; }
            @Override public java.security.Principal getIssuerDN() { return new X500Principal("CN=Bambu"); }
            @Override public java.security.Principal getSubjectDN() { return new X500Principal(dn); }
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
            @Override public X500Principal getSubjectX500Principal() {
                return new X500Principal(dn);
            }
        };
    }

    @Test
    public void testGetCommonNameWithSnPrefix() {
        X509Certificate cert = createCertStub("CN=SN:01S123456789012, O=Bambu Lab");
        String cn = SslDetailsUtils.getCommonName(cert);
        assertEquals("01S123456789012", cn);
    }

    @Test
    public void testGetCommonNameWithoutSnPrefix() {
        X509Certificate cert = createCertStub("CN=01S123456789012, O=Bambu Lab");
        String cn = SslDetailsUtils.getCommonName(cert);
        assertEquals("01S123456789012", cn);
    }
}
