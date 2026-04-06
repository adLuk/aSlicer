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

import cz.ad.print3d.aslicer.logic.net.scanner.dto.*;
import org.junit.jupiter.api.Test;

import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for 3D printer detection based on port banners and configuration profiles.
 */
public class PrinterDetectionTest {

    private static class StubPortScanner implements PortScanner {
        private final Map<Integer, PortScanResult> results = new HashMap<>();
        private final Map<String, Map<Integer, PortScanResult>> hostResults = new HashMap<>();

        void addResult(int port, PortScanResult result) {
            results.put(port, result);
        }

        void addHostResult(String host, int port, PortScanResult result) {
            hostResults.computeIfAbsent(host, k -> new HashMap<>()).put(port, result);
        }

        @Override
        public CompletableFuture<PortScanResult> scanPort(String host, int port) {
            return scanPort(host, port, false);
        }

        @Override
        public CompletableFuture<PortScanResult> scanPort(String host, int port, boolean useBannerGrabbing) {
            PortScanResult result = hostResults.getOrDefault(host, Collections.emptyMap()).get(port);
            if (result == null) {
                result = results.getOrDefault(port, new PortScanResult(port, false));
            }
            return CompletableFuture.completedFuture(result);
        }

        @Override
        public void setTimeout(int timeoutMillis) {}

        @Override
        public int getTimeout() { return 500; }

        @Override
        public void stopScan() {}

        @Override
        public void close() {}
    }

    private static class StubMdnsScanner implements MdnsScanner {
        @Override
        public CompletableFuture<Set<MdnsServiceInfo>> discoverDevices(long timeoutMillis, MdnsDiscoveryListener listener, NetworkInterface networkInterface) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }

        @Override
        public void stopScan() {}

