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
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the components container within a 3MF object.
 *
 * <p>According to the 3MF Core Specification, an {@code <object>} element can
 * contain a {@code <components>} element instead of a {@code <mesh>} element.
 * This represents an assembly, where the object's geometry is defined by the
 * aggregation of its child component instances.</p>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3Components {

    /**
     * The list of component instances.
     */
    @XmlElement(name = "component", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
    private List<Mf3Component> componentList = new ArrayList<>();

    /**
     * Default constructor for JAXB.
     */
    public Mf3Components() {
    }

    /**
     * Returns the list of component instances.
     *
     * @return the component list
     */
    public List<Mf3Component> getComponents() {
        return componentList;
    }

    /**
     * Sets the list of component instances.
     *
     * @param components the list to set
     */
    public void setComponents(final List<Mf3Component> components) {
        this.componentList = components;
    }

    /**
     * Compares this components container with another for equality.
     *
     * @param o the object to compare with
     * @return true if both containers have identical components
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3Components that = (Mf3Components) o;
        return java.util.Objects.equals(componentList, that.componentList);
    }

    /**
     * Returns the hash code for this components container.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(componentList);
    }

    /**
     * Returns a string representation of this components container.
     *
     * @return a string containing components data
     */
    @Override
    public String toString() {
        return "Mf3Components{" +
                "componentList=" + componentList +
                '}';
    }
}
