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

import cz.ad.print3d.aslicer.logic.net.scanner.dto.SsdpServiceInfo;

import java.net.NetworkInterface;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for discovering devices on the local network using SSDP (Simple Service Discovery Protocol).
 */
public interface SsdpScanner extends AutoCloseable {
    /**
     * Interface for listening to discovered SSDP services in real-time.
     */
    interface SsdpDiscoveryListener {
        /**
         * Called when a new SSDP service is discovered.
         *
         * @param service the discovered service information
         */
        void onServiceDiscovered(SsdpServiceInfo service);
    }

    /**
     * Discovers devices on the local network using SSDP.
     *
     * @param timeoutMillis the timeout for the discovery process in milliseconds
     * @return a CompletableFuture that completes with a set of discovered services
     */
    default CompletableFuture<Set<SsdpServiceInfo>> discoverDevices(long timeoutMillis) {
        return discoverDevices(timeoutMillis, null, null);
    }

    /**
     * Discovers devices on the local network using SSDP with a listener for real-time updates.
     *
     * @param timeoutMillis the timeout for the discovery process in milliseconds
     * @param listener      the listener to receive discovery events
     * @return a CompletableFuture that completes with a set of discovered services
     */
    default CompletableFuture<Set<SsdpServiceInfo>> discoverDevices(long timeoutMillis, SsdpDiscoveryListener listener) {
        return discoverDevices(timeoutMillis, listener, null);
    }

    /**
     * Discovers devices on the local network using SSDP with a listener for real-time updates and a specific network interface.
     *
     * @param timeoutMillis the timeout for the discovery process in milliseconds
     * @param listener      the listener to receive discovery events
     * @param networkInterface the network interface to use for SSDP discovery, or null for all interfaces
     * @return a CompletableFuture that completes with a set of discovered services
     */
    CompletableFuture<Set<SsdpServiceInfo>> discoverDevices(long timeoutMillis, SsdpDiscoveryListener listener, NetworkInterface networkInterface);

    /**
     * Stops any currently ongoing SSDP discovery.
     */
    void stopScan();

    @Override
    void close();
}
