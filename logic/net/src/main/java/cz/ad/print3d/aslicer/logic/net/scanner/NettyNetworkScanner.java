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

import cz.ad.print3d.aslicer.logic.net.info.NetworkInformationCollector;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.ScanConfiguration;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.SsdpServiceInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

/**
 * NettyNetworkScanner provides functionality to scan a range of IP addresses for specific open ports.
 * It coordinates discovery services (mDNS, SSDP) and direct port scanning using Netty.
 * This implementation delegates specific logic to specialized tasks and managers.
 */
public class NettyNetworkScanner implements NetworkScanner {

    private static final Logger LOGGER = Logger.getLogger(NettyNetworkScanner.class.getName());
    private static final int MAX_CONCURRENT_PORT_SCANS = 30000;

    private final PortScanner portScanner;
    private final DiscoveryManager discoveryManager;
    private final DeviceEnricher deviceEnricher;
    private final ServiceValidator serviceValidator;
    private final NetworkInformationCollector collector;
    private final ScanTracker scanTracker = new ScanTracker();
    private final java.util.concurrent.ExecutorService scanExecutor = java.util.concurrent.Executors.newFixedThreadPool(64, r -> {
        Thread t = new Thread(r, "scan-pumper");
        t.setDaemon(true);
        return t;
    });

    private final Semaphore portScanSemaphore;

    private boolean includeSelfIp = false;
    private long mdnsTimeoutMs = 1500;
    private long ssdpTimeoutMs = 1500;

    /**
     * Constructs a new NettyNetworkScanner with default components.
     */
    public NettyNetworkScanner() {
        this(new NettyPortScanner(), new NettyMdnsScanner(), new NettySsdpScanner(),
                new NetworkInformationCollector(), new DeviceEnricher(), new DefaultServiceValidator());
    }

    /**
     * Constructs a new NettyNetworkScanner with a provided PortScanner and default other components.
     *
     * @param portScanner the PortScanner to use for connection attempts
     */
    public NettyNetworkScanner(PortScanner portScanner) {
        this(portScanner, new NettyMdnsScanner(), new NettySsdpScanner(),
                new NetworkInformationCollector(), new DeviceEnricher(), new DefaultServiceValidator());
    }

    /**
     * Constructs a new NettyNetworkScanner with a provided PortScanner and MdnsScanner.
     *
     * @param portScanner the PortScanner to use for connection attempts
     * @param mdnsScanner the MdnsScanner to use for mDNS discovery
     */
    public NettyNetworkScanner(PortScanner portScanner, MdnsScanner mdnsScanner) {
        this(portScanner, mdnsScanner, new NettySsdpScanner(),
                new NetworkInformationCollector(), new DeviceEnricher(), new DefaultServiceValidator());
    }

    /**
     * Constructs a new NettyNetworkScanner with provided scanners.
     *
     * @param portScanner the PortScanner to use for connection attempts
     * @param mdnsScanner the MdnsScanner to use for mDNS discovery
     * @param ssdpScanner the SsdpScanner to use for SSDP discovery
     */
    public NettyNetworkScanner(PortScanner portScanner, MdnsScanner mdnsScanner, SsdpScanner ssdpScanner) {
        this(portScanner, mdnsScanner, ssdpScanner,
                new NetworkInformationCollector(), new DeviceEnricher(), new DefaultServiceValidator());
    }

    /**
     * Constructs a new NettyNetworkScanner with a provided PortScanner, MdnsScanner, and NetworkInformationCollector.
     *
     * @param portScanner the PortScanner to use for connection attempts
     * @param mdnsScanner the MdnsScanner to use for mDNS discovery
     * @param collector   the NetworkInformationCollector to use for local IP identification
     */
    public NettyNetworkScanner(PortScanner portScanner, MdnsScanner mdnsScanner, NetworkInformationCollector collector) {
        this(portScanner, mdnsScanner, new NettySsdpScanner(), collector, new DeviceEnricher(), new DefaultServiceValidator());
    }

    /**
     * Constructs a new NettyNetworkScanner with all core scanners and collector.
     *
     * @param portScanner the PortScanner to use for connection attempts
     * @param mdnsScanner the MdnsScanner to use for mDNS discovery
     * @param ssdpScanner the SsdpScanner to use for SSDP discovery
     * @param collector   the NetworkInformationCollector to use for local IP identification
     */
    public NettyNetworkScanner(PortScanner portScanner, MdnsScanner mdnsScanner, SsdpScanner ssdpScanner, NetworkInformationCollector collector) {
        this(portScanner, mdnsScanner, ssdpScanner, collector, new DeviceEnricher(), new DefaultServiceValidator());
    }

    /**
     * Full constructor for NettyNetworkScanner with all components.
     *
     * @param portScanner      port scanner implementation
     * @param mdnsScanner      mDNS scanner implementation
     * @param ssdpScanner      SSDP scanner implementation
     * @param collector        network information collector
     * @param deviceEnricher   device enricher implementation
     * @param serviceValidator service validator implementation
     */
    public NettyNetworkScanner(PortScanner portScanner, MdnsScanner mdnsScanner, SsdpScanner ssdpScanner,
                                NetworkInformationCollector collector, DeviceEnricher deviceEnricher,
                                ServiceValidator serviceValidator) {
        this(portScanner, mdnsScanner, ssdpScanner, collector, deviceEnricher, serviceValidator, MAX_CONCURRENT_PORT_SCANS);
    }

