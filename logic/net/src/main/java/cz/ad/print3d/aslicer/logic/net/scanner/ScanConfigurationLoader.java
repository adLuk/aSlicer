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
package cz.ad.print3d.aslicer.logic.net.scanner;

import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortDiscoveryConfig;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.PrinterDiscoveryProfile;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.ScanConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for loading {@link ScanConfiguration} from properties files.
 */
public class ScanConfigurationLoader {

    private static final Logger LOGGER = Logger.getLogger(ScanConfigurationLoader.class.getName());
    private static final String DEFAULT_CONFIG_PATH = "/cz/ad/print3d/aslicer/logic/net/scanner/discovery.properties";

    /**
     * Loads the default scan configuration from the classpath resource.
     *
     * @return the loaded ScanConfiguration, or an empty one if loading fails
     */
    public static ScanConfiguration loadDefault() {
        try (InputStream is = ScanConfigurationLoader.class.getResourceAsStream(DEFAULT_CONFIG_PATH)) {
            if (is == null) {
                LOGGER.log(Level.WARNING, "Default discovery configuration not found at {0}", DEFAULT_CONFIG_PATH);
                return new ScanConfiguration(Collections.emptyList(), Collections.emptySet());
            }
            Properties props = new Properties();
            props.load(is);
            return parse(props);
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Failed to load default discovery configuration", e);
            return new ScanConfiguration(Collections.emptyList(), Collections.emptySet());
        }
    }

    /**
     * Parses a {@link ScanConfiguration} from the provided properties.
     *
     * @param props the properties to parse
     * @return the parsed ScanConfiguration
     */
    public static ScanConfiguration parse(Properties props) {
        // Parse common ports
        String commonPortsStr = props.getProperty("common.ports", "");
        Set<Integer> commonPorts = Arrays.stream(commonPortsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());

        // Parse profiles
        String profilesStr = props.getProperty("profiles", "");
        List<String> profileIds = Arrays.stream(profilesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        List<PrinterDiscoveryProfile> profiles = new ArrayList<>();
        for (String id : profileIds) {
            String name = props.getProperty("profile." + id + ".name", id);
            String portsStr = props.getProperty("profile." + id + ".ports", "");
            Set<Integer> portNumbers = Arrays.stream(portsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());

            Set<PortDiscoveryConfig> portConfigs = new HashSet<>();
            for (Integer port : portNumbers) {
                String service = props.getProperty("profile." + id + ".port." + port + ".service");
                String patternStr = props.getProperty("profile." + id + ".port." + port + ".pattern");
                Pattern pattern = (patternStr != null && !patternStr.isEmpty()) ? Pattern.compile(patternStr) : null;
                portConfigs.add(new PortDiscoveryConfig(port, service, pattern));
            }
            profiles.add(new PrinterDiscoveryProfile(name, portConfigs));
        }

        return new ScanConfiguration(profiles, commonPorts);
    }
}
