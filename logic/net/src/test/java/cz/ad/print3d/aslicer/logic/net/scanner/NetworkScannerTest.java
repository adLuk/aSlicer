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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

        void addResult(String host, int port, PortScanResult result) {
            results.computeIfAbsent(host, k -> new HashMap<>()).put(port, result);
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
}
