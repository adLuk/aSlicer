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
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsWindowTest {

    @Test
    void testSettingsWindow() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    mockGdxGL();
                    Skin skin = createTestSkin();
                    PerspectiveCamera cam = new PerspectiveCamera();
                    CameraInputController camController = new CameraInputController(cam);
                    AtomicBoolean saved = new AtomicBoolean(false);
                    AtomicReference<Float> updatedGridSize = new AtomicReference<>(0.5f);

                    SettingsWindow window = new SettingsWindow(skin, camController, 0.5f, updatedGridSize::set, () -> saved.set(true));

                    assertEquals("Settings", window.getTitleLabel().getText().toString());
                    assertTrue(window.isVisible());

                    // Find grid size button and test cycling
                    TextButton gridBtn = findButton(window, "0.5");
                    assertNotNull(gridBtn, "Grid size button should be found");
                    gridBtn.fire(new ChangeListener.ChangeEvent());
                    assertEquals(1.0f, updatedGridSize.get(), 0.001f);
                    assertEquals("1.0", gridBtn.getText().toString());

                    // Find and click the save button
                    TextButton saveBtn = findButton(window, "Save");
                    assertNotNull(saveBtn, "Save button should be found");
                    
                    saveBtn.fire(new ChangeListener.ChangeEvent());
                    
                    assertTrue(saved.get(), "Save callback should have been called");
                    assertFalse(window.isVisible(), "Window should be hidden after save");

                } catch (Throwable t) {
                    errorRef.set(t);
                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }

            private Skin createTestSkin() {
                Skin skin = new Skin();
                Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
                pixmap.setColor(Color.WHITE);
                pixmap.fill();
                skin.add("white", new Texture(pixmap));

                BitmapFont font = new BitmapFont();
                skin.add("default", font);

                Label.LabelStyle labelStyle = new Label.LabelStyle();
                labelStyle.font = font;
                skin.add("default", labelStyle);

                TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
                textButtonStyle.up = skin.newDrawable("white", Color.LIGHT_GRAY);
                textButtonStyle.down = skin.newDrawable("white", Color.DARK_GRAY);
                textButtonStyle.font = font;
                skin.add("default", textButtonStyle);

                Window.WindowStyle windowStyle = new Window.WindowStyle();
                windowStyle.titleFont = font;
                windowStyle.background = skin.newDrawable("white", Color.DARK_GRAY);
                skin.add("default", windowStyle);

                return skin;
            }

            private TextButton findButton(Window window, String text) {
                for (Actor actor : window.getChildren()) {
                    if (actor instanceof com.badlogic.gdx.scenes.scene2d.ui.Table) {
                        TextButton btn = findButtonInTable((com.badlogic.gdx.scenes.scene2d.ui.Table) actor, text);
                        if (btn != null) return btn;
                    }
                }
                return null;
            }

            private TextButton findButtonInTable(com.badlogic.gdx.scenes.scene2d.ui.Table table, String text) {
                for (Actor actor : table.getChildren()) {
                    if (actor instanceof TextButton) {
                        TextButton btn = (TextButton) actor;
                        if (text.equals(btn.getText().toString())) {
                            return btn;
                        }
                    } else if (actor instanceof com.badlogic.gdx.scenes.scene2d.ui.Table) {
                        TextButton btn = findButtonInTable((com.badlogic.gdx.scenes.scene2d.ui.Table) actor, text);
                        if (btn != null) return btn;
                    }
                }
                return null;
            }

            private void mockGdxGL() {
                Gdx.gl20 = (GL20) java.lang.reflect.Proxy.newProxyInstance(
                        GL20.class.getClassLoader(),
                        new Class<?>[]{GL20.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("glGenBuffer") || method.getName().equals("glGenTexture")) return 1;
                            if (method.getReturnType().equals(int.class)) return 0;
                            if (method.getReturnType().equals(boolean.class)) return true;
                            return null;
                        }
                );
                Gdx.gl = Gdx.gl20;
            }
        }, new HeadlessApplicationConfiguration());

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (errorRef.get() != null) {
            errorRef.get().printStackTrace();
            fail(errorRef.get().getMessage());
        }
    }
}
