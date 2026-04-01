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

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for scanning a network for devices with open ports.
 */
public interface NetworkScanner extends AutoCloseable {

    /**
     * Interface for monitoring progress of a network scan.
     */
    interface ScanProgressListener {
        /**
         * Called when scan progress is updated.
         *
         * @param progress value from 0.0 to 1.0 representing progress
         * @param currentIp currently scanned IP address
         */
        void onProgress(double progress, String currentIp);

        /**
         * Called when a device is discovered during the scan.
         * This can be used to display results in real-time before the entire
         * scan completes.
         *
         * @param device the discovered device containing its IP and open services
         */
        default void onDeviceDiscovered(DiscoveredDevice device) {}
    }

    /**
     * Scans a range of IP addresses for a set of ports.
     *
     * @param baseIp    the base IP address (e.g., "192.168.1.")
     * @param startHost the starting host number (inclusive, 1-254)
     * @param endHost   the ending host number (inclusive, 1-254)
     * @param ports     the list of ports to scan on each IP
     * @return a CompletableFuture that completes with a list of discovered devices
     */
    CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports);

    /**
     * Scans a range of IP addresses for a set of ports with optional banner grabbing.
     *
     * @param baseIp            the base IP address (e.g., "192.168.1.")
     * @param startHost         the starting host number (inclusive, 1-254)
     * @param endHost           the ending host number (inclusive, 1-254)
     * @param ports             the list of ports to scan on each IP
     * @param useBannerGrabbing true to attempt banner grabbing, false otherwise
     * @return a CompletableFuture that completes with a list of discovered devices
     */
    CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing);

    /**
     * Scans a range of IP addresses for a set of ports with optional banner grabbing and progress monitoring.
     *
     * @param baseIp            the base IP address (e.g., "192.168.1.")
     * @param startHost         the starting host number (inclusive, 1-254)
     * @param endHost           the ending host number (inclusive, 1-254)
     * @param ports             the list of ports to scan on each IP
     * @param useBannerGrabbing true to attempt banner grabbing, false otherwise
     * @param listener          listener to receive progress updates
     * @return a CompletableFuture that completes with a list of discovered devices
     */
    CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener);

    /**
     * Scans a single host for a set of ports.
     *
     * @param host  the host IP address or name
     * @param ports the list of ports to scan
     * @return a CompletableFuture that completes with a DiscoveredDevice containing open ports
     */
    CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports);

    /**
     * Scans a single host for a set of ports with optional banner grabbing.
     *
     * @param host              the host IP address or name
     * @param ports             the list of ports to scan
     * @param useBannerGrabbing true to attempt banner grabbing, false otherwise
     * @return a CompletableFuture that completes with a DiscoveredDevice containing open ports
     */
    CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports, boolean useBannerGrabbing);

    /**
     * Scans a single host for a set of ports with optional banner grabbing and progress monitoring.
     *
     * @param host              the host IP address or name
     * @param ports             the list of ports to scan
     * @param useBannerGrabbing true to attempt banner grabbing, false otherwise
     * @param listener          listener to receive progress updates (for individual ports)
     * @return a CompletableFuture that completes with a DiscoveredDevice containing open ports
     */
    CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener);

    /**
     * Stops the current scan and cancels all active scanning futures.
     * This method will attempt to cancel all ongoing host and port scans
     * started by this scanner. It is recommended to call this method
     * before starting a new scan if one is already in progress.
     */
    void stopScan();

    @Override
    void close();
}
