package cz.ad.print3d.aslicer.logic.model.stl;

import java.util.List;

/**
 * Represents the data from a binary STL file.
 *
 * @param header 80-byte header of the STL file
 * @param facets list of facets in the STL model
 */
public record StlModel(byte[] header, List<StlFacet> facets) {
    /**
     * @return Number of facets in the STL model.
     */
    public int facetCount() {
        return facets != null ? facets.size() : 0;
    }
}
