package cz.ad.print3d.aslicer.logic.net;

import cz.ad.print3d.aslicer.logic.net.bambu.BambuPrinterClient;
import cz.ad.print3d.aslicer.logic.net.klipper.KlipperPrinterClient;
import cz.ad.print3d.aslicer.logic.net.octoprint.OctoPrintPrinterClient;
import cz.ad.print3d.aslicer.logic.net.prusa.PrusaPrinterClient;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrinterClientFactoryTest {

    @Test
    public void testCreateBambuClient() {
        DiscoveredDevice device = new DiscoveredDevice("192.168.1.100");
        device.setVendor("Bambu Lab");
        device.setName("SN12345");
        
        Map<String, String> creds = new HashMap<>();
        creds.put("accessCode", "abc");
        
        PrinterClient client = PrinterClientFactory.createClient(device, creds);
        assertTrue(client instanceof BambuPrinterClient);
    }

    @Test
    public void testCreateOctoPrintClient() {
        DiscoveredDevice device = new DiscoveredDevice("192.168.1.101");
        device.setVendor("OctoPrint");
        
        Map<String, String> creds = new HashMap<>();
        creds.put("apiKey", "key123");
        
        PrinterClient client = PrinterClientFactory.createClient(device, creds);
        assertTrue(client instanceof OctoPrintPrinterClient);
    }

    @Test
    public void testCreatePrusaClient() {
        DiscoveredDevice device = new DiscoveredDevice("192.168.1.102");
        device.setVendor("Prusa");
        
        Map<String, String> creds = new HashMap<>();
        creds.put("apiKey", "key456");
        
        PrinterClient client = PrinterClientFactory.createClient(device, creds);
        assertTrue(client instanceof PrusaPrinterClient);
    }

    @Test
    public void testCreateKlipperClient() {
        DiscoveredDevice device = new DiscoveredDevice("192.168.1.103");
        device.setVendor("Klipper");
        
        Map<String, String> creds = new HashMap<>();
        creds.put("apiKey", "key789");
        
        PrinterClient client = PrinterClientFactory.createClient(device, creds);
        assertTrue(client instanceof KlipperPrinterClient);
    }

    @Test
    public void testUnsupportedVendor() {
        DiscoveredDevice device = new DiscoveredDevice("192.168.1.104");
        device.setVendor("Unknown");
        
        PrinterClient client = PrinterClientFactory.createClient(device, new HashMap<>());
        assertNull(client);
    }
}
