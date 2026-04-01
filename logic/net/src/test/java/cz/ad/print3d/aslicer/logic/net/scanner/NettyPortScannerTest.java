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

import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Sanity tests for {@link NettyPortScanner}.
 * <p>Verifies that the scanner correctly handles connection attempts to offline hosts
 * and respects timeouts.</p>
 */
class NettyPortScannerTest {

    private NettyPortScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new NettyPortScanner(200); // Short timeout for tests
    }

    @AfterEach
    void tearDown() {
        scanner.close();
    }

    @Test
    void testScanOfflineHost() throws Exception {
        // Use an IP that is likely to be offline or non-routable in a test environment
        String host = "192.0.2.1"; // TEST-NET-1
        int port = 80;

        CompletableFuture<PortScanResult> future = scanner.scanPort(host, port);
        PortScanResult result = future.get(1, TimeUnit.SECONDS);

        assertNotNull(result);
        assertFalse(result.isOpen(), "Port should be reported as closed for an offline host");
    }

    @Test
    void testTimeoutConfiguration() {
        scanner.setTimeout(300);
        assertEquals(300, scanner.getTimeout());
    }
}
