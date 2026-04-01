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
package cz.ad.print3d.aslicer.logic.net.info;

/**
 * NetworkAddressInfo represents information about a single IP address and its associated hostname.
 * It supports both IPv4 and IPv6 addresses.
 */
public class NetworkAddressInfo {
    private final String address;
    private final String hostname;
    private final boolean ipv4;
    private final int prefixLength;

    /**
     * Constructs a new {@code NetworkAddressInfo}.
     *
     * @param address      the IP address string in textual representation
     * @param hostname     the hostname obtained from reverse DNS lookup
     * @param ipv4         {@code true} if it's an IPv4 address, {@code false} if it's an IPv6 address
     * @param prefixLength the network prefix length (subnet mask bits)
     */
    public NetworkAddressInfo(String address, String hostname, boolean ipv4, int prefixLength) {
        this.address = address;
        this.hostname = hostname;
        this.ipv4 = ipv4;
        this.prefixLength = prefixLength;
    }

    /**
     * Returns the IP address string in textual representation.
     *
     * @return the IP address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Returns the hostname associated with this address, usually obtained via a reverse DNS lookup.
     *
     * @return the hostname, or the IP address string if no hostname could be determined
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Returns whether this is an IPv4 address.
     *
     * @return {@code true} if IPv4, {@code false} if IPv6
     */
    public boolean isIpv4() {
        return ipv4;
    }

    /**
     * Returns whether this is an IPv6 address.
     *
     * @return {@code true} if IPv6, {@code false} if IPv4
     */
    public boolean isIpv6() {
        return !ipv4;
    }

    /**
     * Returns the network prefix length.
     *
     * @return the prefix length
     */
    public int getPrefixLength() {
        return prefixLength;
    }

    @Override
    public String toString() {
        return "NetworkAddressInfo{" +
                "address='" + address + '\'' +
                ", hostname='" + hostname + '\'' +
                ", ipv4=" + ipv4 +
                ", prefixLength=" + prefixLength +
                '}';
    }
}
