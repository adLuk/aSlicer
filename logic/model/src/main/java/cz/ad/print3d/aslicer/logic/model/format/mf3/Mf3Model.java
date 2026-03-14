package cz.ad.print3d.aslicer.logic.model.format.mf3;

import cz.ad.print3d.aslicer.logic.model.Model;
import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import cz.ad.print3d.aslicer.logic.model.format.mf3.relationship.Mf3Relationships;

import java.util.List;
import java.util.Map;

/**
 * Represents a 3MF (3D Manufacturing Format) model.
 * A 3MF model consists of metadata, a collection of objects, and package relationships.
 * 
 * @param metadata      map of metadata properties
 * @param objects       list of 3MF objects containing mesh data
 * @param unit          the measurement unit used for the model
 * @param relationships package relationships
 */
public record Mf3Model(Map<String, String> metadata, List<Mf3Object> objects, Unit unit, Mf3Relationships relationships) implements Model {
}
