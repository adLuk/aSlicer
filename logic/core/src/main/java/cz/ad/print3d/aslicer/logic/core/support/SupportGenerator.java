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
package cz.ad.print3d.aslicer.logic.core.support;

import com.badlogic.gdx.graphics.g3d.Model;
import clipper2.core.Paths64;
import java.util.List;

/**
 * Interface for support structure generation.
 * Implementations provide various algorithms to identify and generate areas
 * that require support during the 3D printing process.
 */
public interface SupportGenerator {

    /**
     * Sets the overhang threshold angle (in degrees).
     * @param degrees Angle in degrees.
     */
    void setOverhangThreshold(float degrees);

    /**
     * Sets the gap between the support structure and the model.
     * @param gap Gap distance (in mm).
     */
    void setSupportGap(float gap);

    /**
     * Sets the maximum branch angle for tree-like supports.
     * @param degrees Angle in degrees.
     */
    void setMaxBranchAngle(float degrees);

    /**
     * Sets the diameter of the support branches.
     * @param diameter Diameter in mm.
     */
    void setBranchDiameter(float diameter);

    /**
     * Sets the collision detail level.
     * @param detail Detail level.
     */
    void setCollisionDetail(float detail);

    /**
     * Sets the boundaries of the build plate to avoid generating support outside.
     * @param boundaries Build plate boundaries.
     */
    void setBuildPlateBoundaries(Paths64 boundaries);

    /**
     * Identifies overhang triangles.
     * @param triangles List of triangles to analyze.
     */
    void detectOverhangs(List<BfsSupportGenerator.Triangle> triangles);

    /**
     * Generates support areas for each layer.
     * @param model Model to support.
     * @param layerHeight Height of each layer.
     * @param modelLayers Sliced boundaries of the model itself.
     * @return List of Paths64 for each layer.
     */
    List<Paths64> generateSupport(Model model, float layerHeight, List<Paths64> modelLayers);
}
