package cz.ad.print3d.aslicer.logic.model.format.mf3.relationship;


import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the root "Relationships" element in a .rels file, adhering to the
 * Open Packaging Conventions (OPC) relationship schema.
 *
 * <p>The "Relationships" element is the container for all relationship definitions
 * within a given part's relationship file. Each relationship defines a connection
 * from the source part to a target part or external resource.</p>
 *
 * <p>The XML namespace for this element is {@code http://schemas.openxmlformats.org/package/2006/relationships}.</p>
 */
@XmlRootElement(name = "Relationships", namespace = "http://schemas.openxmlformats.org/package/2006/relationships")
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3Relationships {

    /**
     * The list of individual Relationship elements within this container.
     */
    @XmlElement(name = "Relationship", namespace = "http://schemas.openxmlformats.org/package/2006/relationships")
    private List<Mf3Relationship> relationships = new ArrayList<>();

    /**
     * Default constructor for JAXB unmarshalling.
     */
    public Mf3Relationships() {
    }

    /**
     * Returns the list of relationship elements defined in this container.
     *
     * @return a {@link List} of {@link Mf3Relationship} objects
     */
    public List<Mf3Relationship> getRelationships() {
        return relationships;
    }

    /**
     * Sets the list of relationship elements for this container.
     *
     * @param relationships the {@link List} of {@link Mf3Relationship} objects to set
     */
    public void setRelationships(final List<Mf3Relationship> relationships) {
        this.relationships = relationships;
    }
}
