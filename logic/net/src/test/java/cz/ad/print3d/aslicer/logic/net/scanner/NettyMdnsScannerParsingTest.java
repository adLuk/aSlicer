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

import cz.ad.print3d.aslicer.logic.net.scanner.MdnsScanner.MdnsDiscoveryListener;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.dns.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for mDNS response parsing in {@link NettyMdnsScanner}.
 */
class NettyMdnsScannerParsingTest {

    @Test
    void testProcessResponse() throws Exception {
        NettyMdnsScanner scanner = new NettyMdnsScanner();
        Set<MdnsServiceInfo> results = new HashSet<>();
        
        // Use real DatagramDnsResponse instead of mock if possible
        InetSocketAddress sender = new InetSocketAddress("192.168.1.100", 5353);
        InetSocketAddress recipient = new InetSocketAddress("224.0.0.251", 5353);
        DatagramDnsResponse msg = new DatagramDnsResponse(sender, recipient, 0);
        
        // 1. PTR record: _http._tcp.local -> MyPrinter._http._tcp.local
        DnsPtrRecord ptrRecord = new DefaultDnsPtrRecord("_http._tcp.local.", DnsRecord.CLASS_IN, 3600, "MyPrinter._http._tcp.local.");
        msg.addRecord(DnsSection.ANSWER, ptrRecord);
        
        // 2. SRV record: MyPrinter._http._tcp.local -> port 80, target printer.local
        ByteBuf srvContent = Unpooled.buffer();
        srvContent.writeShort(0); // priority
        srvContent.writeShort(0); // weight
        srvContent.writeShort(80); // port
        // Target is skipped in current implementation due to compression, so we don't write it here
        DnsRawRecord srvRecord = new DefaultDnsRawRecord("MyPrinter._http._tcp.local.", DnsRecordType.SRV, 3600, srvContent);
        msg.addRecord(DnsSection.ADDITIONAL, srvRecord);
        
        // 3. TXT record: MyPrinter._http._tcp.local -> mfg=Creality, mdl=Ender-3
        ByteBuf txtContent = Unpooled.buffer();
        writeTxtEntry(txtContent, "mfg=Creality");
        writeTxtEntry(txtContent, "mdl=Ender-3");
        DnsRawRecord txtRecord = new DefaultDnsRawRecord("MyPrinter._http._tcp.local.", DnsRecordType.TXT, 3600, txtContent);
        msg.addRecord(DnsSection.ADDITIONAL, txtRecord);

        // 4. A record: printer.local -> 192.168.1.100
        ByteBuf aContent = Unpooled.wrappedBuffer(new byte[]{(byte)192, (byte)168, 1, 100});
        DnsRawRecord aRecord = new DefaultDnsRawRecord("printer.local.", DnsRecordType.A, 3600, aContent);
        msg.addRecord(DnsSection.ADDITIONAL, aRecord);

        // Access private processResponse method via reflection
        Method method = NettyMdnsScanner.class.getDeclaredMethod("processResponse", DatagramDnsResponse.class, Set.class, MdnsDiscoveryListener.class);
        method.setAccessible(true);
        method.invoke(scanner, msg, results, null);

        assertFalse(results.isEmpty(), "Should have discovered one service");
        MdnsServiceInfo info = null;
        for (MdnsServiceInfo i : results) {
            if ("MyPrinter".equals(i.getName())) {
                info = i;
                break;
            }
        }
        
        assertNotNull(info, "MyPrinter service should be found");
        assertEquals("_http._tcp.local.", info.getType());
        assertEquals(80, info.getPort());
        assertEquals("192.168.1.100", info.getIpAddress());
        
        Map<String, String> attrs = info.getAttributes();
        assertEquals("Creality", attrs.get("mfg"));
        assertEquals("Ender-3", attrs.get("mdl"));
        
        msg.release();
        scanner.close();
    }

