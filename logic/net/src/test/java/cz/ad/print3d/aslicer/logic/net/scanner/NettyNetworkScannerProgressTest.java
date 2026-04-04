package cz.ad.print3d.aslicer.logic.net.scanner;

import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.SsdpServiceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for {@link NettyNetworkScanner} progress reporting.
 * <p>Specifically verifies that the progress percentage doesn't get stuck during 
 * range scans when hosts are determined to be offline (deep scan optimization).</p>
 */
class NettyNetworkScannerProgressTest {

    private StubPortScanner portScanner;
    private StubMdnsScanner mdnsScanner;
    private StubSsdpScanner ssdpScanner;
    private NettyNetworkScanner networkScanner;

    @BeforeEach
    void setUp() {
        portScanner = new StubPortScanner();
        mdnsScanner = new StubMdnsScanner();
        ssdpScanner = new StubSsdpScanner();
        networkScanner = new NettyNetworkScanner(portScanner, mdnsScanner, ssdpScanner);
    }

    private static class StubSsdpScanner implements SsdpScanner {
        @Override
        public CompletableFuture<Set<SsdpServiceInfo>> discoverDevices(long timeoutMillis, SsdpDiscoveryListener listener, java.net.NetworkInterface networkInterface) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }

        @Override
        public void stopScan() {
        }

        @Override
        public void close() {}
    }

    private static class StubPortScanner implements PortScanner {
        int timeout = 500;

        @Override
        public CompletableFuture<PortScanResult> scanPort(String host, int port) {
            return scanPort(host, port, false);
        }

        @Override
        public CompletableFuture<PortScanResult> scanPort(String host, int port, boolean useBannerGrabbing) {
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

    /**
     * Verifies that scanRange correctly calculates progress when offline hosts are skipped.
     * <p>If multiple hosts are skipped due to deep scan optimization, the progress
     * must reflect all ports of those hosts being accounted for, not just one increment
     * per skipped host.</p>
     */
    @Test
    void testProgressWithOfflineHostsInRange() throws Exception {
        List<Integer> manyPorts = new ArrayList<>();
        // Needs to be > DEEP_SCAN_THRESHOLD (100)
        for (int i = 1; i <= 200; i++) {
            manyPorts.add(i);
        }

        // 3 hosts, 200 ports each = 600 total ports
        int startHost = 1;
        int endHost = 3;
        String baseIp = "192.0.2.";

        List<Double> progressUpdates = Collections.synchronizedList(new ArrayList<>());

        CompletableFuture<List<DiscoveredDevice>> future = networkScanner.scanRange(baseIp, startHost, endHost, manyPorts, false, new NetworkScanner.ScanProgressListener() {
            @Override
            public void onProgress(double progress, String currentIp) {
                progressUpdates.add(progress);
            }

            @Override
            public void onDeviceDiscovered(DiscoveredDevice device) {}
        });

        future.get(10, TimeUnit.SECONDS);

        // Analyze progress updates.
        // We are interested in the last progress update BEFORE the final 1.0 that is manually set.
        // If it's correctly calculating progress, it should be close to 1.0 before the final manual set.
        // Total ports = 600.
        // 1st host completes (skipped) -> reports 1.0 to nested listener.
        // Range listener increments global completedPorts by 200.
        // Progress = 200 / 600 = 0.33.
        // 2nd host completes (skipped) -> reports 1.0 to nested listener.
        // Range listener increments global completedPorts by 200.
        // Progress = 400 / 600 = 0.66.
        // 3rd host completes (skipped) -> reports 1.0 to nested listener.
        // Range listener increments global completedPorts by 200.
        // Progress = 600 / 600 = 1.0.
        // Then scanRange finishes and calls onProgress(1.0, ...).

        double maxProgressBeforeFinish = 0;
        synchronized (progressUpdates) {
            for (Double p : progressUpdates) {
                if (p < 1.0) {
                    maxProgressBeforeFinish = Math.max(maxProgressBeforeFinish, p);
                }
            }
        }

        // Before fix, it should be very small (around 2/600 = 0.0033)
        // After fix, it should be at least 0.33 or 0.66.
        
        assertTrue(maxProgressBeforeFinish > 0.1, "Progress was stuck! Max intermediate progress: " + maxProgressBeforeFinish + ", updates: " + progressUpdates);
    }
}
