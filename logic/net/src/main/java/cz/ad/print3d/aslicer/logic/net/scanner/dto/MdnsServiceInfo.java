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
 * Data transfer object representing a service discovered via mDNS (Multicast DNS).
 * It contains detailed information about a printer or service, such as its name,
 * type, IP address, port, and any additional metadata provided in TXT records.
 * 
 * <p>In the context of 3D printer discovery, mDNS often provides rich information in 
 * the ADDITIONAL section of the DNS response. This class captures:
 * <ul>
 *   <li>The service instance name (e.g., "Ender-3 V3 KE").</li>
 *   <li>The service type (e.g., "_http._tcp.local." or "_octoprint._tcp.local.").</li>
 *   <li>The network endpoint (IP address and port).</li>
 *   <li>Additional properties from TXT records (e.g., manufacturer, model, version).</li>
 * </ul>
 * </p>
 * 
 * <p>This information is used to help identify 3D printers and their capabilities
 * before performing a full port scan, allowing for a better user experience by 
 * displaying friendly names and vendor information.</p>
 */
public class MdnsServiceInfo {

    private final String name;
    private final String type;
    private final String ipAddress;
    private final int port;
    private final String hostname;
    private final Map<String, String> attributes;

    /**
     * Constructs a new MdnsServiceInfo.
     *
     * @param name       the service name (e.g., "Prusa_MK3S")
     * @param type       the service type (e.g., "_http._tcp.local.")
     * @param ipAddress  the IP address of the service
     * @param port       the port number of the service
     * @param hostname   the hostname of the service
     * @param attributes additional metadata from TXT records
     */
    public MdnsServiceInfo(String name, String type, String ipAddress, int port, String hostname, Map<String, String> attributes) {
        this.name = name;
        this.type = type;
        this.ipAddress = ipAddress;
        this.port = port;
        this.hostname = hostname;
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
    }

    /**
     * Returns the service name.
     *
     * @return the service name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the service type.
     *
     * @return the service type
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the IP address of the service.
     *
     * @return the IP address
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Returns the port number of the service.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the hostname of the service.
     *
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Returns an unmodifiable map of attributes (from TXT records).
     *
     * @return the attributes map
     */
    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MdnsServiceInfo that = (MdnsServiceInfo) o;
        return port == that.port &&
                Objects.equals(name, that.name) &&
                Objects.equals(type, that.type) &&
                Objects.equals(ipAddress, that.ipAddress) &&
                Objects.equals(hostname, that.hostname) &&
                Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, ipAddress, port, hostname, attributes);
    }

    @Override
    public String toString() {
        return "MdnsServiceInfo{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                ", hostname='" + hostname + '\'' +
                ", attributes=" + attributes +
                '}';
    }
}
