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
package cz.ad.print3d.aslicer.logic.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CryptoServiceTest {

    @TempDir
    Path tempDir;

    private Path keyStorePath;
    private char[] password = "test-password".toCharArray();
    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        keyStorePath = tempDir.resolve("test.bcfks");
        cryptoService = new CryptoService(keyStorePath, password);
    }

    @Test
    void testEncryptDecrypt() {
        String originalText = "SensitiveData123";
        String encrypted = cryptoService.encrypt(originalText);
        
        assertNotNull(encrypted);
        assertNotEquals(originalText, encrypted);
        
        String decrypted = cryptoService.decrypt(encrypted);
        assertEquals(originalText, decrypted);
    }

    @Test
    void testEncryptDecryptNull() {
        assertNull(cryptoService.encrypt(null));
        assertNull(cryptoService.decrypt(null));
    }

    @Test
    void testKeyStorePersistence() throws IOException {
        String originalText = "SensitiveData123";
        String encrypted = cryptoService.encrypt(originalText);
        
        // Re-initialize crypto service from the same key store
        CryptoService newService = new CryptoService(keyStorePath, password);
        String decrypted = newService.decrypt(encrypted);
        
        assertEquals(originalText, decrypted);
    }

    @Test
    void testDecryptInvalidData() {
        assertThrows(SecurityException.class, () -> cryptoService.decrypt("not-base64-and-not-encrypted"));
        assertThrows(SecurityException.class, () -> cryptoService.decrypt("SGVsbG8=")); // "Hello" in Base64, too short for IV
    }
}
