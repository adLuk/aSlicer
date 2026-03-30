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
package cz.ad.print3d.aslicer.logic.net.scanner.dto;

import java.util.Objects;

/**
 * Data transfer object representing the result of scanning a single network port.
 */
public class PortScanResult {

    private final int port;
    private final boolean open;
    private final String service;
    private final String serviceDetails;

    /**
     * Constructs a new PortScanResult.
     *
     * @param port the port number that was scanned
     * @param open true if the port is open and accepting connections, false otherwise
     */
    public PortScanResult(int port, boolean open) {
        this(port, open, null, null);
    }

    /**
     * Constructs a new PortScanResult with service information.
     *
     * @param port    the port number that was scanned
     * @param open    true if the port is open and accepting connections, false otherwise
     * @param service an optional description of the service identified on the port
     */
    public PortScanResult(int port, boolean open, String service) {
        this(port, open, service, null);
    }

    /**
     * Constructs a new PortScanResult with service and details information.
     *
     * @param port           the port number that was scanned
     * @param open           true if the port is open and accepting connections, false otherwise
     * @param service        an optional description of the service identified on the port
     * @param serviceDetails optional details about the service
     */
    public PortScanResult(int port, boolean open, String service, String serviceDetails) {
        this.port = port;
        this.open = open;
        this.service = service;
        this.serviceDetails = serviceDetails;
    }

    /**
     * Returns the scanned port number.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Indicates whether the port is open.
     *
     * @return true if the port is open, false otherwise
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * Returns an optional description of the service found on the port.
     *
     * @return the service description, or null if unknown
     */
    public String getService() {
        return service;
    }

    /**
     * Returns optional details about the service identified on the port.
     *
     * @return the service details, or null if none
     */
    public String getServiceDetails() {
        return serviceDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortScanResult that = (PortScanResult) o;
        return port == that.port && open == that.open && Objects.equals(service, that.service) && Objects.equals(serviceDetails, that.serviceDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(port, open, service, serviceDetails);
    }

    @Override
    public String toString() {
        return "PortScanResult{" +
                "port=" + port +
                ", open=" + open +
                ", service='" + service + '\'' +
                ", serviceDetails='" + serviceDetails + '\'' +
                '}';
    }
}
