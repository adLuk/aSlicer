/*
 * aSlicer - 3D model processing tool.
 * Copyright (C) 2026 cz.ad.print3d.aslicer contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.ad.print3d.aslicer.ui.desktop.view;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for DesktopUI to ensure stages are properly initialized and managed.
 */
public class DesktopUITest {

    @Test
    void testDesktopUIStagesInitialization() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    mockGdxGL();
                    
                    DesktopUI ui = new DesktopUI();
                    assertNotNull(ui.getMenuStage(), "Menu stage should be initialized");
                    assertNotNull(ui.getDialogStage(), "Dialog stage should be initialized");
                    assertNotNull(ui.getActiveViewStage(), "Initial active view stage should be initialized");
                    assertNotNull(ui.getSkin(), "UI Skin should be initialized");

                    // Test view switching
                    Stage view1 = ui.getActiveViewStage();
                    ui.setActiveView(1);
                    Stage view2 = ui.getActiveViewStage();
                    assertTrue(view1 != view2, "Active view stage should change after switching");
                    
                    Window testWindow = new Window("Test Window", ui.getSkin());
                    ui.addDialog(testWindow);
                    
                    assertTrue(ui.getDialogStage().getActors().contains(testWindow, true), 
                            "Dialog should be added to the dialog stage");
                    
                    ui.dispose();
                    success[0] = true;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }
            private void mockGdxGL() {
                Gdx.gl20 = (com.badlogic.gdx.graphics.GL20) java.lang.reflect.Proxy.newProxyInstance(
                        com.badlogic.gdx.graphics.GL20.class.getClassLoader(),
                        new Class[]{com.badlogic.gdx.graphics.GL20.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("glGenBuffer") || method.getName().equals("glGenTexture") || 
                                method.getName().equals("glCreateShader") || method.getName().equals("glCreateProgram")) return 1;
                            if (method.getName().equals("glGetShaderiv") || method.getName().equals("glGetProgramiv")) {
                                java.nio.IntBuffer params = (java.nio.IntBuffer) args[2];
                                params.put(0, 1);
                                return null;
                            }
                            if (method.getName().equals("glGetActiveAttrib")) {
                                java.nio.IntBuffer size = (java.nio.IntBuffer) args[2];
                                java.nio.IntBuffer type = (java.nio.IntBuffer) args[3];
                                size.put(0, 1);
                                type.put(0, GL20.GL_FLOAT_VEC4);
                                int index = (int) args[1];
                                if (index == 0) return "a_position";
                                if (index == 1) return "a_color";
                                if (index == 2) return "a_texCoord0";
                                return "attr" + index;
                            }
                            if (method.getName().equals("glGetActiveUniform")) {
                                java.nio.IntBuffer size = (java.nio.IntBuffer) args[2];
                                java.nio.IntBuffer type = (java.nio.IntBuffer) args[3];
                                size.put(0, 1);
                                type.put(0, GL20.GL_FLOAT_MAT4);
                                int index = (int) args[1];
                                if (index == 0) return "u_projTrans";
                                return "uni" + index;
                            }
                            if (method.getReturnType().equals(int.class)) return 0;
                            if (method.getReturnType().equals(boolean.class)) return true;
                            return null;
                        }
                );
                Gdx.gl = Gdx.gl20;
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        assertTrue(success[0], "Test should have finished successfully");
    }
}
