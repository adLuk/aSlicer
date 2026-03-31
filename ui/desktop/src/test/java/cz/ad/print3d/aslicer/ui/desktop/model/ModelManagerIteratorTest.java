package cz.ad.print3d.aslicer.ui.desktop.model;

import com.badlogic.gdx.utils.Array;
import cz.ad.print3d.aslicer.ui.desktop.config.AppConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ModelManagerIteratorTest {

    @Test
    void testNestedIteratorException() {
        ModelManager modelManager = new ModelManager(new AppConfig(), null);
        modelManager.addListener(new ModelManager.ModelManagerListener() {
            @Override
            public void onModelsChanged() {
                // This will trigger notifySelectionChanged which iterates over the SAME listeners array
                modelManager.setSelectedIndices(new Array<>());
            }

            @Override
            public void onSelectionChanged() {
                // do nothing
            }
        });

        // Should NOT throw anymore
        assertDoesNotThrow(() -> {
            try {
                java.lang.reflect.Method method = ModelManager.class.getDeclaredMethod("notifyModelsChanged");
                method.setAccessible(true);
                method.invoke(modelManager);
            } catch (Exception e) {
                if (e.getCause() != null) {
                    throw e.getCause();
                }
                throw e;
            }
        });
    }
}
