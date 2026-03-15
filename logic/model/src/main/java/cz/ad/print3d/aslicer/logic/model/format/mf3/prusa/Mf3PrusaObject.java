package cz.ad.print3d.aslicer.logic.model.format.mf3.prusa;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an object with Prusa-specific configuration in a 3MF file.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3PrusaObject {

    /**
     * The ID of the 3MF object this configuration refers to.
     */
    @XmlAttribute(name = "id")
    private Integer id;

    /**
     * The number of instances of this object.
     */
    @XmlAttribute(name = "instances_count")
    private Integer instancesCount;

    /**
     * Metadata associated with this object.
     */
    @XmlElement(name = "metadata")
    private List<Mf3PrusaConfigMetadata> metadataList = new ArrayList<>();

    /**
     * Volumes associated with this object.
     */
    @XmlElement(name = "volume")
    private List<Mf3PrusaVolume> volumes = new ArrayList<>();

    /**
     * Default constructor for JAXB.
     */
    public Mf3PrusaObject() {
    }

    /**
     * Returns the object ID.
     * @return the object ID
     */
    public Integer getId() {
        return id;
    }

    /**
     * Returns the instances count.
     * @return the instances count
     */
    public Integer getInstancesCount() {
        return instancesCount;
    }

    /**
     * Returns the list of metadata.
     * @return the metadata list
     */
    public List<Mf3PrusaConfigMetadata> getMetadataList() {
        return metadataList;
    }

    /**
     * Returns the list of volumes.
     * @return the volumes list
     */
    public List<Mf3PrusaVolume> getVolumes() {
        return volumes;
    }

    /**
     * Compares this Prusa object configuration with another for equality.
     *
     * @param o the object to compare with
     * @return true if both have identical configuration values
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3PrusaObject that = (Mf3PrusaObject) o;
        return java.util.Objects.equals(id, that.id) &&
                java.util.Objects.equals(instancesCount, that.instancesCount) &&
                java.util.Objects.equals(metadataList, that.metadataList) &&
                java.util.Objects.equals(volumes, that.volumes);
    }

    /**
     * Returns the hash code for this Prusa object configuration.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, instancesCount, metadataList, volumes);
    }

    /**
     * Returns a string representation of this Prusa object configuration.
     *
     * @return a string containing object configuration data
     */
    @Override
    public String toString() {
        return "Mf3PrusaObject{" +
                "id=" + id +
                ", instancesCount=" + instancesCount +
                ", metadataList=" + metadataList +
                ", volumes=" + volumes +
                '}';
    }
}
