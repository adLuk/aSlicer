package cz.ad.print3d.aslicer.logic.model;

import cz.ad.print3d.aslicer.logic.model.basic.Unit;

/**
 * Base interface for all 3D models.
 * This interface represents a common DTO for different 3D file formats.
 */
public interface Model {
    /**
     * Returns the measurement unit used for coordinate values in this model.
     * 
     * @return the measurement unit, or null if not specified or applicable
     */
    Unit unit();
}