    @Test
    void testProcessResponseWithoutPtr() throws Exception {
        NettyMdnsScanner scanner = new NettyMdnsScanner();
        Set<MdnsServiceInfo> results = new HashSet<>();

        InetSocketAddress sender = new InetSocketAddress("192.168.1.110", 5353);
        InetSocketAddress recipient = new InetSocketAddress("224.0.0.251", 5353);
        DatagramDnsResponse msg = new DatagramDnsResponse(sender, recipient, 0);

        // NO PTR record

        // 1. SRV record: Bambu-A1._bambu-network._tcp.local. -> port 8888
        ByteBuf srvContent = Unpooled.buffer();
        srvContent.writeShort(0); // priority
        srvContent.writeShort(0); // weight
        srvContent.writeShort(8888); // port
        DnsRawRecord srvRecord = new DefaultDnsRawRecord("Bambu-A1._bambu-network._tcp.local.", DnsRecordType.SRV, 3600, srvContent);
        msg.addRecord(DnsSection.ANSWER, srvRecord);

        // 2. TXT record: Bambu-A1._bambu-network._tcp.local. -> mfg=Bambu, mdl=A1
        ByteBuf txtContent = Unpooled.buffer();
        writeTxtEntry(txtContent, "mfg=Bambu");
        writeTxtEntry(txtContent, "mdl=A1");
        DnsRawRecord txtRecord = new DefaultDnsRawRecord("Bambu-A1._bambu-network._tcp.local.", DnsRecordType.TXT, 3600, txtContent);
        msg.addRecord(DnsSection.ANSWER, txtRecord);

        Method method = NettyMdnsScanner.class.getDeclaredMethod("processResponse", DatagramDnsResponse.class, Set.class, MdnsDiscoveryListener.class);
        method.setAccessible(true);
        method.invoke(scanner, msg, results, null);

        assertFalse(results.isEmpty(), "Should have discovered service even without PTR record");
        MdnsServiceInfo info = results.iterator().next();
        assertEquals("Bambu-A1", info.getName());
        assertEquals("_bambu-network._tcp.local.", info.getType());
        assertEquals(8888, info.getPort());
        assertEquals("192.168.1.110", info.getIpAddress());

        msg.release();
        scanner.close();
    }

    @Test
    void testProcessResponseWithSenderIpFallback() throws Exception {
        NettyMdnsScanner scanner = new NettyMdnsScanner();
        Set<MdnsServiceInfo> results = new HashSet<>();

        InetSocketAddress sender = new InetSocketAddress("192.168.1.110", 5353);
        InetSocketAddress recipient = new InetSocketAddress("224.0.0.251", 5353);
        DatagramDnsResponse msg = new DatagramDnsResponse(sender, recipient, 0);

        // PTR: _bambu-network._tcp.local. -> Bambu-A1._bambu-network._tcp.local.
        DnsPtrRecord ptrRecord = new DefaultDnsPtrRecord("_bambu-network._tcp.local.", DnsRecord.CLASS_IN, 3600, "Bambu-A1._bambu-network._tcp.local.");
        msg.addRecord(DnsSection.ANSWER, ptrRecord);

        // SRV: Bambu-A1._bambu-network._tcp.local. -> port 8888, target printer.local.
        ByteBuf srvContent = Unpooled.buffer();
        srvContent.writeShort(0); srvContent.writeShort(0); srvContent.writeShort(8888);
        DnsRawRecord srvRecord = new DefaultDnsRawRecord("Bambu-A1._bambu-network._tcp.local.", DnsRecordType.SRV, 3600, srvContent);
        msg.addRecord(DnsSection.ANSWER, srvRecord);

        // NO A record for printer.local.

        Method method = NettyMdnsScanner.class.getDeclaredMethod("processResponse", DatagramDnsResponse.class, Set.class, MdnsDiscoveryListener.class);
        method.setAccessible(true);
        method.invoke(scanner, msg, results, null);

        assertFalse(results.isEmpty(), "Should have discovered service");
        MdnsServiceInfo info = results.iterator().next();
        assertEquals("192.168.1.110", info.getIpAddress(), "Should fall back to sender IP");
        assertEquals(8888, info.getPort());

        msg.release();
        scanner.close();
    }

    private void writeTxtEntry(ByteBuf buf, String entry) {
        byte[] bytes = entry.getBytes();
        buf.writeByte(bytes.length);
        buf.writeBytes(bytes);
    }
}
