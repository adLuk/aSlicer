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
import java.util.Objects;
import java.util.Set;

/**
 * Profile defining how to discover a specific type of 3D printer.
 * Contains a set of ports and validation criteria specific to a printer type.
 */
public class PrinterDiscoveryProfile {

    private final String printerType;
    private final Set<PortDiscoveryConfig> ports;
    private final Set<Integer> requiredPorts;

    /**
     * Constructs a new PrinterDiscoveryProfile.
     *
     * @param printerType the name or type of the printer (e.g., "OctoPrint", "Prusa")
     * @param ports       a set of ports commonly used by this printer type
     */
    public PrinterDiscoveryProfile(String printerType, Set<PortDiscoveryConfig> ports) {
        this(printerType, ports, Collections.emptySet());
    }

    /**
     * Constructs a new PrinterDiscoveryProfile with required ports.
     *
     * @param printerType   the name or type of the printer
     * @param ports         a set of ports commonly used by this printer type
     * @param requiredPorts a set of ports that must be open to identify this printer type
     */
    public PrinterDiscoveryProfile(String printerType, Set<PortDiscoveryConfig> ports, Set<Integer> requiredPorts) {
        this.printerType = printerType;
        this.ports = ports != null ? Collections.unmodifiableSet(ports) : Collections.emptySet();
        this.requiredPorts = requiredPorts != null ? Collections.unmodifiableSet(requiredPorts) : Collections.emptySet();
    }

    /**
     * Returns the printer type.
     *
     * @return the printer type
     */
    public String getPrinterType() {
        return printerType;
    }

    /**
     * Returns the set of port configurations for this printer type.
     *
     * @return the set of ports
     */
    public Set<PortDiscoveryConfig> getPorts() {
        return ports;
    }

    /**
     * Returns the set of required ports for identification.
     *
     * @return the set of required ports
     */
    public Set<Integer> getRequiredPorts() {
        return requiredPorts;
    }

    /**
     * Checks if the given set of open ports matches the required ports for this profile.
     *
     * @param openPorts the set of currently open ports
     * @return true if all required ports are open, false otherwise
     */
    public boolean matches(Set<Integer> openPorts) {
        if (requiredPorts.isEmpty()) {
            return false;
        }
        return openPorts.containsAll(requiredPorts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrinterDiscoveryProfile that = (PrinterDiscoveryProfile) o;
        return Objects.equals(printerType, that.printerType) && Objects.equals(ports, that.ports) && Objects.equals(requiredPorts, that.requiredPorts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(printerType, ports, requiredPorts);
    }

    @Override
    public String toString() {
        return "PrinterDiscoveryProfile{" +
                "printerType='" + printerType + '\'' +
                ", ports=" + ports +
                ", requiredPorts=" + requiredPorts +
                '}';
    }
}
