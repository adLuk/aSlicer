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
import cz.ad.print3d.aslicer.logic.net.scanner.dto.SsdpServiceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NettyNetworkScannerSsdpTest {

    private StubPortScanner portScanner;
    private StubMdnsScanner mdnsScanner;
    private StubSsdpScanner ssdpScanner;
    private NetworkInformationCollector collector;
    private NettyNetworkScanner networkScanner;

    @BeforeEach
    void setUp() {
        portScanner = new StubPortScanner();
        mdnsScanner = new StubMdnsScanner();
        ssdpScanner = new StubSsdpScanner();
        
        collector = new NetworkInformationCollector() {
            @Override
            public List<NetworkInterfaceInfo> collect() {
                NetworkInterfaceInfo ni = new NetworkInterfaceInfo("eth0", "eth0", "00:11:22:33:44:55",
                        List.of(new NetworkAddressInfo("192.168.1.10", "host.local", true, 24)), false, true, false);
                return List.of(ni);
            }
        };

        networkScanner = new NettyNetworkScanner(portScanner, mdnsScanner, ssdpScanner, collector);
    }

    private static class StubPortScanner implements PortScanner {
        @Override public CompletableFuture<PortScanResult> scanPort(String host, int port) { return CompletableFuture.completedFuture(new PortScanResult(port, false)); }
        @Override public CompletableFuture<PortScanResult> scanPort(String host, int port, boolean useBannerGrabbing) { return scanPort(host, port); }
        @Override public void setTimeout(int timeoutMillis) {}
        @Override public int getTimeout() { return 500; }
        @Override public void stopScan() {}
        @Override public void close() {}
    }

    private static class StubMdnsScanner implements MdnsScanner {
        @Override public CompletableFuture<Set<MdnsServiceInfo>> discoverDevices(long timeoutMillis, MdnsDiscoveryListener listener, java.net.NetworkInterface networkInterface) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }
        @Override public void stopScan() {}
        @Override public void close() {}
    }

    private static class StubSsdpScanner implements SsdpScanner {
        Set<SsdpServiceInfo> discoveredServices = new HashSet<>();
        @Override public CompletableFuture<Set<SsdpServiceInfo>> discoverDevices(long timeoutMillis, SsdpDiscoveryListener listener, java.net.NetworkInterface networkInterface) {
            if (listener != null) {
                for (SsdpServiceInfo service : discoveredServices) {
                    listener.onServiceDiscovered(service);
                }
            }
            return CompletableFuture.completedFuture(discoveredServices);
        }
        @Override public void stopScan() {}
        @Override public void close() {}
    }

    @Test
    void testSsdpDiscoveryIntegration() throws Exception {
        SsdpServiceInfo ssdpInfo = new SsdpServiceInfo(
                "uuid:1234", "upnp:rootdevice", "http://192.168.1.100:80/desc.xml",
                "192.168.1.100", 80, "Test Printer", "Creality", "Ender-3", Collections.emptyMap()
        );
        ssdpScanner.discoveredServices.add(ssdpInfo);

        List<DiscoveredDevice> devices = networkScanner.scanRange("192.168.1.", 100, 100, Collections.emptyList()).get();

        assertFalse(devices.isEmpty());
        DiscoveredDevice device = devices.get(0);
        assertEquals("192.168.1.100", device.getIpAddress());
        assertEquals("Test Printer", device.getName());
        assertEquals("Creality", device.getVendor());
        assertEquals("Ender-3", device.getModel());
        assertFalse(device.getSsdpServices().isEmpty());
        assertEquals("uuid:1234", device.getSsdpServices().get(0).getUsn());
    }
}
