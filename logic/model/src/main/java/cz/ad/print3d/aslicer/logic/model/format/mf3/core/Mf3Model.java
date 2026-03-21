/*
 * aSlicer - 3D model processing tool.
 * Copyright (C) 2026 cz.ad.print3d.aslicer contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.ad.print3d.aslicer.logic.model.format.mf3.core;

import cz.ad.print3d.aslicer.logic.model.Model;
import cz.ad.print3d.aslicer.logic.model.basic.LengthUnit;
import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import cz.ad.print3d.aslicer.logic.model.format.mf3.build.Mf3Build;
import cz.ad.print3d.aslicer.logic.model.format.mf3.geometry.Mf3Triangle;
import cz.ad.print3d.aslicer.logic.model.format.mf3.resource.Mf3BaseMaterials;
import cz.ad.print3d.aslicer.logic.model.format.mf3.resource.Mf3Object;
import cz.ad.print3d.aslicer.logic.model.format.mf3.resource.Mf3Resources;
import cz.ad.print3d.aslicer.logic.model.format.mf3.bambu.Mf3BambuConfig;
import cz.ad.print3d.aslicer.logic.model.format.mf3.bambu.Mf3BambuCustomGCode;
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
    private LengthUnit lengthUnit = LengthUnit.MILLIMETER;

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
     * Bambu-specific configuration for plates.
     */
    @XmlTransient
    private Mf3BambuConfig bambuConfig;

    /**
     * Bambu-specific custom G-code.
     */
    @XmlTransient
    private Mf3BambuCustomGCode bambuCustomGCode;

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
     * @param lengthUnit          the measurement unit used for the model
     * @param relationships package relationships
     */
    public Mf3Model(Map<String, String> metadata, List<Mf3Object> objects, LengthUnit lengthUnit, Mf3Relationships relationships) {
        this(metadata, objects, lengthUnit, relationships, null);
    }

    /**
     * Constructs an Mf3Model with the specified properties.
     *
     * @param metadata      map of metadata properties
     * @param objects       list of 3MF objects containing mesh data
     * @param lengthUnit          the measurement unit used for the model
     * @param relationships package relationships
     * @param contentTypes  package content types
     */
    public Mf3Model(final Map<String, String> metadata, final List<Mf3Object> objects, final LengthUnit lengthUnit, final Mf3Relationships relationships, final Mf3ContentTypes contentTypes) {
        this.lengthUnit = lengthUnit;
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

    @Override
    public List<MeshPart> parts() {
        List<MeshPart> allParts = new ArrayList<>();
        Map<Integer, Mf3BaseMaterials> materialsMap = new HashMap<>();
        if (getResources() != null && getResources().getBaseMaterials() != null) {
            for (Mf3BaseMaterials bm : getResources().getBaseMaterials()) {
                materialsMap.put(bm.getId(), bm);
            }
        }

        for (Mf3Object obj : objects()) {
            List<Vector3f> vertices = obj.vertices();
            List<Mf3Triangle> triangles = obj.triangles();

            if (triangles == null || triangles.isEmpty()) {
                continue;
            }

            Map<String, List<Mf3Triangle>> groupedTriangles = new HashMap<>();
            for (Mf3Triangle tri : triangles) {
                Integer pid = tri.getPid() != null ? tri.getPid() : obj.getPid();
                Integer pindex = tri.getPindex() != null ? tri.getPindex() : obj.getPindex();
                String key = (pid != null && pindex != null) ? (pid + ":" + pindex) : "default";

                groupedTriangles.computeIfAbsent(key, k -> new ArrayList<>()).add(tri);
            }

            for (Map.Entry<String, List<Mf3Triangle>> entry : groupedTriangles.entrySet()) {
                String materialKey = entry.getKey();
                List<Mf3Triangle> tris = entry.getValue();

                Integer color = null;
                if (!"default".equals(materialKey)) {
                    String[] materialParts = materialKey.split(":");
                    int pid = Integer.parseInt(materialParts[0]);
                    int pindex = Integer.parseInt(materialParts[1]);
                    Mf3BaseMaterials bm = materialsMap.get(pid);
                    if (bm != null && pindex >= 0 && pindex < bm.getBases().size()) {
                        color = parseColor(bm.getBases().get(pindex).getDisplayColor());
                    }
                }

                allParts.add(new Mf3MeshPart("mf3_obj_" + obj.id() + "_" + materialKey, color, tris, vertices));
            }
        }
        return allParts;
    }

    private Integer parseColor(String colorStr) {
        if (colorStr == null || !colorStr.startsWith("#")) {
            return null;
        }
        try {
            String hex = colorStr.substring(1);
            if (hex.length() >= 6) {
                return Integer.parseInt(hex.substring(0, 6), 16);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private record Mf3MeshPart(String name, Integer color, List<Mf3Triangle> mf3Triangles, List<Vector3f> vertices) implements MeshPart {
        @Override
        public List<? extends Triangle> triangles() {
            List<Triangle> result = new ArrayList<>(mf3Triangles.size());
            for (Mf3Triangle tri : mf3Triangles) {
                result.add(new Triangle() {
                    @Override public Vector3f v1() { return vertices.get(tri.v1()); }
                    @Override public Vector3f v2() { return vertices.get(tri.v2()); }
                    @Override public Vector3f v3() { return vertices.get(tri.v3()); }
                    @Override public Vector3f normal() { return null; }
                });
            }
            return result;
        }
    }

    /**
     * Returns the measurement unit used for the model.
     *
     * @return the measurement unit
     */
    @Override
    public LengthUnit lengthUnit() {
        return lengthUnit;
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
     * @param lengthUnit the unit to set
     */
    public void setUnit(final LengthUnit lengthUnit) {
        this.lengthUnit = lengthUnit;
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
     * Returns the Bambu-specific plate configuration.
     *
     * @return the Bambu configuration, or null if not present
     */
    public Mf3BambuConfig getBambuConfig() {
        return bambuConfig;
    }

    /**
     * Sets the Bambu-specific plate configuration.
     *
     * @param bambuConfig the configuration to set
     */
    public void setBambuConfig(final Mf3BambuConfig bambuConfig) {
        this.bambuConfig = bambuConfig;
    }

    /**
     * Returns the Bambu-specific custom G-code.
     *
     * @return the Bambu custom G-code, or null if not present
     */
    public Mf3BambuCustomGCode getBambuCustomGCode() {
        return bambuCustomGCode;
    }

    /**
     * Sets the Bambu-specific custom G-code.
     *
     * @param bambuCustomGCode the custom G-code to set
     */
    public void setBambuCustomGCode(final Mf3BambuCustomGCode bambuCustomGCode) {
        this.bambuCustomGCode = bambuCustomGCode;
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

    /**
     * Compares this Mf3Model with another object for equality.
     * Two models are considered equal if all their fields (unit, metadata, resources, 
     * build items, relationships, content types, and storage path) are equal.
     *
     * @param o the object to compare with
     * @return true if the models are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3Model mf3Model = (Mf3Model) o;
        return lengthUnit == mf3Model.lengthUnit &&
                java.util.Objects.equals(metadataList, mf3Model.metadataList) &&
                java.util.Objects.equals(resources, mf3Model.resources) &&
                java.util.Objects.equals(build, mf3Model.build) &&
                java.util.Objects.equals(relationshipParts, mf3Model.relationshipParts) &&
                java.util.Objects.equals(contentTypes, mf3Model.contentTypes) &&
                java.util.Objects.equals(storagePath, mf3Model.storagePath) &&
                java.util.Objects.equals(prusaMainMetadata, mf3Model.prusaMainMetadata) &&
                java.util.Objects.equals(prusaSlicerModelConfig, mf3Model.prusaSlicerModelConfig) &&
                java.util.Objects.equals(prusaSettings, mf3Model.prusaSettings) &&
                java.util.Objects.equals(bambuConfig, mf3Model.bambuConfig) &&
                java.util.Objects.equals(bambuCustomGCode, mf3Model.bambuCustomGCode);
    }

    /**
     * Returns the hash code for this Mf3Model.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(lengthUnit, metadataList, resources, build, relationshipParts, contentTypes, storagePath, prusaMainMetadata, prusaSlicerModelConfig, prusaSettings, bambuConfig, bambuCustomGCode);
    }

    /**
     * Returns a string representation of this Mf3Model.
     *
     * @return a string containing model details
     */
    @Override
    public String toString() {
        return "Mf3Model{" +
                "unit=" + lengthUnit +
                ", metadataList=" + metadataList +
                ", resources=" + resources +
                ", build=" + build +
                ", relationshipParts=" + relationshipParts +
                ", contentTypes=" + contentTypes +
                ", storagePath=" + storagePath +
                ", prusaMainMetadata=" + prusaMainMetadata +
                ", prusaSlicerModelConfig=" + prusaSlicerModelConfig +
                ", prusaSettings=" + prusaSettings +
                ", bambuConfig=" + bambuConfig +
                ", bambuCustomGCode=" + bambuCustomGCode +
                '}';
    }
}
