package cz.ad.print3d.aslicer.logic.model.format.mf3;

/**
 * Represents a triangular facet in a 3MF mesh.
 * It uses vertex indices to define the three vertices of the triangle.
 * 
 * @param v1 index of the first vertex
 * @param v2 index of the second vertex
 * @param v3 index of the third vertex
 */
public record ThreeMfTriangle(int v1, int v2, int v3) {
}
