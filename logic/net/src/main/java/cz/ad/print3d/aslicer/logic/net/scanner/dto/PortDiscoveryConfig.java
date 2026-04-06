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
import java.util.regex.Pattern;

/**
 * Configuration for discovery on a specific network port.
 * Defines the port number and optional validation criteria to identify the service.
 */
public class PortDiscoveryConfig {

    private final int port;
    private final String serviceName;
    private final Pattern validationPattern;

    /**
     * Constructs a new PortDiscoveryConfig with only a port number.
     *
     * @param port the port number to scan
     */
    public PortDiscoveryConfig(int port) {
        this(port, null, null);
    }

    /**
     * Constructs a new PortDiscoveryConfig with a port number and service name.
     *
     * @param port        the port number to scan
     * @param serviceName the expected service name
     */
    public PortDiscoveryConfig(int port, String serviceName) {
        this(port, serviceName, null);
    }

    /**
     * Constructs a new PortDiscoveryConfig with all validation criteria.
     *
     * @param port              the port number to scan
     * @param serviceName       the expected service name
     * @param validationPattern regex pattern to validate the service banner
     */
    public PortDiscoveryConfig(int port, String serviceName, Pattern validationPattern) {
        this.port = port;
        this.serviceName = serviceName;
        this.validationPattern = validationPattern;
    }

    /**
     * Returns the port number.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the expected service name.
     *
     * @return the service name, or null if not specified
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Returns the validation pattern for the service banner.
     *
     * @return the validation pattern, or null if not specified
     */
    public Pattern getValidationPattern() {
        return validationPattern;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortDiscoveryConfig that = (PortDiscoveryConfig) o;
        return port == that.port &&
                Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(validationPattern != null ? validationPattern.pattern() : null,
                        that.validationPattern != null ? that.validationPattern.pattern() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(port, serviceName, validationPattern != null ? validationPattern.pattern() : null);
    }

    @Override
    public String toString() {
        return "PortDiscoveryConfig{" +
                "port=" + port +
                ", serviceName='" + serviceName + '\'' +
                ", validationPattern=" + (validationPattern != null ? validationPattern.pattern() : "null") +
                '}';
    }
}
