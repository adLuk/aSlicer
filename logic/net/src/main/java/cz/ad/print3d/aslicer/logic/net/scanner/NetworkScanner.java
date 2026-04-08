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

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for scanning a network for devices with open ports.
 * <p>Implementations should support discovering devices through multiple methods,
 * such as mDNS (Multicast DNS) and direct port scanning. When scanning a range,
 * devices found through discovery methods should be prioritized in the scan order
 * to provide faster initial results.</p>
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

        /**
         * Called when information about a previously discovered device is updated.
         * This can be used to update existing results in real-time as more information
         * (e.g. from port scanning or enrichment) becomes available.
         *
         * @param device the updated device information
         */
        default void onDeviceUpdated(DiscoveredDevice device) {}

        /**
         * Called when an individual port is discovered on a host.
         *
         * @param host       the host IP address
         * @param portResult the result of the port scan
         */
        default void onPortDiscovered(String host, PortScanResult portResult) {}

        /**
         * Called when an individual port has been scanned, regardless of whether it's open or not.
         *
         * @param host the host IP address
         * @param port the port number
         */
        default void onPortScanned(String host, int port) {}
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
     * Scans a range of IP addresses for a set of ports.
     *
     * @param baseIp    the base IP address (e.g., "192.168.1.")
     * @param startHost the starting host number (inclusive, 1-254)
     * @param endHost   the ending host number (inclusive, 1-254)
     * @param config    the scan configuration containing profiles and ports
     * @return a CompletableFuture that completes with a list of discovered devices
     */
    default CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, ScanConfiguration config) {
        return scanRange(baseIp, startHost, endHost, new java.util.ArrayList<>(config.getAllPorts()));
    }

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
     * Scans a range of IP addresses for a set of ports with optional banner grabbing.
     *
     * @param baseIp            the base IP address (e.g., "192.168.1.")
     * @param startHost         the starting host number (inclusive, 1-254)
     * @param endHost           the ending host number (inclusive, 1-254)
     * @param config            the scan configuration containing profiles and ports
     * @param useBannerGrabbing true to attempt banner grabbing, false otherwise
     * @return a CompletableFuture that completes with a list of discovered devices
     */
    default CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, ScanConfiguration config, boolean useBannerGrabbing) {
        return scanRange(baseIp, startHost, endHost, new java.util.ArrayList<>(config.getAllPorts()), useBannerGrabbing);
    }

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
     * Scans a range of IP addresses for a set of ports with optional banner grabbing and progress monitoring.
     *
     * @param baseIp            the base IP address (e.g., "192.168.1.")
     * @param startHost         the starting host number (inclusive, 1-254)
     * @param endHost           the ending host number (inclusive, 1-254)
     * @param config            the scan configuration containing profiles and ports
     * @param useBannerGrabbing true to attempt banner grabbing, false otherwise
     * @param listener          listener to receive progress updates
     * @return a CompletableFuture that completes with a list of discovered devices
     */
    default CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, ScanConfiguration config, boolean useBannerGrabbing, ScanProgressListener listener) {
        return scanRange(baseIp, startHost, endHost, new java.util.ArrayList<>(config.getAllPorts()), useBannerGrabbing, listener);
    }

    /**
     * Scans a single host for a set of ports.
     *
     * @param host  the host IP address or name
     * @param ports the list of ports to scan
     * @return a CompletableFuture that completes with a DiscoveredDevice containing open ports
     */
    CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports);

    /**
     * Scans a single host for a set of ports.
     *
     * @param host   the host IP address or name
     * @param config the scan configuration containing profiles and ports
     * @return a CompletableFuture that completes with a DiscoveredDevice containing open ports
     */
    default CompletableFuture<DiscoveredDevice> scanHost(String host, ScanConfiguration config) {
        return scanHost(host, new java.util.ArrayList<>(config.getAllPorts()));
    }

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
     * Scans a single host for a set of ports with optional banner grabbing.
     *
     * @param host              the host IP address or name
     * @param config            the scan configuration containing profiles and ports
     * @param useBannerGrabbing true to attempt banner grabbing, false otherwise
     * @return a CompletableFuture that completes with a DiscoveredDevice containing open ports
     */
    default CompletableFuture<DiscoveredDevice> scanHost(String host, ScanConfiguration config, boolean useBannerGrabbing) {
        return scanHost(host, new java.util.ArrayList<>(config.getAllPorts()), useBannerGrabbing);
    }

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
     * Scans a single host for a set of ports with optional banner grabbing and progress monitoring.
     *
     * @param host              the host IP address or name
     * @param config            the scan configuration containing profiles and ports
     * @param useBannerGrabbing true to attempt banner grabbing, false otherwise
     * @param listener          listener to receive progress updates (for individual ports)
     * @return a CompletableFuture that completes with a DiscoveredDevice containing open ports
     */
    default CompletableFuture<DiscoveredDevice> scanHost(String host, ScanConfiguration config, boolean useBannerGrabbing, ScanProgressListener listener) {
        return scanHost(host, new java.util.ArrayList<>(config.getAllPorts()), useBannerGrabbing, listener);
    }

    /**
     * Sets the timeout for connection attempts during the scan.
     *
     * @param timeoutMillis the timeout in milliseconds
     */
    void setTimeout(int timeoutMillis);

    /**
     * Gets the current timeout for connection attempts.
     *
     * @return the timeout in milliseconds
     */
    int getTimeout();

    /**
     * Sets the timeout for mDNS discovery.
     *
     * @param timeoutMillis the timeout in milliseconds
     */
    void setMdnsTimeout(int timeoutMillis);

    /**
     * Gets the current timeout for mDNS discovery.
     *
     * @return the timeout in milliseconds
     */
    int getMdnsTimeout();

    /**
     * Sets the timeout for SSDP discovery.
     *
     * @param timeoutMillis the timeout in milliseconds
     */
    void setSsdpTimeout(int timeoutMillis);

    /**
     * Gets the current timeout for SSDP discovery.
     *
     * @return the timeout in milliseconds
     */
    int getSsdpTimeout();

    /**
     * Sets whether to include the local IP address of the machine performing the scan.
     *
     * @param include true to include self IP, false to exclude it
     */
    void setIncludeSelfIp(boolean include);

    /**
     * Gets whether to include the local IP address of the machine performing the scan.
     *
     * @return true if self IP is included, false otherwise
     */
    boolean isIncludeSelfIp();

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
