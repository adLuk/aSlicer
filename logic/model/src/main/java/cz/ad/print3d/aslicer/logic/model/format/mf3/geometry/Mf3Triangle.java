package cz.ad.print3d.aslicer.logic.model.format.mf3.geometry;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Represents a triangular facet in a 3MF mesh, as defined in the 3MF specification.
 * A 3MF triangle is an oriented surface defined by referencing three vertices
 * from the surrounding mesh. Vertices are specified in counter-clockwise 
 * order when viewed from the outside (front face).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "triangle", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
public class Mf3Triangle {

    /**
     * Index of the first vertex.
     */
    @XmlAttribute(name = "v1")
    private int v1;

    /**
     * Index of the second vertex.
     */
    @XmlAttribute(name = "v2")
    private int v2;

    /**
     * Index of the third vertex.
     */
    @XmlAttribute(name = "v3")
    private int v3;

    /**
     * Property ID referencing a property group (e.g., base materials).
     */
    @XmlAttribute(name = "pid")
    private Integer pid;

    /**
     * Index of the property within the referenced group.
     */
    @XmlAttribute(name = "pindex")
    private Integer pindex;

    /**
     * Default constructor for JAXB.
     */
    public Mf3Triangle() {
    }

    /**
     * Constructs an Mf3Triangle with specified vertex indices.
     *
     * @param v1 the index of the first vertex
     * @param v2 the index of the second vertex
     * @param v3 the index of the third vertex
     */
    public Mf3Triangle(final int v1, final int v2, final int v3) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }

    /**
     * Returns the index of the first vertex.
     *
     * @return the vertex index
     */
    public int v1() {
        return v1;
    }

    /**
     * Returns the index of the second vertex.
     *
     * @return the vertex index
     */
    public int v2() {
        return v2;
    }

    /**
     * Returns the index of the third vertex.
     *
     * @return the vertex index
     */
    public int v3() {
        return v3;
    }

    /**
     * Returns the property ID for the triangle.
     *
     * @return the property ID
     */
    public Integer getPid() {
        return pid;
    }

    /**
     * Sets the property ID for the triangle.
     *
     * @param pid the property ID to set
     */
    public void setPid(Integer pid) {
        this.pid = pid;
    }

    /**
     * Returns the property index for the triangle.
     *
     * @return the property index
     */
    public Integer getPindex() {
        return pindex;
    }

    /**
     * Sets the property index for the triangle.
     *
     * @param pindex the property index to set
     */
    public void setPindex(Integer pindex) {
        this.pindex = pindex;
    }

    /**
     * Sets the index of the first vertex.
     *
     * @param v1 the vertex index to set
     */
    public void setV1(final int v1) {
        this.v1 = v1;
    }

    /**
     * Sets the index of the second vertex.
     *
     * @param v2 the vertex index to set
     */
    public void setV2(final int v2) {
        this.v2 = v2;
    }

    /**
     * Sets the index of the third vertex.
     *
     * @param v3 the vertex index to set
     */
    public void setV3(final int v3) {
        this.v3 = v3;
    }

    /**
     * Compares this triangle with another for equality.
     *
     * @param o the object to compare with
     * @return true if both triangles reference the same vertex indices
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3Triangle that = (Mf3Triangle) o;
        return v1 == that.v1 &&
                v2 == that.v2 &&
                v3 == that.v3 &&
                java.util.Objects.equals(pid, that.pid) &&
                java.util.Objects.equals(pindex, that.pindex);
    }

    /**
     * Returns the hash code for this triangle.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(v1, v2, v3, pid, pindex);
    }

    /**
     * Returns a string representation of this triangle.
     *
     * @return a string containing triangle data
     */
    @Override
    public String toString() {
        return "Mf3Triangle{" +
                "v1=" + v1 +
                ", v2=" + v2 +
                ", v3=" + v3 +
                ", pid=" + pid +
                ", pindex=" + pindex +
                '}';
    }
}
