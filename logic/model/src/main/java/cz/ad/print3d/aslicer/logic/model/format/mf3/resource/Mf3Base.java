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
package cz.ad.print3d.aslicer.logic.model.format.mf3.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import java.util.Objects;

/**
 * Represents a single material in a 3MF base material group.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "base", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
public class Mf3Base {

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "displaycolor")
    private String displayColor;

    public Mf3Base() {
    }

    public Mf3Base(String name, String displayColor) {
        this.name = name;
        this.displayColor = displayColor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayColor() {
        return displayColor;
    }

    public void setDisplayColor(String displayColor) {
        this.displayColor = displayColor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3Base mf3Base = (Mf3Base) o;
        return Objects.equals(name, mf3Base.name) && Objects.equals(displayColor, mf3Base.displayColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, displayColor);
    }

    @Override
    public String toString() {
        return "Mf3Base{" +
                "name='" + name + '\'' +
                ", displayColor='" + displayColor + '\'' +
                '}';
    }
}
