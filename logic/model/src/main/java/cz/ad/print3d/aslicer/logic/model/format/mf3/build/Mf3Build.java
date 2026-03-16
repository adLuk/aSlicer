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
package cz.ad.print3d.aslicer.logic.model.format.mf3.build;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the build section of a 3MF model.
 *
 * <p>According to the 3MF Core Specification, the {@code <build>} element contains
 * a sequence of {@code <item>} elements. The build section defines which objects are to
 * be manufactured and their placement in the print volume.</p>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3Build {

    /**
     * List of items to be built.
     */
    @XmlElement(name = "item", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
    private List<Mf3Item> items = new ArrayList<>();

    /**
     * Default constructor for JAXB.
     */
    public Mf3Build() {
    }

    /**
     * Returns the list of items in the build section.
     *
     * @return the list of items
     */
    public List<Mf3Item> getItems() {
        return items;
    }

    /**
     * Sets the list of items in the build section.
     *
     * @param items the list to set
     */
    public void setItems(final List<Mf3Item> items) {
        this.items = items;
    }

    /**
     * Compares this build section with another for equality.
     *
     * @param o the object to compare with
     * @return true if both have identical items
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3Build build = (Mf3Build) o;
        return java.util.Objects.equals(items, build.items);
    }

    /**
     * Returns the hash code for this build section.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(items);
    }

    /**
     * Returns a string representation of this build section.
     *
     * @return a string containing build items
     */
    @Override
    public String toString() {
        return "Mf3Build{" +
                "items=" + items +
                '}';
    }
}
