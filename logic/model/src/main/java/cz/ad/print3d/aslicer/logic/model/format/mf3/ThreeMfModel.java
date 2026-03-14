package cz.ad.print3d.aslicer.logic.model.format.mf3;

import cz.ad.print3d.aslicer.logic.model.Model;
import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import java.util.List;
import java.util.Map;

/**
 * Represents a 3MF (3D Manufacturing Format) model.
 * A 3MF model consists of metadata and a collection of objects.
 * 
 * @param metadata map of metadata properties
 * @param objects list of 3MF objects containing mesh data
 * @param unit the measurement unit used for the model
 */
public record ThreeMfModel(Map<String, String> metadata, List<ThreeMfObject> objects, Unit unit) implements Model {
}
