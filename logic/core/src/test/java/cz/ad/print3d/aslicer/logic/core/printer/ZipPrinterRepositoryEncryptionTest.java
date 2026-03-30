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
package cz.ad.print3d.aslicer.logic.core.printer;

import cz.ad.print3d.aslicer.logic.core.security.CryptoService;
import cz.ad.print3d.aslicer.logic.printer.Printer3D;
import cz.ad.print3d.aslicer.logic.printer.dto.Printer3DDto;
import cz.ad.print3d.aslicer.logic.printer.dto.PrinterSystemDto;
import cz.ad.print3d.aslicer.logic.printer.system.net.dto.BambuPrinterNetConnectionDto;
import cz.ad.print3d.aslicer.logic.printer.system.net.dto.NetworkPrinterNetConnectionDto;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ZipPrinterRepositoryEncryptionTest {

    @TempDir
    Path tempDir;

    private Path zipPath;
    private Path keyStorePath;
    private char[] keyStorePassword = "test-password".toCharArray();
    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        zipPath = tempDir.resolve("printers.zip");
        keyStorePath = tempDir.resolve("test.bcfks");
        cryptoService = new CryptoService(keyStorePath, keyStorePassword);
    }

    @Test
    void testEncryptionIntegration() throws IOException {
        ZipPrinterRepository repo = new ZipPrinterRepository(zipPath, null, true);

        String group = "SecureGroup";
        String name = "SecurePrinter";
        Printer3DDto printer = createSamplePrinter("Bambu", "X1C");

        BambuPrinterNetConnectionDto bambuConn = new BambuPrinterNetConnectionDto();
        bambuConn.setAccessCode("MY_SECRET_ACCESS_CODE");
        bambuConn.setSerial("SN123");
        printer.addNetConnection("Bambu", bambuConn);

        NetworkPrinterNetConnectionDto netConn = new NetworkPrinterNetConnectionDto();
        netConn.setPairingCode("PAIR_123");
        printer.addNetConnection("Network", netConn);

        repo.savePrinter(group, name, printer);

        // 1. Verify that we can read it back decrypted
        Optional<Printer3D> retrieved = repo.getPrinter(group, name);
        assertTrue(retrieved.isPresent());
        
        BambuPrinterNetConnectionDto retrievedBambu = (BambuPrinterNetConnectionDto) retrieved.get().getNetConnections().get("Bambu");
        assertEquals("MY_SECRET_ACCESS_CODE", retrievedBambu.getAccessCode());
        assertEquals("SN123", retrievedBambu.getSerial());

        NetworkPrinterNetConnectionDto retrievedNet = (NetworkPrinterNetConnectionDto) retrieved.get().getNetConnections().get("Network");
        assertEquals("PAIR_123", retrievedNet.getPairingCode());

        // 2. Verify that it is actually encrypted in the JSON file inside ZIP
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            FileHeader header = zipFile.getFileHeader(group + "/" + name + ".json");
            assertNotNull(header);
            try (InputStream is = zipFile.getInputStream(header)) {
                String jsonContent = new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A").next();
                
                // Serial should be in plain text
                assertTrue(jsonContent.contains("SN123"), "Serial should be in plain text");
                
                // Access code should NOT be in plain text
                assertFalse(jsonContent.contains("MY_SECRET_ACCESS_CODE"), "Access code should be encrypted");
                
                // Pairing code should NOT be in plain text
                assertFalse(jsonContent.contains("PAIR_123"), "Pairing code should be encrypted");
                
                // Verify we can manually decrypt it
                // Note: The actual property name in JSON might be different depending on Jackson config, 
                // but we used @JsonProperty("accessCode") in Mixin.
            }
        }
    }

    @Test
    void testNoEncryptionIfDisabled() throws IOException {
        ZipPrinterRepository repo = new ZipPrinterRepository(zipPath, null, false);

        String group = "PlainGroup";
        String name = "PlainPrinter";
        Printer3DDto printer = createSamplePrinter("Bambu", "X1C");

        BambuPrinterNetConnectionDto bambuConn = new BambuPrinterNetConnectionDto();
        bambuConn.setAccessCode("MY_PLAIN_ACCESS_CODE");
        printer.addNetConnection("Bambu", bambuConn);

        repo.savePrinter(group, name, printer);

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            FileHeader header = zipFile.getFileHeader(group + "/" + name + ".json");
            try (InputStream is = zipFile.getInputStream(header)) {
                String jsonContent = new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A").next();
                assertTrue(jsonContent.contains("MY_PLAIN_ACCESS_CODE"), "Access code should be in plain text when encryption is disabled");
            }
        }
    }

    private Printer3DDto createSamplePrinter(String manufacturer, String name) {
        Printer3DDto printer = new Printer3DDto();
        PrinterSystemDto system = new PrinterSystemDto();
        system.setPrinterManufacturer(manufacturer);
        system.setPrinterName(name);
        printer.setPrinterSystem(system);
        return printer;
    }
}
