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

    /**
     * Compares this build item with another for equality.
     *
     * @param o the object to compare with
     * @return true if both reference the same object and have identical transform
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3Item item = (Mf3Item) o;
        return objectId == item.objectId &&
                java.util.Objects.equals(transform, item.transform);
    }

    /**
     * Returns the hash code for this build item.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(objectId, transform);
    }

    /**
     * Returns a string representation of this build item.
     *
     * @return a string containing item data
     */
    @Override
    public String toString() {
        return "Mf3Item{" +
                "objectId=" + objectId +
                ", transform='" + transform + '\'' +
                '}';
    }
}
