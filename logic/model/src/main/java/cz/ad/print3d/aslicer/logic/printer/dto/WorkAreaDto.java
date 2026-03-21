package cz.ad.print3d.aslicer.logic.printer.dto;

import cz.ad.print3d.aslicer.logic.printer.topology.geometry.MovementArea;
import cz.ad.print3d.aslicer.logic.printer.topology.geometry.WorkArea;

import java.util.ArrayList;
import java.util.List;

public class WorkAreaDto extends GenericAreaDto implements WorkArea {

    private List<MovementArea> movementAreas = new ArrayList<>();

    @Override
    public List<MovementArea> getMovementAreas() {
        return movementAreas;
    }

    public void setMovementAreas(List<MovementArea> movementAreas) {
        this.movementAreas = movementAreas;
    }

    public void addMovementArea(MovementArea area) {
        if (area != null) {
            this.movementAreas.add(area);
        }
    }
}
