package cz.ad.print3d.aslicer.ui.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.g3d.Model;
import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import cz.ad.print3d.aslicer.logic.model.format.stl.StlFacet;
import cz.ad.print3d.aslicer.logic.model.format.stl.StlModel;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StlGdxConverterTest {
    @Test
    void testConversion() throws InterruptedException {
        // We need a GDX application to initialize some static members, 
        // though HeadlessApplication doesn't provide a full GL context.
        // For Mesh creation, it might still fail if it tries to call GL functions.
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        AtomicReference<Model> modelRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                Vector3f normal = new Vector3f(0, 0, 1);
                Vector3f v1 = new Vector3f(0, 0, 0);
                Vector3f v2 = new Vector3f(1, 0, 0);
                Vector3f v3 = new Vector3f(0, 1, 0);
                StlFacet facet = new StlFacet(normal, v1, v2, v3, 0);
                StlModel stlModel = new StlModel(new byte[80], Collections.singletonList(facet), Unit.MILLIMETER);

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

                    modelRef.set(StlGdxConverter.convertToGdxModel(stlModel));
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
}
