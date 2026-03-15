package cz.ad.print3d.aslicer.logic.model.format.mf3.prusa;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the root configuration element in a Prusa-formatted 3MF file's Metadata/Slic3r_PE_model.config.
 */
@XmlRootElement(name = "config")
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3PrusaSlicerModelConfig {

    /**
     * List of object configurations.
     */
    @XmlElement(name = "object")
    private List<Mf3PrusaObject> objects = new ArrayList<>();

    /**
     * Default constructor for JAXB.
     */
    public Mf3PrusaSlicerModelConfig() {
    }

    /**
     * Returns the list of object configurations.
     * @return the objects list
     */
    public List<Mf3PrusaObject> getObjects() {
        return objects;
    }

    /**
     * Compares this Prusa model configuration with another for equality.
     *
     * @param o the object to compare with
     * @return true if both have identical configuration
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3PrusaSlicerModelConfig that = (Mf3PrusaSlicerModelConfig) o;
        return java.util.Objects.equals(objects, that.objects);
    }

    /**
     * Returns the hash code for this Prusa model configuration.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(objects);
    }

    /**
     * Returns a string representation of this Prusa model configuration.
     *
     * @return a string containing object configuration data
     */
    @Override
    public String toString() {
        return "Mf3PrusaSlicerModelConfig{" +
                "objects=" + objects +
                '}';
    }
}
