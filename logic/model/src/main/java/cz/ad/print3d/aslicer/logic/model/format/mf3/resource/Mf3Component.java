package cz.ad.print3d.aslicer.logic.model.format.mf3.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

/**
 * Represents a component instance within a 3MF object.
 *
 * <p>A component references another object (typically a mesh object) from the
 * resources section and defines its placement within the parent assembly object.</p>
 *
 * <p>According to the 3MF Core Specification:
 * <ul>
 *   <li>The {@code objectid} attribute refers to the unique ID of the referenced object.</li>
 *   <li>The {@code transform} attribute specifies an optional 4x3 matrix transformation
 *       applied to the referenced object.</li>
 * </ul>
 * </p>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3Component {

    /**
     * The ID of the referenced object.
     */
    @XmlAttribute(name = "objectid")
    private int objectId;

    /**
     * Transformation matrix to be applied to the object.
     * Optional attribute, usually represented as a 4x3 matrix string.
     */
    @XmlAttribute(name = "transform")
    private String transform;

    /**
     * Default constructor for JAXB.
     */
    public Mf3Component() {
    }

    /**
     * Constructs a Mf3Component with the specified object ID.
     *
     * @param objectId the ID of the referenced object
     */
    public Mf3Component(final int objectId) {
        this.objectId = objectId;
    }

    /**
     * Returns the object ID referenced by this component.
     *
     * @return the object ID
     */
    public int getObjectId() {
        return objectId;
    }

    /**
     * Sets the object ID for this component.
     *
     * @param objectId the ID to set
     */
    public void setObjectId(final int objectId) {
        this.objectId = objectId;
    }

    /**
     * Returns the transformation matrix string.
     *
     * @return the transform string
     */
    public String getTransform() {
        return transform;
    }

    /**
     * Sets the transformation matrix string.
     *
     * @param transform the transform string to set
     */
    public void setTransform(final String transform) {
        this.transform = transform;
    }

    /**
     * Compares this component with another for equality.
     *
     * @param o the object to compare with
     * @return true if both reference the same object and have identical transform
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3Component component = (Mf3Component) o;
        return objectId == component.objectId &&
                java.util.Objects.equals(transform, component.transform);
    }

    /**
     * Returns the hash code for this component.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(objectId, transform);
    }

    /**
     * Returns a string representation of this component.
     *
     * @return a string containing component data
     */
    @Override
    public String toString() {
        return "Mf3Component{" +
                "objectId=" + objectId +
                ", transform='" + transform + '\'' +
                '}';
    }
}
