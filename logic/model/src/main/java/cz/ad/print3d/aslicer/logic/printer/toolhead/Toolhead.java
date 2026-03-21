package cz.ad.print3d.aslicer.logic.printer.toolhead;

import cz.ad.print3d.aslicer.logic.model.basic.Dimension;
import cz.ad.print3d.aslicer.logic.model.basic.Temperature;
import cz.ad.print3d.aslicer.logic.printer.toolhead.loader.Loader;
import cz.ad.print3d.aslicer.logic.printer.toolhead.printer.Printer;

import java.util.List;

public interface Toolhead {

    List<Loader> getLoaders();

    List<Printer> getPrinterHeads();

    Temperature getMaxBedTemperature();

    Dimension getMinimalFirstLayerHeight();
}
