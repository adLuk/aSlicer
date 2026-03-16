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
