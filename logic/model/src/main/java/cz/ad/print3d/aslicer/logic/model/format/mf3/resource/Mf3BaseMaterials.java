package cz.ad.print3d.aslicer.logic.model.format.mf3.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a group of base materials in a 3MF file.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "basematerials", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
public class Mf3BaseMaterials {

    @XmlAttribute(name = "id")
    private int id;

    @XmlElement(name = "base", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
    private List<Mf3Base> bases = new ArrayList<>();

    public Mf3BaseMaterials() {
    }

    public Mf3BaseMaterials(int id, List<Mf3Base> bases) {
        this.id = id;
        this.bases = bases;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Mf3Base> getBases() {
        return bases;
    }

    public void setBases(List<Mf3Base> bases) {
        this.bases = bases;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3BaseMaterials that = (Mf3BaseMaterials) o;
        return id == that.id && Objects.equals(bases, that.bases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bases);
    }

    @Override
    public String toString() {
        return "Mf3BaseMaterials{" +
                "id=" + id +
                ", bases=" + bases +
                '}';
    }
}
