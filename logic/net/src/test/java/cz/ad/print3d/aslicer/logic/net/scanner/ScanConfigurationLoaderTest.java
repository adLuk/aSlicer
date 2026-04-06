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
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ScanConfigurationLoader}.
 */
public class ScanConfigurationLoaderTest {

    @Test
    void testLoadDefault() {
        ScanConfiguration config = ScanConfigurationLoader.loadDefault();
        assertNotNull(config, "Default configuration should not be null");
        
        // Check profiles
        assertFalse(config.getProfiles().isEmpty(), "Profiles should not be empty");
        
        Set<String> profileNames = config.getProfiles().stream()
                .map(PrinterDiscoveryProfile::getPrinterType)
                .collect(Collectors.toSet());

        assertTrue(profileNames.contains("Bambu Lab"), "Should contain Bambu Lab profile");
        assertTrue(profileNames.contains("Prusa"), "Should contain Prusa profile");
        assertTrue(profileNames.contains("Klipper"), "Should contain Klipper profile");
        assertTrue(profileNames.contains("OctoPrint"), "Should contain OctoPrint profile");

        // Check common ports
        assertFalse(config.getCommonPorts().isEmpty(), "Common ports should not be empty");
        assertTrue(config.getCommonPorts().contains(22), "Should contain SSH port 22");
        assertTrue(config.getCommonPorts().contains(80), "Should contain HTTP port 80");

        // Verify Bambu Lab specific ports
        PrinterDiscoveryProfile bambu = config.getProfiles().stream()
                .filter(p -> p.getPrinterType().equals("Bambu Lab"))
                .findFirst()
                .orElseThrow();
        
        Set<Integer> bambuPorts = bambu.getPorts().stream()
                .map(PortDiscoveryConfig::getPort)
                .collect(Collectors.toSet());
        
        assertTrue(bambuPorts.contains(8883), "Bambu Lab should have MQTT port 8883");
        assertTrue(bambuPorts.contains(2021), "Bambu Lab should have FTP port 2021");
        
        PortDiscoveryConfig mqtt = bambu.getPorts().stream()
                .filter(p -> p.getPort() == 8883)
                .findFirst()
                .orElseThrow();
        
        assertEquals("MQTT", mqtt.getServiceName());
        assertNotNull(mqtt.getValidationPattern());
        assertTrue(mqtt.getValidationPattern().matcher("Bambu Lab P1S").find());
    }
}
