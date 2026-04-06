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
import cz.ad.print3d.aslicer.logic.net.scanner.dto.ScanConfiguration;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

/**
 * Task responsible for scanning a single network host for open ports and identifying services.
 * Encapsulates the logic for sequential or parallel port scanning on a host.
 */
public class HostScanTask {

    private final String host;
    private final ScanConfiguration config;
    private final boolean useBannerGrabbing;
    private final NetworkScanner.ScanProgressListener listener;
    private final PortScanner portScanner;
    private final ServiceValidator serviceValidator;
    private final ScanTracker scanTracker;
    private final Semaphore portScanSemaphore;
    private final java.util.concurrent.Executor scanExecutor;
    private int deepScanThreshold = 100;
    private final java.util.concurrent.atomic.AtomicInteger completedPorts = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicBoolean notifiedDiscovery = new java.util.concurrent.atomic.AtomicBoolean(false);
    private boolean initialReachable = false;

    /**
     * Constructs a new HostScanTask.
     *
     * @param host              the host IP address or name
     * @param config            the scan configuration
     * @param useBannerGrabbing true to attempt banner grabbing
     * @param listener          progress listener
     * @param portScanner       the port scanner implementation
     * @param serviceValidator  the service validator implementation
     * @param scanTracker       tracker for active futures
     * @param portScanSemaphore semaphore to limit concurrent port scans
     * @param scanExecutor      executor for port scanning tasks
     */
    public HostScanTask(String host, ScanConfiguration config, boolean useBannerGrabbing,
                        NetworkScanner.ScanProgressListener listener, PortScanner portScanner,
                        ServiceValidator serviceValidator, ScanTracker scanTracker,
                        Semaphore portScanSemaphore, java.util.concurrent.Executor scanExecutor) {
        this.host = host;
        this.config = config;
        this.useBannerGrabbing = useBannerGrabbing;
        this.listener = listener;
        this.portScanner = portScanner;
        this.serviceValidator = serviceValidator;
        this.scanTracker = scanTracker;
        this.portScanSemaphore = portScanSemaphore;
        this.scanExecutor = scanExecutor;
    }

    /**
     * Sets the threshold for deep scan optimization.
     * If the number of ports to scan exceeds this threshold, an "up check" will be performed first.
     *
     * @param threshold the threshold
     */
    public void setDeepScanThreshold(int threshold) {
        this.deepScanThreshold = threshold;
    }

    /**
     * Sets whether the host is already known to be reachable (e.g. from mDNS).
     *
     * @param reachable true if reachable
     */
    public void setInitialReachable(boolean reachable) {
        this.initialReachable = reachable;
    }

    public CompletableFuture<DiscoveredDevice> execute() {
        Set<Integer> ports = config.getAllPorts();
        DiscoveredDevice device = new DiscoveredDevice(host);
        device.setReachable(initialReachable);

        // Perform an "up check" to identify if the host is alive.
        // This acts as an accelerator for the discovery process.
        CompletableFuture<Boolean> upCheckFuture;
        if (initialReachable) {
            upCheckFuture = CompletableFuture.completedFuture(true);
            notifyDiscovery(device);
        } else {
            upCheckFuture = isHostUp(device);
        }
        scanTracker.track(upCheckFuture);

        return upCheckFuture.handle((isUp, ex) -> {
            // We always proceed with the port scan to ensure we have a "source of truth"
            // for the requested ports, but the upCheck result can be used by callers
            // (like RangeScanTask) to optimize or prioritize.
            return true;
        }).thenCompose(ignored -> {
            if (ports.isEmpty()) {
                return CompletableFuture.completedFuture(device);
            }

            List<Integer> portList = new ArrayList<>(ports);
            CompletableFuture<DiscoveredDevice> finalFuture = new CompletableFuture<>();
            scanTracker.track(finalFuture);

            // Use recursive approach to scan ports one by one or in small batches to avoid millions of futures
            scanPortsRecursively(portList, 0, device, finalFuture);

            return finalFuture;
        });
    }

