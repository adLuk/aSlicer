package cz.ad.print3d.aslicer.logic.printer.system.net;

import java.net.URL;

/**
 * Interface representing a network-based connection to a 3D printer.
 *
 * <p>Extends {@link PrinterNetConnection} with network-specific attributes such as
 * the printer's URL and a pairing code.</p>
 */
public interface NetworkPrinterNetConnection extends PrinterNetConnection {

    /**
     * @return the {@link URL} of the printer on the network.
     */
    URL getPrinterUrl();

    /**
     * @return the code used for pairing the slicer with the printer.
     */
    String getPairingCode();

    /**
     * {@inheritDoc}
     */
    @Override
    default PrinterNetConnectionType getConnectionType() {
        return PrinterNetConnectionType.NETWORK;
    }
}
