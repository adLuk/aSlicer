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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Data transfer object representing a service discovered via SSDP (Simple Service Discovery Protocol).
 * It contains detailed information about a device or service, such as its name,
 * type, IP address, port, and any additional metadata provided in UPnP description files.
 * 
 * <p>In the context of 3D printer discovery, SSDP provides a LOCATION header pointing to
 * an XML description file. This class captures information from both the SSDP response
 * and the parsed description file.</p>
 */
public class SsdpServiceInfo {

    private final String usn;
    private final String st;
    private final String location;
    private final String ipAddress;
    private final int port;
    private final String friendlyName;
    private final String manufacturer;
    private final String modelName;
    private final Map<String, String> attributes;

    /**
     * Constructs a new SsdpServiceInfo.
     *
     * @param usn           the Unique Service Name (USN)
     * @param st            the Search Target (ST)
     * @param location      the URL of the device description file
     * @param ipAddress     the IP address of the service
     * @param port          the port number of the service
     * @param friendlyName  the friendly name from description file
     * @param manufacturer  the manufacturer name from description file
     * @param modelName     the model name from description file
     * @param attributes    additional metadata from headers or description
     */
    public SsdpServiceInfo(String usn, String st, String location, String ipAddress, int port, 
                           String friendlyName, String manufacturer, String modelName, 
                           Map<String, String> attributes) {
        this.usn = usn;
        this.st = st;
        this.location = location;
        this.ipAddress = ipAddress;
        this.port = port;
        this.friendlyName = friendlyName;
        this.manufacturer = manufacturer;
        this.modelName = modelName;
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
    }

    public String getUsn() {
        return usn;
    }

    public String getSt() {
        return st;
    }

    public String getLocation() {
        return location;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModelName() {
        return modelName;
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SsdpServiceInfo that = (SsdpServiceInfo) o;
        return port == that.port &&
                Objects.equals(usn, that.usn) &&
                Objects.equals(st, that.st) &&
                Objects.equals(location, that.location) &&
                Objects.equals(ipAddress, that.ipAddress) &&
                Objects.equals(friendlyName, that.friendlyName) &&
                Objects.equals(manufacturer, that.manufacturer) &&
                Objects.equals(modelName, that.modelName) &&
                Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usn, st, location, ipAddress, port, friendlyName, manufacturer, modelName, attributes);
    }

    @Override
    public String toString() {
        return "SsdpServiceInfo{" +
                "usn='" + usn + '\'' +
                ", st='" + st + '\'' +
                ", location='" + location + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                ", friendlyName='" + friendlyName + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", modelName='" + modelName + '\'' +
                ", attributes=" + attributes +
                '}';
    }
}
