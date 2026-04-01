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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * NettyNetworkScanner provides functionality to scan a range of IP addresses for specific open ports.
 * It uses {@link NettyPortScanner} for individual port checks.
 */
public class NettyNetworkScanner implements NetworkScanner {

    private static final Logger LOGGER = Logger.getLogger(NettyNetworkScanner.class.getName());
    private static final int MAX_CONCURRENT_PORT_SCANS = 500;
    private static final long MDNS_TIMEOUT_MS = 1500;
    private static final int DEEP_SCAN_THRESHOLD = 100;
    private final PortScanner portScanner;
    private final MdnsScanner mdnsScanner;
    private final NetworkInformationCollector collector;
    private final List<CompletableFuture<?>> activeFutures = new CopyOnWriteArrayList<>();
    private final Semaphore portScanSemaphore = new Semaphore(MAX_CONCURRENT_PORT_SCANS);
    private boolean includeSelfIp = false;

    /**
     * Constructs a new NettyNetworkScanner with a default NettyPortScanner, NettyMdnsScanner,
     * and a default NetworkInformationCollector.
     */
    public NettyNetworkScanner() {
        this(new NettyPortScanner(), new NettyMdnsScanner(), new NetworkInformationCollector());
    }

    /**
     * Constructs a new NettyNetworkScanner with a provided NettyPortScanner and a default NettyMdnsScanner.
     *
     * @param portScanner the NettyPortScanner to use for connection attempts
     */
    public NettyNetworkScanner(PortScanner portScanner) {
        this(portScanner, new NettyMdnsScanner(), new NetworkInformationCollector());
    }

    /**
     * Constructs a new NettyNetworkScanner with a provided NettyPortScanner and MdnsScanner.
     *
     * @param portScanner the NettyPortScanner to use for connection attempts
     * @param mdnsScanner the MdnsScanner to use for mDNS discovery
     */
    public NettyNetworkScanner(PortScanner portScanner, MdnsScanner mdnsScanner) {
        this(portScanner, mdnsScanner, new NetworkInformationCollector());
    }

    /**
     * Constructs a new NettyNetworkScanner with a provided PortScanner, MdnsScanner, and NetworkInformationCollector.
     *
     * @param portScanner the PortScanner to use for connection attempts
     * @param mdnsScanner the MdnsScanner to use for mDNS discovery
     * @param collector the NetworkInformationCollector to use for local IP identification
     */
    public NettyNetworkScanner(PortScanner portScanner, MdnsScanner mdnsScanner, NetworkInformationCollector collector) {
        this.portScanner = portScanner;
        this.mdnsScanner = mdnsScanner;
        this.collector = collector;
    }

