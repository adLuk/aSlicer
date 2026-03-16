package cz.ad.print3d.aslicer.ui.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import cz.ad.print3d.aslicer.logic.model.format.mf3.core.Mf3Model;
import cz.ad.print3d.aslicer.logic.model.format.mf3.geometry.Mf3Mesh;
import cz.ad.print3d.aslicer.logic.model.format.mf3.geometry.Mf3Triangle;
import cz.ad.print3d.aslicer.logic.model.format.mf3.resource.Mf3Base;
import cz.ad.print3d.aslicer.logic.model.format.mf3.resource.Mf3BaseMaterials;
import cz.ad.print3d.aslicer.logic.model.format.mf3.resource.Mf3Object;
import cz.ad.print3d.aslicer.logic.model.format.mf3.resource.Mf3Resources;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Mf3GdxConverterTest {
    @Test
    void testConversion() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        AtomicReference<Model> modelRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                Mf3Mesh mesh = new Mf3Mesh();
                mesh.getVertices().add(new Vector3f(0, 0, 0));
                mesh.getVertices().add(new Vector3f(1, 0, 0));
                mesh.getVertices().add(new Vector3f(0, 1, 0));
                mesh.getTriangles().add(new Mf3Triangle(0, 1, 2));

                Mf3Object obj = new Mf3Object();
                obj.setId(1);
                obj.setMesh(mesh);

                Mf3Resources resources = new Mf3Resources();
                resources.getObjects().add(obj);

                Mf3Model mf3Model = new Mf3Model();
                mf3Model.setResources(resources);
                mf3Model.setUnit(Unit.MILLIMETER);

                try {
                    // Mock GL20 to avoid NPE during Mesh creation
                    com.badlogic.gdx.Gdx.gl20 = (com.badlogic.gdx.graphics.GL20) java.lang.reflect.Proxy.newProxyInstance(
                            com.badlogic.gdx.graphics.GL20.class.getClassLoader(),
                            new Class[]{com.badlogic.gdx.graphics.GL20.class},
                            (proxy, method, args) -> {
                                if (method.getName().equals("glGenBuffer")) return 1;
                                if (method.getReturnType().equals(int.class)) return 0;
                                if (method.getReturnType().equals(boolean.class)) return true;
                                return null;
                            }
                    );
                    com.badlogic.gdx.Gdx.gl = com.badlogic.gdx.Gdx.gl20;

                    modelRef.set(Mf3GdxConverter.convertToGdxModel(mf3Model));
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    latch.countDown();
                    com.badlogic.gdx.Gdx.app.exit();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Conversion should complete within 5 seconds");
        assertNotNull(modelRef.get(), "Model should not be null after conversion");
    }

    @Test
    void testMaterialConversion() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        AtomicReference<Model> modelRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                Mf3Mesh mesh = new Mf3Mesh();
                mesh.getVertices().add(new Vector3f(0, 0, 0));
                mesh.getVertices().add(new Vector3f(1, 0, 0));
                mesh.getVertices().add(new Vector3f(0, 1, 0));
                // Triangle with specific material
                Mf3Triangle tri1 = new Mf3Triangle(0, 1, 2);
                tri1.setPid(5);
                tri1.setPindex(0);
                mesh.getTriangles().add(tri1);

                Mf3Object obj = new Mf3Object();
                obj.setId(1);
                obj.setMesh(mesh);

                Mf3BaseMaterials bm = new Mf3BaseMaterials(5, Collections.singletonList(new Mf3Base("Red", "#FF0000")));
                Mf3Resources resources = new Mf3Resources();
                resources.getObjects().add(obj);
                resources.getBaseMaterials().add(bm);

                Mf3Model mf3Model = new Mf3Model();
                mf3Model.setResources(resources);

                try {
                    com.badlogic.gdx.Gdx.gl20 = (com.badlogic.gdx.graphics.GL20) java.lang.reflect.Proxy.newProxyInstance(
                            com.badlogic.gdx.graphics.GL20.class.getClassLoader(),
                            new Class[]{com.badlogic.gdx.graphics.GL20.class},
                            (proxy, method, args) -> {
                                if (method.getName().equals("glGenBuffer")) return 1;
                                if (method.getReturnType().equals(int.class)) return 0;
                                if (method.getReturnType().equals(boolean.class)) return true;
                                return null;
                            }
                    );
                    com.badlogic.gdx.Gdx.gl = com.badlogic.gdx.Gdx.gl20;

                    modelRef.set(Mf3GdxConverter.convertToGdxModel(mf3Model));
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    latch.countDown();
                    com.badlogic.gdx.Gdx.app.exit();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Conversion should complete within 5 seconds");
        Model model = modelRef.get();
        assertNotNull(model);
        assertTrue(model.nodes.size > 0);
        
        // Verify that material color is RED
        boolean foundRed = false;
        for (Node node : model.nodes) {
            for (NodePart part : node.parts) {
                ColorAttribute diff = (ColorAttribute) part.material.get(ColorAttribute.Diffuse);
                if (diff != null && diff.color.equals(Color.RED)) {
                    foundRed = true;
                }
            }
        }
        assertTrue(foundRed, "Model should contain at least one part with RED color");
    }
}
