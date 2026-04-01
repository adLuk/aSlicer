package cz.ad.print3d.aslicer.ui.desktop.view;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import cz.ad.print3d.aslicer.ui.desktop.GdxTestUtils;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModelSelectionSyncTest {

    @Test
    void testSelectionLossOnUpdate() throws InterruptedException {
        runInGdxThread(() -> {
            Skin skin = GdxTestUtils.createTestSkin();
            
            Array<String> paths = new Array<>();
            paths.add("model1.stl");
            
            Array<cz.ad.print3d.aslicer.logic.model.Model> logicModels = new Array<>();

            ModelListWindow window = new ModelListWindow(skin, paths, logicModels, new ModelListWindow.ModelListListener() {
                @Override public void onRemoveModel(int index) {}
                @Override public void onDuplicateModel(int index) {}
                @Override public void onSelectModels(Array<Integer> indices) {}
            });

            // Select first item
            Array<Integer> selected = new Array<>();
            selected.add(0);
            window.setSelectedIndices(selected);
            
            assertEquals(1, window.getInternalList().getSelection().size());

            // Add another model path and update list
            paths.add("model2.stl");
            window.updateList();

            // Check if selection is preserved
            assertEquals(1, window.getInternalList().getSelection().size());
        });
    }

    @Test
    void testShiftSelection() throws InterruptedException {
        runInGdxThread(() -> {
            Skin skin = GdxTestUtils.createTestSkin();
            Array<String> paths = new Array<>();
            paths.add("m1.stl");
            paths.add("m2.stl");
            paths.add("m3.stl");
            paths.add("m4.stl");
            Array<cz.ad.print3d.aslicer.logic.model.Model> logicModels = new Array<>();

            final Array<Integer> selectedIndices = new Array<>();
            ModelListWindow window = new ModelListWindow(skin, paths, logicModels, new ModelListWindow.ModelListListener() {
                @Override public void onRemoveModel(int index) {}
                @Override public void onDuplicateModel(int index) {}
                @Override public void onSelectModels(Array<Integer> indices) {
                    selectedIndices.clear();
                    selectedIndices.addAll(indices);
                }
            });

            // Mock click on first item (no modifiers)
            window.handleRowClick(0, window.getListItems().get(0));
            assertEquals(1, selectedIndices.size);
            assertEquals(0, (int)selectedIndices.get(0));

            // Mock click on third item with SHIFT
            // We need to mock Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
            com.badlogic.gdx.Input originalInput = com.badlogic.gdx.Gdx.input;
            try {
                com.badlogic.gdx.Gdx.input = (com.badlogic.gdx.Input) java.lang.reflect.Proxy.newProxyInstance(
                    com.badlogic.gdx.Input.class.getClassLoader(),
                    new Class<?>[]{com.badlogic.gdx.Input.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("isKeyPressed")) {
                            int key = (int) args[0];
                            return key == com.badlogic.gdx.Input.Keys.SHIFT_LEFT;
                        }
                        if (method.getReturnType().equals(boolean.class)) return false;
                        if (method.getReturnType().equals(int.class)) return 0;
                        if (method.getReturnType().equals(float.class)) return 0f;
                        return null;
                    }
                );
                
                window.handleRowClick(2, window.getListItems().get(2));
                
                // Should select 0, 1, 2
                assertEquals(3, selectedIndices.size);
                assertTrue(selectedIndices.contains(0, false));
                assertTrue(selectedIndices.contains(1, false));
                assertTrue(selectedIndices.contains(2, false));
                
            } finally {
                com.badlogic.gdx.Gdx.input = originalInput;
            }
        });
    }

    private void runInGdxThread(Runnable runnable) throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] error = new Throwable[1];

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    runnable.run();
                } catch (Throwable t) {
                    error[0] = t;
                } finally {
                    latch.countDown();
                }
            }
        }, config);

        assertTrueLocal(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        if (error[0] != null) {
            if (error[0] instanceof RuntimeException) throw (RuntimeException)error[0];
            if (error[0] instanceof Error) throw (Error)error[0];
            throw new RuntimeException(error[0]);
        }
    }

    private void assertTrueLocal(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
