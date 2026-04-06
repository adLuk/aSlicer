/*
 * aSlicer - 3D model processing tool.
 * Copyright (C) 2026 cz.ad.print3d.aslicer contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.ad.print3d.aslicer.logic.net.scanner;

import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.ScanConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for {@link DeviceDiscoveryService}.
 * <p>Verifies that the discovery service correctly delegates to {@link NetworkScanner}
 * with appropriate printer-specific ports and settings.</p>
 */
class DeviceDiscoveryServiceTest {

    private StubNetworkScanner networkScanner;
    private DeviceDiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        networkScanner = new StubNetworkScanner();
        discoveryService = new DeviceDiscoveryService(networkScanner);
    }

    private static class StubNetworkScanner implements NetworkScanner {
        private String lastBaseIp;
        private List<Integer> lastPorts;
        private boolean lastUseBannerGrabbing;
        private List<DiscoveredDevice> resultToReturn = new ArrayList<>();

        @Override
        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports) {
            return scanRange(baseIp, startHost, endHost, ports, false);
        }

        @Override
        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing) {
            return scanRange(baseIp, startHost, endHost, ports, useBannerGrabbing, null);
        }

        @Override
        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener) {
            this.lastBaseIp = baseIp;
            this.lastPorts = ports;
            this.lastUseBannerGrabbing = useBannerGrabbing;
            if (listener != null) {
                listener.onProgress(1.0, baseIp + endHost);
            }
            return CompletableFuture.completedFuture(resultToReturn);
        }

        @Override
        public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports) {
            return scanHost(host, ports, false);
        }

        @Override
        public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports, boolean useBannerGrabbing) {
            return scanHost(host, ports, useBannerGrabbing, null);
        }

        @Override
        public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener) {
            if (listener != null) {
                listener.onProgress(1.0, host);
            }
            return CompletableFuture.completedFuture(new DiscoveredDevice(host));
        }

        @Override
        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, ScanConfiguration config) {
            return scanRange(baseIp, startHost, endHost, config, false);
        }

        @Override
        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, ScanConfiguration config, boolean useBannerGrabbing) {
            return scanRange(baseIp, startHost, endHost, config, useBannerGrabbing, null);
        }

        @Override
        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, ScanConfiguration config, boolean useBannerGrabbing, ScanProgressListener listener) {
            this.lastBaseIp = baseIp;
            this.lastPorts = new ArrayList<>(config.getAllPorts());
            this.lastUseBannerGrabbing = useBannerGrabbing;
            if (listener != null) {
                listener.onProgress(1.0, baseIp + endHost);
            }
            return CompletableFuture.completedFuture(resultToReturn);
        }

        @Override
        public CompletableFuture<DiscoveredDevice> scanHost(String host, ScanConfiguration config) {
            return scanHost(host, config, false);
        }

        @Override
        public CompletableFuture<DiscoveredDevice> scanHost(String host, ScanConfiguration config, boolean useBannerGrabbing) {
            return scanHost(host, config, useBannerGrabbing, null);
        }

        @Override
        public CompletableFuture<DiscoveredDevice> scanHost(String host, ScanConfiguration config, boolean useBannerGrabbing, ScanProgressListener listener) {
            if (listener != null) {
                listener.onProgress(1.0, host);
            }
            return CompletableFuture.completedFuture(new DiscoveredDevice(host));
        }

        @Override
        public void setTimeout(int timeoutMillis) {}

        @Override
        public int getTimeout() { return 500; }

        @Override
        public void setIncludeSelfIp(boolean include) {}

        @Override
        public boolean isIncludeSelfIp() { return false; }

        @Override
        public void setMdnsTimeout(int timeoutMillis) {}

        @Override
        public int getMdnsTimeout() { return 1500; }

        @Override
        public void setSsdpTimeout(int timeoutMillis) {}

        @Override
        public int getSsdpTimeout() { return 1500; }

        @Override
        public void stopScan() {
            // No-op for stub
        }

        @Override
        public void close() {}
    }

    @Test
    void testDiscoverPrinters() throws ExecutionException, InterruptedException {
        DiscoveredDevice device = new DiscoveredDevice("192.168.1.10");
        device.addService(new PortScanResult(80, true));
        networkScanner.resultToReturn.add(device);

        CompletableFuture<List<DiscoveredDevice>> future = discoveryService.discoverPrinters("192.168.1.", 1, 254);
        List<DiscoveredDevice> result = future.get();

        assertEquals(1, result.size());
        assertEquals("192.168.1.10", result.get(0).getIpAddress());
        assertEquals("192.168.1.", networkScanner.lastBaseIp);
        // Verify common printer ports were used
        assertTrue(networkScanner.lastPorts.contains(80));
        assertTrue(networkScanner.lastPorts.contains(8883));
    }

    @Test
    void testDiscoverPrintersWithBannerGrabbing() throws ExecutionException, InterruptedException {
        discoveryService.discoverPrinters("192.168.1.", 1, 254, true);
        assertTrue(networkScanner.lastUseBannerGrabbing);
    }
}
