package cz.ad.print3d.aslicer.logic.model.format.mf3.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for 3MF resources, such as objects and materials.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3Resources {

    /**
     * The list of objects defined in the resources.
     */
    @XmlElement(name = "object", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
    private List<Mf3Object> objects = new ArrayList<>();

    /**
     * The list of base materials defined in the resources.
     */
    @XmlElement(name = "basematerials", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
    private List<Mf3BaseMaterials> baseMaterials = new ArrayList<>();

    /**
     * Returns the list of objects in the resources.
     *
     * @return the list of objects
     */
    public List<Mf3Object> getObjects() {
        return objects;
    }

    /**
     * Sets the list of objects in the resources.
     *
     * @param objects the list of objects to set
     */
    public void setObjects(final List<Mf3Object> objects) {
        this.objects = objects;
    }

    /**
     * Returns the list of base materials in the resources.
     *
     * @return the list of base materials
     */
    public List<Mf3BaseMaterials> getBaseMaterials() {
        return baseMaterials;
    }

    /**
     * Sets the list of base materials in the resources.
     *
     * @param baseMaterials the list of base materials to set
     */
    public void setBaseMaterials(final List<Mf3BaseMaterials> baseMaterials) {
        this.baseMaterials = baseMaterials;
    }

    /**
     * Compares this resources container with another for equality.
     *
     * @param o the object to compare with
     * @return true if both containers have identical objects
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3Resources resources = (Mf3Resources) o;
        return java.util.Objects.equals(objects, resources.objects) &&
                java.util.Objects.equals(baseMaterials, resources.baseMaterials);
    }

    /**
     * Returns the hash code for this resources container.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(objects, baseMaterials);
    }

    /**
     * Returns a string representation of this resources container.
     *
     * @return a string containing resources data
     */
    @Override
    public String toString() {
        return "Mf3Resources{" +
                "objects=" + objects +
                ", baseMaterials=" + baseMaterials +
                '}';
    }
}
