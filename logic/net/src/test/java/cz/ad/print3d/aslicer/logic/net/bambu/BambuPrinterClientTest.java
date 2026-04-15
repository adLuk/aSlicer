package cz.ad.print3d.aslicer.logic.net.bambu;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

public class BambuPrinterClientTest {

    @Test
    void testConnectDoesNotThrowHandshakeInitializationError() {
        // Use a dummy IP that won't respond to speed up things or just test the future creation
        BambuPrinterClient client = new BambuPrinterClient("127.0.0.1", "SN123", "access");
        CompletableFuture<Void> future = client.connect();
        
        // At this point, the client should be built and connection initiated.
        // We don't wait for completion because it would fail on localhost,
        // but we've verified that the SSL/MQTT3 builder didn't crash.
        assertNotNull(future);
        client.disconnect();
    }
}
