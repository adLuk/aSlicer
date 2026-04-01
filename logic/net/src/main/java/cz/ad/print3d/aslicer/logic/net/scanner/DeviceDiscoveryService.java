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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * High-level service for discovering 3D printers and related services on the network.
 */
public class DeviceDiscoveryService {

    private static final List<Integer> COMMON_PRINTER_PORTS = Arrays.asList(
            22,   // SSH
            80,   // HTTP
            443,  // HTTPS
            3344, // Repetier-Server
            5000, // OctoPrint
            7080, // Alternative HTTP / others
            7125, // Moonraker (Klipper)
            8080, // MJPG-Streamer / Alternative HTTP
            8883  // MQTT over SSL (Bambu Lab, PrusaLink)
    );

    private final NetworkScanner networkScanner;

    /**
     * Constructs a new DeviceDiscoveryService with a default NettyNetworkScanner.
     */
    public DeviceDiscoveryService() {
        this(new NettyNetworkScanner());
    }

    /**
     * Constructs a new DeviceDiscoveryService with a provided NettyNetworkScanner.
     *
     * @param networkScanner the NettyNetworkScanner to use for discovery
     */
    public DeviceDiscoveryService(NetworkScanner networkScanner) {
        this.networkScanner = networkScanner;
    }

    /**
     * Discovers devices in the specified IP range that have common 3D printer ports open.
     *
     * @param baseIp    the base IP address (e.g., "192.168.1.")
     * @param startHost the starting host number
     * @param endHost   the ending host number
     * @return a CompletableFuture that completes with a list of discovered devices
     */
    public CompletableFuture<List<DiscoveredDevice>> discoverPrinters(String baseIp, int startHost, int endHost) {
        return discoverPrinters(baseIp, startHost, endHost, false);
    }

    /**
     * Discovers devices in the specified IP range that have common 3D printer ports open,
     * optionally using banner grabbing to identify services.
     *
     * @param baseIp            the base IP address (e.g., "192.168.1.")
     * @param startHost         the starting host number
     * @param endHost           the ending host number
     * @param useBannerGrabbing true to attempt banner grabbing, false otherwise
     * @return a CompletableFuture that completes with a list of discovered devices
     */
    public CompletableFuture<List<DiscoveredDevice>> discoverPrinters(String baseIp, int startHost, int endHost, boolean useBannerGrabbing) {
        return networkScanner.scanRange(baseIp, startHost, endHost, COMMON_PRINTER_PORTS, useBannerGrabbing);
    }

    /**
     * Closes the underlying scanner.
     */
    public void shutdown() {
        try {
            networkScanner.close();
        } catch (Exception e) {
            // Log or handle exception
        }
    }
}
