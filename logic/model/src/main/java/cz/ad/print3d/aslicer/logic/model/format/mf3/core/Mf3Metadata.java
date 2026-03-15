package cz.ad.print3d.aslicer.logic.model.format.mf3.core;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * Represents a metadata entry in a 3MF model.
 * Mf3Metadata provides additional information about the model, such as Title, Designer, or Copyright.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3Metadata {

    /**
     * The name of the metadata property.
     */
    @XmlAttribute(name = "name")
    private String name;

    /**
     * The value of the metadata property.
     */
    @XmlValue
    private String value;

    /**
     * Default constructor for JAXB.
     */
    public Mf3Metadata() {
    }

    /**
     * Constructs a Mf3Metadata entry with the specified name and value.
     *
     * @param name  the property name
     * @param value the property value
     */
    public Mf3Metadata(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Returns the name of the metadata property.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the metadata property.
     *
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Returns the value of the metadata property.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the metadata property.
     *
     * @param value the value to set
     */
    public void setValue(final String value) {
        this.value = value;
    }

    /**
     * Compares this metadata entry with another for equality.
     *
     * @param o the object to compare with
     * @return true if both entries have identical name and value
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3Metadata metadata = (Mf3Metadata) o;
        return java.util.Objects.equals(name, metadata.name) &&
                java.util.Objects.equals(value, metadata.value);
    }

    /**
     * Returns the hash code for this metadata entry.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, value);
    }

    /**
     * Returns a string representation of this metadata entry.
     *
     * @return a string containing name and value
     */
    @Override
    public String toString() {
        return "Mf3Metadata{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
