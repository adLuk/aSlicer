package cz.ad.print3d.aslicer.logic.printer.topology.geometry;

import cz.ad.print3d.aslicer.logic.printer.topology.area.GenericArea;

import java.math.BigDecimal;
import java.util.List;

public interface PrintArea extends GenericArea {

    List<BigDecimal> getStart();
}

