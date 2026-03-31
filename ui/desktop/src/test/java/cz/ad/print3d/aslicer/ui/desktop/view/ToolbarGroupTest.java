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
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import cz.ad.print3d.aslicer.ui.desktop.GdxTestUtils;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ToolbarGroup.
 */
public class ToolbarGroupTest {

    @Test
    void testToolbarGroupInitialization() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = createTestSkin();
                    ToolbarGroup group = new ToolbarGroup(skin);

                    assertNotNull(group, "ToolbarGroup should be initialized");
                    assertNotNull(group.getButtonContainer(), "Button container should be initialized");
                    assertNotNull(group.getSeparator(), "Separator line should be initialized");
                    assertNotNull(group.getSeparatorContainer(), "Separator container should be initialized");

                    ImageButton button = new ImageButton(new ImageButton.ImageButtonStyle());
                    group.addButton(button);

                    assertTrue(group.getButtonContainer().getChildren().contains(button, true), "Button should be added to the container");
                    assertTrue(group.getChildren().contains(group.getSeparatorContainer(), true), "Separator container should be a child of the group");
                    assertTrue(group.getSeparatorContainer().getChildren().contains(group.getSeparator(), true), "Separator line should be in the container");

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
                return skin;
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
    }

    @Test
    void testToolbarGroupResizing() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = createTestSkin();
                    ToolbarGroup group = new ToolbarGroup(skin);
                    group.setSize(100, 40);
                    group.getButtonContainer().setSize(80, 40);
                    group.layout();

                    float initialWidth = group.getButtonContainer().getWidth();

                    // Simulate drag on container
                    com.badlogic.gdx.scenes.scene2d.InputEvent event = new com.badlogic.gdx.scenes.scene2d.InputEvent();
                    event.setType(com.badlogic.gdx.scenes.scene2d.InputEvent.Type.touchDown);
                    event.setStageX(50);
                    group.getSeparatorContainer().fire(event);

                    event.setType(com.badlogic.gdx.scenes.scene2d.InputEvent.Type.touchDragged);
                    event.setStageX(70); // Dragged by 20 units
                    group.getSeparatorContainer().fire(event);

                    group.layout();
                    float newWidth = group.getButtonContainer().getWidth();

                    assertTrue(newWidth > initialWidth, "Width should increase after dragging right");

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
                return skin;
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
    }

    @Test
    void testToolbarGroupVerticalResizing() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = createTestSkin();
                    ToolbarGroup group = new ToolbarGroup(skin, true);
                    group.setSize(40, 100);
                    group.getButtonContainer().setSize(40, 80);
                    group.layout();

                    float initialHeight = group.getButtonContainer().getHeight();

                    // Simulate drag on container
                    com.badlogic.gdx.scenes.scene2d.InputEvent event = new com.badlogic.gdx.scenes.scene2d.InputEvent();
                    event.setType(com.badlogic.gdx.scenes.scene2d.InputEvent.Type.touchDown);
                    event.setStageY(50);
                    group.getSeparatorContainer().fire(event);

                    event.setType(com.badlogic.gdx.scenes.scene2d.InputEvent.Type.touchDragged);
                    event.setStageY(30); // Dragged down by 20 units (deltaY = -20)
                    group.getSeparatorContainer().fire(event);

                    group.layout();
                    float newHeight = group.getButtonContainer().getHeight();

                    // deltaY is -20, newHeight = 80 - (-20) = 100
                    assertTrue(newHeight > initialHeight, "Height should increase after dragging down");

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
                return skin;
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
    }

    @Test
    void testSeparatorSizeAndFilling() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = createTestSkin();
                    
                    // Horizontal group: should have vertical separator with full height and 4px width
                    ToolbarGroup horizontalGroup = new ToolbarGroup(skin, false);
                    horizontalGroup.setSize(100, 60);
                    horizontalGroup.layout();
                    
                    assertEquals(60, horizontalGroup.getSeparator().getHeight(), 0.1, "Horizontal group separator should take full height");
                    assertEquals(4, horizontalGroup.getSeparator().getWidth(), 0.1, "Horizontal group separator should be 4 pixels wide");
                    
                    // Vertical group: should have horizontal separator with full width and 4px height
                    ToolbarGroup verticalGroup = new ToolbarGroup(skin, true);
                    verticalGroup.setSize(60, 100);
                    verticalGroup.layout();
                    
                    assertEquals(60, verticalGroup.getSeparator().getWidth(), 0.1, "Vertical group separator should take full width");
                    assertEquals(4, verticalGroup.getSeparator().getHeight(), 0.1, "Vertical group separator should be 4 pixels high");
                    
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
                return skin;
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
    }

    @Test
    void testToolbarGroupSeparatorVisibility() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = createTestSkin();
                    ToolbarGroup group = new ToolbarGroup(skin);

                    assertTrue(group.getSeparatorContainer().isVisible(), "Separator should be visible by default");

                    group.setSeparatorVisible(false);
                    assertFalse(group.getSeparatorContainer().isVisible(), "Separator should be hidden");
                    assertEquals(0, group.getCell(group.getSeparatorContainer()).getMinWidth(), 0.001);

                    group.setSeparatorVisible(true);
                    assertTrue(group.getSeparatorContainer().isVisible(), "Separator should be visible again");
                    assertEquals(24, group.getCell(group.getSeparatorContainer()).getMinWidth(), 0.001);

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
                return skin;
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
    }

    @Test
    void testToolbarGroupVerticalOrientation() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = createTestSkin();
                    ToolbarGroup horizontalGroup = new ToolbarGroup(skin, false);
                    horizontalGroup.addButton(new ImageButton(new ImageButton.ImageButtonStyle()));
                    horizontalGroup.addButton(new ImageButton(new ImageButton.ImageButtonStyle()));

                    ToolbarGroup verticalGroup = new ToolbarGroup(skin, true);
                    verticalGroup.addButton(new ImageButton(new ImageButton.ImageButtonStyle()));
                    verticalGroup.addButton(new ImageButton(new ImageButton.ImageButtonStyle()));

                    // Horizontal group: buttons in same row
                    assertEquals(2, horizontalGroup.getButtonContainer().getColumns(), "Horizontal group should have 2 columns");
                    assertEquals(1, horizontalGroup.getButtonContainer().getRows(), "Horizontal group should have 1 row");

                    // Vertical group: buttons in different rows
                    assertEquals(1, verticalGroup.getButtonContainer().getColumns(), "Vertical group should have 1 column");
                    assertEquals(2, verticalGroup.getButtonContainer().getRows(), "Vertical group should have 2 rows");

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
                return skin;
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
    }

    @Test
    void testContinuousVerticalResizing() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = createTestSkin();
                    ToolbarGroup group = new ToolbarGroup(skin, true);
                    // Add a button with fixed size to establish preferred size
                    ImageButton button = new ImageButton(new ImageButton.ImageButtonStyle());
                    button.setSize(40, 40);
                    group.getButtonContainer().add(button).size(40, 40);
                    group.layout();

                    float initialHeight = group.getButtonContainer().getHeight();

                    com.badlogic.gdx.scenes.scene2d.InputEvent event = new com.badlogic.gdx.scenes.scene2d.InputEvent();
                    event.setType(com.badlogic.gdx.scenes.scene2d.InputEvent.Type.touchDown);
                    event.setStageY(100);
                    group.getSeparatorContainer().fire(event);

                    event.setType(com.badlogic.gdx.scenes.scene2d.InputEvent.Type.touchDragged);
                    event.setStageY(90); // Dragged down by 10 (deltaY = -10)
                    group.getSeparatorContainer().fire(event);
                    
                    event.setStageY(80); // Dragged down by another 10 (deltaY = -10 relative to last)
                    group.getSeparatorContainer().fire(event);

                    group.layout();
                    float newHeight = group.getButtonContainer().getHeight();

                    assertEquals(initialHeight + 20, newHeight, 0.1, "Height should increase by 20 after two 10-unit drags without layout in between");

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
                return skin;
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
    }

    @Test
    void testContinuousHorizontalResizing() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = createTestSkin();
                    ToolbarGroup group = new ToolbarGroup(skin, false);
                    ImageButton button = new ImageButton(new ImageButton.ImageButtonStyle());
                    button.setSize(40, 40);
                    group.getButtonContainer().add(button).size(40, 40);
                    group.layout();

                    float initialWidth = group.getButtonContainer().getWidth();

                    com.badlogic.gdx.scenes.scene2d.InputEvent event = new com.badlogic.gdx.scenes.scene2d.InputEvent();
                    event.setType(com.badlogic.gdx.scenes.scene2d.InputEvent.Type.touchDown);
                    event.setStageX(100);
                    group.getSeparatorContainer().fire(event);

                    event.setType(com.badlogic.gdx.scenes.scene2d.InputEvent.Type.touchDragged);
                    event.setStageX(110); // Dragged right by 10
                    group.getSeparatorContainer().fire(event);
                    
                    event.setStageX(120); // Dragged right by another 10
                    group.getSeparatorContainer().fire(event);

                    group.layout();
                    float newWidth = group.getButtonContainer().getWidth();

                    assertEquals(initialWidth + 20, newWidth, 0.1, "Width should increase by 20 after two 10-unit drags without layout in between");

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
                return skin;
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
    }
}
