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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceIdentifierTest {

    @Test
    void testIdentifySSH() {
        ByteBuf buf = Unpooled.copiedBuffer("SSH-2.0-OpenSSH_8.2p1 Ubuntu-4ubuntu0.5", StandardCharsets.UTF_8);
        ServiceIdentifier.ServiceInfo info = ServiceIdentifier.identify(buf);
        assertEquals("SSH", info.getName());
        assertTrue(info.getDetails().contains("2.0"));
        assertTrue(info.getDetails().contains("OpenSSH_8.2p1"));
    }

    @Test
    void testIdentifyHTTP() {
        ByteBuf buf = Unpooled.copiedBuffer("HTTP/1.1 200 OK\r\nServer: nginx", StandardCharsets.UTF_8);
        ServiceIdentifier.ServiceInfo info = ServiceIdentifier.identify(buf);
        assertEquals("HTTP", info.getName());
        assertTrue(info.getDetails().contains("1.1"));
        assertTrue(info.getDetails().contains("200 OK"));
    }

    @Test
    void testIdentifyFTP() {
        ByteBuf buf = Unpooled.copiedBuffer("220 (vsFTPd 3.0.3)", StandardCharsets.UTF_8);
        ServiceIdentifier.ServiceInfo info = ServiceIdentifier.identify(buf);
        assertEquals("FTP/SMTP", info.getName());
        assertEquals("(vsFTPd 3.0.3)", info.getDetails());
    }

    @Test
    void testIdentifyMySQL() {
        // MySQL Handshake: length (3 bytes), seq (1 byte), protocol (1 byte)
        byte[] mysql = new byte[] { 0x4a, 0x00, 0x00, 0x00, 0x0a, 0x35, 0x2e, 0x37, 0x2e, 0x32, 0x31 };
        ByteBuf buf = Unpooled.wrappedBuffer(mysql);
        ServiceIdentifier.ServiceInfo info = ServiceIdentifier.identify(buf);
        assertEquals("MySQL", info.getName());
        assertEquals("Protocol Version: 10", info.getDetails());
    }

    @Test
    void testIdentifyGIOP() {
        byte[] giop = new byte[] { 'G', 'I', 'O', 'P', 0x01, 0x02, 0x00, 0x01 };
        ByteBuf buf = Unpooled.wrappedBuffer(giop);
        ServiceIdentifier.ServiceInfo info = ServiceIdentifier.identify(buf);
        assertEquals("CORBA/GIOP", info.getName());
        assertEquals("Version 1.2", info.getDetails());
    }

    @Test
    void testIdentifyBinaryFallback() {
        byte[] random = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };
        ByteBuf buf = Unpooled.wrappedBuffer(random);
        ServiceIdentifier.ServiceInfo info = ServiceIdentifier.identify(buf);
        assertEquals("Binary Data", info.getName());
        assertTrue(info.getDetails().startsWith("Hex: 0102030405"));
    }

    @Test
    void testIdentifyTextBanner() {
        ByteBuf buf = Unpooled.copiedBuffer("Welcome to my custom service", StandardCharsets.UTF_8);
        ServiceIdentifier.ServiceInfo info = ServiceIdentifier.identify(buf);
        assertEquals("Text Banner", info.getName());
        assertEquals("Welcome to my custom service", info.getDetails());
    }

    @Test
    void testIdentifyNoData() {
        ServiceIdentifier.ServiceInfo info = ServiceIdentifier.identify(null);
        assertEquals("Unknown", info.getName());
        assertEquals("No data received", info.getDetails());
    }
}
