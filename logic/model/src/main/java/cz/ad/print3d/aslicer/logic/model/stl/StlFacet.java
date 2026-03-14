package cz.ad.print3d.aslicer.logic.model.stl;

import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;

/**
 * Represents a single facet in an STL file.
 * Each facet has a normal vector, three vertices, and an attribute byte count.
 *
 * @param normal the normal vector of the facet
 * @param v1 the first vertex of the triangular facet
 * @param v2 the second vertex of the triangular facet
 * @param v3 the third vertex of the triangular facet
 * @param attributeByteCount the number of attribute bytes for the facet
 */
public record StlFacet(Vector3f normal, Vector3f v1, Vector3f v2, Vector3f v3, int attributeByteCount) {
}
