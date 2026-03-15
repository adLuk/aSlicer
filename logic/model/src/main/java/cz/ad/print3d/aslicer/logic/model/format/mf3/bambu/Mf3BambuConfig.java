package cz.ad.print3d.aslicer.logic.model.format.mf3.bambu;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@XmlRootElement(name = "config")
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3BambuConfig {

    public static class Plate {
        @XmlAttribute(name = "id")
        private String id;

        public Plate() {}
        public Plate(String id) { this.id = id; }

        @XmlTransient
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Plate plate = (Plate) o;
            return Objects.equals(id, plate.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    private List<Plate> plate = new ArrayList<>();

    public List<Plate> getPlate() { return plate; }
    public void setPlate(List<Plate> plate) { this.plate = plate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3BambuConfig that = (Mf3BambuConfig) o;
        return Objects.equals(plate, that.plate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plate);
    }
}
