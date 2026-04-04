package cz.ad.print3d.aslicer.logic.net.scanner;

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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that scanHost also triggers mDNS and SSDP discovery.
 */
class NettyNetworkScannerScanHostDiscoveryTest {

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

    private static class StubPortScanner implements PortScanner {
        @Override public CompletableFuture<PortScanResult> scanPort(String host, int port) { return CompletableFuture.completedFuture(new PortScanResult(port, false)); }
        @Override public CompletableFuture<PortScanResult> scanPort(String host, int port, boolean useBannerGrabbing) { return scanPort(host, port); }
        @Override public void setTimeout(int timeoutMillis) {}
        @Override public int getTimeout() { return 500; }
        @Override public void stopScan() {}
        @Override public void close() {}
    }

    private static class StubMdnsScanner implements MdnsScanner {
        Set<MdnsServiceInfo> services = new HashSet<>();
        boolean triggered = false;

        @Override
        public CompletableFuture<Set<MdnsServiceInfo>> discoverDevices(long timeoutMillis, MdnsDiscoveryListener listener, java.net.NetworkInterface networkInterface) {
            triggered = true;
            if (listener != null) {
                for (MdnsServiceInfo s : services) listener.onServiceDiscovered(s);
            }
            return CompletableFuture.completedFuture(services);
        }

        @Override public void stopScan() {}
        @Override public void close() {}
    }

    private static class StubSsdpScanner implements SsdpScanner {
        Set<SsdpServiceInfo> services = new HashSet<>();
        boolean triggered = false;

        @Override
        public CompletableFuture<Set<SsdpServiceInfo>> discoverDevices(long timeoutMillis, SsdpDiscoveryListener listener, java.net.NetworkInterface networkInterface) {
            triggered = true;
            if (listener != null) {
                for (SsdpServiceInfo s : services) listener.onServiceDiscovered(s);
            }
            return CompletableFuture.completedFuture(services);
        }

        @Override public void stopScan() {}
        @Override public void close() {}
    }

    @Test
    void testScanHostTriggersDiscovery() throws Exception {
        String host = "192.168.1.10";
        MdnsServiceInfo mdnsInfo = new MdnsServiceInfo("Printer", "_http._tcp.local.", host, 80, "printer.local", Collections.emptyMap());
        mdnsScanner.services.add(mdnsInfo);

        CompletableFuture<DiscoveredDevice> future = networkScanner.scanHost(host, List.of(80));
        DiscoveredDevice device = future.get(5, TimeUnit.SECONDS);

        assertTrue(mdnsScanner.triggered, "mDNS discovery should have been triggered by scanHost");
        assertTrue(ssdpScanner.triggered, "SSDP discovery should have been triggered by scanHost");
        assertNotNull(device.getName(), "Device should have been enriched with mDNS name");
        assertEquals("Printer", device.getName());
    }
}
