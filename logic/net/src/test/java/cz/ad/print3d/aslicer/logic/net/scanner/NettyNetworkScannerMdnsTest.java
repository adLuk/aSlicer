package cz.ad.print3d.aslicer.logic.net.scanner;

import cz.ad.print3d.aslicer.logic.net.info.NetworkInformationCollector;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for {@link NettyNetworkScanner} with mDNS prioritization.
 * <p>Verifies that devices discovered via mDNS are prioritized in the port scan order
 * during a range scan.</p>
 */
class NettyNetworkScannerMdnsTest {

    private StubPortScanner portScanner;
    private StubMdnsScanner mdnsScanner;
    private NettyNetworkScanner networkScanner;

    @BeforeEach
    void setUp() {
        portScanner = new StubPortScanner();
        mdnsScanner = new StubMdnsScanner();
        NetworkInformationCollector mockCollector = new NetworkInformationCollector() {
            @Override
            public List<cz.ad.print3d.aslicer.logic.net.info.NetworkInterfaceInfo> collect() {
                return Collections.emptyList();
            }
        };
        networkScanner = new NettyNetworkScanner(portScanner, mdnsScanner, mockCollector);
    }

    private static class StubPortScanner implements PortScanner {
        final List<String> scanOrder = new CopyOnWriteArrayList<>();
        final Set<String> openPorts = Collections.synchronizedSet(new java.util.HashSet<>());

        @Override
        public CompletableFuture<PortScanResult> scanPort(String host, int port) {
            return scanPort(host, port, false);
        }

        @Override
        public CompletableFuture<PortScanResult> scanPort(String host, int port, boolean useBannerGrabbing) {
            scanOrder.add(host);
            boolean open = openPorts.contains(host + ":" + port);
            return CompletableFuture.completedFuture(new PortScanResult(port, open));
        }

        @Override
        public void setTimeout(int timeoutMillis) {}

        @Override
        public int getTimeout() { return 500; }

        @Override
        public void stopScan() {
        }

        @Override
        public void close() {}
    }

    private static class StubMdnsScanner implements MdnsScanner {
        Set<MdnsServiceInfo> discoveredServices = Set.of();

        @Override
        public CompletableFuture<Set<MdnsServiceInfo>> discoverDevices(long timeoutMillis, MdnsDiscoveryListener listener, java.net.NetworkInterface networkInterface) {
            if (listener != null) {
                for (MdnsServiceInfo service : discoveredServices) {
                    listener.onServiceDiscovered(service);
                }
            }
            return CompletableFuture.completedFuture(discoveredServices);
        }

        @Override
        public void stopScan() {
        }

        @Override
        public void close() {}
    }

    @Test
    void testNetworkInterfaceSelection() throws Exception {
        // Try to find any active non-loopback interface to use for testing
        java.net.NetworkInterface targetInterface = java.net.NetworkInterface.networkInterfaces()
                .filter(ni -> {
                    try {
                        return ni.isUp() && !ni.isLoopback() && ni.getInetAddresses().hasMoreElements();
                    } catch (java.net.SocketException e) {
                        return false;
                    }
                })
                .findFirst()
                .orElse(java.net.NetworkInterface.getByName("lo"));

        if (targetInterface == null) return; // Should not happen on any normal system

        String targetIp = targetInterface.getInetAddresses().nextElement().getHostAddress();
        if (!targetIp.contains(".")) return; // IPv4 only for this test simple matching
        
        String baseIp = targetIp.substring(0, targetIp.lastIndexOf('.') + 1);

        NetworkInformationCollector mockCollector = new NetworkInformationCollector() {
            @Override
            public List<cz.ad.print3d.aslicer.logic.net.info.NetworkInterfaceInfo> collect() {
                return List.of(new cz.ad.print3d.aslicer.logic.net.info.NetworkInterfaceInfo(
                        targetInterface.getName(), targetInterface.getDisplayName(), "",
                        List.of(new cz.ad.print3d.aslicer.logic.net.info.NetworkAddressInfo(targetIp, "host", true, 24)),
                        targetInterface.getName().equals("lo"), true, false
                ));
            }
        };

        final java.net.NetworkInterface[] capturedInterface = new java.net.NetworkInterface[1];
        StubMdnsScanner interfaceCapturingMdnsScanner = new StubMdnsScanner() {
            @Override
            public CompletableFuture<Set<MdnsServiceInfo>> discoverDevices(long timeoutMillis, MdnsDiscoveryListener listener, java.net.NetworkInterface networkInterface) {
                capturedInterface[0] = networkInterface;
                return super.discoverDevices(timeoutMillis, listener, networkInterface);
            }
        };

        NettyNetworkScanner scannerWithMock = new NettyNetworkScanner(portScanner, interfaceCapturingMdnsScanner, mockCollector);
        scannerWithMock.setIncludeSelfIp(true);

        scannerWithMock.scanRange(baseIp, 1, 1, List.of(80)).get(5, TimeUnit.SECONDS);

        assertNotNull(capturedInterface[0], "Should have selected a NetworkInterface");
        assertEquals(targetInterface.getName(), capturedInterface[0].getName());
    }

