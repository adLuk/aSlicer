package cz.ad.print3d.aslicer.logic.net.bambu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for the system status information from a Bambu Lab printer.
 *
 * <p>Contains system-level information such as Wi-Fi signal strength, the printer's
 * IP address, and version information for various modules. This data is typically 
 * found under the {@code "system"} root key in the telemetry JSON.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BambuSystemStatus {

    @JsonProperty("wifi_signal")
    private Integer wifiSignal;

    @JsonProperty("ip")
    private String ip;

    @JsonProperty("get_version")
    private GetVersion getVersion;

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

    /**
     * @return the version information for the printer modules.
     */
    public GetVersion getGetVersion() {
        return getVersion;
    }

    /**
     * @param getVersion the version information to set.
     */
    public void setGetVersion(GetVersion getVersion) {
        this.getVersion = getVersion;
    }

    /**
     * DTO for the version information response.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GetVersion {
        @JsonProperty("module")
        private List<Module> modules;

        public List<Module> getModules() {
            return modules;
        }

        public void setModules(List<Module> modules) {
            this.modules = modules;
        }
    }

    /**
     * DTO for a single module version information.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Module {
        private String name;
        @JsonProperty("project_name")
        private String projectName;
        @JsonProperty("sw_ver")
        private String swVer;
        @JsonProperty("hw_ver")
        private String hwVer;
        private String sn;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public String getSwVer() { return swVer; }
        public void setSwVer(String swVer) { this.swVer = swVer; }
        public String getHwVer() { return hwVer; }
        public void setHwVer(String hwVer) { this.hwVer = hwVer; }
        public String getSn() { return sn; }
        public void setSn(String sn) { this.sn = sn; }
    }
}
