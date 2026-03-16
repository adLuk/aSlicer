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
package cz.ad.print3d.aslicer.logic.model.basic;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Represents a vertex in a 3MF mesh, as defined in the 3MF specification.
 * A 3MF vertex specifies a location in a right-handed Cartesian 3D coordinate 
 * system using floating-point coordinates.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "vertex", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
public class Vector3f {

    /**
     * X-coordinate.
     */
    @XmlAttribute(name = "x")
    private float x;

    /**
     * Y-coordinate.
     */
    @XmlAttribute(name = "y")
    private float y;

    /**
     * Z-coordinate.
     */
    @XmlAttribute(name = "z")
    private float z;

    /**
     * Default constructor for JAXB.
     */
    public Vector3f() {
    }

    /**
     * Constructs a Vector3f with the specified coordinates.
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @param z the z-coordinate
     */
    public Vector3f(final float x, final float y, final float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Returns the x-coordinate.
     *
     * @return the x-coordinate
     */
    public float x() {
        return x;
    }

    /**
     * Returns the y-coordinate.
     *
     * @return the y-coordinate
     */
    public float y() {
        return y;
    }

    /**
     * Returns the z-coordinate.
     *
     * @return the z-coordinate
     */
    public float z() {
        return z;
    }

    /**
     * Sets the x-coordinate.
     *
     * @param x the x-coordinate to set
     */
    public void setX(final float x) {
        this.x = x;
    }

    /**
     * Sets the y-coordinate.
     *
     * @param y the y-coordinate to set
     */
    public void setY(final float y) {
        this.y = y;
    }

    /**
     * Sets the z-coordinate.
     *
     * @param z the z-coordinate to set
     */
    public void setZ(final float z) {
        this.z = z;
    }

    /**
     * Compares this vector with another object for equality.
     *
     * @param o the object to compare with
     * @return true if the vectors have identical coordinates
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vector3f vector3f = (Vector3f) o;
        return Float.compare(vector3f.x, x) == 0 &&
                Float.compare(vector3f.y, y) == 0 &&
                Float.compare(vector3f.z, z) == 0;
    }

    /**
     * Returns the hash code for this vector.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(x, y, z);
    }

    /**
     * Returns a string representation of this vector.
     *
     * @return a string containing coordinates
     */
    @Override
    public String toString() {
        return "Vector3f{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}
