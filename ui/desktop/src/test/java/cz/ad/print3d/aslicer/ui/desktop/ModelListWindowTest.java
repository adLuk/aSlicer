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
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
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
                    final Array<String> paths = new Array<>();
                    paths.add("/path/to/model1.stl");
                    paths.add("/models/model2.3mf");

                    final ModelListWindow[] windowRef = new ModelListWindow[1];
                    windowRef[0] = new ModelListWindow(skin, paths, new ModelListWindow.ModelListListener() {
                        @Override
                        public void onRemoveModel(int index) {
                            paths.removeIndex(index);
                            // Mock updateList logic since we are in test
                            if (windowRef[0] != null) {
                                windowRef[0].updateList();
                            }
                        }

                        @Override
                        public void onDuplicateModel(int index) {
                            paths.add(paths.get(index));
                            // Mock updateList logic since we are in test
                            if (windowRef[0] != null) {
                                windowRef[0].updateList();
                            }
                        }
                    });
                    ModelListWindow window = windowRef[0];
                    assertNotNull(window);
                    assertEquals("Loaded Models", window.getTitleLabel().getText().toString());

                    // Check list items
                    List<?> list = window.getInternalList();
                    assertNotNull(list);
                    assertEquals(2, list.getItems().size);
                    assertEquals("model1.stl", list.getItems().get(0).toString());
                    assertEquals("model2.3mf", list.getItems().get(1).toString());

                    // Verify Duplicate and Remove Buttons presence
                    Table listTable = findTable(window);
                    assertNotNull(listTable, "List table should exist");
                    assertEquals(2, listTable.getChildren().size, "Should have 2 rows");
                    
                    // Force window layout to propagate sizes
                    window.setSize(250, 400); // Set fixed size for layout testing
                    window.validate();
                    listTable.validate();

                    // Verify buttons in the first row
                    Table firstRow = (Table) listTable.getChildren().get(0);
                    firstRow.validate(); // Ensure layout is calculated
                    ImageButton duplicateBtn = findDuplicateButton(firstRow);
                    assertNotNull(duplicateBtn, "Duplicate button should exist in row");
                    assertTrue(duplicateBtn.isVisible(), "Duplicate button should be visible");
                    assertTrue(duplicateBtn.getWidth() > 0, "Duplicate button should have width");
                    
                    ImageButton removeBtn = findRemoveButton(firstRow);
                    assertNotNull(removeBtn, "Remove button should exist in row");
                    assertTrue(removeBtn.isVisible(), "Remove button should be visible");
                    assertTrue(removeBtn.getWidth() > 0, "Remove button should have width");

                    // Check if remove button is within row bounds
                    float removeBtnRight = removeBtn.getX() + removeBtn.getWidth();
                    assertTrue(firstRow.getWidth() > 0, "Row should have width after validate()");
                    assertTrue(removeBtnRight <= firstRow.getWidth(), "Remove button (" + removeBtnRight + ") should be within row width (" + firstRow.getWidth() + ")");

                    // Test duplication
                    duplicateBtn.fire(new ChangeListener.ChangeEvent());
                    assertEquals(3, paths.size, "Path should be duplicated in source array");
                    assertEquals(3, list.getItems().size, "List should be updated with duplicated item");
                    assertEquals("model1.stl", list.getItems().get(2).toString());

                    // Actually, let's simulate the removal button click on the first item
                    removeBtn.fire(new ChangeListener.ChangeEvent());
                    
                    assertEquals(2, paths.size, "Path should be removed from source array");
                    assertEquals(2, list.getItems().size, "List should be updated");
                    assertEquals("model2.3mf", list.getItems().get(0).toString());
                    assertEquals("model1.stl", list.getItems().get(1).toString());

                    // Add item and update
                    paths.add("new_model.stl");
                    window.updateList();
                    assertEquals(3, list.getItems().size);
                    assertEquals("new_model.stl", list.getItems().get(2).toString());

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

                    // Verify multi-selection settings
                    assertTrue(list.getSelection().getMultiple(), "Multi-selection should be enabled");
                    assertNotNull(list.getSelection(), "Selection should not be null");

                    // Test selection (multiple items)
                    list.getSelection().clear();
                    Object item1 = list.getItems().get(0);
                    Object item2 = list.getItems().get(1);
                    ((com.badlogic.gdx.scenes.scene2d.utils.Selection)list.getSelection()).add(item1);
                    ((com.badlogic.gdx.scenes.scene2d.utils.Selection)list.getSelection()).add(item2);
                    assertEquals(2, list.getSelection().size(), "Should be able to select multiple items programmatically");
                    assertTrue(((com.badlogic.gdx.scenes.scene2d.utils.Selection)list.getSelection()).contains(item1));
                    assertTrue(((com.badlogic.gdx.scenes.scene2d.utils.Selection)list.getSelection()).contains(item2));

                    // Reproduction for the new issue: loading the same file multiple times
                    paths.clear();
                    paths.add("duplicate.stl");
                    paths.add("duplicate.stl");
                    window.updateList();
                    assertEquals(2, list.getItems().size, "List should have two items");
                    assertEquals("duplicate.stl", list.getItems().get(0).toString());
                    assertEquals("duplicate.stl", list.getItems().get(1).toString());

                    // When items are unique objects with same toString, they are separate items
                    list.getSelection().clear();
                    Object dup1 = list.getItems().get(0);
                    Object dup2 = list.getItems().get(1);
                    
                    ((com.badlogic.gdx.scenes.scene2d.utils.Selection)list.getSelection()).add(dup1);
                    assertEquals(1, list.getSelection().size(), "Should have only one item in selection");
                    assertTrue(((com.badlogic.gdx.scenes.scene2d.utils.Selection)list.getSelection()).contains(dup1));
                    assertTrue(!((com.badlogic.gdx.scenes.scene2d.utils.Selection)list.getSelection()).contains(dup2), "Second identical-looking item should NOT be selected");

                    ((com.badlogic.gdx.scenes.scene2d.utils.Selection)list.getSelection()).add(dup2);
                    assertEquals(2, list.getSelection().size(), "Should now have both items in selection");
                    assertTrue(((com.badlogic.gdx.scenes.scene2d.utils.Selection)list.getSelection()).contains(dup1));
                    assertTrue(((com.badlogic.gdx.scenes.scene2d.utils.Selection)list.getSelection()).contains(dup2));

                    // Verify selection persistence after updateList
                    window.updateList();
                    assertEquals(2, list.getSelection().size(), "Selection should be preserved after updateList with same items");

                    // Test multi-selection after item removal
                    paths.removeIndex(0); // Remove one item
                    window.updateList();
                    assertEquals(1, list.getItems().size, "Should have 1 item left");
                    assertEquals(0, list.getSelection().size(), "Selection should be cleared when list structure changes significantly");
                    
                    // Re-select
                    Object finalItem = list.getItems().get(0);
                    ((com.badlogic.gdx.scenes.scene2d.utils.Selection)list.getSelection()).add(finalItem);
                    assertEquals(1, list.getSelection().size());

                    // Test with very long filename to ensure buttons are still visible and within bounds
                    paths.clear();
                    paths.add("very_long_filename_that_should_be_truncated_with_ellipsis_to_keep_buttons_visible_in_the_list_window.stl");
                    window.updateList();
                    window.setSize(200, 400); // Narrower window
                    window.validate();
                    Table narrowListTable = findTable(window);
                    narrowListTable.validate();
                    Table longRow = (Table) narrowListTable.getChildren().get(0);
                    longRow.validate();
                    
                    ImageButton longDuplicateBtn = findDuplicateButton(longRow);
                    ImageButton longRemoveBtn = findRemoveButton(longRow);
                    assertNotNull(longDuplicateBtn);
                    assertNotNull(longRemoveBtn);
                    assertTrue(longRemoveBtn.isVisible());
                    
                    float longRemoveBtnRight = longRemoveBtn.getX() + longRemoveBtn.getWidth();
                    // Window padding is 10 on left/right. Window width is 200. Table fills window (minus padding).
                    // So row width should be around 180.
                    assertTrue(longRow.getWidth() <= 180, "Row width (" + longRow.getWidth() + ") should be within window constraints");
                    assertTrue(longRemoveBtnRight <= longRow.getWidth(), "Remove button (" + longRemoveBtnRight + ") must be within row width (" + longRow.getWidth() + ") even with long filenames");

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

            private List<?> findList(ModelListWindow window) {
                return window.getInternalList();
            }

            private Table findTable(ModelListWindow window) {
                for (com.badlogic.gdx.scenes.scene2d.Actor actor : window.getChildren()) {
                    if (actor instanceof ScrollPane) {
                        ScrollPane sp = (ScrollPane) actor;
                        if (sp.getActor() instanceof Table) {
                            return (Table) sp.getActor();
                        }
                    }
                }
                return null;
            }

            private ImageButton findDuplicateButton(Table row) {
                for (com.badlogic.gdx.scenes.scene2d.Actor actor : row.getChildren()) {
                    if (actor instanceof ImageButton && "duplicateButton".equals(actor.getName())) {
                        return (ImageButton) actor;
                    }
                }
                return null;
            }

            private ImageButton findRemoveButton(Table row) {
                for (com.badlogic.gdx.scenes.scene2d.Actor actor : row.getChildren()) {
                    if (actor instanceof ImageButton && "removeButton".equals(actor.getName())) {
                        return (ImageButton) actor;
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
                
                Label.LabelStyle labelStyle = new Label.LabelStyle();
                labelStyle.font = font;
                labelStyle.fontColor = Color.WHITE;
                skin.add("default", labelStyle);
                
                ImageButton.ImageButtonStyle imageButtonStyle = new ImageButton.ImageButtonStyle();
                skin.add("default", imageButtonStyle);

                return skin;
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
    }
}
