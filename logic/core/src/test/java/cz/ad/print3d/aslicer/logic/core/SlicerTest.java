package cz.ad.print3d.aslicer.logic.core;

import clipper2.core.Paths64;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class SlicerTest {

    @Test
    void testSliceCube() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        AtomicReference<List<Paths64>> layersRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    mockGdxGL();
                    ModelBuilder modelBuilder = new ModelBuilder();
                    modelBuilder.begin();
                    MeshPartBuilder partBuilder = modelBuilder.part("cube", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position, new Material(ColorAttribute.createDiffuse(Color.GRAY)));
                    BoxShapeBuilder.build(partBuilder, 1f, 1f, 1f);
                    Model model = modelBuilder.end();

                    Slicer slicer = new Slicer();
                    layersRef.set(slicer.slice(model, 0.1f));
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    latch.countDown();
                    com.badlogic.gdx.Gdx.app.exit();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        List<Paths64> layers = layersRef.get();
        assertNotNull(layers);
        assertFalse(layers.isEmpty());
        
        // A 1x1x1 cube from -0.5 to 0.5. 
        // With layerHeight 0.1, we expect about 10 layers.
        assertEquals(10, layers.size());
        
        for (Paths64 layer : layers) {
            assertFalse(layer.isEmpty());
            // Each layer of a cube should be a square (1 path)
            assertEquals(1, layer.size());
            // Square area should be roughly 1000*1000 = 1,000,000 (scaled by 1000)
            // Clipper2 area for 1x1 square scaled by 1000 is 1000 * 1000 = 1,000,000
            double area = clipper2.Clipper.Area(layer);
            assertTrue(Math.abs(area) > 900000);
        }
    }

    @Test
    void testEmptyModel() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        AtomicReference<List<Paths64>> layersRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    mockGdxGL();
                    ModelBuilder modelBuilder = new ModelBuilder();
                    modelBuilder.begin();
                    Model model = modelBuilder.end();

                    Slicer slicer = new Slicer();
                    layersRef.set(slicer.slice(model, 0.1f));
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    latch.countDown();
                    com.badlogic.gdx.Gdx.app.exit();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        List<Paths64> layers = layersRef.get();
        assertNotNull(layers);
        assertTrue(layers.isEmpty());
    }

    @Test
    void testSingleTriangle() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        AtomicReference<List<Paths64>> layersRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    mockGdxGL();
                    ModelBuilder modelBuilder = new ModelBuilder();
                    modelBuilder.begin();
                    modelBuilder.part("tri", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position, new Material())
                            .triangle(new com.badlogic.gdx.math.Vector3(0, 0, 0),
                                      new com.badlogic.gdx.math.Vector3(1, 0, 0),
                                      new com.badlogic.gdx.math.Vector3(0, 1, 0));
                    Model model = modelBuilder.end();

                    Slicer slicer = new Slicer();
                    // This triangle is from Y=0 to Y=1 (it's in XY plane, wait, no, (0,0,0), (1,0,0), (0,1,0) is in XY plane)
                    // Wait, GDX usually uses Y-up.
                    // Vertices: (0,0,0), (1,0,0), (0,1,0)
                    // Y ranges from 0 to 1.
                    // At layerHeight 0.1, we expect 10 layers.
                    // But each layer is just a line segment. 
                    // Clipper2 SimplifyPaths(Paths64, 1.0) might remove it if it's not a closed loop.
                    layersRef.set(slicer.slice(model, 0.1f));
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    latch.countDown();
                    com.badlogic.gdx.Gdx.app.exit();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        List<Paths64> layers = layersRef.get();
        assertNotNull(layers);
        // Each layer intersection is a line segment, but Clipper2-java SimplifyPaths with default rule may skip it.
        // Actually, we expect 0 closed loops for a single triangle (unless it's in the plane, but it's not).
        for (Paths64 layer : layers) {
            assertTrue(layer.isEmpty());
        }
    }

    private void mockGdxGL() {
        com.badlogic.gdx.Gdx.gl20 = (com.badlogic.gdx.graphics.GL20) java.lang.reflect.Proxy.newProxyInstance(
                com.badlogic.gdx.graphics.GL20.class.getClassLoader(),
                new Class<?>[]{com.badlogic.gdx.graphics.GL20.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("glGenBuffer")) return 1;
                    if (method.getReturnType().equals(int.class)) return 0;
                    if (method.getReturnType().equals(boolean.class)) return true;
                    return null;
                }
        );
        com.badlogic.gdx.Gdx.gl = com.badlogic.gdx.Gdx.gl20;
    }
}
