package cz.ad.print3d.aslicer.logic.model.format.mf3.contenttype;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the root "Types" element of the [Content_Types].xml file in a 3MF package.
 *
 * <p>The [Content_Types].xml file is a required part of an Open Packaging Conventions (OPC)
 * package, such as a 3MF file. This file defines the content types for all parts within the
 * ZIP package, enabling consumers to correctly interpret and process the various components
 * (e.g., the 3D model, textures, and print tickets).</p>
 *
 * <p>According to the OPC specification (ECMA-376 Part 2), the "Types" element contains child
 * elements that specify the content types of all parts in the package. Each part name and
 * its content type must be specified once in the [Content_Types].xml part.</p>
 *
 * <p>The XML namespace for this element is {@code http://schemas.openxmlformats.org/package/2006/content-types}.</p>
 */
@XmlRootElement(name = "Types", namespace = "http://schemas.openxmlformats.org/package/2006/content-types")
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3ContentTypes {

    /**
     * List of {@code <Default>} elements that specify content types based on file extensions.
     */
    @XmlElement(name = "Default", namespace = "http://schemas.openxmlformats.org/package/2006/content-types")
    private List<Mf3Default> defaults = new ArrayList<>();

    /**
     * List of {@code <Override>} elements that specify content types for specific part names.
     */
    @XmlElement(name = "Override", namespace = "http://schemas.openxmlformats.org/package/2006/content-types")
    private List<Mf3Override> overrides = new ArrayList<>();

    /**
     * Returns the list of default content type mappings for the package.
     *
     * @return a {@link List} of {@link Mf3Default} objects
     */
    public List<Mf3Default> getDefaults() {
        return defaults;
    }

    /**
     * Sets the list of default content type mappings for the package.
     *
     * @param defaults the {@link List} of {@link Mf3Default} objects to set
     */
    public void setDefaults(List<Mf3Default> defaults) {
        this.defaults = defaults;
    }

    /**
     * Returns the list of override content type mappings for the package.
     *
     * @return a {@link List} of {@link Mf3Override} objects
     */
    public List<Mf3Override> getOverrides() {
        return overrides;
    }

    /**
     * Sets the list of override content type mappings for the package.
     *
     * @param overrides the {@link List} of {@link Mf3Override} objects to set
     */
    public void setOverrides(List<Mf3Override> overrides) {
        this.overrides = overrides;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3ContentTypes that = (Mf3ContentTypes) o;
        return java.util.Objects.equals(defaults, that.defaults) &&
                java.util.Objects.equals(overrides, that.overrides);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(defaults, overrides);
    }

    @Override
    public String toString() {
        return "Mf3ContentTypes{" +
                "defaults=" + defaults +
                ", overrides=" + overrides +
                '}';
    }
}
