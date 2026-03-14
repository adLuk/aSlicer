package cz.ad.print3d.aslicer.logic.model.format.mf3.build;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

/**
 * Represents a build item in a 3MF model.
 *
 * <p>Each {@code <item>} references a specific object from the resources section to be
 * manufactured. The item can specify an optional transformation to be applied to that
 * object's coordinate system.</p>
 *
 * <p>According to the 3MF Core Specification:
 * <ul>
 *   <li>The {@code objectid} attribute refers to the unique ID of an {@code <object>}
 *       element in the {@code <resources>} section.</li>
 *   <li>The {@code transform} attribute specifies a 4x3 matrix to be applied to the
 *       object before it is built.</li>
 * </ul>
 * </p>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3Item {

    /**
     * The ID of the object to be built.
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
    public Mf3Item() {
    }

    /**
     * Constructs an Mf3Item with the specified object ID.
     *
     * @param objectId the ID of the object
     */
    public Mf3Item(final int objectId) {
        this.objectId = objectId;
    }

    /**
     * Returns the object ID referenced by this item.
     *
     * @return the object ID
     */
    public int getObjectId() {
        return objectId;
    }

    /**
     * Sets the object ID for this item.
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3Item item = (Mf3Item) o;
        return objectId == item.objectId &&
                java.util.Objects.equals(transform, item.transform);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(objectId, transform);
    }

    @Override
    public String toString() {
        return "Mf3Item{" +
                "objectId=" + objectId +
                ", transform='" + transform + '\'' +
                '}';
    }
}
