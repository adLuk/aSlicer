package cz.ad.print3d.aslicer.logic.printer.dto;

import cz.ad.print3d.aslicer.logic.model.basic.Dimension;
import cz.ad.print3d.aslicer.logic.model.basic.Temperature;
import cz.ad.print3d.aslicer.logic.printer.toolhead.Toolhead;
import cz.ad.print3d.aslicer.logic.printer.toolhead.loader.Loader;
import cz.ad.print3d.aslicer.logic.printer.toolhead.printer.Printer;

import java.util.ArrayList;
import java.util.List;

public class ToolheadDto implements Toolhead {

    private List<Loader> loaders = new ArrayList<>();
    private List<Printer> printerHeads = new ArrayList<>();
    private Temperature maxBedTemperature;
    private Dimension minimalFirstLayerHeight;

    @Override
    public List<Loader> getLoaders() {
        return loaders;
    }

    public void setLoaders(List<Loader> loaders) {
        this.loaders = loaders;
    }

    public void addLoader(Loader loader) {
        if (loader != null) {
            this.loaders.add(loader);
        }
    }

    @Override
    public List<Printer> getPrinterHeads() {
        return printerHeads;
    }

    public void setPrinterHeads(List<Printer> printerHeads) {
        this.printerHeads = printerHeads;
    }

    public void addPrinterHead(Printer printer) {
        if (printer != null) {
            this.printerHeads.add(printer);
        }
    }

    @Override
    public Temperature getMaxBedTemperature() {
        return maxBedTemperature;
    }

    public void setMaxBedTemperature(Temperature maxBedTemperature) {
        this.maxBedTemperature = maxBedTemperature;
    }

    public void setMinimalFirstLayerHeight(Dimension minimalFirstLayerHeight) {
        this.minimalFirstLayerHeight = minimalFirstLayerHeight;
    }

    @Override
    public Dimension getMinimalFirstLayerHeight() {
        return minimalFirstLayerHeight;
    }
}
