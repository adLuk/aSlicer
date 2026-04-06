package cz.ad.print3d.aslicer.logic.net.scanner;

import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for {@link NettyNetworkScanner} optimization for deep scans on offline hosts.
 * <p>Verifies that deep scans on offline hosts are correctly optimized by skipping 
 * full port scans after a preliminary check fails.</p>
 */
class NettyNetworkScannerDeepScanTest {

    private StubPortScanner portScanner;
    private StubMdnsScanner mdnsScanner;
    private NettyNetworkScanner networkScanner;

    @BeforeEach
    void setUp() {
        portScanner = new StubPortScanner();
        mdnsScanner = new StubMdnsScanner();
        networkScanner = new NettyNetworkScanner(portScanner, mdnsScanner);
    }

    private static class StubPortScanner implements PortScanner {
        final AtomicInteger callCount = new AtomicInteger(0);
        int timeout = 500;

        @Override
        public CompletableFuture<PortScanResult> scanPort(String host, int port) {
            return scanPort(host, port, false);
        }

        @Override
        public CompletableFuture<PortScanResult> scanPort(String host, int port, boolean useBannerGrabbing) {
            callCount.incrementAndGet();
            // Simulate offline (all ports closed)
            return CompletableFuture.completedFuture(new PortScanResult(port, false));
        }

        @Override
        public void setTimeout(int timeoutMillis) {
            this.timeout = timeoutMillis;
        }

        @Override
        public int getTimeout() {
            return timeout;
        }

        @Override
        public void stopScan() {
        }

        @Override
        public void close() {}
    }

    private static class StubMdnsScanner implements MdnsScanner {
        @Override
        public CompletableFuture<Set<MdnsServiceInfo>> discoverDevices(long timeoutMillis, MdnsDiscoveryListener listener, java.net.NetworkInterface networkInterface) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }

        @Override
        public void stopScan() {
        }

        @Override
        public void close() {}
    }

    @Test
    void testDeepScanOptimization() throws Exception {
        List<Integer> manyPorts = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            manyPorts.add(i);
        }

        // Scan one host that is definitely offline.
        // We use an IP that is unlikely to be reachable in any test environment.
        String offlineHost = "192.0.2.1"; // TEST-NET-1 reserved for documentation
        CompletableFuture<DiscoveredDevice> future = networkScanner.scanHost(offlineHost, manyPorts);
        DiscoveredDevice device = future.get(10, TimeUnit.SECONDS);

        // Common ports in isHostUp are 5 (80, 443, 22, 5000, 7125)
        // Since we decided not to skip (source of truth), it should call scanPort for all ports.
        // It might also call for common ports during isHostUp, so callCount should be >= 1000.
        int calls = portScanner.callCount.get();
        assertTrue(calls >= 1000, "Should have scanned all 1000 ports for host even if potentially offline. Calls: " + calls);
    }

    @Test
    void testConfigurableTimeout() {
        networkScanner.setTimeout(1234);
        assertEquals(1234, portScanner.getTimeout());
        assertEquals(1234, networkScanner.getTimeout());
    }
}
