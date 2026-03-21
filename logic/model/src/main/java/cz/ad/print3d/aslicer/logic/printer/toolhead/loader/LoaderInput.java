package cz.ad.print3d.aslicer.logic.printer.toolhead.loader;

import cz.ad.print3d.aslicer.logic.model.basic.Dimension;
import cz.ad.print3d.aslicer.logic.model.basic.Speed;

import java.util.List;

public interface LoaderInput {

    List<Dimension> getFilamentDiameters();

    Speed getRetractionSpeed();

    Speed getMaxLoadSpeed();

    Speed getMinLoadSpeed();

    List<String> getFilamentTypes();

}
