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
package cz.ad.print3d.aslicer.logic.model.format.mf3.prusa;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

/**
 * Represents a Prusa-specific metadata entry in a Prusa-formatted 3MF file.
 * These are typically found in Metadata/Slic3r_PE_model.config.
 * Known keys include:
 * <ul>
 *   <li>name: The name of the object or volume.</li>
 *   <li>volume_type: The type of volume (e.g., ModelPart, NegativeVolume, ParameterModifier).</li>
 *   <li>matrix: The transformation matrix for the volume.</li>
 *   <li>source_file: The original source file name.</li>
 *   <li>source_object_id: The ID of the object in the source file.</li>
 *   <li>source_volume_id: The ID of the volume in the source file.</li>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Mf3PrusaConfigMetadata {

    /**
     * The type of the metadata (e.g., 'object', 'volume').
     */
    @XmlAttribute(name = "type")
    private String type;

    /**
     * The key for this metadata entry (e.g., 'name', 'volume_type', 'matrix').
     */
    @XmlAttribute(name = "key")
    private String key;

    /**
     * The value for this metadata entry.
     */
    @XmlAttribute(name = "value")
    private String value;

    /**
     * Default constructor for JAXB.
     */
    public Mf3PrusaConfigMetadata() {
    }

    /**
     * Constructs a metadata entry with the specified type, key, and value.
     *
     * @param type  the type
     * @param key   the key
     * @param value the value
     */
    public Mf3PrusaConfigMetadata(final String type, final String key, final String value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    /**
     * Returns the type of the metadata.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the key of the metadata.
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the value of the metadata.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Compares this metadata entry with another for equality.
     *
     * @param o the object to compare with
     * @return true if both have identical values
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3PrusaConfigMetadata that = (Mf3PrusaConfigMetadata) o;
        return java.util.Objects.equals(type, that.type) &&
                java.util.Objects.equals(key, that.key) &&
                java.util.Objects.equals(value, that.value);
    }

    /**
     * Returns the hash code for this metadata entry.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, key, value);
    }

    /**
     * Returns a string representation of this metadata entry.
     *
     * @return a string containing metadata values
     */
    @Override
    public String toString() {
        return "Mf3PrusaConfigMetadata{" +
                "type='" + type + '\'' +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
