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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main configuration for network scanning and 3D printer discovery.
 * Aggregates multiple printer discovery profiles and a set of common ports to scan.
 */
public class ScanConfiguration {

    private final List<PrinterDiscoveryProfile> profiles;
    private final Set<Integer> commonPorts;
    private final boolean deepScan;

    /**
     * Constructs a new ScanConfiguration.
     *
     * @param profiles    a list of printer discovery profiles
     * @param commonPorts a set of common ports to scan regardless of printer type
     */
    public ScanConfiguration(List<PrinterDiscoveryProfile> profiles, Set<Integer> commonPorts) {
        this(profiles, commonPorts, false);
    }

    /**
     * Constructs a new ScanConfiguration with optional deep scan flag.
     *
     * @param profiles    a list of printer discovery profiles
     * @param commonPorts a set of common ports to scan regardless of printer type
     * @param deepScan    true if this is a deep scan
     */
    public ScanConfiguration(List<PrinterDiscoveryProfile> profiles, Set<Integer> commonPorts, boolean deepScan) {
        this.profiles = profiles != null ? Collections.unmodifiableList(new ArrayList<>(profiles)) : Collections.emptyList();
        this.commonPorts = commonPorts != null ? Collections.unmodifiableSet(new HashSet<>(commonPorts)) : Collections.emptySet();
        this.deepScan = deepScan;
    }

    /**
     * Returns whether this configuration is for a deep scan.
     *
     * @return true if it is a deep scan
     */
    public boolean isDeepScan() {
        return deepScan;
    }

    /**
     * Returns the list of printer discovery profiles.
     *
     * @return the list of profiles
     */
    public List<PrinterDiscoveryProfile> getProfiles() {
        return profiles;
    }

    /**
     * Returns the set of common ports.
     *
     * @return the set of common ports
     */
    public Set<Integer> getCommonPorts() {
        return commonPorts;
    }

    /**
     * Returns all unique ports defined across all profiles and common ports.
     * If this is a deep scan, it returns all possible ports (1-65535).
     *
     * @return a set of all ports to scan
     */
    public Set<Integer> getAllPorts() {
        if (deepScan) {
            return new PortRangeSet(1, 65535);
        }
        Set<Integer> allPorts = new LinkedHashSet<>(commonPorts);
        for (PrinterDiscoveryProfile profile : profiles) {
            for (PortDiscoveryConfig portConfig : profile.getPorts()) {
                allPorts.add(portConfig.getPort());
            }
        }
        return allPorts;
    }

    /**
     * Returns a collection of port configurations for a specific port number from all profiles.
     *
     * @param port the port number
     * @return a collection of port discovery configurations for the given port
     */
    public Collection<PortDiscoveryConfig> getPortConfigs(int port) {
        return profiles.stream()
                .flatMap(p -> p.getPorts().stream())
                .filter(pc -> pc.getPort() == port)
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScanConfiguration that = (ScanConfiguration) o;
        return deepScan == that.deepScan && Objects.equals(profiles, that.profiles) && Objects.equals(commonPorts, that.commonPorts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profiles, commonPorts, deepScan);
    }

    @Override
    public String toString() {
        return "ScanConfiguration{" +
                "profiles=" + profiles +
                ", commonPorts=" + commonPorts +
                '}';
    }
}
