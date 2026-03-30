package cz.ad.print3d.aslicer.logic.printer.system.net;

/**
 * Interface for a network connection specifically for Bambu Lab printers.
 *
 * <p>Extends {@link NetworkPrinterNetConnection} with Bambu-specific requirements,
 * such as the printer's serial number and an access code for authentication.</p>
 */
public interface BambuPrinterNetConnection extends NetworkPrinterNetConnection {

    /**
     * @return the printer serial number.
     */
    String getSerial();

    /**
     * @return the access code used for pairing and subsequent authentication.
     */
    String getAccessCode();

    /**
     * {@inheritDoc}
     */
    @Override
    default PrinterNetConnectionType getConnectionType() {
        return PrinterNetConnectionType.NETWORK;
    }
}
