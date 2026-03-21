package cz.ad.print3d.aslicer.logic.printer.dto;

import cz.ad.print3d.aslicer.logic.model.basic.Coordinates;
import cz.ad.print3d.aslicer.logic.model.basic.Dimension;
import cz.ad.print3d.aslicer.logic.model.basic.LengthUnit;
import cz.ad.print3d.aslicer.logic.printer.topology.geometry.Geometry;
import cz.ad.print3d.aslicer.logic.printer.topology.geometry.WorkArea;

import java.util.ArrayList;
import java.util.List;

public class GeometryDto implements Geometry {

    private Coordinates coordinateType;
    private List<Dimension> minStepSize = new ArrayList<>();
    private List<Dimension> maxStepNumber = new ArrayList<>();
    private LengthUnit units;
    private WorkArea workArea;

    @Override
    public Coordinates getCoordinateType() {
        return coordinateType;
    }

    public void setCoordinateType(Coordinates coordinateType) {
        this.coordinateType = coordinateType;
    }

    @Override
    public List<Dimension> getMinStepSize() {
        return minStepSize;
    }

    public void setMinStepSize(List<Dimension> minStepSize) {
        this.minStepSize = minStepSize;
    }

    public void addMinStepSize(Dimension dimension) {
        if (dimension != null) {
            this.minStepSize.add(dimension);
        }
    }

    @Override
    public List<Dimension> getMaxStepNumber() {
        return maxStepNumber;
    }

    public void setMaxStepNumber(List<Dimension> maxStepNumber) {
        this.maxStepNumber = maxStepNumber;
    }

    public void addMaxStepNumber(Dimension dimension) {
        if (dimension != null) {
            this.maxStepNumber.add(dimension);
        }
    }

    @Override
    public LengthUnit getUnits() {
        return units;
    }

    public void setUnits(LengthUnit units) {
        this.units = units;
    }

    @Override
    public WorkArea getWorkArea() {
        return workArea;
    }

    public void setWorkArea(WorkArea workArea) {
        this.workArea = workArea;
    }
}
