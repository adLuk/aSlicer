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
package cz.ad.print3d.aslicer.logic.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;

import java.util.List;

public class ModelGdxConverter {

    public static Model convertToGdxModel(cz.ad.print3d.aslicer.logic.model.Model model) {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        for (cz.ad.print3d.aslicer.logic.model.Model.MeshPart part : model.parts()) {
            Color color = Color.GRAY;
            if (part.color() != null) {
                int c = part.color();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                color = new Color(r, g, b, 1.0f);
            }

            MeshPartBuilder meshPartBuilder = modelBuilder.part(part.name(),
                    GL20.GL_TRIANGLES,
                    VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
                    new Material(ColorAttribute.createDiffuse(color)));

            VertexInfo vi1 = new VertexInfo();
            VertexInfo vi2 = new VertexInfo();
            VertexInfo vi3 = new VertexInfo();

            for (cz.ad.print3d.aslicer.logic.model.Model.Triangle tri : part.triangles()) {
                Vector3f v1 = tri.v1();
                Vector3f v2 = tri.v2();
                Vector3f v3 = tri.v3();
                Vector3f n = tri.normal();

                if (n == null) {
                    // Calculate normal (assuming counter-clockwise order)
                    float ax = v2.x() - v1.x();
                    float ay = v2.y() - v1.y();
                    float az = v2.z() - v1.z();
                    float bx = v3.x() - v1.x();
                    float by = v3.y() - v1.y();
                    float bz = v3.z() - v1.z();

                    float nx = ay * bz - az * by;
                    float ny = az * bx - ax * bz;
                    float nz = ax * by - ay * bx;

                    float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if (len > 0) {
                        nx /= len;
                        ny /= len;
                        nz /= len;
                    }
                    vi1.setPos(v1.x(), v1.y(), v1.z()).setNor(nx, ny, nz);
                    vi2.setPos(v2.x(), v2.y(), v2.z()).setNor(nx, ny, nz);
                    vi3.setPos(v3.x(), v3.y(), v3.z()).setNor(nx, ny, nz);
                } else {
                    vi1.setPos(v1.x(), v1.y(), v1.z()).setNor(n.x(), n.y(), n.z());
                    vi2.setPos(v2.x(), v2.y(), v2.z()).setNor(n.x(), n.y(), n.z());
                    vi3.setPos(v3.x(), v3.y(), v3.z()).setNor(n.x(), n.y(), n.z());
                }

                meshPartBuilder.triangle(vi1, vi2, vi3);
            }
        }

        return modelBuilder.end();
    }
}