    private void scanPortsRecursively(List<Integer> portList, int index, DiscoveredDevice device, CompletableFuture<DiscoveredDevice> finalFuture) {
        if (index >= portList.size() || finalFuture.isDone()) {
            if (!finalFuture.isDone()) {
                if (!device.getServices().isEmpty()) {
                    notifyDiscovery(device);
                }
                finalFuture.complete(device);
            }
            return;
        }

        // Scan in small batches to balance between concurrency and memory
        int batchSize = 50;
        int nextIndex = Math.min(index + batchSize, portList.size());
        List<CompletableFuture<PortScanResult>> batchFutures = new ArrayList<>();

        for (int i = index; i < nextIndex; i++) {
            int port = portList.get(i);
            CompletableFuture<PortScanResult> chainFuture = scanPortWithThrottling(port)
                    .thenApply(result -> {
                        if (result.isOpen() && useBannerGrabbing) {
                            return serviceValidator.validate(port, result.getServiceDetails(), config);
                        }
                        return result;
                    })
                    .thenApply(result -> {
                        completedPorts.incrementAndGet();
                        if (listener != null) {
                            listener.onPortScanned(host, port);
                        }
                        if (result.isOpen()) {
                            device.addService(result);
                            if (listener != null) {
                                listener.onPortDiscovered(host, result);
                                notifyDiscovery(device);
                            }
                        }
                        return result;
                    });
            batchFutures.add(chainFuture);
        }

        CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture<?>[0]))
                .thenRunAsync(() -> scanPortsRecursively(portList, nextIndex, device, finalFuture), scanExecutor)
                .exceptionally(ex -> {
                    finalFuture.completeExceptionally(ex);
                    return null;
                });
    }

    private CompletableFuture<PortScanResult> scanPortWithThrottling(int port) {
        CompletableFuture<Void> acquireFuture = new CompletableFuture<>();
        scanExecutor.execute(() -> {
            boolean acquired = false;
            try {
                portScanSemaphore.acquire();
                acquired = true;
                if (!acquireFuture.complete(null)) {
                    // Future was already cancelled or completed elsewhere
                    portScanSemaphore.release();
                }
            } catch (InterruptedException e) {
                if (acquired) {
                    portScanSemaphore.release();
                }
                Thread.currentThread().interrupt();
                acquireFuture.completeExceptionally(e);
            }
        });
        scanTracker.track(acquireFuture);

        return acquireFuture.thenCompose(v -> {
            CompletableFuture<PortScanResult> rawPortFuture = portScanner.scanPort(host, port, useBannerGrabbing);
            scanTracker.track(rawPortFuture);
            return rawPortFuture.handle((result, ex) -> {
                portScanSemaphore.release();
                if (ex != null) {
                    if (ex instanceof java.util.concurrent.CompletionException) {
                        Throwable cause = ex.getCause();
                        if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                        throw new RuntimeException(cause);
                    }
                    if (ex instanceof RuntimeException) throw (RuntimeException) ex;
                    throw new RuntimeException(ex);
                }
                return result;
            });
        }).exceptionally(ex -> {
            // Ensure semaphore is released if acquireFuture completed but thenCompose/rawPortFuture failed or was cancelled
            // before handle() could release it.
            // Note: If acquireFuture is cancelled, thenCompose doesn't run, so we handle it in runAsync.
            // If rawPortFuture is cancelled, handle() runs and releases.
            // This exceptionally block handles cases where thenCompose itself might fail.
            if (ex instanceof java.util.concurrent.CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                throw new RuntimeException(cause);
            }
            if (ex instanceof RuntimeException) throw (RuntimeException) ex;
            throw new RuntimeException(ex);
        });
    }

    private void notifyDiscovery(DiscoveredDevice device) {
        if (listener != null && notifiedDiscovery.compareAndSet(false, true)) {
            listener.onDeviceDiscovered(device);
        }
    }

    protected boolean checkReachable(String host, int timeout) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            return addr.isReachable(timeout);
        } catch (Exception ignored) {
            return false;
        }
    }

    private CompletableFuture<Boolean> isHostUp(DiscoveredDevice device) {
        return CompletableFuture.supplyAsync(() -> {
            if (checkReachable(host, portScanner.getTimeout())) {
                device.setReachable(true);
                return true;
            }
            return false;
        }, scanExecutor).thenCompose(reachable -> {
            if (reachable) return CompletableFuture.completedFuture(true);

            // Fallback: check a few common ports and required ports from profiles
            Set<Integer> checkPorts = new java.util.HashSet<>(List.of(80, 443, 22, 5000, 7125, 8883, 990));
            for (cz.ad.print3d.aslicer.logic.net.scanner.dto.PrinterDiscoveryProfile profile : config.getProfiles()) {
                checkPorts.addAll(profile.getRequiredPorts());
            }
            
            List<CompletableFuture<PortScanResult>> checks = new ArrayList<>();
            for (int p : checkPorts) {
                checks.add(scanPortWithThrottling(p));
            }

            return CompletableFuture.allOf(checks.toArray(new CompletableFuture<?>[0]))
                    .thenApply(v -> {
                        boolean anyOpen = checks.stream().anyMatch(f -> f.join().isOpen());
                        if (anyOpen) device.setReachable(true);
                        return anyOpen;
                    });
        });
    }
}
