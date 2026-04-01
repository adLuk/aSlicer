package cz.ad.print3d.aslicer.logic.net.scanner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for {@link NettyMdnsScanner}.
 * <p>Verifies that mDNS discovery can be initiated and completes without errors,
 * even if no devices are found in the test environment.</p>
 */
class MdnsScannerTest {

    private NettyMdnsScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new NettyMdnsScanner();
    }

    @AfterEach
    void tearDown() {
        scanner.close();
    }

    @Test
    void testDiscoverDevices() throws Exception {
        // This test might not find any devices in a CI environment, 
        // but we can at least verify it completes without errors.
        CompletableFuture<Set<String>> future = scanner.discoverDevices(500);
        Set<String> discoveredIps = future.get(2, TimeUnit.SECONDS);
        
        assertNotNull(discoveredIps);
        // We can't guarantee any IPs are discovered, but it should not be null
    }
}
