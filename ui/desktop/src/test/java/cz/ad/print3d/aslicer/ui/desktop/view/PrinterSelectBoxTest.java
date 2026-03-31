package cz.ad.print3d.aslicer.ui.desktop.view;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import cz.ad.print3d.aslicer.logic.printer.Printer3D;
import cz.ad.print3d.aslicer.logic.printer.PrinterRepository;
import cz.ad.print3d.aslicer.logic.printer.system.PrinterSystem;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class PrinterSelectBoxTest {

    @Test
    void testPrinterSelectBox() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    mockGdxGL();
                    Skin skin = createTestSkin();
                    DummyPrinterRepository repository = new DummyPrinterRepository();
                    
                    // Case 1: Empty repository
                    PrinterSelectBox selectBox = new PrinterSelectBox(skin, repository);
                    
                    assertEquals("No printers", selectBox.getSelectionText());
                    assertTrue(selectBox.getSelectedPrinters().isEmpty());
                    
                    // Case 2: Repository with printers
                    Printer3D p1 = createMockPrinter("Printer 1");
                    Printer3D p2 = createMockPrinter("Printer 2");
                    
                    repository.addGroup("Group 1");
                    repository.savePrinter("Group 1", "P1", p1);
                    repository.savePrinter("Group 1", "P2", p2);
                    
                    selectBox.refresh();
                    
                    assertEquals("None", selectBox.getSelectionText());
                    
                    // Select one
                    selectBox.selectPrinter(p1, true);
                    assertEquals("Printer 1", selectBox.getSelectionText());
                    assertEquals(1, selectBox.getSelectedPrinters().size());
                    
                    // Select multiple
                    selectBox.selectPrinter(p2, true);
                    assertEquals("2 printers", selectBox.getSelectionText());
                    assertEquals(2, selectBox.getSelectedPrinters().size());
                    
                    // Select none
                    selectBox.clearSelection();
                    assertEquals("None", selectBox.getSelectionText());
                    assertTrue(selectBox.getSelectedPrinters().isEmpty());

                } catch (Throwable t) {
                    errorRef.set(t);
                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }

            private void mockGdxGL() {
                Gdx.gl20 = (GL20) Proxy.newProxyInstance(
                    GL20.class.getClassLoader(),
                    new Class<?>[]{GL20.class},
                    (proxy, method, args) -> {
                        Class<?> returnType = method.getReturnType();
                        if (returnType == void.class) return null;
                        if (returnType == int.class) return 0;
                        if (returnType == boolean.class) return false;
                        if (returnType == float.class) return 0.0f;
                        if (returnType == String.class) return "";
                        if (returnType.isPrimitive()) {
                            if (returnType == long.class) return 0L;
                            if (returnType == double.class) return 0.0;
                            if (returnType == byte.class) return (byte) 0;
                            if (returnType == short.class) return (short) 0;
                            if (returnType == char.class) return '\0';
                        }
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

            private Printer3D createMockPrinter(String name) {
                PrinterSystem system = (PrinterSystem) Proxy.newProxyInstance(
                    PrinterSystem.class.getClassLoader(),
                    new Class<?>[]{PrinterSystem.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("getPrinterName")) return name;
                        if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                        if (method.getName().equals("equals")) return proxy == args[0];
                        if (method.getName().equals("toString")) return "PrinterSystem[" + name + "]";
                        return "";
                    }
                );
                return (Printer3D) Proxy.newProxyInstance(
                    Printer3D.class.getClassLoader(),
                    new Class<?>[]{Printer3D.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("getPrinterSystem")) return system;
                        if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                        if (method.getName().equals("equals")) return proxy == args[0];
                        if (method.getName().equals("toString")) return "Printer3D[" + name + "]";
                        return null;
                    }
                );
            }
        }, new HeadlessApplicationConfiguration());

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        if (errorRef.get() != null) {
            System.out.println("[DEBUG_LOG] Error during test: " + errorRef.get());
            errorRef.get().printStackTrace(System.out);
            fail("Error during PrinterSelectBox test: " + errorRef.get().getMessage(), errorRef.get());
        }
    }

    private static class DummyPrinterRepository implements PrinterRepository {
        private final Map<String, Map<String, Printer3D>> groups = new HashMap<>();

        @Override
        public List<String> getGroups() {
            return new ArrayList<>(groups.keySet());
        }

        @Override
        public Map<String, Printer3D> getPrintersByGroup(String groupName) {
            return groups.getOrDefault(groupName, Collections.emptyMap());
        }

        @Override
        public Optional<Printer3D> getPrinter(String groupName, String printerName) {
            return Optional.ofNullable(groups.getOrDefault(groupName, Collections.emptyMap()).get(printerName));
        }

        @Override
        public void savePrinter(String groupName, String printerName, Printer3D printer) {
            groups.computeIfAbsent(groupName, k -> new HashMap<>()).put(printerName, printer);
        }

        @Override
        public boolean deletePrinter(String groupName, String printerName) {
            return groups.containsKey(groupName) && groups.get(groupName).remove(printerName) != null;
        }

        @Override
        public boolean deleteGroup(String groupName) {
            return groups.remove(groupName) != null;
        }

        public void addGroup(String group) {
            groups.putIfAbsent(group, new HashMap<>());
        }
    }
}
