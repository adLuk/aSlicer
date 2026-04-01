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
package cz.ad.print3d.aslicer.logic.net.info;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class NetworkInformationCollectorTest {

    @BeforeEach
    void setUp() {
        NetworkInformationCollector.resetAsyncCollection();
    }

    @Test
    void testCollectInterfaces() {
        NetworkInformationCollector collector = new NetworkInformationCollector();
        List<NetworkInterfaceInfo> interfaces = collector.collect();

        assertNotNull(interfaces);
        assertFalse(interfaces.isEmpty(), "Expected at least one network interface (e.g. loopback)");

        // Find loopback interface to verify its properties
        NetworkInterfaceInfo loopback = interfaces.stream()
                .filter(NetworkInterfaceInfo::isLoopback)
                .findFirst()
                .orElse(null);

        assertNotNull(loopback, "Should have a loopback interface");
        assertTrue(loopback.isUp(), "Loopback interface should be up");
        
        // Check loopback addresses
        assertFalse(loopback.getAddresses().isEmpty(), "Loopback should have addresses");
        
        boolean hasIpv4Localhost = loopback.getAddresses().stream()
                .anyMatch(addr -> addr.isIpv4() && addr.getAddress().equals("127.0.0.1"));
        boolean hasIpv6Localhost = loopback.getAddresses().stream()
                .anyMatch(addr -> addr.isIpv6() && (addr.getAddress().equals("0:0:0:0:0:0:0:1") || addr.getAddress().equals("::1")));
        
        assertTrue(hasIpv4Localhost || hasIpv6Localhost, "Loopback should have at least 127.0.0.1 or ::1");
    }

    @Test
    void testCollectAsyncAndCaching() throws Exception {
        NetworkInformationCollector collector = new NetworkInformationCollector();
        
        // Before collection, cache should be null
        assertNull(NetworkInformationCollector.getCachedInfo());
        
        // Start async collection
        CompletableFuture<List<NetworkInterfaceInfo>> future = collector.collectAsync();
        assertNotNull(future);
        
        // Wait for it to complete
        List<NetworkInterfaceInfo> result = future.get(30, TimeUnit.SECONDS);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // After completion, cache should be populated
        List<NetworkInterfaceInfo> cached = NetworkInformationCollector.getCachedInfo();
        assertNotNull(cached);
        assertEquals(result.size(), cached.size());
        
        // Subsequent calls to collectAsync should return the same future
        CompletableFuture<List<NetworkInterfaceInfo>> future2 = collector.collectAsync();
        assertSame(future, future2);
    }

    @Test
    void testIpv6Collection() {
        NetworkInformationCollector collector = new NetworkInformationCollector();
        List<NetworkInterfaceInfo> interfaces = collector.collect();

        boolean foundIpv6 = false;
        for (NetworkInterfaceInfo ni : interfaces) {
            for (NetworkAddressInfo addr : ni.getAddresses()) {
                if (addr.isIpv6()) {
                    foundIpv6 = true;
                    assertNotNull(addr.getAddress());
                    // IPv6 addresses often contain colons
                    assertTrue(addr.getAddress().contains(":") || addr.getAddress().equals("::1"), 
                            "IPv6 address should contain colons or be ::1: " + addr.getAddress());
                }
            }
        }
        
        // On most modern systems, at least the loopback ::1 should be present.
        // We log it if not found, as some environments might have IPv6 disabled.
        if (!foundIpv6) {
            System.out.println("[DEBUG_LOG] No IPv6 addresses were found on this machine.");
        }
    }

    @Test
    void testAddressInfoProperties() {
        NetworkInformationCollector collector = new NetworkInformationCollector();
        List<NetworkInterfaceInfo> interfaces = collector.collect();

        for (NetworkInterfaceInfo ni : interfaces) {
            for (NetworkAddressInfo addr : ni.getAddresses()) {
                assertNotNull(addr.getAddress());
                assertNotNull(addr.getHostname());
                // DNS lookup might fail to resolve a custom hostname, but it should return something (at least the IP address)
                assertFalse(addr.getHostname().isEmpty());
            }
        }
    }

    @Test
    void testDnsInformation() {
        DnsInformation dnsInfo = new DnsInformation();
        List<String> dnsServers = dnsInfo.getConfiguredDnsServers();
        
        assertNotNull(dnsServers);
        // It's possible that no DNS servers are configured in some environments (e.g. CI), 
        // but normally there should be at least one if network is active.
        // We just verify it doesn't crash.
    }
}
