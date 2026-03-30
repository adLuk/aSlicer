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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Data transfer object representing a device discovered on the network.
 * It contains the IP address of the device and a list of services (open ports) found.
 */
public class DiscoveredDevice {

    private final String ipAddress;
    private final List<PortScanResult> services;

    /**
     * Constructs a new DiscoveredDevice.
     *
     * @param ipAddress the IP address of the discovered device
     */
    public DiscoveredDevice(String ipAddress) {
        this(ipAddress, new ArrayList<>());
    }

    /**
     * Constructs a new DiscoveredDevice with already discovered services.
     *
     * @param ipAddress the IP address of the discovered device
     * @param services  the list of services found on the device
     */
    public DiscoveredDevice(String ipAddress, List<PortScanResult> services) {
        this.ipAddress = ipAddress;
        this.services = new ArrayList<>(services);
    }

    /**
     * Returns the IP address of the device.
     *
     * @return the IP address
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Returns an unmodifiable list of services (open ports) discovered on the device.
     *
     * @return the list of services
     */
    public List<PortScanResult> getServices() {
        return Collections.unmodifiableList(services);
    }

    /**
     * Adds a service discovery result to this device.
     *
     * @param result the port scan result to add
     */
    public void addService(PortScanResult result) {
        if (result != null) {
            services.add(result);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscoveredDevice that = (DiscoveredDevice) o;
        return Objects.equals(ipAddress, that.ipAddress) && Objects.equals(services, that.services);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, services);
    }

    @Override
    public String toString() {
        return "DiscoveredDevice{" +
                "ipAddress='" + ipAddress + '\'' +
                ", services=" + services +
                '}';
    }
}
