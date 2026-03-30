package cz.ad.print3d.aslicer.logic.printer.system.net.dto;

import cz.ad.print3d.aslicer.logic.printer.system.net.NetworkPrinterNetConnection;
import cz.ad.print3d.aslicer.logic.printer.system.net.PrinterNetConnectionType;

import java.net.URL;

/**
 * DTO for {@link NetworkPrinterNetConnection}.
 */
public class NetworkPrinterNetConnectionDto extends PrinterNetConnectionDto implements NetworkPrinterNetConnection {

    private URL printerUrl;
    private String pairingCode;

    public NetworkPrinterNetConnectionDto() {
        setConnectionType(PrinterNetConnectionType.NETWORK);
    }

    @Override
    public URL getPrinterUrl() {
        return printerUrl;
    }

    public void setPrinterUrl(URL printerUrl) {
        this.printerUrl = printerUrl;
    }

    @Override
    public String getPairingCode() {
        return pairingCode;
    }

    public void setPairingCode(String pairingCode) {
        this.pairingCode = pairingCode;
    }
}
