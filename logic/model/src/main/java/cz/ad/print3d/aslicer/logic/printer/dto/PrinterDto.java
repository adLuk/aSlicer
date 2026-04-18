package cz.ad.print3d.aslicer.logic.printer.dto;

import cz.ad.print3d.aslicer.logic.model.basic.Dimension;
import cz.ad.print3d.aslicer.logic.model.basic.Speed;
import cz.ad.print3d.aslicer.logic.model.basic.Temperature;
import cz.ad.print3d.aslicer.logic.printer.toolhead.printer.Printer;

import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer object implementation of the {@link Printer} interface.
 * Used for serializing and transporting printer capability information.
 */
public class PrinterDto implements Printer {

    /**
     * List of supported nozzle diameters.
     */
    private List<Dimension> printDiameters = new ArrayList<>();

    /**
     * List of maximum print temperatures.
     */
    private List<Temperature> maxPrintTemperature = new ArrayList<>();

    /**
     * The configured retraction speed.
     */
    private Speed retractionSpeed;

    @Override
    public List<Dimension> getPrintDiameters() {
        return printDiameters;
    }

    /**
     * Sets the list of supported nozzle diameters.
     *
     * @param printDiameters list of diameters
     */
    public void setPrintDiameters(List<Dimension> printDiameters) {
        this.printDiameters = printDiameters;
    }

    /**
     * Adds a single nozzle diameter to the supported list.
     *
     * @param diameter the diameter to add
     */
    public void addPrintDiameter(Dimension diameter) {
        if (diameter != null) {
            this.printDiameters.add(diameter);
        }
    }

    @Override
    public List<Temperature> getMaxPrintTemperature() {
        return maxPrintTemperature;
    }

    /**
     * Sets the list of maximum operating temperatures.
     *
     * @param maxPrintTemperature list of temperatures
     */
    public void setMaxPrintTemperature(List<Temperature> maxPrintTemperature) {
        this.maxPrintTemperature = maxPrintTemperature;
    }

    /**
     * Adds a single maximum temperature limit to the list.
     *
     * @param temperature the temperature to add
     */
    public void addMaxPrintTemperature(Temperature temperature) {
        if (temperature != null) {
            this.maxPrintTemperature.add(temperature);
        }
    }

    @Override
    public Speed getRetractionSpeed() {
        return retractionSpeed;
    }

    /**
     * Sets the retraction speed.
     *
     * @param retractionSpeed the speed to set
     */
    public void setRetractionSpeed(Speed retractionSpeed) {
        this.retractionSpeed = retractionSpeed;
    }
}
