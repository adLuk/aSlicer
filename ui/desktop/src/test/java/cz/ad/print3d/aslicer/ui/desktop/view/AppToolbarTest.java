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
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import cz.ad.print3d.aslicer.ui.desktop.GdxTestUtils;
import org.junit.jupiter.api.Test;

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
        AtomicBoolean switchViewCalled = new AtomicBoolean(false);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = createTestSkin();
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
                        public void onSwitchView(int index) {
                            switchViewCalled.set(true);
                        }
                    });

                    assertNotNull(toolbar);

                    // Manually trigger button clicks
                    for (Actor actor : toolbar.getChildren()) {
                        if (actor instanceof ImageButton) {
                            ImageButton button = (ImageButton) actor;
                            button.fire(new ChangeListener.ChangeEvent());
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
                skin.add("default", new BitmapFont());
                return skin;
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        assertTrue(clearCalled.get(), "onClear should have been called");
        assertTrue(openCalled.get(), "onOpen should have been called");
        assertTrue(settingsCalled.get(), "onSettings should have been called");
        assertTrue(switchViewCalled.get(), "onSwitchView should have been called");
    }
}
