package cz.ad.print3d.aslicer.logic.model.format.mf3.relationship;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

/**
 * Represents an individual "Relationship" element within an Open Packaging
 * Conventions (OPC) relationship file.
 *
 * <p>A relationship expresses a connection between a source (the part containing
 * the relationship file) and a target (either another part or an external resource).
 * Each relationship is uniquely identified by an "Id" attribute and categorised
 * by a "Type" URI.</p>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3Relationship {

    /**
     * Unique ID for the relationship, typically in the form "rIdN".
     */
    @XmlAttribute(name = "Id")
    private String id;

    /**
     * URI defining the relationship type (e.g.,
     * "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel").
     */
    @XmlAttribute(name = "Type")
    private String type;

    /**
     * URI pointing to the target part or external resource.
     */
    @XmlAttribute(name = "Target")
    private String target;

    /**
     * Mode of the target: "Internal" (default) or "External".
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
}
