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
package cz.ad.print3d.aslicer.logic.model.format.mf3.geometry;

import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the mesh data of a 3MF object.
 * A mesh consists of a set of vertices and a set of triangles that reference those vertices.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3Mesh {

    /**
     * The list of vertices in the mesh.
     */
    @XmlElementWrapper(name = "vertices", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
    @XmlElement(name = "vertex", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
    private List<Vector3f> vertices = new ArrayList<>();

    /**
     * The list of triangles in the mesh.
     */
    @XmlElementWrapper(name = "triangles", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
    @XmlElement(name = "triangle", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
    private List<Mf3Triangle> triangles = new ArrayList<>();

    /**
     * Returns the list of vertices in the mesh.
     *
     * @return the list of vertices
     */
    public List<Vector3f> getVertices() {
        return vertices;
    }

    /**
     * Sets the list of vertices in the mesh.
     *
     * @param vertices the list of vertices to set
     */
    public void setVertices(final List<Vector3f> vertices) {
        this.vertices = vertices;
    }

    /**
     * Returns the list of triangles in the mesh.
     *
     * @return the list of triangles
     */
    public List<Mf3Triangle> getTriangles() {
        return triangles;
    }

    /**
     * Sets the list of triangles in the mesh.
     *
     * @param triangles the list of triangles to set
     */
    public void setTriangles(final List<Mf3Triangle> triangles) {
        this.triangles = triangles;
    }

    /**
     * Compares this mesh with another object for equality.
     *
     * @param o the object to compare with
     * @return true if both meshes have identical vertices and triangles
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3Mesh mesh = (Mf3Mesh) o;
        return java.util.Objects.equals(vertices, mesh.vertices) &&
                java.util.Objects.equals(triangles, mesh.triangles);
    }

    /**
     * Returns the hash code for this mesh.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(vertices, triangles);
    }

    /**
     * Returns a string representation of this mesh.
     *
     * @return a string containing mesh data
     */
    @Override
    public String toString() {
        return "Mf3Mesh{" +
                "vertices=" + vertices +
                ", triangles=" + triangles +
                '}';
    }
}
