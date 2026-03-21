package cz.ad.print3d.aslicer.logic.printer.dto;

import cz.ad.print3d.aslicer.logic.printer.topology.geometry.PrintArea;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PrintAreaDto extends GenericAreaDto implements PrintArea {

    private List<BigDecimal> start = new ArrayList<>();

    @Override
    public List<BigDecimal> getStart() {
        return start;
    }

    public void setStart(List<BigDecimal> start) {
        this.start = start;
    }

    public void addStart(BigDecimal point) {
        if (point != null) {
            this.start.add(point);
        }
    }
}
