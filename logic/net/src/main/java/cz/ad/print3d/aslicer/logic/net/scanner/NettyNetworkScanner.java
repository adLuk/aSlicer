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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * NettyNetworkScanner provides functionality to scan a range of IP addresses for specific open ports.
 * It uses {@link NettyPortScanner} for individual port checks.
 */
public class NettyNetworkScanner implements NetworkScanner {

    private static final int MAX_CONCURRENT_PORT_SCANS = 500;
    private final PortScanner portScanner;
    private final List<CompletableFuture<?>> activeFutures = new CopyOnWriteArrayList<>();
    private final Semaphore portScanSemaphore = new Semaphore(MAX_CONCURRENT_PORT_SCANS);

    /**
     * Constructs a new NettyNetworkScanner with a default NettyPortScanner.
     */
    public NettyNetworkScanner() {
        this(new NettyPortScanner());
    }

    /**
     * Constructs a new NettyNetworkScanner with a provided NettyPortScanner.
     *
     * @param portScanner the NettyPortScanner to use for connection attempts
     */
    public NettyNetworkScanner(PortScanner portScanner) {
        this.portScanner = portScanner;
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
                List<CompletableFuture<DiscoveredDevice>> hostFutures = new ArrayList<>();
                int totalHosts = endHost - startHost + 1;
                long totalPorts = (long) totalHosts * ports.size();
                AtomicLong completedPorts = new AtomicLong(0);
                AtomicLong lastProgressUpdate = new AtomicLong(0);

                for (int i = startHost; i <= endHost; i++) {
                    if (externalFuture.isCancelled()) break;
                    String ip = (baseIp.endsWith(".") ? baseIp : baseIp + ".") + i;
                    CompletableFuture<DiscoveredDevice> hostFuture = scanHost(ip, ports, useBannerGrabbing, new ScanProgressListener() {
                        @Override
                        public void onProgress(double progress, String currentIp) {
                            if (listener != null) {
                                long completed = completedPorts.incrementAndGet();
                                long now = System.currentTimeMillis();
                                long last = lastProgressUpdate.get();
                                // Throttled progress update
                                if (now - last > 100 && lastProgressUpdate.compareAndSet(last, now)) {
                                    listener.onProgress((double) completed / totalPorts, currentIp);
                                }
                            }
                        }

                        @Override
                        public void onDeviceDiscovered(DiscoveredDevice device) {
                            if (listener != null) {
                                listener.onDeviceDiscovered(device);
                            }
                        }
                    });
                    hostFutures.add(hostFuture);
                }

                CompletableFuture.allOf(hostFutures.toArray(new CompletableFuture<?>[0]))
                        .thenApply(v -> {
                            if (listener != null) {
                                String lastIp = (baseIp.endsWith(".") ? baseIp : baseIp + ".") + endHost;
                                listener.onProgress(1.0, lastIp);
                            }
                            return hostFutures.stream()
                                    .map(CompletableFuture::join)
                                    .filter(device -> !device.getServices().isEmpty())
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

        CompletableFuture.runAsync(() -> {
            try {
                if (externalFuture.isCancelled()) return;

                int totalPorts = ports.size();
                java.util.concurrent.atomic.AtomicInteger completedPorts = new java.util.concurrent.atomic.AtomicInteger(0);
                java.util.concurrent.atomic.AtomicLong lastProgressUpdate = new java.util.concurrent.atomic.AtomicLong(0);

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
                        continue;
                    }
                    
                    pf.whenComplete((res, ex) -> {
                        portScanSemaphore.release();
                        if (listener != null) {
                            int completed = completedPorts.incrementAndGet();
                            // Report every completion to range listener
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
    public void close() {
        portScanner.close();
    }
}
