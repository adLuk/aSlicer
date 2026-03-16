package cz.ad.print3d.aslicer.ui.desktop;

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
import cz.ad.print3d.aslicer.logic.model.format.mf3.core.Mf3Model;
import cz.ad.print3d.aslicer.logic.model.format.mf3.geometry.Mf3Triangle;
import cz.ad.print3d.aslicer.logic.model.format.mf3.resource.Mf3BaseMaterials;
import cz.ad.print3d.aslicer.logic.model.format.mf3.resource.Mf3Object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mf3GdxConverter {

    public static Model convertToGdxModel(Mf3Model mf3Model) {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        Map<Integer, Mf3BaseMaterials> materialsMap = new HashMap<>();
        if (mf3Model.getResources() != null && mf3Model.getResources().getBaseMaterials() != null) {
            for (Mf3BaseMaterials bm : mf3Model.getResources().getBaseMaterials()) {
                materialsMap.put(bm.getId(), bm);
            }
        }

        for (Mf3Object obj : mf3Model.objects()) {
            List<Vector3f> vertices = obj.vertices();
            List<Mf3Triangle> triangles = obj.triangles();

            if (triangles == null || triangles.isEmpty()) {
                continue;
            }

            Map<String, List<Mf3Triangle>> groupedTriangles = new HashMap<>();
            for (Mf3Triangle tri : triangles) {
                Integer pid = tri.getPid() != null ? tri.getPid() : obj.getPid();
                Integer pindex = tri.getPindex() != null ? tri.getPindex() : obj.getPindex();
                String key = (pid != null && pindex != null) ? (pid + ":" + pindex) : "default";

                groupedTriangles.computeIfAbsent(key, k -> new ArrayList<>()).add(tri);
            }

            for (Map.Entry<String, List<Mf3Triangle>> entry : groupedTriangles.entrySet()) {
                String materialKey = entry.getKey();
                List<Mf3Triangle> tris = entry.getValue();

                Color color = Color.GRAY;
                if (!"default".equals(materialKey)) {
                    String[] parts = materialKey.split(":");
                    int pid = Integer.parseInt(parts[0]);
                    int pindex = Integer.parseInt(parts[1]);
                    Mf3BaseMaterials bm = materialsMap.get(pid);
                    if (bm != null && pindex >= 0 && pindex < bm.getBases().size()) {
                        color = parseColor(bm.getBases().get(pindex).getDisplayColor());
                    }
                }

                MeshPartBuilder meshPartBuilder = modelBuilder.part("mf3_obj_" + obj.id() + "_" + materialKey,
                        GL20.GL_TRIANGLES,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
                        new Material(ColorAttribute.createDiffuse(color)));

                VertexInfo vi1 = new VertexInfo();
                VertexInfo vi2 = new VertexInfo();
                VertexInfo vi3 = new VertexInfo();

                for (Mf3Triangle tri : tris) {
                    Vector3f v1 = vertices.get(tri.v1());
                    Vector3f v2 = vertices.get(tri.v2());
                    Vector3f v3 = vertices.get(tri.v3());

                    // Calculate normal (counter-clockwise order in 3MF)
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

                    meshPartBuilder.triangle(vi1, vi2, vi3);
                }
            }
        }

        return modelBuilder.end();
    }

    private static Color parseColor(String colorStr) {
        if (colorStr == null || !colorStr.startsWith("#")) {
            return Color.GRAY;
        }
        try {
            String hex = colorStr.substring(1);
            if (hex.length() == 6) {
                hex += "FF";
            }
            return Color.valueOf(hex);
        } catch (Exception e) {
            return Color.GRAY;
        }
    }
}
