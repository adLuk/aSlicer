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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NetworkInformationCollector collects information about network interfaces and their configured addresses.
 * It provides a comprehensive view of the machine's network configuration, including both IPv4 and IPv6 details,
 * and configured DNS servers.
 */
public class NetworkInformationCollector {

    private static final Logger LOGGER = Logger.getLogger(NetworkInformationCollector.class.getName());

    /**
     * Collects all available network interfaces and their details.
     * This method enumerates all network interfaces found on the machine and retrieves
     * their status, hardware addresses, and all associated IP addresses (IPv4 and IPv6).
     *
     * @return an unmodifiable list of {@link NetworkInterfaceInfo} objects representing each interface
     */
    public List<NetworkInterfaceInfo> collect() {
        List<NetworkInterfaceInfo> interfaces = new ArrayList<>();
        try {
            // Utilizing standard Java for network interface enumeration as it is reliable across platforms.
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            if (networkInterfaces == null) {
                return Collections.emptyList();
            }

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                interfaces.add(createInterfaceInfo(ni));
            }
        } catch (SocketException e) {
            LOGGER.log(Level.SEVERE, "Failed to collect network interfaces", e);
        }
        return List.copyOf(interfaces);
    }

    /**
     * Returns a list of configured DNS server addresses discovered on the system.
     * This method delegates to {@link DnsInformation} which uses Netty's discovery mechanisms.
     *
     * @return an unmodifiable list of DNS server IP addresses
     */
    public List<String> getConfiguredDnsServers() {
        return new DnsInformation().getConfiguredDnsServers();
    }

    /**
     * Creates a {@link NetworkInterfaceInfo} from a {@link NetworkInterface}.
     *
     * @param ni the network interface to process
     * @return the populated information object
     * @throws SocketException if an I/O error occurs while retrieving interface details
     */
    private NetworkInterfaceInfo createInterfaceInfo(NetworkInterface ni) throws SocketException {
        List<NetworkAddressInfo> addresses = new ArrayList<>();
        Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
        while (inetAddresses.hasMoreElements()) {
            InetAddress addr = inetAddresses.nextElement();
            addresses.add(new NetworkAddressInfo(
                    addr.getHostAddress(),
                    addr.getHostName(),
                    addr instanceof Inet4Address
            ));
        }

        String macAddress = null;
        byte[] hardwareAddress = ni.getHardwareAddress();
        if (hardwareAddress != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hardwareAddress.length; i++) {
                sb.append(String.format("%02X%s", hardwareAddress[i], (i < hardwareAddress.length - 1) ? ":" : ""));
            }
            macAddress = sb.toString();
        }

        return new NetworkInterfaceInfo(
                ni.getName(),
                ni.getDisplayName(),
                macAddress,
                addresses,
                ni.isLoopback(),
                ni.isUp(),
                ni.isVirtual()
        );
    }
}