        @Override
        public void close() {}
    }

    private static class StubSsdpScanner implements SsdpScanner {
        @Override
        public CompletableFuture<Set<SsdpServiceInfo>> discoverDevices(long timeoutMillis, SsdpDiscoveryListener listener, NetworkInterface networkInterface) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }

        @Override
        public void stopScan() {}

        @Override
        public void close() {}
    }

    @Test
    void testDetectPrinterByBanner() throws Exception {
        // 1. Setup stubs
        StubPortScanner stubPortScanner = new StubPortScanner();
        StubMdnsScanner stubMdnsScanner = new StubMdnsScanner();
        StubSsdpScanner stubSsdpScanner = new StubSsdpScanner();

        // Mock a successful port scan with a banner
        stubPortScanner.addResult(80, new PortScanResult(80, true, "HTTP", "OctoPrint 1.9.0"));

        // 2. Setup Configuration
        PortDiscoveryConfig octoPrintPort = new PortDiscoveryConfig(80, "OctoPrint Web", Pattern.compile("OctoPrint"));
        PrinterDiscoveryProfile octoPrintProfile = new PrinterDiscoveryProfile("OctoPrint", Set.of(octoPrintPort));
        ScanConfiguration config = new ScanConfiguration(List.of(octoPrintProfile), Collections.emptySet());

        // 3. Initialize Scanner
        NettyNetworkScanner scanner = new NettyNetworkScanner(stubPortScanner, stubMdnsScanner, stubSsdpScanner);

        // 4. Execute Scan
        DiscoveredDevice device = scanner.scanHost("192.168.1.100", config, true).get();

        // 5. Verify results
        assertEquals("192.168.1.100", device.getIpAddress());
        assertEquals(1, device.getServices().size());
        
        PortScanResult service = device.getServices().get(0);
        assertEquals(80, service.getPort());
        assertEquals("OctoPrint Web", service.getService());
        assertEquals("OctoPrint 1.9.0", service.getServiceDetails());
    }

    @Test
    void testDetectPrinterByMultiplePorts() throws Exception {
        StubPortScanner stubPortScanner = new StubPortScanner();
        StubMdnsScanner stubMdnsScanner = new StubMdnsScanner();
        StubSsdpScanner stubSsdpScanner = new StubSsdpScanner();

        // Mock multiple open ports
        stubPortScanner.addResult(80, new PortScanResult(80, true, "HTTP", "OctoPrint"));
        stubPortScanner.addResult(5000, new PortScanResult(5000, true, "GCode", "Marlin 2.0"));
        
        PortDiscoveryConfig port80 = new PortDiscoveryConfig(80, "OctoPrint UI", Pattern.compile("OctoPrint"));
        PortDiscoveryConfig port5000 = new PortDiscoveryConfig(5000, "GCode API", Pattern.compile("Marlin"));
        PrinterDiscoveryProfile octoPrintProfile = new PrinterDiscoveryProfile("OctoPrint", Set.of(port80, port5000));
        
        ScanConfiguration config = new ScanConfiguration(List.of(octoPrintProfile), Collections.emptySet());

        NettyNetworkScanner scanner = new NettyNetworkScanner(stubPortScanner, stubMdnsScanner, stubSsdpScanner);
        DiscoveredDevice device = scanner.scanHost("192.168.1.100", config, true).get();

        assertEquals(2, device.getServices().size());
        assertTrue(device.getServices().stream().anyMatch(s -> s.getService().equals("OctoPrint UI")));
        assertTrue(device.getServices().stream().anyMatch(s -> s.getService().equals("GCode API")));
    }

    /**
     * Verifies that a generic device (like a router) with port 80 and 443 open
     * is NOT incorrectly identified as a Prusa printer when the banner doesn't match.
     */
    @Test
    void testPrusaFalsePositiveRouter() throws Exception {
        StubPortScanner stubPortScanner = new StubPortScanner();
        StubMdnsScanner stubMdnsScanner = new StubMdnsScanner();
        StubSsdpScanner stubSsdpScanner = new StubSsdpScanner();

        // Router with port 80 and 443 open, but NO Prusa banner
        stubPortScanner.addHostResult("192.168.1.1", 80, new PortScanResult(80, true, "HTTP", "TP-Link Router"));
        stubPortScanner.addHostResult("192.168.1.1", 443, new PortScanResult(443, true, "HTTPS", "TP-Link Router (Secure)"));

        ScanConfiguration config = ScanConfigurationLoader.loadDefault();
        NettyNetworkScanner scanner = new NettyNetworkScanner(stubPortScanner, stubMdnsScanner, stubSsdpScanner);

        DiscoveredDevice routerDevice = scanner.scanHost("192.168.1.1", config, true).get();

        // Should NOT be identified as Prusa
        assertTrue(routerDevice.getVendor() == null || !routerDevice.getVendor().equals("Prusa"),
                "Router should not be identified as Prusa printer. Vendor was: " + routerDevice.getVendor());

        // Port 80 and 443 should NOT have Prusa specific service names
        for (PortScanResult service : routerDevice.getServices()) {
            if (service.getPort() == 80 || service.getPort() == 443) {
                assertTrue(service.getService() == null || !service.getService().contains("Prusa"),
                        "Port " + service.getPort() + " should not have Prusa service name. Service was: " + service.getService());
            }
        }
    }

    /**
     * Verifies that a real Prusa device is correctly identified when its banner matches the pattern.
     */
    @Test
    void testPrusaPositiveIdentification() throws Exception {
        StubPortScanner stubPortScanner = new StubPortScanner();
        StubMdnsScanner stubMdnsScanner = new StubMdnsScanner();
        StubSsdpScanner stubSsdpScanner = new StubSsdpScanner();

        // Real Prusa with pattern in banner
        stubPortScanner.addHostResult("192.168.1.100", 80, new PortScanResult(80, true, "HTTP", "Welcome to PrusaLink"));

        ScanConfiguration config = ScanConfigurationLoader.loadDefault();
        NettyNetworkScanner scanner = new NettyNetworkScanner(stubPortScanner, stubMdnsScanner, stubSsdpScanner);

        DiscoveredDevice prusaDevice = scanner.scanHost("192.168.1.100", config, true).get();

        // Should be identified as Prusa
        assertEquals("Prusa", prusaDevice.getVendor());
        assertTrue(prusaDevice.getServices().stream().anyMatch(s -> s.getService().equals("PrusaLink (HTTP)")),
                "PrusaLink (HTTP) should be identified by pattern match.");
    }

    /**
     * Comprehensive test verifying detection of various printer types (Bambu, Klipper, OctoPrint)
     * using the real default discovery configuration.
     */
    @Test
    void testDetectPrintersWithDefaultConfig() throws Exception {
        StubPortScanner stubPortScanner = new StubPortScanner();
        StubMdnsScanner stubMdnsScanner = new StubMdnsScanner();
        StubSsdpScanner stubSsdpScanner = new StubSsdpScanner();

        // Mock various printers on DIFFERENT IPs
        // 1. Bambu Lab on port 8883 and 990 (required ports)
        stubPortScanner.addHostResult("192.168.1.101", 990, new PortScanResult(990, true));
        stubPortScanner.addHostResult("192.168.1.101", 8883, new PortScanResult(8883, true));
        
        // 2. Klipper on port 7125 (required port)
        stubPortScanner.addHostResult("192.168.1.102", 7125, new PortScanResult(7125, true));

        // 3. OctoPrint on port 5000
        stubPortScanner.addHostResult("192.168.1.103", 5000, new PortScanResult(5000, true, "OctoPrint", "OctoPrint 1.9.3"));
        
        ScanConfiguration config = ScanConfigurationLoader.loadDefault();
        NettyNetworkScanner scanner = new NettyNetworkScanner(stubPortScanner, stubMdnsScanner, stubSsdpScanner);

        // Scan Bambu host
        DiscoveredDevice bambuDevice = scanner.scanHost("192.168.1.101", config, true).get();
        assertEquals("Bambu Lab", bambuDevice.getVendor());
        assertTrue(bambuDevice.getServices().stream().anyMatch(s -> s.getService().equals("MQTT (Secure)")));
        assertTrue(bambuDevice.getServices().stream().anyMatch(s -> s.getService().equals("FTPS (Secure FTP)")));
        
        // Scan Klipper host
        DiscoveredDevice klipperDevice = scanner.scanHost("192.168.1.102", config, true).get();
        assertEquals("Klipper", klipperDevice.getVendor());
        assertEquals("Moonraker API", klipperDevice.getServices().stream().filter(s -> s.getPort() == 7125).findFirst().orElseThrow().getService());

        // Scan OctoPrint host
        DiscoveredDevice octoDevice = scanner.scanHost("192.168.1.103", config, true).get();
        assertEquals(1, octoDevice.getServices().size());
        assertEquals("OctoPrint", octoDevice.getServices().get(0).getService());
    }
}
