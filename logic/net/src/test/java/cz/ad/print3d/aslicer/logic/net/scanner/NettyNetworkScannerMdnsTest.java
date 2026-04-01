package cz.ad.print3d.aslicer.logic.net.scanner;

import cz.ad.print3d.aslicer.logic.net.info.NetworkInformationCollector;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for {@link NettyNetworkScanner} with mDNS prioritization.
 * <p>Verifies that devices discovered via mDNS are prioritized in the port scan order
 * during a range scan.</p>
 */
class NettyNetworkScannerMdnsTest {

    private StubPortScanner portScanner;
    private StubMdnsScanner mdnsScanner;
    private NettyNetworkScanner networkScanner;

    @BeforeEach
    void setUp() {
        portScanner = new StubPortScanner();
        mdnsScanner = new StubMdnsScanner();
        NetworkInformationCollector mockCollector = new NetworkInformationCollector() {
            @Override
            public List<cz.ad.print3d.aslicer.logic.net.info.NetworkInterfaceInfo> collect() {
                return Collections.emptyList();
            }
        };
        networkScanner = new NettyNetworkScanner(portScanner, mdnsScanner, mockCollector);
    }

    private static class StubPortScanner implements PortScanner {
        final List<String> scanOrder = new CopyOnWriteArrayList<>();

        @Override
        public CompletableFuture<PortScanResult> scanPort(String host, int port) {
            return scanPort(host, port, false);
        }

        @Override
        public CompletableFuture<PortScanResult> scanPort(String host, int port, boolean useBannerGrabbing) {
            scanOrder.add(host);
            return CompletableFuture.completedFuture(new PortScanResult(port, false));
        }

        @Override
        public void setTimeout(int timeoutMillis) {}

        @Override
        public int getTimeout() { return 500; }

        @Override
        public void close() {}
    }

    private static class StubMdnsScanner implements MdnsScanner {
        Set<MdnsServiceInfo> discoveredServices = Set.of();

        @Override
        public CompletableFuture<Set<MdnsServiceInfo>> discoverDevices(long timeoutMillis) {
            return CompletableFuture.completedFuture(discoveredServices);
        }

        @Override
        public void close() {}
    }

    @Test
    void testMdnsPrioritization() throws Exception {
        String baseIp = "192.168.1.";
        List<Integer> ports = List.of(80);
        
        // mDNS finds 192.168.1.50
        mdnsScanner.discoveredServices = Set.of(new MdnsServiceInfo("TestPrinter", "_http._tcp.local.", "192.168.1.50", 80, "printer.local", Collections.emptyMap()));
        
        // Scan range 1 to 50
        CompletableFuture<List<DiscoveredDevice>> future = networkScanner.scanRange(baseIp, 1, 50, ports);
        future.get(5, TimeUnit.SECONDS);

        // The first IP in scanOrder should be 192.168.1.50 because it was found by mDNS
        assertTrue(portScanner.scanOrder.size() > 0);
        assertEquals("192.168.1.50", portScanner.scanOrder.get(0), "Prioritized IP should be scanned first");
    }
}
