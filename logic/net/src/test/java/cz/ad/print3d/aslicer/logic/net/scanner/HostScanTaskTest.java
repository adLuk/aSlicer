package cz.ad.print3d.aslicer.logic.net.scanner;

import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.ScanConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HostScanTaskTest {

    @Test
    void testDeepScanDoesNotSkipHostWhenIsHostUpFails() throws Exception {
        // Mocking isReachable is hard, but we can simulate it by letting it fail (default)
        // and making sure all "common ports" used by isHostUp also fail.
        
        String host = "192.168.1.100";
        // Only port 8883 is open (not in common ports: 80, 443, 22, 5000, 7125)
        PortScanner stubScanner = new PortScanner() {
            @Override public CompletableFuture<PortScanResult> scanPort(String h, int p) { return scanPort(h, p, false); }
            @Override public CompletableFuture<PortScanResult> scanPort(String h, int p, boolean useBannerGrabbing) {
                return CompletableFuture.completedFuture(new PortScanResult(p, p == 8883));
            }
            @Override public void setTimeout(int timeoutMillis) {}
            @Override public int getTimeout() { return 500; }
            @Override public void stopScan() {}
            @Override public void close() {}
        };

        ScanConfiguration config = new ScanConfiguration(Collections.emptyList(), Set.of(80, 443, 8883));
        ScanTracker tracker = new ScanTracker();
        java.util.concurrent.Semaphore semaphore = new java.util.concurrent.Semaphore(100);
        
        // Use a threshold lower than our port count to trigger isHostUp
        HostScanTask task = new HostScanTask(host, config, false, null, stubScanner, new DefaultServiceValidator(), new DeviceEnricher(), tracker, semaphore, Runnable::run);
        task.setDeepScanThreshold(1); // Force isHostUp
        
        CompletableFuture<DiscoveredDevice> future = task.execute();
        DiscoveredDevice device = future.get(5, TimeUnit.SECONDS);
        
        assertFalse(device.getServices().isEmpty(), "Device should have services even if isHostUp fails");
        assertTrue(device.getServices().stream().anyMatch(s -> s.getPort() == 8883), "Port 8883 should be discovered");
    }

    @Test
    void testIsHostUpAddsDiscoveredPortsToDevice() throws Exception {
        String host = "192.168.1.101";
        // Port 80 (a common port) is open, but it's NOT in the main config
        PortScanner stubScanner = new PortScanner() {
            @Override public CompletableFuture<PortScanResult> scanPort(String h, int p) { return scanPort(h, p, false); }
            @Override public CompletableFuture<PortScanResult> scanPort(String h, int p, boolean useBannerGrabbing) {
                return CompletableFuture.completedFuture(new PortScanResult(p, p == 80));
            }
            @Override public void setTimeout(int timeoutMillis) {}
            @Override public int getTimeout() { return 100; }
            @Override public void stopScan() {}
            @Override public void close() {}
        };

        // Main config only wants port 9100
        ScanConfiguration config = new ScanConfiguration(Collections.emptyList(), Set.of(9100));
        ScanTracker tracker = new ScanTracker();
        
        HostScanTask task = new HostScanTask(host, config, false, null, stubScanner, new DefaultServiceValidator(), new DeviceEnricher(), tracker, new java.util.concurrent.Semaphore(10), Runnable::run) {
            @Override
            protected boolean checkReachable(String host, int timeout) {
                return false; // Force fallback port scan
            }
        };
        task.setDeepScanThreshold(0); // Force isHostUp
        
        DiscoveredDevice device = task.execute().get(2, TimeUnit.SECONDS);
        
        // Port 80 should be added to the device because it was discovered during isHostUp
        assertTrue(device.getServices().stream().anyMatch(s -> s.getPort() == 80), "Port 80 found during isHostUp should be added to device");
    }

    @Test
    void testFastScanSkipsIsHostUp() throws Exception {
        String host = "192.168.1.102";
        
        // Counter for isReachable (checkReachable)
        final java.util.concurrent.atomic.AtomicInteger reachableChecks = new java.util.concurrent.atomic.AtomicInteger(0);
        
        PortScanner stubScanner = new PortScanner() {
            @Override public CompletableFuture<PortScanResult> scanPort(String h, int p) { return scanPort(h, p, false); }
            @Override public CompletableFuture<PortScanResult> scanPort(String h, int p, boolean useBannerGrabbing) {
                return CompletableFuture.completedFuture(new PortScanResult(p, true));
            }
            @Override public void setTimeout(int timeoutMillis) {}
            @Override public int getTimeout() { return 100; }
            @Override public void stopScan() {}
            @Override public void close() {}
        };

        ScanConfiguration config = new ScanConfiguration(Collections.emptyList(), Set.of(9100));
        ScanTracker tracker = new ScanTracker();
        
        HostScanTask task = new HostScanTask(host, config, false, null, stubScanner, new DefaultServiceValidator(), new DeviceEnricher(), tracker, new java.util.concurrent.Semaphore(10), Runnable::run) {
            @Override
            protected boolean checkReachable(String host, int timeout) {
                reachableChecks.incrementAndGet();
                return false;
            }
        };
        
        // Threshold is 100, we only have 1 port. It should skip isHostUp.
        task.setDeepScanThreshold(100);
        
        task.execute().get(2, TimeUnit.SECONDS);
        
        assertTrue(reachableChecks.get() == 0, "isHostUp should be skipped for fast scan (few ports)");
    }

    @Test
    void testOnDeviceUpdatedTriggered() throws Exception {
        String host = "192.168.1.103";
        final java.util.concurrent.atomic.AtomicInteger updateCount = new java.util.concurrent.atomic.AtomicInteger(0);
        
        NetworkScanner.ScanProgressListener listener = new NetworkScanner.ScanProgressListener() {
            @Override public void onProgress(double progress, String currentIp) {}
            @Override public void onDeviceUpdated(DiscoveredDevice device) {
                updateCount.incrementAndGet();
            }
        };

        PortScanner stubScanner = new PortScanner() {
            @Override public CompletableFuture<PortScanResult> scanPort(String h, int p) { return scanPort(h, p, false); }
            @Override public CompletableFuture<PortScanResult> scanPort(String h, int p, boolean useBannerGrabbing) {
                return CompletableFuture.completedFuture(new PortScanResult(p, true));
            }
            @Override public void setTimeout(int timeoutMillis) {}
            @Override public int getTimeout() { return 100; }
            @Override public void stopScan() {}
            @Override public void close() {}
        };

        ScanConfiguration config = new ScanConfiguration(Collections.emptyList(), Set.of(9100, 9101));
        ScanTracker tracker = new ScanTracker();
        
        HostScanTask task = new HostScanTask(host, config, false, listener, stubScanner, new DefaultServiceValidator(), new DeviceEnricher(), tracker, new java.util.concurrent.Semaphore(10), Runnable::run);
        
        task.execute().get(2, TimeUnit.SECONDS);
        
        // Should be at least 2 updates (one for each open port)
        assertTrue(updateCount.get() >= 2, "onDeviceUpdated should be called for each discovered port");
    }
}
