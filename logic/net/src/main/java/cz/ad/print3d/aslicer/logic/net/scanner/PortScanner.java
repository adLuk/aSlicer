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

import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for scanning ports on a network host.
 */
public interface PortScanner extends AutoCloseable {
    /**
     * Scans a single port on the given host.
     *
     * @param host the host IP address or name
     * @param port the port number to scan
     * @return a CompletableFuture that completes with a PortScanResult
     */
    CompletableFuture<PortScanResult> scanPort(String host, int port);

    /**
     * Scans a single port on the given host with optional banner grabbing.
     *
     * @param host               the host IP address or name
     * @param port               the port number to scan
     * @param useBannerGrabbing true to attempt banner grabbing, false otherwise
     * @return a CompletableFuture that completes with a PortScanResult
     */
    CompletableFuture<PortScanResult> scanPort(String host, int port, boolean useBannerGrabbing);

    @Override
    void close();
}
