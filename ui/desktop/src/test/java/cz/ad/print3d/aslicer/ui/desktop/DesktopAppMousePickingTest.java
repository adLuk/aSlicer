package cz.ad.print3d.aslicer.ui.desktop;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for mouse picking functionality in DesktopApp.
 */
public class DesktopAppMousePickingTest {

    static class TestCamera extends PerspectiveCamera {
        public Ray mockRay;
        public TestCamera() {
            super(67, 100, 100);
            position.set(10, 10, 10);
            near = 0.1f;
            far = 1000f;
            update();
        }
        @Override
        public Ray getPickRay(float screenX, float screenY) {
            return mockRay;
        }
    }

    @Test
    void testMousePickingAndSelection() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] error = new Throwable[1];

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    // Mock GDX environment
                    Gdx.gl = mock(GL20.class);
                    Gdx.gl20 = Gdx.gl;
                    Gdx.input = mock(Input.class);

                    DesktopApp app = new DesktopApp();
                    
                    // Setup camera
                    TestCamera testCam = new TestCamera();
                    app.cam = testCam;
                    
                    // Add a model at (0,0,0)
                    ModelBuilder modelBuilder = new ModelBuilder();
                    Model model = modelBuilder.createBox(2f, 2f, 2f, 
                            new Material(ColorAttribute.createDiffuse(Color.GRAY)),
                            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                    app.models.add(model);
                    ModelInstance instance = new ModelInstance(model);
                    instance.transform.setToTranslation(0, 0, 0);
                    app.instances.add(instance);

                    // Add a second model at (10,0,0)
                    Model model2 = modelBuilder.createBox(2f, 2f, 2f, 
                            new Material(ColorAttribute.createDiffuse(Color.BLUE)),
                            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                    app.models.add(model2);
                    ModelInstance instance2 = new ModelInstance(model2);
                    instance2.transform.setToTranslation(10, 0, 0);
                    app.instances.add(instance2);

                    // Access selectedIndices via reflection
                    Field selectedIndicesField = DesktopApp.class.getDeclaredField("selectedIndices");
                    selectedIndicesField.setAccessible(true);
                    Array<Integer> selectedIndices = (Array<Integer>) selectedIndicesField.get(app);

                    // Initialize the selectionProcessor logic (copied from DesktopApp)
                    app.selectionProcessor = new InputAdapter() {
                        @Override
                        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                            if (button == Input.Buttons.LEFT) {
                                int index = app.getObject(screenX, screenY);
                                if (index >= 0) {
                                    if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
                                        if (selectedIndices.contains(index, true)) {
                                            selectedIndices.removeValue(index, true);
                                        } else {
                                            selectedIndices.add(index);
                                        }
                                    } else {
                                        selectedIndices.clear();
                                        selectedIndices.add(index);
                                    }
                                } else {
                                    if (!Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && !Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
                                        selectedIndices.clear();
                                    }
                                }
                                try {
                                    Method updateHighlights = DesktopApp.class.getDeclaredMethod("updateHighlights");
                                    updateHighlights.setAccessible(true);
                                    updateHighlights.invoke(app);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            return false;
                        }
                    };

                    // 1. Click on first model
                    testCam.mockRay = new Ray(new Vector3(0, 0, 10), new Vector3(0, 0, -1));
                    app.selectionProcessor.touchDown(100, 100, 0, Input.Buttons.LEFT);
                    
                    assertEquals(1, selectedIndices.size);
                    assertEquals(0, (int)selectedIndices.get(0));
                    assertEquals(Color.ORANGE, ((ColorAttribute)instance.materials.get(0).get(ColorAttribute.Diffuse)).color);

                    // 2. Click on second model without CTRL
                    testCam.mockRay = new Ray(new Vector3(10, 0, 10), new Vector3(0, 0, -1));
                    app.selectionProcessor.touchDown(200, 200, 0, Input.Buttons.LEFT);
                    
                    assertEquals(1, selectedIndices.size);
                    assertEquals(1, (int)selectedIndices.get(0));
                    assertEquals(Color.GRAY, ((ColorAttribute)instance.materials.get(0).get(ColorAttribute.Diffuse)).color);
                    assertEquals(Color.ORANGE, ((ColorAttribute)instance2.materials.get(0).get(ColorAttribute.Diffuse)).color);

                    // 3. Click on first model with CTRL
                    testCam.mockRay = new Ray(new Vector3(0, 0, 10), new Vector3(0, 0, -1));
                    when(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)).thenReturn(true);
                    app.selectionProcessor.touchDown(100, 100, 0, Input.Buttons.LEFT);
                    
                    assertEquals(2, selectedIndices.size);
                    assertTrue(selectedIndices.contains(0, true));
                    assertTrue(selectedIndices.contains(1, true));
                    assertEquals(Color.ORANGE, ((ColorAttribute)instance.materials.get(0).get(ColorAttribute.Diffuse)).color);
                    assertEquals(Color.ORANGE, ((ColorAttribute)instance2.materials.get(0).get(ColorAttribute.Diffuse)).color);

                    // 4. Click on second model with CTRL (remove)
                    testCam.mockRay = new Ray(new Vector3(10, 0, 10), new Vector3(0, 0, -1));
                    app.selectionProcessor.touchDown(200, 200, 0, Input.Buttons.LEFT);
                    
                    assertEquals(1, selectedIndices.size);
                    assertEquals(0, (int)selectedIndices.get(0));
                    assertEquals(Color.ORANGE, ((ColorAttribute)instance.materials.get(0).get(ColorAttribute.Diffuse)).color);
                    assertEquals(Color.BLUE, ((ColorAttribute)instance2.materials.get(0).get(ColorAttribute.Diffuse)).color);

                    // 5. Click on empty space without CTRL
                    testCam.mockRay = new Ray(new Vector3(5, 5, 10), new Vector3(0, 0, -1));
                    when(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)).thenReturn(false);
                    app.selectionProcessor.touchDown(300, 300, 0, Input.Buttons.LEFT);
                    
                    assertEquals(0, selectedIndices.size);
                    assertEquals(Color.GRAY, ((ColorAttribute)instance.materials.get(0).get(ColorAttribute.Diffuse)).color);

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
