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
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import cz.ad.print3d.aslicer.logic.printer.PrinterRepository;
import cz.ad.print3d.aslicer.ui.desktop.GdxTestUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AppToolbarTest {

    @Test
    void testToolbarButtons() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean clearCalled = new AtomicBoolean(false);
        AtomicBoolean openCalled = new AtomicBoolean(false);
        AtomicBoolean settingsCalled = new AtomicBoolean(false);
        AtomicBoolean addPrinterCalled = new AtomicBoolean(false);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = createTestSkin();
                    PrinterRepository repository = createMockRepository();
                    AppToolbar toolbar = new AppToolbar(skin, new AppToolbar.ToolbarListener() {
                        @Override
                        public void onClear() {
                            clearCalled.set(true);
                        }

                        @Override
                        public void onOpen() {
                            openCalled.set(true);
                        }

                        @Override
                        public void onSettings() {
                            settingsCalled.set(true);
                        }

                        @Override
                        public void onAddPrinter() {
                            addPrinterCalled.set(true);
                        }
                    }, repository);

                    assertNotNull(toolbar);

                    // Manually trigger button clicks by recursing into ToolbarGroups
                    for (Actor actor : toolbar.getChildren()) {
                        if (actor instanceof ToolbarGroup) {
                            ToolbarGroup group = (ToolbarGroup) actor;
                            for (Actor groupActor : group.getButtonContainer().getChildren()) {
                                if (groupActor instanceof ImageButton) {
                                    ImageButton button = (ImageButton) groupActor;
                                    button.fire(new ChangeListener.ChangeEvent());
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
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
                
                TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
                textButtonStyle.font = font;
                skin.add("default", textButtonStyle);
                
                CheckBox.CheckBoxStyle checkBoxStyle = new CheckBox.CheckBoxStyle();
                checkBoxStyle.font = font;
                skin.add("default", checkBoxStyle);
                
                ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle();
                skin.add("default", scrollPaneStyle);
                
                Window.WindowStyle windowStyle = new Window.WindowStyle();
                windowStyle.titleFont = font;
                skin.add("default", windowStyle);

                com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle listStyle = new com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle();
                listStyle.font = font;
                listStyle.selection = skin.newDrawable("white", Color.LIGHT_GRAY);
                skin.add("default", listStyle);

                return skin;
            }

            private PrinterRepository createMockRepository() {
                return (PrinterRepository) Proxy.newProxyInstance(
                    PrinterRepository.class.getClassLoader(),
                    new Class<?>[]{PrinterRepository.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("getGroups")) return Collections.emptyList();
                        return null;
                    }
                );
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        assertTrue(clearCalled.get(), "onClear should have been called");
        assertTrue(openCalled.get(), "onOpen should have been called");
        assertTrue(settingsCalled.get(), "onSettings should have been called");
        assertTrue(addPrinterCalled.get(), "onAddPrinter should have been called");
    }
}
