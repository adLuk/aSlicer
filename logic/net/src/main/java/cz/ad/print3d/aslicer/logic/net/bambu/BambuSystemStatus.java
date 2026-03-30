package cz.ad.print3d.aslicer.logic.net.bambu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for the system status information from a Bambu Lab printer.
 *
 * <p>Contains system-level information such as Wi-Fi signal strength and the printer's
 * IP address. This data is typically found under the {@code "system"} root key
 * in the telemetry JSON.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BambuSystemStatus {

    @JsonProperty("wifi_signal")
    private Integer wifiSignal;

    @JsonProperty("ip")
    private String ip;

    /**
     * @return the Wi-Fi signal strength as reported by the printer.
     */
    public Integer getWifiSignal() {
        return wifiSignal;
    }

    /**
     * @param wifiSignal the Wi-Fi signal strength to set.
     */
    public void setWifiSignal(Integer wifiSignal) {
        this.wifiSignal = wifiSignal;
    }

    /**
     * @return the IP address of the printer.
     */
    public String getIp() {
        return ip;
    }

    /**
     * @param ip the IP address to set.
     */
    public void setIp(String ip) {
        this.ip = ip;
    }
}
