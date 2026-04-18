package cz.ad.print3d.aslicer.logic.net.bambu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Base telemetry data container from a Bambu Lab printer.
 *
 * <p>This DTO (Data Transfer Object) represents the structure of the JSON message
 * published by the printer on its telemetry topic. It maps the {@code "print"} root key.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BambuTelemetry {

    @JsonProperty("print")
    private BambuPrintStatus print;

    /**
     * @return the print status information.
     */
    public BambuPrintStatus getPrint() {
        return print;
    }

    /**
     * @param print the print status information to set.
     */
    public void setPrint(BambuPrintStatus print) {
        this.print = print;
    }

    /**
     * DTO for the detailed print status of a Bambu Lab printer.
     *
     * <p>Contains information such as the current G-code state, progress percentage,
     * remaining time, and temperatures of the nozzle and the bed.</p>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BambuPrintStatus {
        @JsonProperty("gcode_state")
        private String gcodeState;

        @JsonProperty("mc_percent")
        private int percent;

        @JsonProperty("mc_remaining_time")
        private int remainingTime;

        @JsonProperty("nozzle_temper")
        private double nozzleTemperature;

        @JsonProperty("bed_temper")
        private double bedTemperature;

        @JsonProperty("ams")
        private AmsData ams;

        @JsonProperty("nozzle_diameter")
        private Double nozzleDiameter;

        @JsonProperty("nozzle_type")
        private String nozzleType;

        /**
         * @return the nozzle diameter in mm.
         */
        public Double getNozzleDiameter() { return nozzleDiameter; }
        public void setNozzleDiameter(Double nozzleDiameter) { this.nozzleDiameter = nozzleDiameter; }

        /**
         * @return the type of nozzle (e.g., "stainless_steel", "hardened_steel").
         */
        public String getNozzleType() { return nozzleType; }
        public void setNozzleType(String nozzleType) { this.nozzleType = nozzleType; }

        /**
         * @return the current G-code execution state (e.g., "IDLE", "RUNNING", "PAUSE").
         */
        public String getGcodeState() { return gcodeState; }

        /**
         * @param gcodeState the G-code state to set.
         */
        public void setGcodeState(String gcodeState) { this.gcodeState = gcodeState; }

        /**
         * @return the print progress percentage (0-100).
         */
        public int getPercent() { return percent; }

        /**
         * @param percent the progress percentage to set.
         */
        public void setPercent(int percent) { this.percent = percent; }

        /**
         * @return the estimated remaining print time in minutes.
         */
        public int getRemainingTime() { return remainingTime; }

        /**
         * @param remainingTime the remaining time to set.
         */
        public void setRemainingTime(int remainingTime) { this.remainingTime = remainingTime; }

        /**
         * @return the current temperature of the extruder nozzle in Celsius.
         */
        public double getNozzleTemperature() { return nozzleTemperature; }

        /**
         * @param nozzleTemperature the nozzle temperature to set.
         */
        public void setNozzleTemperature(double nozzleTemperature) { this.nozzleTemperature = nozzleTemperature; }

        /**
         * @return the current temperature of the heated bed in Celsius.
         */
        public double getBedTemperature() { return bedTemperature; }

        /**
         * @param bedTemperature the bed temperature to set.
         */
        public void setBedTemperature(double bedTemperature) { this.bedTemperature = bedTemperature; }

        /**
         * @return the AMS status information.
         */
        public AmsData getAms() { return ams; }

        /**
         * @param ams the AMS status information to set.
         */
        public void setAms(AmsData ams) { this.ams = ams; }
    }

    /**
     * DTO for the AMS (Automatic Material System) data.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AmsData {
        @JsonProperty("ams")
        private List<AmsDevice> amsDevices;

        public List<AmsDevice> getAmsDevices() { return amsDevices; }
        public void setAmsDevices(List<AmsDevice> amsDevices) { this.amsDevices = amsDevices; }
    }

    /**
     * DTO for a single AMS device.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AmsDevice {
        private String id;
        private String temp;
        private String humidity;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTemp() { return temp; }
        public void setTemp(String temp) { this.temp = temp; }
        public String getHumidity() { return humidity; }
        public void setHumidity(String humidity) { this.humidity = humidity; }
    }
}
