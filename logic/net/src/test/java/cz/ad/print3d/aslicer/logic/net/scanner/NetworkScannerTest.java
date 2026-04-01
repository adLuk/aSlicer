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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkScannerTest {

    private StubPortScanner portScanner;
    private NettyNetworkScanner networkScanner;

    @BeforeEach
    void setUp() {
        portScanner = new StubPortScanner();
        networkScanner = new NettyNetworkScanner(portScanner);
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
        
        // Give it a tiny bit of time to start the async loop and create at least one port future
        // Since we are using StubPortScanner with immediate completion (if delayCompletion is false) 
        // or adding to activeFutures, we need to ensure it reached that point.
        int attempts = 0;
        while (portScanner.activeFutures.isEmpty() && attempts < 50) {
            Thread.sleep(10);
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
        // Common ports are closed
        for (int p : normalPorts) {
            portScanner.addResult(host, p, new PortScanResult(p, false));
        }

        // Normal scan
        CompletableFuture<DiscoveredDevice> normalFuture = networkScanner.scanHost(host, normalPorts);
        DiscoveredDevice normalDevice = normalFuture.get();
        assertTrue(normalDevice.getServices().isEmpty(), "Normal scan should not find the rare port");

        // Deep scan
        CompletableFuture<DiscoveredDevice> deepFuture = networkScanner.scanHost(host, allPorts);
        DiscoveredDevice deepDevice = deepFuture.get();
        assertEquals(1, deepDevice.getServices().size(), "Deep scan should find the rare port");
        assertEquals(rarePort, deepDevice.getServices().get(0).getPort());
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
}
