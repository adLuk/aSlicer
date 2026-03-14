package cz.ad.print3d.aslicer.logic.model.format.stl;

import cz.ad.print3d.aslicer.logic.model.Model;
import cz.ad.print3d.aslicer.logic.model.basic.Unit;

import java.util.List;

/**
 * Represents the data from a binary STL file.
 *
 * @param header 80-byte header of the STL file
 * @param facets list of facets in the STL model
 * @param unit the measurement unit used for coordinate values in this model
 */
public record StlModel(byte[] header, List<StlFacet> facets, Unit unit) implements Model {
    /**
     * @return Number of facets in the STL model.
     */
    public int facetCount() {
        return facets != null ? facets.size() : 0;
    }

    /**
     * Returns the measurement unit used for coordinate values in this model.
     * 
     * @return the measurement unit, or null if not specified or applicable
     */
    @Override
    public Unit unit() {
        return unit;
    }
}
