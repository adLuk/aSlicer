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
package cz.ad.print3d.aslicer.logic.model;

import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import java.util.List;

/**
 * Base interface for all 3D models.
 * This interface represents a common DTO for different 3D file formats.
 */
public interface Model {
    /**
     * Returns the measurement unit used for coordinate values in this model.
     * 
     * @return the measurement unit, or null if not specified or applicable
     */
    Unit unit();

    /**
     * Returns a list of mesh parts that make up this model.
     * 
     * @return the list of mesh parts
     */
    List<? extends MeshPart> parts();

    /**
     * Represents a single part of a mesh with common properties like name and color.
     */
    interface MeshPart {
        /**
         * Returns the name of the mesh part.
         * 
         * @return the name
         */
        String name();

        /**
         * Returns the sRGB color of the mesh part.
         * 
         * @return the sRGB color (0xRRGGBB) or null if not specified
         */
        Integer color();

        /**
         * Returns a list of triangles in this mesh part.
         * 
         * @return the list of triangles
         */
        List<? extends Triangle> triangles();
    }

    /**
     * Represents a single triangle in a mesh.
     */
    interface Triangle {
        /**
         * Returns the first vertex.
         * 
         * @return the first vertex
         */
        Vector3f v1();

        /**
         * Returns the second vertex.
         * 
         * @return the second vertex
         */
        Vector3f v2();

        /**
         * Returns the third vertex.
         * 
         * @return the third vertex
         */
        Vector3f v3();

        /**
         * Returns the normal vector of the triangle.
         * 
         * @return the normal vector, or null if it should be calculated from vertices
         */
        Vector3f normal();
    }
}
