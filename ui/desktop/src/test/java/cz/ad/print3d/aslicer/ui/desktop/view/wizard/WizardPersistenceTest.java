package cz.ad.print3d.aslicer.ui.desktop.view.wizard;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import cz.ad.print3d.aslicer.logic.net.PrinterConnectionPool;
import cz.ad.print3d.aslicer.ui.desktop.GdxTestUtils;
import cz.ad.print3d.aslicer.ui.desktop.view.DesktopUI;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WizardPersistenceTest {

    @Test
    void testWizardPersistenceCallback() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger savedWidth = new AtomicInteger(0);
        AtomicInteger savedHeight = new AtomicInteger(0);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = GdxTestUtils.createTestSkin();
                    
                    DesktopUI ui = new DesktopUI() {
                        @Override
                        protected Skin createSkin() {
                            return skin;
                        }
                    };

                    // Open wizard with initial size 900x700
                    ui.togglePrinterDiscoveryWindow(new PrinterConnectionPool(), new cz.ad.print3d.aslicer.logic.printer.PrinterRepository() {
                        @Override public java.util.List<String> getGroups() { return java.util.Collections.emptyList(); }
                        @Override public java.util.Map<String, cz.ad.print3d.aslicer.logic.printer.Printer3D> getPrintersByGroup(String groupName) { return java.util.Collections.emptyMap(); }
                        @Override public java.util.Optional<cz.ad.print3d.aslicer.logic.printer.Printer3D> getPrinter(String groupName, String printerName) { return java.util.Optional.empty(); }
                        @Override public void savePrinter(String groupName, String printerName, cz.ad.print3d.aslicer.logic.printer.Printer3D printer) {}
                        @Override public boolean deletePrinter(String groupName, String printerName) { return false; }
                        @Override public boolean deleteGroup(String groupName) { return false; }
                    }, 900, 700, (w, h) -> {
                        savedWidth.set(w);
                        savedHeight.set(h);
                    });

                    Wizard wizard = ui.getPrinterWizard();
                    assertEquals(900, (int) wizard.getWidth());
                    assertEquals(700, (int) wizard.getHeight());

                    // Resize wizard manually (as if by user)
                    wizard.setSize(1000, 800);

                    // Cancel wizard, which should trigger callback
                    wizard.cancel();

                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        System.out.println("[DEBUG_LOG] Saved width: " + savedWidth.get());
        assertEquals(1000, savedWidth.get());
        assertEquals(800, savedHeight.get());
    }
}
