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
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import cz.ad.print3d.aslicer.logic.printer.PrinterRepository;
import cz.ad.print3d.aslicer.ui.desktop.GdxTestUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class PrinterToolbarTest {

    @Test
    void testPrinterSelectorInToolbar() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = createTestSkin();
                    PrinterRepository repository = createMockRepository();
                    
                    AppToolbar toolbar = new AppToolbar(skin, null, repository);
                    
                    assertNotNull(toolbar);
                    
                    // Validate groups presence and order
                    // Expected: modelGroup (index 0), printerGroup (index 1), spacer (index 2), settingsGroup (index 3)
                    Actor[] children = toolbar.getChildren().begin();
                    int childCount = toolbar.getChildren().size;
                    
                    assertTrue(childCount >= 3, "Toolbar should have at least 3 children (groups + spacer)");
                    
                    // 1. Model Group
                    assertTrue(children[0] instanceof ToolbarGroup, "First child should be ToolbarGroup (modelGroup)");
                    
                    // 2. Printer Group
                    assertTrue(children[1] instanceof ToolbarGroup, "Second child should be ToolbarGroup (printerGroup)");
                    ToolbarGroup printerGroup = (ToolbarGroup) children[1];
                    
                    // Verify PrinterSelectBox is in printerGroup
                    boolean foundPrinterSelectBox = false;
                    for (Actor actor : printerGroup.getButtonContainer().getChildren()) {
                        if (actor instanceof PrinterSelectBox) {
                            foundPrinterSelectBox = true;
                            break;
                        }
                    }
                    assertTrue(foundPrinterSelectBox, "PrinterSelectBox should be present in the printerGroup");
                    
                    // 3. Settings Group should be at the end (after spacer)
                    assertTrue(children[childCount - 1] instanceof ToolbarGroup, "Last child should be ToolbarGroup (settingsGroup)");

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
        }, new HeadlessApplicationConfiguration());

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        if (errorRef.get() != null) {
            errorRef.get().printStackTrace();
            fail("Error during test: " + errorRef.get().getMessage());
        }
    }
}
