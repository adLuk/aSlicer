package cz.ad.print3d.aslicer.logic.model.format.mf3.resource;

import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import cz.ad.print3d.aslicer.logic.model.format.mf3.geometry.Mf3Mesh;
import cz.ad.print3d.aslicer.logic.model.format.mf3.geometry.Mf3Triangle;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.Collections;
import java.util.List;

/**
 * Represents an object in a 3MF model, typically containing a mesh.
 * Objects are the primary building blocks of a 3MF model and can represent
 * individual parts or components.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3Object {

    /**
     * Unique identifier of the object within the model.
     */
    @XmlAttribute(name = "id")
    private int id;

    /**
     * Human-readable name of the object.
     */
    @XmlAttribute(name = "name")
    private String name;

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
     * The type of the object (e.g., "model", "support", "other").
     */
    @XmlAttribute(name = "type")
    private String type;

    /**
     * Part number for the object.
     */
    @XmlAttribute(name = "partnumber")
    private String partNumber;

    /**
     * The mesh data associated with this object.
     */
    @XmlElement(name = "mesh", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
    private Mf3Mesh mesh;

    /**
     * The components data associated with this object.
     */
    @XmlElement(name = "components", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
    private Mf3Components components;

    /**
     * Default constructor for JAXB.
     */
    public Mf3Object() {
    }

    /**
     * Constructs an Mf3Object with the specified properties.
     *
     * @param id        the unique identifier
     * @param name      the object name
     * @param vertices  the list of mesh vertices
     * @param triangles the list of mesh triangles
     */
    public Mf3Object(final int id, final String name, final List<Vector3f> vertices, final List<Mf3Triangle> triangles) {
        this.id = id;
        this.name = name;
        this.mesh = new Mf3Mesh();
        this.mesh.setVertices(vertices);
        this.mesh.setTriangles(triangles);
    }

    /**
     * Returns the unique identifier of the object.
     *
     * @return the ID
     */
    public int id() {
        return id;
    }

    /**
     * Returns the name of the object.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the property ID for the object.
     *
     * @return the property ID
     */
    public Integer getPid() {
        return pid;
    }

    /**
     * Sets the property ID for the object.
     *
     * @param pid the property ID to set
     */
    public void setPid(Integer pid) {
        this.pid = pid;
    }

    /**
     * Returns the property index for the object.
     *
     * @return the property index
     */
    public Integer getPindex() {
        return pindex;
    }

    /**
     * Sets the property index for the object.
     *
     * @param pindex the property index to set
     */
    public void setPindex(Integer pindex) {
        this.pindex = pindex;
    }

    /**
     * Returns the type of the object.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the object.
     *
     * @param type the type to set
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * Returns the part number of the object.
     *
     * @return the part number
     */
    public String getPartNumber() {
        return partNumber;
    }

    /**
     * Sets the part number of the object.
     *
     * @param partNumber the part number to set
     */
    public void setPartNumber(final String partNumber) {
        this.partNumber = partNumber;
    }

    /**
     * Returns the list of mesh vertices.
     *
     * @return the list of vertices
     */
    public List<Vector3f> vertices() {
        return mesh != null ? mesh.getVertices() : Collections.emptyList();
    }

    /**
     * Returns the list of mesh triangles referencing vertices by index.
     *
     * @return the list of triangles
     */
    public List<Mf3Triangle> triangles() {
        return mesh != null ? mesh.getTriangles() : Collections.emptyList();
    }

    /**
     * Sets the unique identifier of the object.
     *
     * @param id the ID to set
     */
    public void setId(final int id) {
        this.id = id;
    }

    /**
     * Sets the name of the object.
     *
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Returns the mesh data.
     *
     * @return the mesh
     */
    public Mf3Mesh getMesh() {
        return mesh;
    }

    /**
     * Sets the mesh data.
     *
     * @param mesh the mesh to set
     */
    public void setMesh(final Mf3Mesh mesh) {
        this.mesh = mesh;
    }

    /**
     * Returns the components data.
     *
     * @return the components
     */
    public Mf3Components getComponents() {
        return components;
    }

    /**
     * Sets the components data.
     *
     * @param components the components to set
     */
    public void setComponents(final Mf3Components components) {
        this.components = components;
    }

    /**
     * Compares this object with another for equality.
     *
     * @param o the object to compare with
     * @return true if both objects have identical data
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3Object mf3Object = (Mf3Object) o;
        return id == mf3Object.id &&
                java.util.Objects.equals(name, mf3Object.name) &&
                java.util.Objects.equals(pid, mf3Object.pid) &&
                java.util.Objects.equals(pindex, mf3Object.pindex) &&
                java.util.Objects.equals(type, mf3Object.type) &&
                java.util.Objects.equals(partNumber, mf3Object.partNumber) &&
                java.util.Objects.equals(mesh, mf3Object.mesh) &&
                java.util.Objects.equals(components, mf3Object.components);
    }

    /**
     * Returns the hash code for this object.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name, pid, pindex, type, partNumber, mesh, components);
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string containing object data
     */
    @Override
    public String toString() {
        return "Mf3Object{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", pid=" + pid +
                ", pindex=" + pindex +
                ", type='" + type + '\'' +
                ", partNumber='" + partNumber + '\'' +
                ", mesh=" + mesh +
                ", components=" + components +
                '}';
    }
}
