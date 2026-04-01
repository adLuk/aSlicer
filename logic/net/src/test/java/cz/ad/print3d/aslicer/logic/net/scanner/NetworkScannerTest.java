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

import cz.ad.print3d.aslicer.logic.net.info.NetworkAddressInfo;
import cz.ad.print3d.aslicer.logic.net.info.NetworkInformationCollector;
import cz.ad.print3d.aslicer.logic.net.info.NetworkInterfaceInfo;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for {@link NettyNetworkScanner}.
 * <p>Verifies range scanning, progress reporting, and scan cancellation
 * using a stubbed {@link PortScanner}.</p>
 */
class NetworkScannerTest {

    private StubPortScanner portScanner;
    private NettyNetworkScanner networkScanner;

    @BeforeEach
    void setUp() {
        portScanner = new StubPortScanner();
        // Use a mock MdnsScanner that returns immediately to avoid delays in tests
        MdnsScanner mockMdns = new MdnsScanner() {
            @Override
            public CompletableFuture<Set<MdnsServiceInfo>> discoverDevices(long timeoutMillis, MdnsDiscoveryListener listener, java.net.NetworkInterface networkInterface) {
                return CompletableFuture.completedFuture(Collections.emptySet());
            }

            @Override
            public void close() {}
        };
        // Use a mock collector that returns empty list immediately
        NetworkInformationCollector mockCollector = new NetworkInformationCollector() {
            @Override
            public List<NetworkInterfaceInfo> collect() {
                return Collections.emptyList();
            }
        };
        networkScanner = new NettyNetworkScanner(portScanner, mockMdns, mockCollector);
    }

    private static class StubPortScanner implements PortScanner {
        private final Map<String, Map<Integer, PortScanResult>> results = new HashMap<>();
        private boolean delayCompletion = false;
        private final List<CompletableFuture<PortScanResult>> activeFutures = new CopyOnWriteArrayList<>();

        void addResult(String host, int port, PortScanResult result) {
            results.computeIfAbsent(host, k -> new HashMap<>()).put(port, result);
        }

        void setDelayCompletion(boolean delay) {
            this.delayCompletion = delay;
        }

        @Override
        public CompletableFuture<PortScanResult> scanPort(String host, int port) {
            return scanPort(host, port, false);
        }

        @Override
        public CompletableFuture<PortScanResult> scanPort(String host, int port, boolean useBannerGrabbing) {
            PortScanResult result = results.getOrDefault(host, Map.of()).get(port);
            if (result == null) {
                result = new PortScanResult(port, false);
            }
            if (delayCompletion) {
                CompletableFuture<PortScanResult> future = new CompletableFuture<>();
                activeFutures.add(future);
                return future;
            }
            return CompletableFuture.completedFuture(result);
        }

        @Override
        public void setTimeout(int timeoutMillis) {}

        @Override
        public int getTimeout() { return 500; }

        @Override
        public void close() {}
    }

