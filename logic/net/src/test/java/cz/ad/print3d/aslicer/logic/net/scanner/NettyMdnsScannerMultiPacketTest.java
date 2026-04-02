package cz.ad.print3d.aslicer.logic.net.scanner;

import cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.dns.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NettyMdnsScannerMultiPacketTest {

    @Test
    void testProcessMultiPacketResponse() throws Exception {
        NettyMdnsScanner scanner = new NettyMdnsScanner();
        Set<MdnsServiceInfo> results = new HashSet<>();
        
        InetSocketAddress sender = new InetSocketAddress("192.168.1.100", 5353);
        InetSocketAddress recipient = new InetSocketAddress("224.0.0.251", 5353);
        
        // Packet 1: PTR record
        DatagramDnsResponse msg1 = new DatagramDnsResponse(sender, recipient, 0);
        msg1.addRecord(DnsSection.ANSWER, new DefaultDnsPtrRecord("_http._tcp.local.", DnsRecord.CLASS_IN, 3600, "MyPrinter._http._tcp.local."));
        
        // Packet 2: SRV record
        DatagramDnsResponse msg2 = new DatagramDnsResponse(sender, recipient, 0);
        ByteBuf srvContent = Unpooled.buffer();
        srvContent.writeShort(0); srvContent.writeShort(0); srvContent.writeShort(80);
        msg2.addRecord(DnsSection.ANSWER, new DefaultDnsRawRecord("MyPrinter._http._tcp.local.", DnsRecordType.SRV, 3600, srvContent));
        
        // Packet 3: TXT record
        DatagramDnsResponse msg3 = new DatagramDnsResponse(sender, recipient, 0);
        ByteBuf txtContent = Unpooled.buffer();
        byte[] attr = "mfg=Prusa".getBytes();
        txtContent.writeByte(attr.length);
        txtContent.writeBytes(attr);
        msg3.addRecord(DnsSection.ANSWER, new DefaultDnsRawRecord("MyPrinter._http._tcp.local.", DnsRecordType.TXT, 3600, txtContent));

        Method method = NettyMdnsScanner.class.getDeclaredMethod("processResponse", DatagramDnsResponse.class, java.util.Map.class, java.util.Map.class, Set.class, MdnsScanner.MdnsDiscoveryListener.class);
        method.setAccessible(true);
        
        java.util.Map<String, Object> sessionBuilders = new java.util.HashMap<>();
        java.util.Map<String, String> sessionHostnameToIp = new java.util.HashMap<>();
        
        // In current implementation, each call to processResponse will try to build a result
        method.invoke(scanner, msg1, sessionBuilders, sessionHostnameToIp, results, null);
        method.invoke(scanner, msg2, sessionBuilders, sessionHostnameToIp, results, null);
        method.invoke(scanner, msg3, sessionBuilders, sessionHostnameToIp, results, null);
        
        // If it's working correctly (correlated), we should have ONE service with all info.
        // If it's NOT working correctly (current), we will have multiple partial services or just one partial.
        
        System.out.println("[DEBUG_LOG] Results size: " + results.size());
        for (MdnsServiceInfo info : results) {
            System.out.println("[DEBUG_LOG] Result: " + info);
        }
        
        // Current implementation will likely produce 3 entries in 'results' because they all have different attributes/port
        // and processResponse doesn't share state between calls.
        
        msg1.release();
        msg2.release();
        msg3.release();
        scanner.close();
        
        // The goal is to have 1 result with port 80 and mfg=Prusa
        long validResults = results.stream().filter(i -> i.getPort() == 80 && "Prusa".equals(i.getAttributes().get("mfg"))).count();
        assertTrue(validResults > 0, "Should have at least one complete result");
        assertEquals(1, results.size(), "Should have exactly one result (correlated)");
    }
}
