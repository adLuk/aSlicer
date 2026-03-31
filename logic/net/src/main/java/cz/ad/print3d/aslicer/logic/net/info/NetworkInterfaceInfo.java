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

import java.util.Collections;
import java.util.List;

/**
 * NetworkInterfaceInfo represents information about a network interface and its associated addresses.
 * It provides details about the interface name, display name, MAC address, and status.
 */
public class NetworkInterfaceInfo {
    private final String name;
    private final String displayName;
    private final String macAddress;
    private final List<NetworkAddressInfo> addresses;
    private final boolean loopback;
    private final boolean up;
    private final boolean virtual;

    /**
     * Constructs a new {@code NetworkInterfaceInfo}.
     *
     * @param name        the system-assigned name for the interface (e.g., "eth0", "wlan0")
     * @param displayName a more descriptive name for the interface
     * @param macAddress  the hardware (MAC) address in colon-separated hexadecimal format (e.g., "01:23:45:67:89:AB")
     * @param addresses   list of {@link NetworkAddressInfo} containing IP addresses associated with this interface
     * @param loopback    {@code true} if the interface is a loopback interface
     * @param up          {@code true} if the interface is currently active and operational
     * @param virtual     {@code true} if the interface is a sub-interface or a virtual interface
     */
    public NetworkInterfaceInfo(String name, String displayName, String macAddress, List<NetworkAddressInfo> addresses, boolean loopback, boolean up, boolean virtual) {
        this.name = name;
        this.displayName = displayName;
        this.macAddress = macAddress;
        this.addresses = addresses != null ? List.copyOf(addresses) : Collections.emptyList();
        this.loopback = loopback;
        this.up = up;
        this.virtual = virtual;
    }

    /**
     * Returns the system-assigned interface name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the human-readable display name for the interface.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the hardware (MAC) address of the interface.
     *
     * @return the MAC address, or {@code null} if not available or not applicable
     */
    public String getMacAddress() {
        return macAddress;
    }

    /**
     * Returns an unmodifiable list of associated IP addresses for this interface.
     *
     * @return the list of {@link NetworkAddressInfo}
     */
    public List<NetworkAddressInfo> getAddresses() {
        return addresses;
    }

    /**
     * Checks if this interface is a loopback interface.
     *
     * @return {@code true} if loopback, {@code false} otherwise
     */
    public boolean isLoopback() {
        return loopback;
    }

    /**
     * Checks if the interface is currently up and running.
     *
     * @return {@code true} if up, {@code false} otherwise
     */
    public boolean isUp() {
        return up;
    }

    /**
     * Checks if the interface is virtual (e.g., a VLAN sub-interface or bridge).
     *
     * @return {@code true} if virtual, {@code false} otherwise
     */
    public boolean isVirtual() {
        return virtual;
    }

    @Override
    public String toString() {
        return "NetworkInterfaceInfo{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", macAddress='" + macAddress + '\'' +
                ", addresses=" + addresses +
                ", loopback=" + loopback +
                ", up=" + up +
                ", virtual=" + virtual +
                '}';
    }
}
