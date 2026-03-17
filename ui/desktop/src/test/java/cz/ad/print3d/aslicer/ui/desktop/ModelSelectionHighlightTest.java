package cz.ad.print3d.aslicer.ui.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Array;
import cz.ad.print3d.aslicer.ui.desktop.view.ModelListWindow;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

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
                    // Mock GL
                    Gdx.gl = mock(GL20.class);
                    Gdx.gl20 = Gdx.gl;

                    // Create a DesktopApp instance
                    // We need to bypass some initialization that might fail in headless mode without full setup
                    DesktopApp app = new DesktopApp();
                    
                    // Manually setup minimum requirements for the test
                    ModelBuilder modelBuilder = new ModelBuilder();
                    Model model = modelBuilder.createBox(1f, 1f, 1f, 
                            new Material(ColorAttribute.createDiffuse(Color.GRAY)),
                            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                    
                    app.models.add(model);
                    ModelInstance instance = new ModelInstance(model);
                    app.instances.add(instance);
                    
                    // Access selectedIndices via reflection if private
                    Field selectedIndicesField = DesktopApp.class.getDeclaredField("selectedIndices");
                    selectedIndicesField.setAccessible(true);
                    Array<Integer> selectedIndices = (Array<Integer>) selectedIndicesField.get(app);
                    
                    Method updateHighlightsMethod = DesktopApp.class.getDeclaredMethod("updateHighlights");
                    updateHighlightsMethod.setAccessible(true);

                    // 1. Initially NOT selected, color should be GRAY
                    updateHighlightsMethod.invoke(app);
                    ColorAttribute attr = (ColorAttribute) instance.materials.get(0).get(ColorAttribute.Diffuse);
                    assertEquals(Color.GRAY, attr.color, "Initially color should be GRAY");

                    // 2. Select the model, color should become ORANGE
                    selectedIndices.add(0);
                    updateHighlightsMethod.invoke(app);
                    attr = (ColorAttribute) instance.materials.get(0).get(ColorAttribute.Diffuse);
                    assertEquals(Color.ORANGE, attr.color, "Selected color should be ORANGE");

                    // 3. Unselect the model, color should return to GRAY
                    selectedIndices.clear();
                    updateHighlightsMethod.invoke(app);
                    attr = (ColorAttribute) instance.materials.get(0).get(ColorAttribute.Diffuse);
                    assertEquals(Color.GRAY, attr.color, "Unselected color should return to GRAY");

                    // 4. Test multiple models
                    Model model2 = modelBuilder.createBox(1f, 1f, 1f, 
                            new Material(ColorAttribute.createDiffuse(Color.BLUE)),
                            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                    app.models.add(model2);
                    ModelInstance instance2 = new ModelInstance(model2);
                    app.instances.add(instance2);

                    selectedIndices.add(1);
                    updateHighlightsMethod.invoke(app);
                    
                    attr = (ColorAttribute) instance.materials.get(0).get(ColorAttribute.Diffuse);
                    assertEquals(Color.GRAY, attr.color, "First model should be GRAY");
                    
                    attr = (ColorAttribute) instance2.materials.get(0).get(ColorAttribute.Diffuse);
                    assertEquals(Color.ORANGE, attr.color, "Second model should be ORANGE");

                    model.dispose();
                    model2.dispose();
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
