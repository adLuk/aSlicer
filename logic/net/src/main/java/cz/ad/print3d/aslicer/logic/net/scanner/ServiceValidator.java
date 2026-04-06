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
import cz.ad.print3d.aslicer.logic.net.scanner.dto.ScanConfiguration;
import io.netty.buffer.ByteBuf;

/**
 * Interface for validating network services and identifying 3D printers based on their response banners.
 */
public interface ServiceValidator {

    /**
     * Validates a service on a given port using the provided response banner and scan configuration.
     *
     * @param port        the port number
     * @param banner      the response banner received from the port (may be null)
     * @param config      the scan configuration containing discovery profiles
     * @return a PortScanResult with identified service information
     */
    PortScanResult validate(int port, String banner, ScanConfiguration config);

    /**
     * Validates a service on a given port using the provided ByteBuf message and scan configuration.
     *
     * @param port   the port number
     * @param msg    the ByteBuf containing the service response
     * @param config the scan configuration
     * @return a PortScanResult with identified service information
     */
    PortScanResult validate(int port, ByteBuf msg, ScanConfiguration config);
}
