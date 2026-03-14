package cz.ad.print3d.aslicer.logic.model.format.mf3.core;

import cz.ad.print3d.aslicer.logic.model.Model;
import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import cz.ad.print3d.aslicer.logic.model.format.mf3.build.Mf3Build;
import cz.ad.print3d.aslicer.logic.model.format.mf3.resource.Mf3Object;
import cz.ad.print3d.aslicer.logic.model.format.mf3.resource.Mf3Resources;
import cz.ad.print3d.aslicer.logic.model.format.mf3.contenttype.Mf3ContentTypes;
import cz.ad.print3d.aslicer.logic.model.format.mf3.prusa.Mf3PrusaMainMetadata;
import cz.ad.print3d.aslicer.logic.model.format.mf3.prusa.Mf3PrusaSettings;
import cz.ad.print3d.aslicer.logic.model.format.mf3.prusa.Mf3PrusaSlicerModelConfig;
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
     * Package relationships for the 3MF document, organized by their path in the package.
     * This field is not part of the model XML itself but is associated with the 3MF package.
     */
    @XmlTransient
    private Map<String, Mf3Relationships> relationshipParts = new HashMap<>();

    /**
     * Content types for the 3MF document.
     * This field is not part of the model XML itself but is associated with the 3MF package.
     */
    @XmlTransient
    private Mf3ContentTypes contentTypes;

    /**
     * Location of the extracted files on the filesystem.
     * This field is not part of the model XML itself but is associated with the 3MF package.
     */
    @XmlTransient
    private java.nio.file.Path storagePath;

    /**
     * Prusa-specific metadata from the main model file.
     */
    @XmlTransient
    private Mf3PrusaMainMetadata prusaMainMetadata;

    /**
     * PrusaSlicer-specific configuration for objects and volumes.
     */
    @XmlTransient
    private Mf3PrusaSlicerModelConfig prusaSlicerModelConfig;

    /**
     * PrusaSlicer settings (printer, print, and filament settings).
     */
    @XmlTransient
    private Mf3PrusaSettings prusaSettings;

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
    public Mf3Model(final Map<String, String> metadata, final List<Mf3Object> objects, final Unit unit, final Mf3Relationships relationships, final Mf3ContentTypes contentTypes) {
        this.unit = unit;
        if (relationships != null) {
            this.relationshipParts.put("_rels/.rels", relationships);
        }
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
     * Returns the root package relationships parsed from the {@code /_rels/.rels} part.
     *
     * @return the {@link Mf3Relationships} container, or null if not present
     */
    public Mf3Relationships relationships() {
        return relationshipParts.get("_rels/.rels");
    }

    /**
     * Returns all relationship parts in the package, keyed by their path.
     *
     * @return a map of relationship parts
     */
    public Map<String, Mf3Relationships> relationshipParts() {
        return relationshipParts;
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
     * Sets the root package relationships.
     *
     * @param relationships the relationships to set
     */
    public void setRelationships(final Mf3Relationships relationships) {
        if (relationships != null) {
            this.relationshipParts.put("_rels/.rels", relationships);
        } else {
            this.relationshipParts.remove("_rels/.rels");
        }
    }

    /**
     * Sets a specific relationship part in the package.
     *
     * @param path          the path to the relationship file (e.g., "_rels/.rels" or "3D/_rels/3dmodel.model.rels")
     * @param relationships the relationships for this part
     */
    public void setRelationshipPart(final String path, final Mf3Relationships relationships) {
        if (relationships != null) {
            this.relationshipParts.put(path, relationships);
        } else {
            this.relationshipParts.remove(path);
        }
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
     * Returns the location of the extracted files on the filesystem.
     *
     * @return the storage path
     */
    public java.nio.file.Path storagePath() {
        return storagePath;
    }

    /**
     * Sets the location of the extracted files on the filesystem.
     *
     * @param storagePath the storage path to set
     */
    public void setStoragePath(final java.nio.file.Path storagePath) {
        this.storagePath = storagePath;
    }

    /**
     * Returns the Prusa-specific main metadata.
     *
     * @return the Prusa main metadata, or null if not present
     */
    public Mf3PrusaMainMetadata getPrusaMainMetadata() {
        if (prusaMainMetadata == null && metadataList != null) {
            prusaMainMetadata = Mf3PrusaMainMetadata.fromMap(metadata());
        }
        return prusaMainMetadata;
    }

    /**
     * Sets the Prusa-specific main metadata.
     *
     * @param prusaMainMetadata the metadata to set
     */
    public void setPrusaMainMetadata(final Mf3PrusaMainMetadata prusaMainMetadata) {
        this.prusaMainMetadata = prusaMainMetadata;
    }

    /**
     * Returns the PrusaSlicer object and volume configuration.
     *
     * @return the Prusa configuration, or null if not present
     */
    public Mf3PrusaSlicerModelConfig getPrusaSlicerModelConfig() {
        return prusaSlicerModelConfig;
    }

    /**
     * Sets the PrusaSlicer object and volume configuration.
     *
     * @param prusaSlicerModelConfig the configuration to set
     */
    public void setPrusaSlicerModelConfig(final Mf3PrusaSlicerModelConfig prusaSlicerModelConfig) {
        this.prusaSlicerModelConfig = prusaSlicerModelConfig;
    }

    /**
     * Returns the PrusaSlicer settings.
     *
     * @return the Prusa settings, or null if not present
     */
    public Mf3PrusaSettings getPrusaSettings() {
        return prusaSettings;
    }

    /**
     * Sets the PrusaSlicer settings.
     *
     * @param prusaSettings the settings to set
     */
    public void setPrusaSettings(final Mf3PrusaSettings prusaSettings) {
        this.prusaSettings = prusaSettings;
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
                java.util.Objects.equals(relationshipParts, mf3Model.relationshipParts) &&
                java.util.Objects.equals(contentTypes, mf3Model.contentTypes) &&
                java.util.Objects.equals(storagePath, mf3Model.storagePath) &&
                java.util.Objects.equals(prusaMainMetadata, mf3Model.prusaMainMetadata) &&
                java.util.Objects.equals(prusaSlicerModelConfig, mf3Model.prusaSlicerModelConfig) &&
                java.util.Objects.equals(prusaSettings, mf3Model.prusaSettings);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(unit, metadataList, resources, build, relationshipParts, contentTypes, storagePath, prusaMainMetadata, prusaSlicerModelConfig, prusaSettings);
    }

    @Override
    public String toString() {
        return "Mf3Model{" +
                "unit=" + unit +
                ", metadataList=" + metadataList +
                ", resources=" + resources +
                ", build=" + build +
                ", relationshipParts=" + relationshipParts +
                ", contentTypes=" + contentTypes +
                ", storagePath=" + storagePath +
                ", prusaMainMetadata=" + prusaMainMetadata +
                ", prusaSlicerModelConfig=" + prusaSlicerModelConfig +
                ", prusaSettings=" + prusaSettings +
                '}';
    }
}
