package cz.ad.print3d.aslicer.logic.model.format.mf3.prusa;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a volume in a Prusa-formatted 3MF file.
 * A volume defines a part of an object, often used for multi-material printing or modifiers.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3PrusaVolume {

    /**
     * Index of the first triangle in the object's mesh that belongs to this volume.
     */
    @XmlAttribute(name = "firstid")
    private Integer firstId;

    /**
     * Index of the last triangle in the object's mesh that belongs to this volume.
     */
    @XmlAttribute(name = "lastid")
    private Integer lastId;

    /**
     * Metadata associated with this volume.
     */
    @XmlElement(name = "metadata")
    private List<Mf3PrusaConfigMetadata> metadataList = new ArrayList<>();

    /**
     * Statistics about the mesh for this volume.
     */
    @XmlElement(name = "mesh")
    private Mf3PrusaMeshStats mesh;

    /**
     * Default constructor for JAXB.
     */
    public Mf3PrusaVolume() {
    }

    /**
     * Returns the first triangle ID.
     * @return the first triangle ID
     */
    public Integer getFirstId() {
        return firstId;
    }

    /**
     * Returns the last triangle ID.
     * @return the last triangle ID
     */
    public Integer getLastId() {
        return lastId;
    }

    /**
     * Returns the list of metadata for this volume.
     * @return the metadata list
     */
    public List<Mf3PrusaConfigMetadata> getMetadataList() {
        return metadataList;
    }

    /**
     * Returns the mesh statistics for this volume.
     * @return the mesh stats
     */
    public Mf3PrusaMeshStats getMesh() {
        return mesh;
    }

    /**
     * Returns the name of the volume if present in metadata.
     * @return the name, or null
     */
    public String getName() {
        return getMetadataValue("name");
    }

    /**
     * Returns the volume type (e.g., ModelPart, ParameterModifier).
     * @return the volume type, or null
     */
    public String getVolumeType() {
        return getMetadataValue("volume_type");
    }

    /**
     * Helper to get a metadata value by key.
     * @param key the metadata key
     * @return the value, or null
     */
    private String getMetadataValue(String key) {
        if (metadataList == null) return null;
        for (Mf3PrusaConfigMetadata meta : metadataList) {
            if (key.equals(meta.getKey())) {
                return meta.getValue();
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3PrusaVolume that = (Mf3PrusaVolume) o;
        return java.util.Objects.equals(firstId, that.firstId) &&
                java.util.Objects.equals(lastId, that.lastId) &&
                java.util.Objects.equals(metadataList, that.metadataList) &&
                java.util.Objects.equals(mesh, that.mesh);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(firstId, lastId, metadataList, mesh);
    }

    @Override
    public String toString() {
        return "Mf3PrusaVolume{" +
                "firstId=" + firstId +
                ", lastId=" + lastId +
                ", metadataList=" + metadataList +
                ", mesh=" + mesh +
                '}';
    }
}
