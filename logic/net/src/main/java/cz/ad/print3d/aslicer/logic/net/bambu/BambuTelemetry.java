package cz.ad.print3d.aslicer.logic.net.bambu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    }
}
