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
        HostScanTask task = new HostScanTask(host, config, false, null, stubScanner, new DefaultServiceValidator(), tracker, semaphore, Runnable::run);
        task.setDeepScanThreshold(1); // Force isHostUp
        
        CompletableFuture<DiscoveredDevice> future = task.execute();
        DiscoveredDevice device = future.get(5, TimeUnit.SECONDS);
        
        // Currently this will FAIL (it will be empty because isHostUp will return false)
        // We want it to be TRUE (port 8883 should be found)
        assertFalse(device.getServices().isEmpty(), "Device should have services even if isHostUp fails");
        assertTrue(device.getServices().stream().anyMatch(s -> s.getPort() == 8883), "Port 8883 should be discovered");
    }
}
