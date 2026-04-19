package cz.ad.print3d.aslicer.ui.desktop.view;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import cz.ad.print3d.aslicer.ui.desktop.GdxTestUtils;
import cz.ad.print3d.aslicer.ui.desktop.I18N;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class LanguageSelectBoxTest {

    @Test
    void testLanguageSelection() throws InterruptedException {
        final AtomicBoolean finished = new AtomicBoolean(false);
        final AtomicBoolean success = new AtomicBoolean(false);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = GdxTestUtils.createTestSkin();
                    I18N.init(Locale.ENGLISH);

                    LanguageSelectBox selectBox = new LanguageSelectBox(skin);
                    Stage stage = new Stage(new ScreenViewport());
                    stage.getViewport().update(800, 600, true);
                    stage.addActor(selectBox);
                    
                    // Set some size for selectBox if it's not layouted by a table
                    selectBox.setSize(100, 40);
                    selectBox.setPosition(400, 300); // Put it in the middle
                    
                    stage.act();

                    // 1. Test initial selection
                    assertEquals("English", selectBox.getSelected().getName());
                    assertEquals("Settings", I18N.get("toolbar.settings"));

                    // 2. Test loaded values (should match I18N.getSupportedLocales())
                    Locale[] supported = I18N.getSupportedLocales();
                    assertEquals(supported.length, selectBox.getItemsCount(), "Should load all supported locales");
                    
                    // 3. Test selection change and its effect on I18N
                    // We need to add a listener that actually calls I18N.init, mimicking AppToolbar behavior
                    selectBox.addListener(event -> {
                        if (event instanceof com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent) {
                            LanguageSelectBox.LanguageItem selected = selectBox.getSelected();
                            if (selected != null) {
                                I18N.init(selected.getLocale());
                            }
                        }
                        return false;
                    });

                    // Change selection to Czech
                    selectBox.setSelectedByLocale(Locale.forLanguageTag("cs-CZ"));
                    assertEquals("Čeština", selectBox.getSelected().getName());
                    assertEquals("cs", selectBox.getSelected().getLocale().getLanguage());
                    
                    // Verify I18N changed
                    assertEquals("Nastavení", I18N.get("toolbar.settings"));

                    // 4. Test dropdown interaction
                    // Initially scrollPane should not have a parent
                    // We need a way to access scrollPane, it's private.
                    // But we can check if it's visible or added to stage after showDropdown
                    // Since showDropdown is private, we simulate click on selectionButton
                    com.badlogic.gdx.scenes.scene2d.ui.Button button = (com.badlogic.gdx.scenes.scene2d.ui.Button) selectBox.getChildren().get(0);
                    
                    // Click to show
                    com.badlogic.gdx.scenes.scene2d.InputEvent event = new com.badlogic.gdx.scenes.scene2d.InputEvent();
                    event.setType(com.badlogic.gdx.scenes.scene2d.InputEvent.Type.touchDown);
                    button.fire(event);
                    event.setType(com.badlogic.gdx.scenes.scene2d.InputEvent.Type.touchUp);
                    button.fire(event);
                    
                    // Dropdown should be added to stage
                    boolean dropdownFound = false;
                    for (com.badlogic.gdx.scenes.scene2d.Actor actor : stage.getActors()) {
                        if (actor instanceof com.badlogic.gdx.scenes.scene2d.ui.ScrollPane) {
                            dropdownFound = true;
                            assertTrue(actor.isVisible());
                            break;
                        }
                    }
                    assertTrue(dropdownFound, "Dropdown should be added to stage after click");
                    
                    // NEW: Verify dropdown stays open and handles interactions
                    com.badlogic.gdx.scenes.scene2d.ui.ScrollPane sp = null;
                    for (com.badlogic.gdx.scenes.scene2d.Actor actor : stage.getActors()) {
                        if (actor instanceof com.badlogic.gdx.scenes.scene2d.ui.ScrollPane) {
                            sp = (com.badlogic.gdx.scenes.scene2d.ui.ScrollPane) actor;
                            assertTrue(sp.isVisible(), "ScrollPane should be visible");
                            assertTrue(sp.getWidth() > 0, "ScrollPane width should be > 0");
                            assertTrue(sp.getHeight() > 0, "ScrollPane height should be > 0");
                            
                            com.badlogic.gdx.scenes.scene2d.Actor widget = sp.getActor();
                            assertNotNull(widget, "ScrollPane should have a widget");
                            assertTrue(widget.getWidth() > 0, "Widget width should be > 0");
                            assertTrue(widget.getHeight() > 0, "Widget height should be > 0");
                            
                            // Check if language list has items
                            com.badlogic.gdx.scenes.scene2d.ui.List<LanguageSelectBox.LanguageItem> list = 
                                (com.badlogic.gdx.scenes.scene2d.ui.List<LanguageSelectBox.LanguageItem>) widget;
                            assertTrue(list.getItems().size > 0, "Language list should have items");
                            
                            // Test programmatic selection change doesn't close dropdown
                            selectBox.setSelectedByLocale(Locale.GERMAN);
                            stage.act();
                            assertTrue(sp.isVisible() && sp.getParent() != null, "Programmatic selection change should NOT close the dropdown");
                            
                            // Test user selection DOES close dropdown
                            // Use selection.choose() which simulates user interaction and ignores programmaticChangeEvents flag
                            list.getSelection().choose(list.getItems().get(0));
                            stage.act();
                            
                            assertFalse(sp.isVisible() && sp.getParent() != null, "User selection SHOULD close the dropdown");
                        }
                    }

                    success.set(true);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finished.set(true);
                    Gdx.app.exit();
                }
            }
        }, new HeadlessApplicationConfiguration());

        long start = System.currentTimeMillis();
        while (!finished.get() && System.currentTimeMillis() - start < 5000) {
            Thread.sleep(100);
        }

        assertTrue(success.get(), "Test should complete successfully");
    }
}
