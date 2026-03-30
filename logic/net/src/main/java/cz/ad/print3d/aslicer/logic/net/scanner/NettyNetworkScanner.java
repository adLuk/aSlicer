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
import java.util.stream.Collectors;

/**
 * NettyNetworkScanner provides functionality to scan a range of IP addresses for specific open ports.
 * It uses {@link NettyPortScanner} for individual port checks.
 */
public class NettyNetworkScanner implements NetworkScanner {

    private final PortScanner portScanner;

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
        List<CompletableFuture<DiscoveredDevice>> hostFutures = new ArrayList<>();

        for (int i = startHost; i <= endHost; i++) {
            String ip = (baseIp.endsWith(".") ? baseIp : baseIp + ".") + i;
            hostFutures.add(scanHost(ip, ports, useBannerGrabbing));
        }

        return CompletableFuture.allOf(hostFutures.toArray(new CompletableFuture<?>[0]))
                .thenApply(v -> hostFutures.stream()
                        .map(CompletableFuture::join)
                        .filter(device -> !device.getServices().isEmpty())
                        .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports) {
        return scanHost(host, ports, false);
    }

    @Override
    public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports, boolean useBannerGrabbing) {
        List<CompletableFuture<PortScanResult>> portFutures = ports.stream()
                .map(port -> portScanner.scanPort(host, port, useBannerGrabbing))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(portFutures.toArray(new CompletableFuture<?>[0]))
                .thenApply(v -> {
                    DiscoveredDevice device = new DiscoveredDevice(host);
                    portFutures.stream()
                            .map(CompletableFuture::join)
                            .filter(PortScanResult::isOpen)
                            .forEach(device::addService);
                    return device;
                });
    }

    @Override
    public void close() {
        portScanner.close();
    }
}
