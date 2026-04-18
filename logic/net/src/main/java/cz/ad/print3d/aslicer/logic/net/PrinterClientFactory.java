package cz.ad.print3d.aslicer.logic.net;

import cz.ad.print3d.aslicer.logic.net.bambu.BambuPrinterClient;
import cz.ad.print3d.aslicer.logic.net.klipper.KlipperPrinterClient;
import cz.ad.print3d.aslicer.logic.net.octoprint.OctoPrintPrinterClient;
import cz.ad.print3d.aslicer.logic.net.prusa.PrusaPrinterClient;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import java.util.Map;

/**
 * Factory for creating printer-specific clients.
 */
public class PrinterClientFactory {

    /**
     * Creates a {@link PrinterClient} based on the discovered device and provided credentials.
     *
     * @param device      the discovered device.
     * @param credentials the credentials (e.g., API key, access code).
     * @return a specific {@link PrinterClient} implementation, or {@code null} if vendor is unsupported.
     */
    public static PrinterClient createClient(DiscoveredDevice device, Map<String, String> credentials) {
        String vendor = device.getVendor();
        if (vendor == null) return null;

        String ip = device.getIpAddress();
        switch (vendor.toLowerCase()) {
            case "bambu lab":
                String serial = credentials.get("serial");
                if (serial == null || serial.isEmpty()) {
                    // Try to get serial from device name if not provided
                    serial = device.getName();
                    // If name is just a generic placeholder, treat as null to trigger discovery
                    if (serial != null && (serial.equalsIgnoreCase("Bambu Printer") || serial.equalsIgnoreCase("Bambu Lab Printer") || serial.equalsIgnoreCase("Unknown"))) {
                        serial = null;
                    }
                }
                String accessCode = credentials.get("accessCode");
                return new BambuPrinterClient(ip, serial, accessCode);
            case "octoprint":
                return new OctoPrintPrinterClient(ip, credentials.get("apiKey"));
            case "prusa":
                return new PrusaPrinterClient(ip, credentials.get("apiKey"));
            case "klipper":
                return new KlipperPrinterClient(ip, credentials.get("apiKey"));
            default:
                return null;
        }
    }
}
