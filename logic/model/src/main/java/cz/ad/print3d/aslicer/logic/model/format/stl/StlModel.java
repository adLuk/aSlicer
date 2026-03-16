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
import cz.ad.print3d.aslicer.logic.model.basic.Unit;

import java.util.Collections;
import java.util.List;

/**
 * Represents the data from a binary STL file.
 *
 * @param header 80-byte header of the STL file
 * @param facets list of facets in the STL model
 * @param unit the measurement unit used for coordinate values in this model
 */
public record StlModel(byte[] header, List<StlFacet> facets, Unit unit) implements Model {
    /**
     * @return Number of facets in the STL model.
     */
    public int facetCount() {
        return facets != null ? facets.size() : 0;
    }

    /**
     * Returns the measurement unit used for coordinate values in this model.
     * 
     * @return the measurement unit, or null if not specified or applicable
     */
    @Override
    public Unit unit() {
        return unit;
    }

    @Override
    public List<MeshPart> parts() {
        return Collections.singletonList(new MeshPart() {
            @Override
            public String name() {
                return "stl_part";
            }

            @Override
            public Integer color() {
                return null;
            }

            @Override
            public List<? extends Triangle> triangles() {
                return facets;
            }
        });
    }
}
