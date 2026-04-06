package cz.ad.print3d.aslicer.logic.net.scanner;

import cz.ad.print3d.aslicer.logic.net.info.NetworkInformationCollector;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.ScanConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeepScanPrioritizationTest {

    private MockPortScanner portScanner;
    private MdnsScanner mdnsScanner;
    private SsdpScanner ssdpScanner;
    private NetworkInformationCollector collector;
    private DiscoveryManager discoveryManager;
    private ScanTracker scanTracker;
    private Semaphore semaphore;

    @BeforeEach
    void setUp() {
        portScanner = new MockPortScanner();
        mdnsScanner = new MockMdnsScanner();
        ssdpScanner = new MockSsdpScanner();
        collector = new NetworkInformationCollector();
        discoveryManager = new DiscoveryManager(mdnsScanner, ssdpScanner);
        scanTracker = new ScanTracker();
        semaphore = new Semaphore(100);
    }

    @Test
    void testDeepScanPrioritization() throws Exception {
        // Range: 192.168.1.1 - 192.168.1.3
        String baseIp = "192.168.1";
        int startHost = 1;
        int endHost = 3;
        
        // Host .2 is "up" (has common port open)
        // Host .1 and .3 have only non-common ports open
        portScanner.addOpenPort("192.168.1.1", 1234);
        portScanner.addOpenPort("192.168.1.2", 80);
        portScanner.addOpenPort("192.168.1.3", 5678);

        // Deep scan config: common ports [80], deepScan = true
        ScanConfiguration config = new ScanConfiguration(Collections.emptyList(), Set.of(80), true);

        RangeScanTask task = new RangeScanTask(baseIp, startHost, endHost, config, false, null,
                portScanner, discoveryManager, new DeviceEnricher(), new DefaultServiceValidator(),
                semaphore, scanTracker, true, 50, 50, collector, java.util.concurrent.Executors.newCachedThreadPool());

        CompletableFuture<List<DiscoveredDevice>> future = task.execute();
        List<DiscoveredDevice> results = future.get(10, TimeUnit.SECONDS);

        // All 3 hosts should be in results because deep scan is "source of truth"
        // and our MockPortScanner returns open/closed for all ports.
        // Wait, results only contain devices with services.
        // MockPortScanner only returns isOpen=true for open ports.
        
        assertEquals(3, results.size(), "Should have found 3 devices");
        
        // Check that all 65535 ports were scanned for all 3 hosts
        // (Actually MockPortScanner records calls, including Phase 1 and isHostUp checks)
        assertTrue(portScanner.getCallCount() >= 3 * 65535, 
                "Should have scanned all ports for all hosts. Expected at least " + (3 * 65535) + " but got " + portScanner.getCallCount());
    }

    @Test
    void testBatchingMemoryEfficiency() throws Exception {
        // This test ensures that even with 65535 ports, we don't crash or hang
        // by verifying that a single host scan completes.
        String host = "127.0.0.1";
        ScanConfiguration config = new ScanConfiguration(Collections.emptyList(), Set.of(80), true);
        
        HostScanTask task = new HostScanTask(host, config, false, null, portScanner, new DefaultServiceValidator(), scanTracker, semaphore, java.util.concurrent.Executors.newCachedThreadPool());
        CompletableFuture<DiscoveredDevice> future = task.execute();
        
        DiscoveredDevice device = future.get(10, TimeUnit.SECONDS);
        assertEquals(65535, portScanner.getCallCount());
    }

    private static class MockPortScanner implements PortScanner {
        private final AtomicInteger callCount = new AtomicInteger(0);
        private final Map<String, Set<Integer>> openPorts = new ConcurrentHashMap<>();

        void addOpenPort(String host, int port) {
            openPorts.computeIfAbsent(host, k -> ConcurrentHashMap.newKeySet()).add(port);
        }

        @Override public CompletableFuture<PortScanResult> scanPort(String host, int port) { return scanPort(host, port, false); }
        @Override public CompletableFuture<PortScanResult> scanPort(String host, int port, boolean useBannerGrabbing) {
            callCount.incrementAndGet();
            boolean isOpen = openPorts.containsKey(host) && openPorts.get(host).contains(port);
            return CompletableFuture.completedFuture(new PortScanResult(port, isOpen));
        }
        @Override public void setTimeout(int timeoutMillis) {}
        @Override public int getTimeout() { return 100; }
        @Override public void stopScan() {}
        @Override public void close() {}
        int getCallCount() { return callCount.get(); }
    }

    private static class MockMdnsScanner implements MdnsScanner {
        @Override public CompletableFuture<Set<cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo>> discoverDevices(long t, MdnsDiscoveryListener l, java.net.NetworkInterface ni) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }
        @Override public void stopScan() {}
        @Override public void close() {}
    }

    private static class MockSsdpScanner implements SsdpScanner {
        @Override public CompletableFuture<Set<cz.ad.print3d.aslicer.logic.net.scanner.dto.SsdpServiceInfo>> discoverDevices(long t, SsdpDiscoveryListener l, java.net.NetworkInterface ni) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }
        @Override public void stopScan() {}
        @Override public void close() {}
    }
}
