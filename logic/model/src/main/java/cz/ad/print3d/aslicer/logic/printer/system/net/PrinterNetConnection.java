package cz.ad.print3d.aslicer.logic.printer.system.net;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.ad.print3d.aslicer.logic.printer.system.net.dto.BambuPrinterNetConnectionDto;
import cz.ad.print3d.aslicer.logic.printer.system.net.dto.NetworkPrinterNetConnectionDto;

/**
 * Base interface for representing a network connection to a 3D printer.
 *
 * <p>Provides access to the type of connection being used.</p>
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NetworkPrinterNetConnectionDto.class, name = "network"),
        @JsonSubTypes.Type(value = BambuPrinterNetConnectionDto.class, name = "bambu")
})
public interface PrinterNetConnection {

    /**
     * @return the type of connection (e.g., USB, NETWORK).
     */
    PrinterNetConnectionType getConnectionType();
}