    @Override
    public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports) {
        return scanRange(baseIp, startHost, endHost, ports, false);
    }

    @Override
    public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing) {
        return scanRange(baseIp, startHost, endHost, ports, useBannerGrabbing, null);
    }

    /**
     * Scans a range of IP addresses for a set of ports.
     * This implementation initiates the scan asynchronously to avoid blocking the caller thread.
     *
     * @param baseIp    the base IP address (e.g., "192.168.1.")
     * @param startHost the starting host number (inclusive, 1-254)
     * @param endHost   the ending host number (inclusive, 1-254)
     * @param ports     the list of ports to scan on each IP
     * @param useBannerGrabbing true to attempt banner grabbing, false otherwise
     * @param listener          listener to receive progress updates (throttled for high volume)
     * @return a CompletableFuture that completes with a list of discovered devices
     */
    @Override
    public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener) {
        CompletableFuture<List<DiscoveredDevice>> externalFuture = new CompletableFuture<>();
        activeFutures.add(externalFuture);

        CompletableFuture.runAsync(() -> {
            try {
                if (listener != null) {
                    listener.onProgress(0.0, "Initiating mDNS discovery...");
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

                // Find matching NetworkInterface for mDNS
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

                Set<MdnsServiceInfo> discoveredServices = mdnsScanner.discoverDevices(MDNS_TIMEOUT_MS, service -> {
                    String ip = service.getIpAddress();
                    if (ip != null && ip.startsWith(basePrefix)) {
                        try {
                            int hostPart = Integer.parseInt(ip.substring(basePrefix.length()));
                            if (hostPart >= startHost && hostPart <= endHost) {
                                if (includeSelfIp || !finalSelfIps.contains(ip)) {
                                    DiscoveredDevice device = new DiscoveredDevice(ip);
                                    enrichDeviceWithMdns(device, List.of(service), true);
                                    if (listener != null) {
                                        listener.onDeviceDiscovered(device);
                                    }
                                }
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }, networkInterface).join();
                
                if (externalFuture.isCancelled()) return;

                Map<String, List<MdnsServiceInfo>> servicesByIp = discoveredServices.stream()
                        .collect(Collectors.groupingBy(MdnsServiceInfo::getIpAddress));

                Set<String> discoveredIps = servicesByIp.keySet();

                List<String> hostsToScan = new ArrayList<>();

                // Prioritize discovered IPs that are within the range
                for (String ip : discoveredIps) {
                    if (ip.startsWith(basePrefix)) {
                        try {
                            int hostPart = Integer.parseInt(ip.substring(basePrefix.length()));
                            if (hostPart >= startHost && hostPart <= endHost) {
                                if (includeSelfIp || !selfIps.contains(ip)) {
                                    hostsToScan.add(ip);
                                }
                            }
                        } catch (NumberFormatException ignored) {
                            // Not a simple host part, ignore or handle differently
                        }
                    }
                }

                // Add remaining IPs in the range
                for (int i = startHost; i <= endHost; i++) {
                    String ip = basePrefix + i;
                    if (!hostsToScan.contains(ip)) {
                        if (includeSelfIp || !selfIps.contains(ip)) {
                            hostsToScan.add(ip);
                        }
                    }
                }

                List<CompletableFuture<DiscoveredDevice>> hostFutures = new ArrayList<>();
                int totalHosts = hostsToScan.size();
                
                // Pre-calculate total ports to scan for accurate progress
                long totalPortsToScan = 0;
                Map<String, List<Integer>> hostPortsMap = new HashMap<>();
                for (String ip : hostsToScan) {
                    List<MdnsServiceInfo> hostMdnsServices = servicesByIp.get(ip);
                    List<Integer> hostPorts;
                    if (hostMdnsServices != null && !hostMdnsServices.isEmpty()) {
                        List<Integer> mdnsPorts = hostMdnsServices.stream()
                                .map(MdnsServiceInfo::getPort)
                                .filter(p -> p > 0)
                                .distinct()
                                .collect(Collectors.toList());
                        
                        hostPorts = new ArrayList<>(mdnsPorts);
                        for (int p : ports) {
                            if (!mdnsPorts.contains(p)) {
                                hostPorts.add(p);
                            }
                        }
                    } else {
                        hostPorts = ports;
                    }
                    hostPortsMap.put(ip, hostPorts);
                    totalPortsToScan += hostPorts.size();
                }
                
                final long totalPorts = totalPortsToScan;
                java.util.concurrent.atomic.AtomicLong completedPorts = new java.util.concurrent.atomic.AtomicLong(0);
                java.util.concurrent.atomic.AtomicLong lastProgressUpdate = new java.util.concurrent.atomic.AtomicLong(0);

                for (String ip : hostsToScan) {
                    if (externalFuture.isCancelled()) break;

                    List<MdnsServiceInfo> hostMdnsServices = servicesByIp.get(ip);
                    List<Integer> hostPorts = hostPortsMap.get(ip);
                    int hostPortsCount = hostPorts.size();

                    /**
                     * Tracks how many ports have already been accounted for this specific host
                     * to ensure global progress is incremented correctly even with skipped hosts.
                     */
                    java.util.concurrent.atomic.AtomicInteger hostAccountedPorts = new java.util.concurrent.atomic.AtomicInteger(0);
                    
                    CompletableFuture<DiscoveredDevice> hostFuture = scanHost(ip, hostPorts, useBannerGrabbing, new ScanProgressListener() {
                        @Override
                        public void onProgress(double progress, String currentIp) {
                            if (listener != null) {
                                // Calculate how many ports are completed for this host based on progress (0.0 to 1.0)
                                int currentCompletedForHost = (int) Math.round(progress * hostPortsCount);
                                int prevAccounted;
                                int newlyCompleted = 0;
                                
                                // Safely update hostAccountedPorts and calculate newlyCompleted
                                while ((prevAccounted = hostAccountedPorts.get()) < currentCompletedForHost) {
                                    if (hostAccountedPorts.compareAndSet(prevAccounted, currentCompletedForHost)) {
                                        newlyCompleted = currentCompletedForHost - prevAccounted;
                                        break;
                                    }
                                }

                                if (newlyCompleted > 0) {
                                    long completed = completedPorts.addAndGet(newlyCompleted);
                                    long now = System.currentTimeMillis();
                                    long last = lastProgressUpdate.get();
                                    double currentProgress = (double) completed / totalPorts;
                                    // Throttled progress update to avoid flooding the listener, 
                                    // but always allow final progress and significant jumps
                                    if (now - last > 100 || completed == totalPorts || Math.abs(currentProgress - 1.0) < 0.001) {
                                        if (lastProgressUpdate.compareAndSet(last, now)) {
                                            listener.onProgress(currentProgress, currentIp);
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        public void onDeviceDiscovered(DiscoveredDevice device) {
                            if (listener != null) {
                                listener.onDeviceDiscovered(device);
                            }
                        }

                        @Override
                        public void onPortDiscovered(String host, PortScanResult portResult) {
                            if (listener != null) {
                                // If this port was from mDNS, mark it as such
                                if (hostMdnsServices != null && hostMdnsServices.stream().anyMatch(s -> s.getPort() == portResult.getPort())) {
                                    PortScanResult enriched = new PortScanResult(
                                            portResult.getPort(),
                                            portResult.isOpen(),
                                            portResult.getService(),
                                            portResult.getServiceDetails(),
                                            true
                                    );
                                    listener.onPortDiscovered(host, enriched);
                                } else {
                                    listener.onPortDiscovered(host, portResult);
                                }
                            }
                        }
                    });
                    hostFutures.add(hostFuture);
                }

                CompletableFuture.allOf(hostFutures.toArray(new CompletableFuture<?>[0]))
                        .thenApply(v -> {
                            if (listener != null) {
                                String lastIp = basePrefix + endHost;
                                listener.onProgress(1.0, lastIp);
                            }
                            return hostFutures.stream()
                                    .map(CompletableFuture::join)
                                    .filter(device -> !device.getServices().isEmpty() || servicesByIp.containsKey(device.getIpAddress()))
                                    .peek(device -> {
                                        List<MdnsServiceInfo> services = servicesByIp.get(device.getIpAddress());
                                        if (services != null) {
                                            enrichDeviceWithMdns(device, services);
                                        }
                                    })
                                    .collect(Collectors.toList());
                        })
                        .thenAccept(externalFuture::complete)
                        .exceptionally(ex -> {
                            externalFuture.completeExceptionally(ex);
                            return null;
                        });

                externalFuture.whenComplete((res, ex) -> {
                    if (externalFuture.isCancelled()) {
                        hostFutures.forEach(f -> f.cancel(true));
                    }
                });
            } catch (Exception e) {
                externalFuture.completeExceptionally(e);
            }
        });

        externalFuture.whenComplete((res, ex) -> activeFutures.remove(externalFuture));
        return externalFuture;
    }

    @Override
    public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports) {
        return scanHost(host, ports, false);
    }

    @Override
    public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports, boolean useBannerGrabbing) {
        return scanHost(host, ports, useBannerGrabbing, null);
    }

    /**
     * Scans a single host for a set of ports with optional banner grabbing and progress monitoring.
     * This implementation initiates the scan asynchronously.
     *
     * @param host              the host IP address or name
     * @param ports             the list of ports to scan
     * @param useBannerGrabbing true to attempt banner grabbing, false otherwise
     * @param listener          listener to receive progress updates
     * @return a CompletableFuture that completes with a DiscoveredDevice containing open ports
     */
    @Override
    public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener) {
        CompletableFuture<DiscoveredDevice> externalFuture = new CompletableFuture<>();
        activeFutures.add(externalFuture);

        CompletableFuture<Boolean> upCheckFuture;
        if (ports.size() > DEEP_SCAN_THRESHOLD) {
            upCheckFuture = isHostUp(host);
            activeFutures.add(upCheckFuture);
            upCheckFuture.whenComplete((res, ex) -> activeFutures.remove(upCheckFuture));
        } else {
            upCheckFuture = CompletableFuture.completedFuture(true);
        }

        upCheckFuture.thenAcceptAsync(isUp -> {
            try {
                if (externalFuture.isCancelled()) return;

                if (!isUp) {
                    if (listener != null) {
                        listener.onProgress(1.0, host);
                    }
                    externalFuture.complete(new DiscoveredDevice(host));
                    return;
                }

                int totalPorts = ports.size();
                java.util.concurrent.atomic.AtomicInteger completedPorts = new java.util.concurrent.atomic.AtomicInteger(0);

                List<CompletableFuture<PortScanResult>> portFutures = new ArrayList<>();
                for (int port : ports) {
                    if (externalFuture.isCancelled() || Thread.currentThread().isInterrupted()) break;

                    try {
                        portScanSemaphore.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    CompletableFuture<PortScanResult> pf;
                    try {
                        pf = portScanner.scanPort(host, port, useBannerGrabbing);
                    } catch (Exception e) {
                        portScanSemaphore.release();
                        int completed = completedPorts.incrementAndGet();
                        if (listener != null) {
                            listener.onProgress((double) completed / totalPorts, host);
                        }
                        continue;
                    }

                    pf.whenComplete((res, ex) -> {
                        portScanSemaphore.release();
                        if (listener != null) {
                            if (res != null) {
                                listener.onPortDiscovered(host, res);
                            }
                            int completed = completedPorts.incrementAndGet();
                            listener.onProgress((double) completed / totalPorts, host);
                        }
                    });
                    portFutures.add(pf);
                }

                CompletableFuture.allOf(portFutures.toArray(new CompletableFuture<?>[0]))
                        .thenApply(v -> {
                            DiscoveredDevice device = new DiscoveredDevice(host);
                            portFutures.stream()
                                    .map(CompletableFuture::join)
                                    .filter(PortScanResult::isOpen)
                                    .forEach(device::addService);

                            if (listener != null && !device.getServices().isEmpty()) {
                                listener.onDeviceDiscovered(device);
                            }
                            return device;
                        })
                        .thenAccept(externalFuture::complete)
                        .exceptionally(ex -> {
                            externalFuture.completeExceptionally(ex);
                            return null;
                        });

                externalFuture.whenComplete((res, ex) -> {
                    if (externalFuture.isCancelled()) {
                        portFutures.forEach(f -> f.cancel(true));
                    }
                });
            } catch (Exception e) {
                externalFuture.completeExceptionally(e);
            }
        }).exceptionally(ex -> {
            externalFuture.completeExceptionally(ex);
            return null;
        });

        externalFuture.whenComplete((res, ex) -> activeFutures.remove(externalFuture));
        return externalFuture;
    }

    /**
     * Stops all currently ongoing scans. It iterates over all active futures
     * (range scans and host scans) and cancels them. This will also
     * propagate cancellation to the underlying port scans.
     */
    @Override
    public void stopScan() {
        for (CompletableFuture<?> future : activeFutures) {
            future.cancel(true);
        }
        activeFutures.clear();
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
    public void setIncludeSelfIp(boolean include) {
        this.includeSelfIp = include;
    }

    @Override
    public boolean isIncludeSelfIp() {
        return includeSelfIp;
    }

    /**
     * Checks if a host is up by trying to ping it and checking common ports.
     *
     * @param host the host to check
     * @return a CompletableFuture that completes with true if host is up, false otherwise
     */
    private CompletableFuture<Boolean> isHostUp(String host) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                InetAddress addr = InetAddress.getByName(host);
                // Try ping first (ICMP Echo or TCP Echo on port 7)
                if (addr.isReachable(getTimeout())) {
                    return true;
                }
            } catch (Exception ignored) {
            }
            return false;
        }).thenCompose(reachable -> {
            if (reachable) return CompletableFuture.completedFuture(true);

            // If ping failed, try a few common ports that are likely to be open on 3D printers or servers
            List<Integer> checkPorts = List.of(80, 443, 22, 5000, 7125);
            List<CompletableFuture<PortScanResult>> checks = new ArrayList<>();
            for (int p : checkPorts) {
                checks.add(portScanner.scanPort(host, p, false));
            }

            return CompletableFuture.allOf(checks.toArray(new CompletableFuture<?>[0]))
                    .thenApply(v -> checks.stream().anyMatch(f -> f.join().isOpen()));
        });
    }

    @Override
    public void close() {
        portScanner.close();
        mdnsScanner.close();
    }

    private void enrichDeviceWithMdns(DiscoveredDevice device, List<MdnsServiceInfo> services) {
        enrichDeviceWithMdns(device, services, false);
    }

    /**
     * Enriches a DiscoveredDevice with information from mDNS discovery.
     * Extracts name, vendor, and model from mDNS service info and TXT records.
     *
     * @param device           the device to enrich
     * @param services the list of mDNS services associated with this device's IP
     * @param markAsInProgress true to mark ports as verification in progress, false as verified
     */
    private void enrichDeviceWithMdns(DiscoveredDevice device, List<MdnsServiceInfo> services, boolean markAsInProgress) {
        for (MdnsServiceInfo service : services) {
            // Store the full mDNS service info for later inspection
            device.addMdnsService(service);

            // Set name if not already set
            if (device.getName() == null || device.getName().isEmpty()) {
                device.setName(service.getName());
            }

            Map<String, String> attrs = service.getAttributes();

            // Try to identify vendor from common TXT record keys
            if (device.getVendor() == null) {
                String vendor = attrs.get("mfg");
                if (vendor == null) vendor = attrs.get("manufacturer");
                if (vendor == null) vendor = attrs.get("usb_MFG");
                if (vendor != null) device.setVendor(vendor);
            }

            // Try to identify model from common TXT record keys
            if (device.getModel() == null) {
                String model = attrs.get("mdl");
                if (model == null) model = attrs.get("model");
                if (model == null) model = attrs.get("ty");
                if (model == null) model = attrs.get("product");
                if (model == null) model = attrs.get("usb_MDL");
                if (model != null) device.setModel(model);
            }

            // Add the service discovered by mDNS to the device's services list if not already present
            boolean portFound = device.getServices().stream().anyMatch(s -> s.getPort() == service.getPort());
            if (!portFound && service.getPort() > 0) {
                String serviceType = service.getType();
                // Strip leading underscore and .local suffix for better readability
                if (serviceType.startsWith("_") && serviceType.contains(".")) {
                    serviceType = serviceType.substring(1, serviceType.indexOf('.'));
                }
                device.addService(new PortScanResult(service.getPort(), !markAsInProgress, serviceType, "Discovered via mDNS", true));
            }
        }
    }
}
