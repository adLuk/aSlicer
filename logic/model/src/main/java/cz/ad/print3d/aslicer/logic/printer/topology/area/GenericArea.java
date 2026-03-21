package cz.ad.print3d.aslicer.logic.printer.topology.area;

import cz.ad.print3d.aslicer.logic.model.basic.Dimension;

import java.math.BigDecimal;
import java.util.List;

public interface GenericArea {

    AreaShape getShape();

    List<Dimension> getDimensions();

    List<BigDecimal> getGlobalPosition();

}
