package cz.ad.print3d.aslicer.logic.model.format.mf3.relationship;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

/**
 * Represents an individual "Relationship" element within an Open Packaging
 * Conventions (OPC) relationship file.
 *
 * <p>A relationship expresses a connection between a source (the part containing
 * the relationship file) and a target (either another part in the package or an
 * external resource). Each relationship is categorized by a "Type" URI and
 * uniquely identified by an "Id" attribute within the relationship part.</p>
 *
 * <p>According to the OPC specification (ECMA-376 Part 2):
 * <ul>
 *   <li>The {@code Id} attribute specifies a unique identifier for the relationship.</li>
 *   <li>The {@code Type} attribute specifies the relationship type URI, defining the role of the connection.</li>
 *   <li>The {@code Target} attribute specifies the URI of the target resource.</li>
 *   <li>The {@code TargetMode} attribute specifies whether the target is internal or external to the package.</li>
 * </ul>
 * </p>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3Relationship {

    /**
     * Unique identifier for the relationship, typically in the form "rIdN".
     * This ID must be unique within the relationship part.
     */
    @XmlAttribute(name = "Id")
    private String id;

    /**
     * URI defining the relationship type, which defines the role or category of the
     * relationship (e.g., "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel").
     */
    @XmlAttribute(name = "Type")
    private String type;

    /**
     * URI pointing to the target part (relative path within the package) or an
     * external resource (absolute URI).
     */
    @XmlAttribute(name = "Target")
    private String target;

    /**
     * Mode of the target: "Internal" (default) if the target is another part in the
     * same package, or "External" if the target is outside the package.
     */
    @XmlAttribute(name = "TargetMode")
    private String targetMode;

    /**
     * Default constructor for JAXB unmarshalling.
     */
    public Mf3Relationship() {
    }

    /**
     * Returns the unique identifier for this relationship.
     *
     * @return the unique ID (e.g., "rId1")
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this relationship.
     *
     * @param id the unique ID string to set
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Returns the relationship type URI.
     *
     * @return the URI string defining the relationship category
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the relationship type URI.
     *
     * @param type the URI string defining the relationship category
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * Returns the URI of the target resource or part.
     *
     * @return the path or URI string to the target
     */
    public String getTarget() {
        return target;
    }

    /**
     * Sets the URI of the target resource or part.
     *
     * @param target the path or URI string to the target
     */
    public void setTarget(final String target) {
        this.target = target;
    }

    /**
     * Returns the target mode (Internal or External).
     *
     * @return the mode string (e.g., "Internal", "External")
     */
    public String getTargetMode() {
        return targetMode;
    }

    /**
     * Sets the target mode (Internal or External).
     *
     * @param targetMode the mode string (e.g., "Internal", "External")
     */
    public void setTargetMode(final String targetMode) {
        this.targetMode = targetMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3Relationship that = (Mf3Relationship) o;
        return java.util.Objects.equals(id, that.id) &&
                java.util.Objects.equals(type, that.type) &&
                java.util.Objects.equals(target, that.target) &&
                java.util.Objects.equals(targetMode, that.targetMode);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, type, target, targetMode);
    }

    @Override
    public String toString() {
        return "Mf3Relationship{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", target='" + target + '\'' +
                ", targetMode='" + targetMode + '\'' +
                '}';
    }
}
