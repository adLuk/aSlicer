package cz.ad.print3d.aslicer.logic.model.format.mf3.core;

import cz.ad.print3d.aslicer.logic.model.Model;
import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import cz.ad.print3d.aslicer.logic.model.format.mf3.build.Mf3Build;
import cz.ad.print3d.aslicer.logic.model.format.mf3.resource.Mf3Object;
import cz.ad.print3d.aslicer.logic.model.format.mf3.resource.Mf3Resources;
import cz.ad.print3d.aslicer.logic.model.format.mf3.contenttype.Mf3ContentTypes;
import cz.ad.print3d.aslicer.logic.model.format.mf3.relationship.Mf3Relationships;
import jakarta.xml.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a 3MF (3D Manufacturing Format) model, defined by the 
 * http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel specification.
 * 
 * <p>A 3MF model is the top-level container that includes all resources,
 * metadata, and build information required to describe a 3D object for 
 * manufacturing. This implementation adheres to the core 3MF XML structure.</p>
 */
@XmlRootElement(name = "model", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3Model implements Model {

    /**
     * The measurement unit used for the model.
     * Default value is MILLIMETER as per 3MF specification.
     */
    @XmlAttribute(name = "unit")
    private Unit unit = Unit.MILLIMETER;

    /**
     * List of metadata properties providing additional info about the model.
     */
    @XmlElement(name = "metadata", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
    private List<Mf3Metadata> metadataList = new ArrayList<>();

    /**
     * The resources section containing objects and other reusable elements.
     */
    @XmlElement(name = "resources", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
    private Mf3Resources resources;

    /**
     * The build section specifying which objects are part of the model.
     */
    @XmlElement(name = "build", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
    private Mf3Build build;

    /**
     * Package relationships for the 3MF document.
     * This field is not part of the model XML itself but is associated with the 3MF package.
     */
    @XmlTransient
    private Mf3Relationships relationships;

    /**
     * Content types for the 3MF document.
     * This field is not part of the model XML itself but is associated with the 3MF package.
     */
    @XmlTransient
    private Mf3ContentTypes contentTypes;

    /**
     * Default constructor for JAXB.
     */
    public Mf3Model() {
    }

    /**
     * Constructs an Mf3Model with the specified properties.
     *
     * @param metadata      map of metadata properties
     * @param objects       list of 3MF objects containing mesh data
     * @param unit          the measurement unit used for the model
     * @param relationships package relationships
     */
    public Mf3Model(Map<String, String> metadata, List<Mf3Object> objects, Unit unit, Mf3Relationships relationships) {
        this(metadata, objects, unit, relationships, null);
    }

    /**
     * Constructs an Mf3Model with the specified properties.
     *
     * @param metadata      map of metadata properties
     * @param objects       list of 3MF objects containing mesh data
     * @param unit          the measurement unit used for the model
     * @param relationships package relationships
     * @param contentTypes  package content types
     */
    public Mf3Model(Map<String, String> metadata, List<Mf3Object> objects, Unit unit, Mf3Relationships relationships, Mf3ContentTypes contentTypes) {
        this.unit = unit;
        this.relationships = relationships;
        this.contentTypes = contentTypes;
        if (metadata != null) {
            this.metadataList = metadata.entrySet().stream()
                    .map(e -> new Mf3Metadata(e.getKey(), e.getValue()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        this.resources = new Mf3Resources();
        this.resources.setObjects(objects);
    }

    /**
     * Returns the measurement unit used for the model.
     *
     * @return the measurement unit
     */
    @Override
    public Unit unit() {
        return unit;
    }

    /**
     * Returns a map of metadata properties.
     *
     * @return a map of metadata names to values
     */
    public Map<String, String> metadata() {
        if (metadataList == null) {
            return Collections.emptyMap();
        }
        return metadataList.stream().collect(Collectors.toMap(
                Mf3Metadata::getName,
                Mf3Metadata::getValue,
                (v1, v2) -> v1,
                LinkedHashMap::new
        ));
    }

    /**
     * Returns the list of 3MF objects containing mesh data.
     *
     * @return the list of objects
     */
    public List<Mf3Object> objects() {
        return resources != null ? resources.getObjects() : Collections.emptyList();
    }

    /**
     * Returns the package relationships parsed from the {@code /_rels/.rels} part.
     *
     * @return the {@link Mf3Relationships} container
     */
    public Mf3Relationships relationships() {
        return relationships;
    }

    /**
     * Returns the package content types parsed from the {@code [Content_Types].xml} part.
     *
     * @return the {@link Mf3ContentTypes} container
     */
    public Mf3ContentTypes contentTypes() {
        return contentTypes;
    }

    /**
     * Sets the measurement unit.
     *
     * @param unit the unit to set
     */
    public void setUnit(final Unit unit) {
        this.unit = unit;
    }

    /**
     * Sets the package relationships.
     *
     * @param relationships the relationships to set
     */
    public void setRelationships(final Mf3Relationships relationships) {
        this.relationships = relationships;
    }

    /**
     * Sets the package content types.
     *
     * @param contentTypes the content types to set
     */
    public void setContentTypes(final Mf3ContentTypes contentTypes) {
        this.contentTypes = contentTypes;
    }

    /**
     * Returns the metadata list.
     *
     * @return the metadata list
     */
    public List<Mf3Metadata> getMetadataList() {
        return metadataList;
    }

    /**
     * Sets the metadata list.
     *
     * @param metadataList the list to set
     */
    public void setMetadataList(final List<Mf3Metadata> metadataList) {
        this.metadataList = metadataList;
    }

    /**
     * Returns the resources container.
     *
     * @return the resources
     */
    public Mf3Resources getResources() {
        return resources;
    }

    /**
     * Sets the resources container.
     *
     * @param resources the resources to set
     */
    public void setResources(final Mf3Resources resources) {
        this.resources = resources;
    }

    /**
     * Returns the build section.
     *
     * @return the build section
     */
    public Mf3Build getBuild() {
        return build;
    }

    /**
     * Sets the build section.
     *
     * @param build the build to set
     */
    public void setBuild(final Mf3Build build) {
        this.build = build;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3Model mf3Model = (Mf3Model) o;
        return unit == mf3Model.unit &&
                java.util.Objects.equals(metadataList, mf3Model.metadataList) &&
                java.util.Objects.equals(resources, mf3Model.resources) &&
                java.util.Objects.equals(build, mf3Model.build) &&
                java.util.Objects.equals(relationships, mf3Model.relationships) &&
                java.util.Objects.equals(contentTypes, mf3Model.contentTypes);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(unit, metadataList, resources, build, relationships, contentTypes);
    }

    @Override
    public String toString() {
        return "Mf3Model{" +
                "unit=" + unit +
                ", metadataList=" + metadataList +
                ", resources=" + resources +
                ", build=" + build +
                ", relationships=" + relationships +
                ", contentTypes=" + contentTypes +
                '}';
    }
}
