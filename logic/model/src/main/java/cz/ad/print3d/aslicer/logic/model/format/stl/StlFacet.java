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
package cz.ad.print3d.aslicer.logic.model.format.stl;

import cz.ad.print3d.aslicer.logic.model.Model;
import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;

/**
 * Represents a single facet in an STL file.
 * Each facet has a normal vector, three vertices, and an attribute byte count.
 *
 * @param normal the normal vector of the facet
 * @param v1 the first vertex of the triangular facet
 * @param v2 the second vertex of the triangular facet
 * @param v3 the third vertex of the triangular facet
 * @param attributeByteCount the number of attribute bytes for the facet
 */
public record StlFacet(Vector3f normal, Vector3f v1, Vector3f v2, Vector3f v3, int attributeByteCount) implements Model.Triangle {
}
