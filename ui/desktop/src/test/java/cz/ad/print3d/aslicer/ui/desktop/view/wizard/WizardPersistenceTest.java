package cz.ad.print3d.aslicer.ui.desktop.view.wizard;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
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
                    ui.togglePrinterDiscoveryWindow(900, 700, (w, h) -> {
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
        assertEquals(1000, savedWidth.get());
        assertEquals(800, savedHeight.get());
    }
}
