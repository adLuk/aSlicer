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

import cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.SsdpServiceInfo;

import java.net.NetworkInterface;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Manages various network discovery services such as mDNS and SSDP.
 * Coordinates discovery tasks and provides aggregated results.
 */
public class DiscoveryManager {

    private final MdnsScanner mdnsScanner;
    private final SsdpScanner ssdpScanner;

    /**
     * Constructs a new DiscoveryManager with provided scanners.
     *
     * @param mdnsScanner the mDNS scanner
     * @param ssdpScanner the SSDP scanner
     */
    public DiscoveryManager(MdnsScanner mdnsScanner, SsdpScanner ssdpScanner) {
        this.mdnsScanner = mdnsScanner;
        this.ssdpScanner = ssdpScanner;
    }

    /**
     * Discovers devices using mDNS.
     *
     * @param timeoutMs        the discovery timeout in milliseconds
     * @param listener         listener for each discovered mDNS service
     * @param networkInterface the network interface to use
     * @return a CompletableFuture that completes with the set of all discovered services
     */
    public CompletableFuture<Set<MdnsServiceInfo>> discoverMdns(long timeoutMs, MdnsScanner.MdnsDiscoveryListener listener, NetworkInterface networkInterface) {
        return mdnsScanner.discoverDevices(timeoutMs, listener, networkInterface);
    }

    /**
     * Discovers devices using SSDP.
     *
     * @param timeoutMs        the discovery timeout in milliseconds
     * @param listener         listener for each discovered SSDP service
     * @param networkInterface the network interface to use
     * @return a CompletableFuture that completes with the set of all discovered services
     */
    public CompletableFuture<Set<SsdpServiceInfo>> discoverSsdp(long timeoutMs, SsdpScanner.SsdpDiscoveryListener listener, NetworkInterface networkInterface) {
        return ssdpScanner.discoverDevices(timeoutMs, listener, networkInterface);
    }

    /**
     * Stops all active discovery tasks.
     */
    public void stopDiscovery() {
        mdnsScanner.stopScan();
        ssdpScanner.stopScan();
    }

    /**
     * Closes all discovery scanners and releases resources.
     */
    public void close() {
        stopDiscovery();
        mdnsScanner.close();
        ssdpScanner.close();
    }
}
