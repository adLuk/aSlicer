package cz.ad.print3d.aslicer.ui.desktop.view.wizard;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.ui.desktop.GdxTestUtils;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class PrinterConnectionStepTest {

    @Test
    void testConnectionDetailsInput() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = GdxTestUtils.createTestSkin();

                    DiscoveredDevice device = new DiscoveredDevice("192.168.1.100");
                    device.setVendor("Bambu Lab");
                    device.setSelected(true);

                    PrinterDiscoveryStep mockDiscoveryStep = new PrinterDiscoveryStep(skin) {
                        @Override
                        public List<DiscoveredDevice> getSelectedDevices() {
                            return Collections.singletonList(device);
                        }
                    };

                    PrinterConnectionStep connectionStep = new PrinterConnectionStep(skin, mockDiscoveryStep);
                    Wizard wizard = new Wizard("Test", skin);
                    wizard.addStep(mockDiscoveryStep);
                    wizard.addStep(connectionStep);

                    // Move to connection step
                    wizard.setStep(1);

                    // Should not be valid because validation is required
                    assertFalse(connectionStep.isValid());
                    assertFalse(connectionStep.isComplete());

                    // Check if codes are empty initially
                    Map<String, String> codes = connectionStep.getConnectionCodes();
                    assertEquals(1, codes.size());
                    assertEquals("", codes.get("192.168.1.100"));

                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
