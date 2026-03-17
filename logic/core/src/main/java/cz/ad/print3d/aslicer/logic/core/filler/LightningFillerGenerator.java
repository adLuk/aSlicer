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
package cz.ad.print3d.aslicer.logic.core.filler;

import clipper2.Clipper;
import clipper2.core.FillRule;
import clipper2.core.Paths64;
import clipper2.offset.EndType;
import clipper2.offset.JoinType;

import java.util.List;
import java.util.TreeMap;
import java.util.Map;

/**
 * LightningFillerGenerator implements the Lightning Infill algorithm.
 * This algorithm generates a sparse tree-like structure that grows inwards and downwards
 * from the top surfaces (ceilings) of the model. It is designed to provide just enough
 * support for internal roofs while minimizing material usage in the rest of the model volume.
 *
 * The implementation follows a top-down propagation approach:
 * 1. For each layer, it identifies "ceiling" areas (regions that have air above them).
 * 2. These areas are propagated to the layers below.
 * 3. During propagation, the areas are "thinned" (shrunk inwards) by a thinning factor.
 * 4. This results in a branching structure that originates from the model's top surfaces.
 * 5. Finally, each layer's support area is filled with a standard infill pattern (default is rectilinear).
 *
 * Note: This generator is stateful and requires the full set of model layers to be provided
 * via {@link #setLayers(List, float, float)} before generation.
 */
public class LightningFillerGenerator implements FillerGenerator {
    /**
     * Scaling factor to convert between float units and Clipper2's long units.
     */
    private static final double SCALE = 1000.0;

    /**
     * Precalculated support areas for each layer altitude.
     */
    private final TreeMap<Float, Paths64> supportAreas = new TreeMap<>();

    /**
     * Thinning factor determining how fast the support tree shrinks as it goes down.
     * A value of 1.0 roughly corresponds to a 45-degree angle.
     */
    private float thinningFactor = 1.0f;

    /**
     * Internal filler used to generate the actual toolpaths within the calculated support areas.
     */
    private final BasicFillerGenerator internalFiller = new BasicFillerGenerator();

    /**
     * Constructs a new LightningFillerGenerator with default rectilinear internal pattern.
     */
    public LightningFillerGenerator() {
        internalFiller.setPattern(BasicFillerGenerator.InfillPattern.RECTILINEAR);
    }

    /**
     * Sets the thinning factor.
     *
     * @param factor The thinning factor (mm/mm). Default is 1.0.
     */
    public void setThinningFactor(float factor) {
        this.thinningFactor = factor;
    }

    /**
     * Provides the layers of the model and precalculates the support structure.
     * This method must be called before {@link #generateInfill}.
     *
     * @param layers        List of layer boundaries (bottom-to-top).
     * @param firstAltitude The altitude of the first layer in the list.
     * @param layerHeight   The height increment between layers.
     */
    public void setLayers(List<Paths64> layers, float firstAltitude, float layerHeight) {
        supportAreas.clear();
        if (layers == null || layers.isEmpty()) {
            return;
        }

        int numLayers = layers.size();
        Paths64 currentSupport = new Paths64();

        // Process layers from top to bottom
        for (int i = numLayers - 1; i >= 0; i--) {
            float altitude = firstAltitude + i * layerHeight;
            Paths64 layer = layers.get(i);
            Paths64 layerAbove = (i < numLayers - 1) ? layers.get(i + 1) : new Paths64();

            // Identify internal ceiling at this layer
            Paths64 ceiling = Clipper.Difference(layer, layerAbove, FillRule.EvenOdd);

            // Thinning: shrink the support from above as it moves down
            if (!currentSupport.isEmpty()) {
                double delta = -thinningFactor * layerHeight * SCALE;
                currentSupport = Clipper.InflatePaths(currentSupport, delta, JoinType.Miter, EndType.Polygon);
            }

            // Union current ceiling with support requirements from above
            currentSupport = Clipper.Union(ceiling, currentSupport, FillRule.EvenOdd);

            // Ensure the support area stays within the current layer's physical boundary
            currentSupport = Clipper.Intersect(currentSupport, layer, FillRule.EvenOdd);

            if (!currentSupport.isEmpty()) {
                supportAreas.put(altitude, new Paths64(currentSupport));
            }
        }
    }

    /**
     * Sets the internal infill pattern type.
     *
     * @param pattern The pattern to use within the support tree.
     */
    public void setInternalPattern(BasicFillerGenerator.InfillPattern pattern) {
        internalFiller.setPattern(pattern);
    }

    @Override
    public Paths64 generateInfill(List<Paths64> walls, Paths64 originalLayer, float nozzleSize, float spacing, float angle, float altitude) {
        // Find the precalculated support area for this altitude.
        // We look for the exact altitude or the closest one below it.
        Map.Entry<Float, Paths64> entry = supportAreas.floorEntry(altitude + 0.001f);
        if (entry == null) {
            return new Paths64();
        }

        Paths64 boundary = entry.getValue();
        if (boundary.isEmpty()) {
            return new Paths64();
        }

        // Generate the infill toolpaths inside the calculated support boundary.
        // We don't pass walls here because the boundary is already computed to be 
        // within the model and intended specifically for infill.
        return internalFiller.generateInfill(null, boundary, nozzleSize, spacing, angle, altitude);
    }
}
