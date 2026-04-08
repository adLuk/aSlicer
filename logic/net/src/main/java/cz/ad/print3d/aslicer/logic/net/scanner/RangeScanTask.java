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
 * along with this program.  See the file LICENSE for more details.
 */
package cz.ad.print3d.aslicer.logic.net.scanner;

import cz.ad.print3d.aslicer.logic.net.info.NetworkAddressInfo;
import cz.ad.print3d.aslicer.logic.net.info.NetworkInformationCollector;
import cz.ad.print3d.aslicer.logic.net.info.NetworkInterfaceInfo;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.*;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Task responsible for scanning a range of IP addresses.
 * Coordinates mDNS, SSDP, and direct port scanning in parallel.
 */
public class RangeScanTask {

    private static final Logger LOGGER = Logger.getLogger(RangeScanTask.class.getName());

    private final String baseIp;
    private final int startHost;
    private final int endHost;
    private final ScanConfiguration config;
    private final boolean useBannerGrabbing;
    private final NetworkScanner.ScanProgressListener listener;
    private final PortScanner portScanner;
    private final DiscoveryManager discoveryManager;
    private final DeviceEnricher deviceEnricher;
    private final ServiceValidator serviceValidator;
    private final Semaphore portScanSemaphore;
    private final ScanTracker scanTracker;
    private final boolean includeSelfIp;
    private final long mdnsTimeoutMs;
    private final long ssdpTimeoutMs;
    private final NetworkInformationCollector collector;
    private final java.util.concurrent.Executor scanExecutor;
    private final java.util.concurrent.atomic.AtomicLong totalScannedPorts = new java.util.concurrent.atomic.AtomicLong(0);

    /**
     * Constructs a new RangeScanTask.
     *
     * @param baseIp            the base IP address
     * @param startHost         starting host number
     * @param endHost           ending host number
     * @param config            the scan configuration
     * @param useBannerGrabbing true to attempt banner grabbing
     * @param listener          progress listener
     * @param portScanner       port scanner implementation
     * @param discoveryManager  discovery manager implementation
     * @param deviceEnricher    device enricher implementation
     * @param serviceValidator  service validator implementation
     * @param portScanSemaphore semaphore for concurrency control
     * @param scanTracker       tracker for active futures
     * @param includeSelfIp     true to include local IP
     * @param mdnsTimeoutMs     mDNS timeout
     * @param ssdpTimeoutMs     SSDP timeout
     * @param collector         network information collector
     * @param scanExecutor      executor for port scanning tasks
     */
    public RangeScanTask(String baseIp, int startHost, int endHost, ScanConfiguration config,
                        boolean useBannerGrabbing, NetworkScanner.ScanProgressListener listener,
                        PortScanner portScanner, DiscoveryManager discoveryManager,
                        DeviceEnricher deviceEnricher, ServiceValidator serviceValidator,
                        Semaphore portScanSemaphore, ScanTracker scanTracker, boolean includeSelfIp,
                        long mdnsTimeoutMs, long ssdpTimeoutMs, NetworkInformationCollector collector,
                        java.util.concurrent.Executor scanExecutor) {
        this.baseIp = baseIp;
        this.startHost = startHost;
        this.endHost = endHost;
        this.config = config;
        this.useBannerGrabbing = useBannerGrabbing;
        this.listener = listener;
        this.portScanner = portScanner;
        this.discoveryManager = discoveryManager;
        this.deviceEnricher = deviceEnricher;
        this.serviceValidator = serviceValidator;
        this.portScanSemaphore = portScanSemaphore;
        this.scanTracker = scanTracker;
        this.includeSelfIp = includeSelfIp;
        this.mdnsTimeoutMs = mdnsTimeoutMs;
        this.ssdpTimeoutMs = ssdpTimeoutMs;
        this.collector = collector;
        this.scanExecutor = scanExecutor;
    }

