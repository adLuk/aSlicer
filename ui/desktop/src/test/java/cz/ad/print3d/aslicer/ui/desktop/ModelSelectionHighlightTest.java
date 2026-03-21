package cz.ad.print3d.aslicer.ui.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the model selection highlighting functionality in DesktopApp.
 */
public class ModelSelectionHighlightTest {

    @Test
    void testHighlightingLogic() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] error = new Throwable[1];

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    DesktopApp app = new DesktopApp();
                    app.create();
                    
                    ModelBuilder modelBuilder = new ModelBuilder();
                    Model model = modelBuilder.createBox(1f, 1f, 1f, 
                            new Material(ColorAttribute.createDiffuse(Color.GRAY)),
                            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                    
                    app.modelManager.getInstances().add(new ModelInstance(model));
                    java.lang.reflect.Field modelsField = app.modelManager.getClass().getDeclaredField("models");
                    modelsField.setAccessible(true);
                    ((Array<Model>)modelsField.get(app.modelManager)).add(model);
                    
                    ModelInstance instance = app.modelManager.getInstances().get(0);

                    // 1. Initially NOT selected, color should be GRAY
                    app.modelManager.updateHighlights();
                    ColorAttribute attr = (ColorAttribute) instance.materials.get(0).get(ColorAttribute.Diffuse);
                    assertEquals(Color.GRAY, attr.color, "Initially color should be GRAY");

                    // 2. Select the model, color should become ORANGE
                    app.modelManager.selectModel(0);
                    attr = (ColorAttribute) instance.materials.get(0).get(ColorAttribute.Diffuse);
                    assertEquals(Color.ORANGE, attr.color, "Selected color should be ORANGE");

                    // 3. Unselect the model, color should return to GRAY
                    app.modelManager.clearSelection();
                    attr = (ColorAttribute) instance.materials.get(0).get(ColorAttribute.Diffuse);
                    assertEquals(Color.GRAY, attr.color, "Unselected color should return to GRAY");

                    model.dispose();
                    latch.countDown();
                } catch (Throwable t) {
                    error[0] = t;
                    latch.countDown();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        if (error[0] != null) {
            throw new RuntimeException(error[0]);
        }
    }
}
