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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import cz.ad.print3d.aslicer.logic.model.Model;
import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import cz.ad.print3d.aslicer.ui.desktop.GdxTestUtils;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that model data (e.g. triangle count, parts) is correctly displayed in the UI.
 */
public class ModelDataDisplayTest {

    @Test
    void testModelDetailDisplay() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = GdxTestUtils.createTestSkin();

                    Array<String> paths = new Array<>();
                    paths.add("test.stl");

                    Model mockModel = createMockModel("TestPart", 100);
                    Array<Model> logicModels = new Array<>();
                    logicModels.add(mockModel);

                    ModelListWindow window = new ModelListWindow(skin, paths, logicModels, null);
                    
                    // Initially no selection
                    Label detailLabel = findLabelByName(window, ""); // It's the one we added without name, but we can find it by text or position
                    // Actually, let's find it by checking all labels in the window
                    Label foundDetailLabel = null;
                    for (com.badlogic.gdx.scenes.scene2d.Actor actor : window.getChildren()) {
                        if (actor instanceof Label) {
                            Label l = (Label) actor;
                            if (l.getText().toString().contains("No model selected")) {
                                foundDetailLabel = l;
                                break;
                            }
                        }
                    }
                    
                    assertTrue(foundDetailLabel != null, "Detail label should show 'No model selected' initially");

                    // Select the model
                    Array<Integer> selection = new Array<>();
                    selection.add(0);
                    window.setSelectedIndices(selection);

                    String text = foundDetailLabel.getText().toString();
                    assertTrue(text.contains("Parts: 1"), "Should show part count");
                    assertTrue(text.contains("Triangles: 100"), "Should show triangle count");
                    assertTrue(text.contains("Units: millimeter"), "Should show units");

                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    Gdx.app.exit();
                }
            }

            private Label findLabelByName(ModelListWindow window, String name) {
                // Not ideal since it's anonymous, so we use the search in create()
                return null;
            }

            private Model createMockModel(String partName, int triangleCount) {
                return new Model() {
                    @Override public Unit unit() { return Unit.MILLIMETER; }
                    @Override public List<? extends MeshPart> parts() {
                        return Collections.singletonList(new MeshPart() {
                            @Override public String name() { return partName; }
                            @Override public Integer color() { return null; }
                            @Override public List<? extends Triangle> triangles() {
                                return Collections.nCopies(triangleCount, null);
                            }
                        });
                    }
                };
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
