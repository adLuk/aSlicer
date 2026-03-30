package cz.ad.print3d.aslicer.logic.printer.system.net.dto;

import cz.ad.print3d.aslicer.logic.printer.system.net.BambuPrinterNetConnection;

/**
 * DTO for {@link BambuPrinterNetConnection}.
 */
public class BambuPrinterNetConnectionDto extends NetworkPrinterNetConnectionDto implements BambuPrinterNetConnection {

    private String serial;
    private String accessCode;

    @Override
    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    @Override
    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }
}
