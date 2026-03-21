package cz.ad.print3d.aslicer.ui.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for mouse picking functionality in DesktopApp.
 */
public class DesktopAppMousePickingTest {

    @Test
    void testMousePickingAndSelection() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] error = new Throwable[1];

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Gdx.input = mock(Input.class);
                    Gdx.graphics = mock(Graphics.class);
                    when(Gdx.graphics.getWidth()).thenReturn(800);
                    when(Gdx.graphics.getHeight()).thenReturn(600);

                    DesktopApp app = new DesktopApp();
                    app.create();
                    
                    // Clear any models loaded during create() from other tests' leftovers
                    app.modelManager.getInstances().clear();
                    java.lang.reflect.Field modelsField = app.modelManager.getClass().getDeclaredField("models");
                    modelsField.setAccessible(true);
                    ((Array<Model>)modelsField.get(app.modelManager)).clear();
                    app.modelManager.getLoadedModelPaths().clear();
                    app.modelManager.clearSelection();

                    // Add a model at (0,0,0)
                    ModelBuilder modelBuilder = new ModelBuilder();
                    Model model = modelBuilder.createBox(2f, 2f, 2f, 
                            new Material(ColorAttribute.createDiffuse(Color.GRAY)),
                            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                    ModelInstance instance = new ModelInstance(model);
                    instance.transform.setToTranslation(0, 0, 0);
                    app.modelManager.getInstances().add(instance);
                    ((Array<Model>)modelsField.get(app.modelManager)).add(model);

                    // Add a second model at (10,0,0)
                    Model model2 = modelBuilder.createBox(2f, 2f, 2f, 
                            new Material(ColorAttribute.createDiffuse(Color.BLUE)),
                            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                    ModelInstance instance2 = new ModelInstance(model2);
                    instance2.transform.setToTranslation(10, 0, 0);
                    app.modelManager.getInstances().add(instance2);
                    ((Array<Model>)modelsField.get(app.modelManager)).add(model2);

                    // 1. Click on first model
                    PerspectiveCamera realCam = app.sceneManager.getCamera();
                    realCam.viewportWidth = 800;
                    realCam.viewportHeight = 600;
                    realCam.position.set(0, 0, 10);
                    realCam.direction.set(0, 0, -1);
                    realCam.up.set(0, 1, 0);
                    realCam.update();

                    app.selectionProcessor.touchDown(400, 300, 0, Input.Buttons.LEFT);
                    
                    Array<Integer> selectedIndices = app.modelManager.getSelectedIndices();
                    assertEquals(1, selectedIndices.size);
                    assertEquals(0, (int)selectedIndices.get(0));
                    assertEquals(Color.ORANGE, ((ColorAttribute)instance.materials.get(0).get(ColorAttribute.Diffuse)).color);

                    // 2. Click on second model without CTRL
                    realCam.position.set(10, 0, 10);
                    realCam.update();
                    app.selectionProcessor.touchDown(400, 300, 0, Input.Buttons.LEFT);
                    
                    selectedIndices = app.modelManager.getSelectedIndices();
                    assertEquals(1, selectedIndices.size);
                    assertEquals(1, (int)selectedIndices.get(0));
                    assertEquals(Color.GRAY, ((ColorAttribute)instance.materials.get(0).get(ColorAttribute.Diffuse)).color);
                    assertEquals(Color.ORANGE, ((ColorAttribute)instance2.materials.get(0).get(ColorAttribute.Diffuse)).color);

                    // 3. Click on first model with CTRL
                    realCam.position.set(0, 0, 10);
                    realCam.update();
                    when(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)).thenReturn(true);
                    app.selectionProcessor.touchDown(400, 300, 0, Input.Buttons.LEFT);
                    
                    selectedIndices = app.modelManager.getSelectedIndices();
                    assertEquals(2, selectedIndices.size);
                    assertTrue(selectedIndices.contains(0, true));
                    assertTrue(selectedIndices.contains(1, true));
                    assertEquals(Color.ORANGE, ((ColorAttribute)instance.materials.get(0).get(ColorAttribute.Diffuse)).color);
                    assertEquals(Color.ORANGE, ((ColorAttribute)instance2.materials.get(0).get(ColorAttribute.Diffuse)).color);

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
