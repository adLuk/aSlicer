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
import com.badlogic.gdx.math.Vector3;
import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import cz.ad.print3d.aslicer.logic.model.format.stl.StlFacet;
import cz.ad.print3d.aslicer.logic.model.format.stl.StlModel;

public class StlGdxConverter {
 
    public static Model convertToGdxModel(StlModel stlModel) {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        MeshPartBuilder meshPartBuilder = modelBuilder.part("stl", GL20.GL_TRIANGLES, 
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, 
                new Material(ColorAttribute.createDiffuse(Color.GRAY)));
 
        VertexInfo vi1 = new VertexInfo();
        VertexInfo vi2 = new VertexInfo();
        VertexInfo vi3 = new VertexInfo();
 
        for (StlFacet facet : stlModel.facets()) {
            Vector3f n = facet.normal();
            Vector3f v1 = facet.v1();
            Vector3f v2 = facet.v2();
            Vector3f v3 = facet.v3();
 
            vi1.setPos(v1.x(), v1.y(), v1.z()).setNor(n.x(), n.y(), n.z());
            vi2.setPos(v2.x(), v2.y(), v2.z()).setNor(n.x(), n.y(), n.z());
            vi3.setPos(v3.x(), v3.y(), v3.z()).setNor(n.x(), n.y(), n.z());
 
            meshPartBuilder.triangle(vi1, vi2, vi3);
        }
        return modelBuilder.end();
    }
}
