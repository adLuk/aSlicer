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

import cz.ad.print3d.aslicer.logic.net.scanner.dto.SsdpServiceInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for {@link NettySsdpScanner}.
 */
class NettySsdpScannerTest {

    private NettySsdpScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new NettySsdpScanner();
    }

    @AfterEach
    void tearDown() {
        scanner.close();
    }

    @Test
    void testDiscoverDevices() throws Exception {
        // Basic test to ensure it doesn't crash
        CompletableFuture<Set<SsdpServiceInfo>> future = scanner.discoverDevices(500);
        Set<SsdpServiceInfo> discoveredServices = future.get(5, TimeUnit.SECONDS);
        assertNotNull(discoveredServices);
    }

    @Test
    void testParseXmlDescription() throws Exception {
        String xml = "<?xml version=\"1.0\"?>\n" +
                "<root xmlns=\"urn:schemas-upnp-org:device-1-0\">\n" +
                "  <device>\n" +
                "    <friendlyName>Ender-3 V3 KE</friendlyName>\n" +
                "    <manufacturer>Creality</manufacturer>\n" +
                "    <modelName>Ender-3 V3 KE</modelName>\n" +
                "  </device>\n" +
                "</root>";

        ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        URL url = new URL("http://192.168.1.100:8080/description.xml");
        Map<String, String> headers = Collections.singletonMap("USN", "uuid:1234");

        SsdpServiceInfo info = scanner.parseXmlDescription(is, url, "uuid:1234", "upnp:rootdevice", "192.168.1.100", headers);

        assertNotNull(info);
        assertEquals("Ender-3 V3 KE", info.getFriendlyName());
        assertEquals("Creality", info.getManufacturer());
        assertEquals("Ender-3 V3 KE", info.getModelName());
        assertEquals("192.168.1.100", info.getIpAddress());
        assertEquals(8080, info.getPort());
        assertEquals("uuid:1234", info.getUsn());
    }
}
