package cz.ad.print3d.aslicer.logic.printer.dto;

import cz.ad.print3d.aslicer.logic.model.basic.Dimension;
import cz.ad.print3d.aslicer.logic.model.basic.Speed;
import cz.ad.print3d.aslicer.logic.model.basic.Temperature;
import cz.ad.print3d.aslicer.logic.printer.toolhead.printer.Printer;

import java.util.ArrayList;
import java.util.List;

public class PrinterDto implements Printer {

    private List<Dimension> printDiameters = new ArrayList<>();
    private List<Temperature> maxPrintTemperature = new ArrayList<>();
    private Speed retractionSpeed;

    @Override
    public List<Dimension> getPrintDiameters() {
        return printDiameters;
    }

    public void setPrintDiameters(List<Dimension> printDiameters) {
        this.printDiameters = printDiameters;
    }

    public void addPrintDiameter(Dimension diameter) {
        if (diameter != null) {
            this.printDiameters.add(diameter);
        }
    }

    @Override
    public List<Temperature> getMaxPrintTemperature() {
        return maxPrintTemperature;
    }

    public void setMaxPrintTemperature(List<Temperature> maxPrintTemperature) {
        this.maxPrintTemperature = maxPrintTemperature;
    }

    public void addMaxPrintTemperature(Temperature temperature) {
        if (temperature != null) {
            this.maxPrintTemperature.add(temperature);
        }
    }

    @Override
    public Speed getRetractionSpeed() {
        return retractionSpeed;
    }

    public void setRetractionSpeed(Speed retractionSpeed) {
        this.retractionSpeed = retractionSpeed;
    }
}