    /**
     * Executes the range scan asynchronously.
     *
     * @return a CompletableFuture that completes with a list of discovered devices
     */
    public CompletableFuture<List<DiscoveredDevice>> execute() {
        CompletableFuture<List<DiscoveredDevice>> externalFuture = new CompletableFuture<>();
        scanTracker.track(externalFuture);

        CompletableFuture.runAsync(() -> {
            try {
                if (listener != null) {
                    listener.onProgress(0.0, "Starting parallel network scan...");
                }

                String basePrefix = baseIp.endsWith(".") ? baseIp : baseIp + ".";
                List<NetworkInterfaceInfo> cachedInfo = NetworkInformationCollector.getCachedInfo();
                if (cachedInfo == null) {
                    cachedInfo = collector.collect();
                }
                final List<NetworkInterfaceInfo> finalInfo = cachedInfo;

                Set<String> selfIps = Collections.emptySet();
                if (!includeSelfIp) {
                    selfIps = cachedInfo.stream()
                            .flatMap(ni -> ni.getAddresses().stream())
                            .map(NetworkAddressInfo::getAddress)
                            .collect(Collectors.toSet());
                }
                final Set<String> finalSelfIps = selfIps;

                // Find matching NetworkInterface for mDNS & SSDP
                NetworkInterface networkInterface = null;
                for (NetworkInterfaceInfo info : finalInfo) {
                    for (NetworkAddressInfo addr : info.getAddresses()) {
                        if (addr.isIpv4() && addr.getAddress().startsWith(basePrefix)) {
                            try {
                                networkInterface = NetworkInterface.getByName(info.getName());
                                break;
                            } catch (SocketException e) {
                                LOGGER.log(Level.WARNING, "Failed to get NetworkInterface by name: " + info.getName(), e);
                            }
                        }
                    }
                    if (networkInterface != null) break;
                }

                final Map<String, DiscoveredDevice> discoveredDevicesMap = new ConcurrentHashMap<>();
                final Map<String, List<MdnsServiceInfo>> mdnsByIpMap = new ConcurrentHashMap<>();
                final Map<String, List<SsdpServiceInfo>> ssdpByIpMap = new ConcurrentHashMap<>();

                CompletableFuture<Set<MdnsServiceInfo>> mdnsFuture = discoveryManager.discoverMdns(mdnsTimeoutMs, service -> {
                    String ip = service.getIpAddress();
                    if (ip != null && ip.startsWith(basePrefix)) {
                        try {
                            int hostPart = Integer.parseInt(ip.substring(basePrefix.length()));
                            if (hostPart >= startHost && hostPart <= endHost && (includeSelfIp || !finalSelfIps.contains(ip))) {
                                mdnsByIpMap.computeIfAbsent(ip, k -> new CopyOnWriteArrayList<>()).add(service);
                                DiscoveredDevice device = discoveredDevicesMap.computeIfAbsent(ip, DiscoveredDevice::new);
                                deviceEnricher.enrichWithMdns(device, List.of(service), true);
                                if (listener != null) {
                                    listener.onDeviceDiscovered(device);
                                }
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }, networkInterface);
                scanTracker.track(mdnsFuture);

                CompletableFuture<Set<SsdpServiceInfo>> ssdpFuture = discoveryManager.discoverSsdp(ssdpTimeoutMs, service -> {
                    String ip = service.getIpAddress();
                    if (ip != null && ip.startsWith(basePrefix)) {
                        try {
                            int hostPart = Integer.parseInt(ip.substring(basePrefix.length()));
                            if (hostPart >= startHost && hostPart <= endHost && (includeSelfIp || !finalSelfIps.contains(ip))) {
                                ssdpByIpMap.computeIfAbsent(ip, k -> new CopyOnWriteArrayList<>()).add(service);
                                DiscoveredDevice device = discoveredDevicesMap.computeIfAbsent(ip, DiscoveredDevice::new);
                                deviceEnricher.enrichWithSsdp(device, List.of(service), true);
                                if (listener != null) {
                                    listener.onDeviceDiscovered(device);
                                }
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }, networkInterface);
                scanTracker.track(ssdpFuture);

                // Wait for fast discovery results to populate maps
                if (mdnsFuture.isDone() && ssdpFuture.isDone()) {
                    // Already done (synchronous stub)
                } else {
                    // give it a tiny bit of time if it's really fast
                    try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                }

                Set<String> discoveredIps = Collections.newSetFromMap(new ConcurrentHashMap<>());
                discoveredIps.addAll(mdnsByIpMap.keySet());
                discoveredIps.addAll(ssdpByIpMap.keySet());

                List<String> allHostsInRange = new ArrayList<>(discoveredIps);
                for (int i = startHost; i <= endHost; i++) {
                    String ip = basePrefix + i;
                    if (!discoveredIps.contains(ip)) {
                        if (includeSelfIp || !finalSelfIps.contains(ip)) {
                            allHostsInRange.add(ip);
                        }
                    }
                }

                int totalHosts = allHostsInRange.size();
                if (totalHosts == 0) {
                    externalFuture.complete(Collections.emptyList());
                    return;
                }

                if (config.isDeepScan()) {
                    performDeepScan(allHostsInRange, mdnsByIpMap, ssdpByIpMap, discoveredDevicesMap, externalFuture);
                } else {
                    performNormalScan(allHostsInRange, mdnsByIpMap, ssdpByIpMap, discoveredDevicesMap, externalFuture);
                }

            } catch (Exception e) {
                externalFuture.completeExceptionally(e);
            }
        });

        return externalFuture;
    }

    /**
     * Performs a normal network scan using the provided scan configuration.
     * Hosts are scanned in the order they appear in the provided list.
     *
     * @param hostsToScan          list of hosts to scan
     * @param mdnsByIpMap          map of discovered mDNS services
     * @param ssdpByIpMap          map of discovered SSDP services
     * @param discoveredDevicesMap map for tracking discovered devices
     * @param externalFuture       the future to complete when finished
     */
    private void performNormalScan(List<String> hostsToScan, Map<String, List<MdnsServiceInfo>> mdnsByIpMap,
                                   Map<String, List<SsdpServiceInfo>> ssdpByIpMap,
                                   Map<String, DiscoveredDevice> discoveredDevicesMap,
                                   CompletableFuture<List<DiscoveredDevice>> externalFuture) {
        int totalHosts = hostsToScan.size();
        int portsPerHost = config.getAllPorts().size();
        long totalPorts = (long) totalHosts * (portsPerHost > 0 ? portsPerHost : 1);
        List<CompletableFuture<DiscoveredDevice>> hostFutures = new ArrayList<>();

        for (String hostIp : hostsToScan) {
            if (externalFuture.isCancelled()) break;

            CompletableFuture<DiscoveredDevice> hostFuture = scanHostInternal(hostIp, config, new NetworkScanner.ScanProgressListener() {
                @Override public void onProgress(double progress, String currentIp) {}
                @Override public void onDeviceDiscovered(DiscoveredDevice device) {
                    if (listener != null) listener.onDeviceDiscovered(device);
                }
                @Override public void onDeviceUpdated(DiscoveredDevice device) {
                    if (listener != null) listener.onDeviceUpdated(device);
                }
                @Override public void onPortDiscovered(String host, PortScanResult portResult) {
                    if (listener != null) listener.onPortDiscovered(host, portResult);
                }
                @Override public void onPortScanned(String host, int port) {
                    long completed = totalScannedPorts.incrementAndGet();
                    if (listener != null) {
                        listener.onProgress((double) completed / totalPorts, host);
                    }
                }
            }, mdnsByIpMap, ssdpByIpMap);

            hostFutures.add(hostFuture);
        }

        CompletableFuture.allOf(hostFutures.toArray(new CompletableFuture<?>[0]))
                .thenRun(() -> {
                    List<DiscoveredDevice> results = hostFutures.stream()
                            .map(f -> {
                                try { return f.join(); } catch (Exception e) { return null; }
                            })
                            .filter(d -> d != null && !d.getServices().isEmpty())
                            .collect(Collectors.toList());
                    externalFuture.complete(results);
                })
                .exceptionally(ex -> {
                    externalFuture.completeExceptionally(ex);
                    return null;
                });
    }

    /**
     * Performs a deep network scan in two phases:
     * 1. A fast scan of all hosts using common ports.
     * 2. A prioritized full port scan of hosts that responded in phase 1 or via other methods.
     * This approach ensures quick initial discovery while maintaining thoroughness.
     *
     * @param hostsToScan          list of hosts to scan
     * @param mdnsByIpMap          map of discovered mDNS services
     * @param ssdpByIpMap          map of discovered SSDP services
     * @param discoveredDevicesMap map for tracking discovered devices
     * @param externalFuture       the future to complete when finished
     */
    private void performDeepScan(List<String> hostsToScan, Map<String, List<MdnsServiceInfo>> mdnsByIpMap,
                                 Map<String, List<SsdpServiceInfo>> ssdpByIpMap,
                                 Map<String, DiscoveredDevice> discoveredDevicesMap,
                                 CompletableFuture<List<DiscoveredDevice>> externalFuture) {
        int totalHosts = hostsToScan.size();
        ScanConfiguration normalConfig = new ScanConfiguration(config.getProfiles(), config.getCommonPorts(), false);
        int p1PortsPerHost = normalConfig.getAllPorts().size();
        int p2PortsPerHost = config.getAllPorts().size();
        long totalPorts = (long) totalHosts * (p1PortsPerHost + p2PortsPerHost);
        
        List<CompletableFuture<DiscoveredDevice>> fullScanFutures = new ArrayList<>();
        
        for (String hostIp : hostsToScan) {
            if (externalFuture.isCancelled()) break;

            // Phase 1: Fast scan / Check if already discovered
            CompletableFuture<Boolean> isUpFuture;
            if (mdnsByIpMap.containsKey(hostIp) || ssdpByIpMap.containsKey(hostIp)) {
                long completed = totalScannedPorts.addAndGet(p1PortsPerHost);
                if (listener != null) {
                    listener.onProgress((double) completed / totalPorts, hostIp);
                }
                isUpFuture = CompletableFuture.completedFuture(true);
            } else {
                isUpFuture = scanHostInternal(hostIp, normalConfig, new NetworkScanner.ScanProgressListener() {
                    @Override public void onProgress(double progress, String currentIp) {}
                    @Override public void onPortScanned(String host, int port) {
                        long completed = totalScannedPorts.incrementAndGet();
                        if (listener != null) {
                            listener.onProgress((double) completed / totalPorts, "Pre-scanning: " + host);
                        }
                    }
                }, mdnsByIpMap, ssdpByIpMap).thenApply(device -> device.isReachable() || !device.getServices().isEmpty());
            }

            CompletableFuture<DiscoveredDevice> fullScanFuture = isUpFuture.thenCompose(isUp -> {
                if (externalFuture.isCancelled()) return CompletableFuture.failedFuture(new java.util.concurrent.CancellationException());
                
                NetworkScanner.ScanProgressListener phase2Listener = new NetworkScanner.ScanProgressListener() {
                    @Override public void onProgress(double progress, String currentIp) {}
                    @Override public void onDeviceDiscovered(DiscoveredDevice device) {
                        if (listener != null) listener.onDeviceDiscovered(device);
                    }
                    @Override public void onDeviceUpdated(DiscoveredDevice device) {
                        if (listener != null) listener.onDeviceUpdated(device);
                    }
                    @Override public void onPortDiscovered(String host, PortScanResult portResult) {
                        if (listener != null) listener.onPortDiscovered(host, portResult);
                    }
                    @Override public void onPortScanned(String host, int port) {
                        long completed = totalScannedPorts.incrementAndGet();
                        if (listener != null) {
                            listener.onProgress((double) completed / totalPorts, "Deep scanning: " + host);
                        }
                    }
                };

                if (isUp) {
                    // Phase 2: Prioritized full scan
                    return scanHostInternal(hostIp, config, phase2Listener, mdnsByIpMap, ssdpByIpMap);
                } else {
                    // Phase 3: Full scan for others (source of truth)
                    return scanHostInternal(hostIp, config, phase2Listener, mdnsByIpMap, ssdpByIpMap);
                }
            });
            
            fullScanFutures.add(fullScanFuture);
            scanTracker.track(fullScanFuture);
        }

        CompletableFuture.allOf(fullScanFutures.toArray(new CompletableFuture<?>[0]))
                .thenRun(() -> {
                    List<DiscoveredDevice> results = fullScanFutures.stream()
                            .map(f -> {
                                try { return f.join(); } catch (Exception e) { return null; }
                            })
                            .filter(d -> d != null && !d.getServices().isEmpty())
                            .collect(Collectors.toList());
                    externalFuture.complete(results);
                })
                .exceptionally(ex -> {
                    externalFuture.completeExceptionally(ex);
                    return null;
                });
    }

    private CompletableFuture<DiscoveredDevice> scanHostInternal(String host, ScanConfiguration currentConfig,
                                                                 NetworkScanner.ScanProgressListener listener,
                                                                 Map<String, List<MdnsServiceInfo>> mdnsByIpMap,
                                                                 Map<String, List<SsdpServiceInfo>> ssdpByIpMap) {
        HostScanTask task = new HostScanTask(host, currentConfig, useBannerGrabbing, listener, portScanner,
                serviceValidator, deviceEnricher, scanTracker, portScanSemaphore, scanExecutor);
        if (mdnsByIpMap.containsKey(host) || ssdpByIpMap.containsKey(host)) {
            task.setInitialReachable(true);
        }
        CompletableFuture<DiscoveredDevice> taskFuture = task.execute();
        scanTracker.track(taskFuture);

        return taskFuture.handle((device, ex) -> {
            if (ex != null) {
                if (ex instanceof java.util.concurrent.CompletionException) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                    throw new RuntimeException(cause);
                }
                if (ex instanceof RuntimeException) throw (RuntimeException) ex;
                throw new RuntimeException(ex);
            }

            // Final enrichment with discovery info
            List<MdnsServiceInfo> mdnsServices = mdnsByIpMap.get(host);
            if (mdnsServices != null) {
                deviceEnricher.enrichWithMdns(device, mdnsServices, false);
            }
            List<SsdpServiceInfo> ssdpServices = ssdpByIpMap.get(host);
            if (ssdpServices != null) {
                deviceEnricher.enrichWithSsdp(device, ssdpServices, false);
            }
            deviceEnricher.enrichFromPortScan(device, currentConfig);
            return device;
        });
    }
}
