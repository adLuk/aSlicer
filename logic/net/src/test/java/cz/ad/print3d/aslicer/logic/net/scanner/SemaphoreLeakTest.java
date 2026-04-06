package cz.ad.print3d.aslicer.logic.net.scanner;

import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.ScanConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class SemaphoreLeakTest {

    private static class DelegatingPortScanner implements PortScanner {
        volatile PortScanner delegate;
        DelegatingPortScanner(PortScanner initial) { this.delegate = initial; }
        @Override public CompletableFuture<PortScanResult> scanPort(String host, int port) { return delegate.scanPort(host, port); }
        @Override public CompletableFuture<PortScanResult> scanPort(String host, int port, boolean useBanner) { return delegate.scanPort(host, port, useBanner); }
        @Override public void stopScan() { delegate.stopScan(); }
        @Override public void close() { delegate.close(); }
        @Override public void setTimeout(int t) { delegate.setTimeout(t); }
        @Override public int getTimeout() { return delegate.getTimeout(); }
    }

    @Test
    void testSemaphoreLeakAfterCancellation() throws Exception {
        PortScanner stubScanner = new PortScanner() {
            @Override public CompletableFuture<PortScanResult> scanPort(String host, int port) { return new CompletableFuture<>(); }
            @Override public CompletableFuture<PortScanResult> scanPort(String host, int port, boolean useBanner) { return new CompletableFuture<>(); }
            @Override public void stopScan() {}
            @Override public void close() {}
            @Override public void setTimeout(int t) {}
            @Override public int getTimeout() { return 500; }
        };

        DelegatingPortScanner delegator = new DelegatingPortScanner(stubScanner);
        
        // Mock Mdns and Ssdp to return immediately
        MdnsScanner mockMdns = new MdnsScanner() {
            @Override public CompletableFuture<java.util.Set<cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo>> discoverDevices(long t, MdnsDiscoveryListener l, java.net.NetworkInterface ni) { return CompletableFuture.completedFuture(Collections.emptySet()); }
            @Override public void stopScan() {}
            @Override public void close() {}
        };
        SsdpScanner mockSsdp = new SsdpScanner() {
            @Override public CompletableFuture<java.util.Set<cz.ad.print3d.aslicer.logic.net.scanner.dto.SsdpServiceInfo>> discoverDevices(long t, SsdpDiscoveryListener l, java.net.NetworkInterface ni) { return CompletableFuture.completedFuture(Collections.emptySet()); }
            @Override public void stopScan() {}
            @Override public void close() {}
        };
        
        NettyNetworkScanner scanner = new NettyNetworkScanner(delegator, mockMdns, mockSsdp, new cz.ad.print3d.aslicer.logic.net.info.NetworkInformationCollector(), new DeviceEnricher(), new DefaultServiceValidator(), 10);
        
        ScanConfiguration config = new ScanConfiguration(Collections.emptyList(), Collections.singleton(80));

        // Start 20 scans (exceeding 10 limit)
        for (int i = 0; i < 20; i++) {
            scanner.scanHost("192.168.1." + i, config);
        }

        // Wait a bit for them to attempt acquire
        Thread.sleep(500);

        // Cancel all
        scanner.stopScan();

        // Give it time to cancel and release (if it works)
        Thread.sleep(500);

        // Swap to responsive scanner
        AtomicInteger scanCount = new AtomicInteger(0);
        delegator.delegate = new PortScanner() {
            @Override public CompletableFuture<PortScanResult> scanPort(String host, int port) { 
                scanCount.incrementAndGet();
                return CompletableFuture.completedFuture(new PortScanResult(port, true)); 
            }
            @Override public CompletableFuture<PortScanResult> scanPort(String host, int port, boolean useBanner) { return scanPort(host, port); }
            @Override public void stopScan() {}
            @Override public void close() {}
            @Override public void setTimeout(int t) {}
            @Override public int getTimeout() { return 500; }
        };
        
        // Start a new scan. If there was a leak, this will be stuck on acquire() and never call our new delegate.
        CompletableFuture<PortScanResult> secondScan = scanner.scanHost("192.168.2.1", config)
                .thenApply(d -> d.getServices().iterator().next());
        
        try {
            PortScanResult result = secondScan.get(2, TimeUnit.SECONDS);
            assertTrue(result.isOpen());
            assertTrue(scanCount.get() > 0);
        } catch (Exception e) {
            fail("Second scan timed out or failed - likely due to semaphore leak! " + e);
        }
    }
}
