package cz.ad.print3d.aslicer.logic.model.format.mf3;

import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import java.util.List;

/**
 * Represents an object in a 3MF model, typically containing a mesh.
 * 
 * @param id unique identifier of the object
 * @param name name of the object
 * @param vertices list of mesh vertices
 * @param triangles list of mesh triangles referencing vertices by index
 */
public record ThreeMfObject(int id, String name, List<Vector3f> vertices, List<ThreeMfTriangle> triangles) {
}