    @Test
    void testMdnsPrioritization() throws Exception {
        String baseIp = "192.168.1.";
        List<Integer> ports = List.of(80);
        
        // mDNS finds 192.168.1.50
        mdnsScanner.discoveredServices = Set.of(new MdnsServiceInfo("TestPrinter", "_http._tcp.local.", "192.168.1.50", 80, "printer.local", Collections.emptyMap()));
        
        // Scan range 1 to 50
        CompletableFuture<List<DiscoveredDevice>> future = networkScanner.scanRange(baseIp, 1, 50, ports);
        future.get(5, TimeUnit.SECONDS);

        // The first IP in scanOrder should be 192.168.1.50 because it was found by mDNS
        assertTrue(portScanner.scanOrder.size() > 0);
        assertEquals("192.168.1.50", portScanner.scanOrder.get(0), "Prioritized IP should be scanned first");
    }

    @Test
    void testMdnsImmediateNotification() throws Exception {
        String baseIp = "192.168.1.";
        List<Integer> ports = List.of(80);
        
        mdnsScanner.discoveredServices = Set.of(new MdnsServiceInfo("TestPrinter", "_http._tcp.local.", "192.168.1.10", 80, "printer.local", Collections.emptyMap()));
        
        List<DiscoveredDevice> discoveredViaListener = new CopyOnWriteArrayList<>();
        NetworkScanner.ScanProgressListener listener = new NetworkScanner.ScanProgressListener() {
            @Override
            public void onProgress(double progress, String currentIp) {}

            @Override
            public void onDeviceDiscovered(DiscoveredDevice device) {
                discoveredViaListener.add(device);
            }
        };

        networkScanner.scanRange(baseIp, 1, 10, ports, false, listener).get(5, TimeUnit.SECONDS);

        // Should find 192.168.1.10
        assertTrue(discoveredViaListener.stream().anyMatch(d -> "192.168.1.10".equals(d.getIpAddress())));
        DiscoveredDevice mdnsDevice = discoveredViaListener.stream().filter(d -> "192.168.1.10".equals(d.getIpAddress())).findFirst().get();
        assertEquals("TestPrinter", mdnsDevice.getName());
        // Initial mDNS report marks as in progress
        assertTrue(mdnsDevice.getServices().stream().anyMatch(s -> s.getPort() == 80 && s.isVerificationInProgress()));
        assertTrue(mdnsDevice.getServices().stream().anyMatch(s -> s.getPort() == 80 && s.isFromMdns()));
    }

    @Test
    void testPortDiscoveredNotification() throws Exception {
        String baseIp = "192.168.1.";
        List<Integer> ports = List.of(80);
        
        // Mock port 80 as open for 192.168.1.5
        portScanner.openPorts.add("192.168.1.5:80");
        
        List<PortScanResult> discoveredPorts = new CopyOnWriteArrayList<>();
        NetworkScanner.ScanProgressListener listener = new NetworkScanner.ScanProgressListener() {
            @Override
            public void onProgress(double progress, String currentIp) {}

            @Override
            public void onPortDiscovered(String host, PortScanResult portResult) {
                if (portResult.isOpen()) {
                    discoveredPorts.add(portResult);
                }
            }
        };

        networkScanner.scanRange(baseIp, 1, 10, ports, false, listener).get(5, TimeUnit.SECONDS);

        assertTrue(discoveredPorts.stream().anyMatch(p -> p.getPort() == 80 && p.isOpen()));
    }

    @Test
    void testMdnsServicesStored() throws Exception {
        String baseIp = "192.168.1.";
        List<Integer> ports = List.of(80);
        
        MdnsServiceInfo service = new MdnsServiceInfo("TestPrinter", "_http._tcp.local.", "192.168.1.10", 80, "printer.local", Map.of("version", "1.0"));
        mdnsScanner.discoveredServices = Set.of(service);
        
        CompletableFuture<List<DiscoveredDevice>> future = networkScanner.scanRange(baseIp, 1, 10, ports);
        List<DiscoveredDevice> results = future.get(5, TimeUnit.SECONDS);

        DiscoveredDevice device = results.stream().filter(d -> "192.168.1.10".equals(d.getIpAddress())).findFirst().orElseThrow();
        assertEquals(1, device.getMdnsServices().size());
        assertEquals(service, device.getMdnsServices().get(0));
        assertEquals("1.0", device.getMdnsServices().get(0).getAttributes().get("version"));
    }
}
