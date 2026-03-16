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
package cz.ad.print3d.aslicer.ui.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ModelListWindow.
 */
public class ModelListWindowTest {

    @Test
    void testModelListWindow() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    mockGdxGL();
                    Skin skin = createTestSkin();
                    Array<String> paths = new Array<>();
                    paths.add("/path/to/model1.stl");
                    paths.add("/models/model2.3mf");

                    ModelListWindow window = new ModelListWindow(skin, paths);
                    assertNotNull(window);
                    assertEquals("Loaded Models", window.getTitleLabel().getText().toString());

                    // Check list items
                    List<String> list = findList(window);
                    assertNotNull(list);
                    assertEquals(2, list.getItems().size);
                    assertEquals("model1.stl", list.getItems().get(0));
                    assertEquals("model2.3mf", list.getItems().get(1));

                    // Add item and update
                    paths.add("new_model.stl");
                    window.updateList();
                    assertEquals(3, list.getItems().size);
                    assertEquals("new_model.stl", list.getItems().get(2));

                    // Verify layout and alignment
                    window.layout();
                    float padTop = window.getPadTop();
                    assertTrue(padTop >= 30, "PadTop should be at least 30 to avoid title overlap");
                    assertTrue(window.getPadLeft() >= 10, "PadLeft should be at least 10 for proper alignment");

                    ScrollPane scrollPane = findScrollPane(window);
                    assertNotNull(scrollPane);
                    float topOfScrollPane = scrollPane.getY() + scrollPane.getHeight();
                    float expectedMaxTop = window.getHeight() - padTop;
                    assertTrue(topOfScrollPane <= expectedMaxTop, "ScrollPane content overlaps window title");

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }

            private ScrollPane findScrollPane(ModelListWindow window) {
                for (com.badlogic.gdx.scenes.scene2d.Actor actor : window.getChildren()) {
                    if (actor instanceof ScrollPane) {
                        return (ScrollPane) actor;
                    }
                }
                return null;
            }

            @SuppressWarnings("unchecked")
            private List<String> findList(ModelListWindow window) {
                for (com.badlogic.gdx.scenes.scene2d.Actor actor : window.getChildren()) {
                    if (actor instanceof ScrollPane) {
                        ScrollPane sp = (ScrollPane) actor;
                        if (sp.getActor() instanceof List) {
                            return (List<String>) sp.getActor();
                        }
                    }
                }
                return null;
            }

            private void mockGdxGL() {
                Gdx.gl20 = (com.badlogic.gdx.graphics.GL20) java.lang.reflect.Proxy.newProxyInstance(
                        com.badlogic.gdx.graphics.GL20.class.getClassLoader(),
                        new Class[]{com.badlogic.gdx.graphics.GL20.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("glGenBuffer") || method.getName().equals("glGenTexture")) return 1;
                            if (method.getReturnType().equals(int.class)) return 0;
                            if (method.getReturnType().equals(boolean.class)) return true;
                            return null;
                        }
                );
                Gdx.gl = Gdx.gl20;
            }

            private Skin createTestSkin() {
                Skin skin = new Skin();
                Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
                pixmap.setColor(Color.WHITE);
                pixmap.fill();
                skin.add("white", new Texture(pixmap));
                BitmapFont font = new BitmapFont();
                skin.add("default", font);
                
                List.ListStyle listStyle = new List.ListStyle();
                listStyle.font = font;
                listStyle.selection = skin.newDrawable("white", Color.GRAY);
                listStyle.fontColorSelected = Color.BLACK;
                listStyle.fontColorUnselected = Color.WHITE;
                skin.add("default", listStyle);
                
                ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle();
                skin.add("default", scrollPaneStyle);
                
                Window.WindowStyle windowStyle = new Window.WindowStyle();
                windowStyle.titleFont = font;
                skin.add("default", windowStyle);
                
                return skin;
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
    }
}
