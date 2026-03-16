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
package cz.ad.print3d.aslicer.logic.model.format.mf3.relationship;


import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the root "Relationships" element in a relationship part (.rels file),
 * adhering to the Open Packaging Conventions (OPC) relationship schema.
 *
 * <p>A relationship part is a special XML part in an OPC package (like 3MF) that
 * defines connections between parts. Every part in the package, including the
 * package itself, can have an associated relationship part. The relationships
 * part for the package is located at {@code /_rels/.rels}.</p>
 *
 * <p>According to the OPC specification (ECMA-376 Part 2), the {@code <Relationships>}
 * element is the container for all relationship definitions within a given part's
 * relationship file. Each relationship defines a connection from a source part
 * (implicitly the part containing the relationship file) to a target part or
 * external resource.</p>
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3Relationships that = (Mf3Relationships) o;
        return java.util.Objects.equals(relationships, that.relationships);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(relationships);
    }

    @Override
    public String toString() {
        return "Mf3Relationships{" +
                "relationships=" + relationships +
                '}';
    }
}
