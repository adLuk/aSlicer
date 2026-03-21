package cz.ad.print3d.aslicer.logic.printer.toolhead.printer;

import cz.ad.print3d.aslicer.logic.model.basic.Dimension;
import cz.ad.print3d.aslicer.logic.model.basic.Speed;
import cz.ad.print3d.aslicer.logic.model.basic.Temperature;

import java.util.List;

public interface Printer {

    List<Dimension> getPrintDiameters();

    List<Temperature> getMaxPrintTemperature();

    Speed getRetractionSpeed();
}