    /**
     * Full constructor for NettyNetworkScanner with all components and custom concurrency limit.
     *
     * @param portScanner      port scanner implementation
     * @param mdnsScanner      mDNS scanner implementation
     * @param ssdpScanner      SSDP scanner implementation
     * @param collector        network information collector
     * @param deviceEnricher   device enricher implementation
     * @param serviceValidator service validator implementation
     * @param maxConcurrentScans maximum concurrent port scans
     */
    public NettyNetworkScanner(PortScanner portScanner, MdnsScanner mdnsScanner, SsdpScanner ssdpScanner,
                                NetworkInformationCollector collector, DeviceEnricher deviceEnricher,
                                ServiceValidator serviceValidator, int maxConcurrentScans) {
        this.portScanner = portScanner;
        this.discoveryManager = new DiscoveryManager(mdnsScanner, ssdpScanner);
        this.collector = collector;
        this.deviceEnricher = deviceEnricher;
        this.serviceValidator = serviceValidator;
        this.portScanSemaphore = new Semaphore(maxConcurrentScans);
    }

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
        ScanConfiguration config = new ScanConfiguration(null, new HashSet<>(ports));
        return scanRange(baseIp, startHost, endHost, config, useBannerGrabbing, listener);
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
        scanTracker.reset();
        RangeScanTask task = new RangeScanTask(baseIp, startHost, endHost, config, useBannerGrabbing, listener,
                portScanner, discoveryManager, deviceEnricher, serviceValidator, portScanSemaphore, scanTracker,
                includeSelfIp, mdnsTimeoutMs, ssdpTimeoutMs, collector, scanExecutor);

        return task.execute();
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
        ScanConfiguration config = new ScanConfiguration(null, new HashSet<>(ports));
        return scanHost(host, config, useBannerGrabbing, listener);
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
        scanTracker.reset();
        ConcurrentHashMap<String, List<MdnsServiceInfo>> mdnsByIpMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, List<SsdpServiceInfo>> ssdpByIpMap = new ConcurrentHashMap<>();

        CompletableFuture<Set<MdnsServiceInfo>> mdnsFuture = discoveryManager.discoverMdns(mdnsTimeoutMs, service -> {
            if (host.equals(service.getIpAddress())) {
                mdnsByIpMap.computeIfAbsent(service.getIpAddress(), k -> new CopyOnWriteArrayList<>()).add(service);
            }
        }, null);
        scanTracker.track(mdnsFuture);

        CompletableFuture<Set<SsdpServiceInfo>> ssdpFuture = discoveryManager.discoverSsdp(ssdpTimeoutMs, service -> {
            if (host.equals(service.getIpAddress())) {
                ssdpByIpMap.computeIfAbsent(service.getIpAddress(), k -> new CopyOnWriteArrayList<>()).add(service);
            }
        }, null);
        scanTracker.track(ssdpFuture);
        
        HostScanTask task = new HostScanTask(host, config, useBannerGrabbing, listener, portScanner,
                serviceValidator, deviceEnricher, scanTracker, portScanSemaphore, scanExecutor);
        // Hint that we might already know it's reachable via mDNS/SSDP
        // (Wait, discovery is still in progress, so we might not know yet).
        // For scanHost, we wait for discovery anyway.
        
        CompletableFuture<DiscoveredDevice> portScanFuture = task.execute();
        scanTracker.track(portScanFuture);

        return CompletableFuture.allOf(mdnsFuture, ssdpFuture, portScanFuture)
                .thenApply(v -> {
                    DiscoveredDevice device = portScanFuture.join();
                    List<MdnsServiceInfo> mdnsServices = mdnsByIpMap.get(host);
                    if (mdnsServices != null) {
                        deviceEnricher.enrichWithMdns(device, mdnsServices, false);
                    }
                    List<SsdpServiceInfo> ssdpServices = ssdpByIpMap.get(host);
                    if (ssdpServices != null) {
                        deviceEnricher.enrichWithSsdp(device, ssdpServices, false);
                    }
                    deviceEnricher.enrichFromPortScan(device, config);
                    return device;
                });
    }

    @Override
    public void stopScan() {
        scanTracker.cancelAll();
        discoveryManager.stopDiscovery();
        portScanner.stopScan();
    }

    @Override
    public void setTimeout(int timeoutMillis) {
        portScanner.setTimeout(timeoutMillis);
    }

    @Override
    public int getTimeout() {
        return portScanner.getTimeout();
    }

    @Override
    public void setMdnsTimeout(int timeoutMillis) {
        this.mdnsTimeoutMs = timeoutMillis;
    }

    @Override
    public int getMdnsTimeout() {
        return (int) mdnsTimeoutMs;
    }

    @Override
    public void setSsdpTimeout(int timeoutMillis) {
        this.ssdpTimeoutMs = timeoutMillis;
    }

    @Override
    public int getSsdpTimeout() {
        return (int) ssdpTimeoutMs;
    }

    @Override
    public void setIncludeSelfIp(boolean include) {
        this.includeSelfIp = include;
    }

    @Override
    public boolean isIncludeSelfIp() {
        return includeSelfIp;
    }

    @Override
    public void close() {
        stopScan();
        portScanner.close();
        discoveryManager.close();
        scanExecutor.shutdown();
    }
}
