package cz.ad.print3d.aslicer.logic.printer.dto;

import cz.ad.print3d.aslicer.logic.model.basic.Dimension;
import cz.ad.print3d.aslicer.logic.model.basic.Speed;
import cz.ad.print3d.aslicer.logic.printer.toolhead.loader.LoaderInput;

import java.util.ArrayList;
import java.util.List;

public class LoaderInputDto implements LoaderInput {

    private List<Dimension> filamentDiameters = new ArrayList<>();
    private Speed retractionSpeed;
    private Speed maxLoadSpeed;
    private Speed minLoadSpeed;
    private List<String> filamentTypes = new ArrayList<>();

    @Override
    public List<Dimension> getFilamentDiameters() {
        return filamentDiameters;
    }

    public void setFilamentDiameters(List<Dimension> filamentDiameters) {
        this.filamentDiameters = filamentDiameters;
    }

    public void addFilamentDiameter(Dimension diameter) {
        if (diameter != null) {
            this.filamentDiameters.add(diameter);
        }
    }

    @Override
    public Speed getRetractionSpeed() {
        return retractionSpeed;
    }

    public void setRetractionSpeed(Speed retractionSpeed) {
        this.retractionSpeed = retractionSpeed;
    }

    @Override
    public Speed getMaxLoadSpeed() {
        return maxLoadSpeed;
    }

    public void setMaxLoadSpeed(Speed maxLoadSpeed) {
        this.maxLoadSpeed = maxLoadSpeed;
    }

    @Override
    public Speed getMinLoadSpeed() {
        return minLoadSpeed;
    }

    public void setMinLoadSpeed(Speed minLoadSpeed) {
        this.minLoadSpeed = minLoadSpeed;
    }

    @Override
    public List<String> getFilamentTypes() {
        return filamentTypes;
    }

    public void setFilamentTypes(List<String> filamentTypes) {
        this.filamentTypes = filamentTypes;
    }

    public void addFilamentType(String type) {
        if (type != null) {
            this.filamentTypes.add(type);
        }
    }
}