    @Test
    void testScanRange() throws ExecutionException, InterruptedException {
        String baseIp = "192.168.1.";
        List<Integer> ports = Arrays.asList(80, 443);

        // Setup stub results
        portScanner.addResult("192.168.1.1", 80, new PortScanResult(80, true));
        portScanner.addResult("192.168.1.1", 443, new PortScanResult(443, false));
        
        // 192.168.1.2 defaults to closed

        portScanner.addResult("192.168.1.3", 80, new PortScanResult(80, false));
        portScanner.addResult("192.168.1.3", 443, new PortScanResult(443, true));

        CompletableFuture<List<DiscoveredDevice>> future = networkScanner.scanRange(baseIp, 1, 3, ports);
        List<DiscoveredDevice> result = future.get();

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(d -> d.getIpAddress().equals("192.168.1.1") && d.getServices().size() == 1));
        assertTrue(result.stream().anyMatch(d -> d.getIpAddress().equals("192.168.1.3") && d.getServices().size() == 1));
    }

    @Test
    void testScanProgress() throws Exception {
        String baseIp = "192.168.1.";
        List<Integer> ports = Arrays.asList(80);
        int startHost = 1;
        int endHost = 10;
        int totalHosts = endHost - startHost + 1;

        java.util.concurrent.atomic.AtomicInteger callCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.CopyOnWriteArrayList<Double> progressValues = new java.util.concurrent.CopyOnWriteArrayList<>();

        CompletableFuture<List<DiscoveredDevice>> future = networkScanner.scanRange(baseIp, startHost, endHost, ports, false, (progress, currentIp) -> {
            callCount.incrementAndGet();
            progressValues.add(progress);
        });

        future.get(5, TimeUnit.SECONDS);

        assertTrue(callCount.get() >= 1, "Should have at least one progress update");
        assertTrue(progressValues.stream().anyMatch(v -> Math.abs(v - 1.0) < 0.001), "Should reach 1.0 progress");
    }

    @Test
    void testStopScan() throws Exception {
        String baseIp = "192.168.1.";
        List<Integer> ports = Arrays.asList(80);

        portScanner.setDelayCompletion(true);

        CompletableFuture<List<DiscoveredDevice>> future = networkScanner.scanRange(baseIp, 1, 10, ports);
        
        // Give it a bit more time to start the async loop and create at least one port future
        int attempts = 0;
        while (portScanner.activeFutures.isEmpty() && attempts < 100) {
            Thread.sleep(20);
            attempts++;
        }

        networkScanner.stopScan();

        // future should be either cancelled or failed with cancellation
        assertTrue(future.isCompletedExceptionally(), "Future should be completed exceptionally");
        // We might not have 10 because it might have been stopped mid-loop
        assertTrue(portScanner.activeFutures.size() > 0, "Should have at least some active futures");
        assertTrue(portScanner.activeFutures.stream().allMatch(CompletableFuture::isCancelled), "All port futures should be cancelled");
    }

    @Test
    void testDeepScanVsNormalScan() throws Exception {
        String host = "192.168.1.10";
        List<Integer> normalPorts = Arrays.asList(80, 443, 8080);
        List<Integer> allPorts = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) allPorts.add(i); // Simulating deep scan with 1000 ports

        // Device only has a service on a non-common port
        int rarePort = 999;
        portScanner.addResult(host, rarePort, new PortScanResult(rarePort, true));
        // Keep port 80 open so the host is considered UP for deep scan pre-check
        portScanner.addResult(host, 80, new PortScanResult(80, true));
        // Common ports (except 80) are closed
        for (int p : normalPorts) {
            if (p != 80) {
                portScanner.addResult(host, p, new PortScanResult(p, false));
            }
        }

        // Normal scan
        CompletableFuture<DiscoveredDevice> normalFuture = networkScanner.scanHost(host, normalPorts);
        DiscoveredDevice normalDevice = normalFuture.get();
        assertEquals(1, normalDevice.getServices().size(), "Normal scan should find port 80");
        assertEquals(80, normalDevice.getServices().get(0).getPort());

        // Deep scan
        CompletableFuture<DiscoveredDevice> deepFuture = networkScanner.scanHost(host, allPorts);
        DiscoveredDevice deepDevice = deepFuture.get();
        assertEquals(2, deepDevice.getServices().size(), "Deep scan should find port 80 and the rare port");
        assertTrue(deepDevice.getServices().stream().anyMatch(s -> s.getPort() == rarePort));
    }

    @Test
    void testRealTimeDiscoveredDevice() throws Exception {
        String baseIp = "192.168.1.";
        List<Integer> ports = Arrays.asList(80);
        
        portScanner.addResult("192.168.1.5", 80, new PortScanResult(80, true));
        
        List<DiscoveredDevice> discoveredRealTime = new CopyOnWriteArrayList<>();
        
        CompletableFuture<List<DiscoveredDevice>> future = networkScanner.scanRange(baseIp, 1, 10, ports, false, new NetworkScanner.ScanProgressListener() {
            @Override
            public void onProgress(double progress, String currentIp) {}

            @Override
            public void onDeviceDiscovered(DiscoveredDevice device) {
                discoveredRealTime.add(device);
            }
        });
        
        future.get(5, TimeUnit.SECONDS);
        
        assertEquals(1, discoveredRealTime.size(), "Should have discovered one device in real-time");
        assertEquals("192.168.1.5", discoveredRealTime.get(0).getIpAddress());
    }

    @Test
    void testExcludeSelfIp() throws Exception {
        String baseIp = "192.168.1.";
        List<Integer> ports = Arrays.asList(80);
        String selfIp = "192.168.1.5";

        // Mock collector to return selfIp
        NetworkAddressInfo addrInfo = new NetworkAddressInfo(selfIp, "localhost", true, 24);
        NetworkInterfaceInfo niInfo = new NetworkInterfaceInfo("eth0", "Ethernet", null, List.of(addrInfo), false, true, false);
        
        NetworkInformationCollector mockCollector = new NetworkInformationCollector() {
            @Override
            public List<NetworkInterfaceInfo> collect() {
                return List.of(niInfo);
            }
        };

        // Create scanner with mock collector
        NettyNetworkScanner scanner = new NettyNetworkScanner(portScanner, new NettyMdnsScanner(), mockCollector);
        
        // Ensure static cache is bypassed by our scanner which will call collector.collect() if cachedInfo is null.
        // We can't easily clear the static cache, but we can ensure it's not used by clearing it if possible or 
        // by the fact that we're providing a custom collector that will be called if needed.
        
        // By default, self IP should be excluded
        scanner.setIncludeSelfIp(false);
        
        portScanner.addResult("192.168.1.5", 80, new PortScanResult(80, true));
        portScanner.addResult("192.168.1.6", 80, new PortScanResult(80, true));

        CompletableFuture<List<DiscoveredDevice>> future = scanner.scanRange(baseIp, 1, 10, ports);
        List<DiscoveredDevice> result = future.get();

        // 192.168.1.5 should be excluded
        assertFalse(result.stream().anyMatch(d -> d.getIpAddress().equals(selfIp)), "Self IP should be excluded");
        assertTrue(result.stream().anyMatch(d -> d.getIpAddress().equals("192.168.1.6")), "Other IP should be included");

        // Now include self IP
        scanner.setIncludeSelfIp(true);
        future = scanner.scanRange(baseIp, 1, 10, ports);
        result = future.get();

        assertTrue(result.stream().anyMatch(d -> d.getIpAddress().equals(selfIp)), "Self IP should be included when set to true");
    }
}
