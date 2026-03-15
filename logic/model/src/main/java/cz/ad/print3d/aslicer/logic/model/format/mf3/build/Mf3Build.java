package cz.ad.print3d.aslicer.logic.model.format.mf3.build;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the build section of a 3MF model.
 *
 * <p>According to the 3MF Core Specification, the {@code <build>} element contains
 * a sequence of {@code <item>} elements. The build section defines which objects are to
 * be manufactured and their placement in the print volume.</p>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3Build {

    /**
     * List of items to be built.
     */
    @XmlElement(name = "item", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
    private List<Mf3Item> items = new ArrayList<>();

    /**
     * Default constructor for JAXB.
     */
    public Mf3Build() {
    }

    /**
     * Returns the list of items in the build section.
     *
     * @return the list of items
     */
    public List<Mf3Item> getItems() {
        return items;
    }

    /**
     * Sets the list of items in the build section.
     *
     * @param items the list to set
     */
    public void setItems(final List<Mf3Item> items) {
        this.items = items;
    }

    /**
     * Compares this build section with another for equality.
     *
     * @param o the object to compare with
     * @return true if both have identical items
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3Build build = (Mf3Build) o;
        return java.util.Objects.equals(items, build.items);
    }

    /**
     * Returns the hash code for this build section.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(items);
    }

    /**
     * Returns a string representation of this build section.
     *
     * @return a string containing build items
     */
    @Override
    public String toString() {
        return "Mf3Build{" +
                "items=" + items +
                '}';
    }
}
