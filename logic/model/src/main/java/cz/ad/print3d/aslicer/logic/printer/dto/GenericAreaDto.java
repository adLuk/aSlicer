package cz.ad.print3d.aslicer.logic.printer.dto;

import cz.ad.print3d.aslicer.logic.model.basic.Dimension;
import cz.ad.print3d.aslicer.logic.printer.topology.area.AreaShape;
import cz.ad.print3d.aslicer.logic.printer.topology.area.GenericArea;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class GenericAreaDto implements GenericArea {

    private AreaShape shape;
    private List<Dimension> dimensions = new ArrayList<>();
    private List<BigDecimal> globalPosition = new ArrayList<>();

    @Override
    public AreaShape getShape() {
        return shape;
    }

    public void setShape(AreaShape shape) {
        this.shape = shape;
    }

    @Override
    public List<Dimension> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<Dimension> dimensions) {
        this.dimensions = dimensions;
    }

    public void addDimension(Dimension dimension) {
        if (dimension != null) {
            this.dimensions.add(dimension);
        }
    }

    @Override
    public List<BigDecimal> getGlobalPosition() {
        return globalPosition;
    }

    public void setGlobalPosition(List<BigDecimal> globalPosition) {
        this.globalPosition = globalPosition;
    }

    public void addGlobalPosition(BigDecimal position) {
        if (position != null) {
            this.globalPosition.add(position);
        }
    }
}
