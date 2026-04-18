package cz.ad.print3d.aslicer.ui.desktop.view.wizard;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.net.PrinterConnectionPool;
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

                    PrinterConnectionStep connectionStep = new PrinterConnectionStep(skin, mockDiscoveryStep, new PrinterConnectionPool());
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

    @Test
    void testMissingButtonStyleThrowsException() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = GdxTestUtils.createTestSkin();
                    // Remove ButtonStyle to simulate the bug
                    skin.remove("default", Button.ButtonStyle.class);

                    DiscoveredDevice device = new DiscoveredDevice("192.168.1.100");
                    device.setSelected(true);

                    PrinterDiscoveryStep mockDiscoveryStep = new PrinterDiscoveryStep(skin) {
                        @Override
                        public List<DiscoveredDevice> getSelectedDevices() {
                            return Collections.singletonList(device);
                        }
                    };

                    PrinterConnectionStep connectionStep = new PrinterConnectionStep(skin, mockDiscoveryStep, new PrinterConnectionPool());
                    
                    // This should throw GdxRuntimeException
                    assertThrows(com.badlogic.gdx.utils.GdxRuntimeException.class, () -> {
                        connectionStep.onEnter(new Wizard("Test", skin));
                    });

                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
    @Test
    void testUnsupportedVendorShowsErrorWithClickDetails() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = GdxTestUtils.createTestSkin();

                    DiscoveredDevice device = new DiscoveredDevice("192.168.1.100");
                    device.setVendor("Unknown Vendor");
                    device.setSelected(true);

                    PrinterDiscoveryStep mockDiscoveryStep = new PrinterDiscoveryStep(skin) {
                        @Override
                        public List<DiscoveredDevice> getSelectedDevices() {
                            return Collections.singletonList(device);
                        }
                    };

                    PrinterConnectionStep connectionStep = new PrinterConnectionStep(skin, mockDiscoveryStep, new PrinterConnectionPool());
                    Wizard wizard = new Wizard("Test", skin);
                    wizard.addStep(mockDiscoveryStep);
                    wizard.addStep(connectionStep);
                    wizard.setStep(1);

                    // Find the validate button for this IP
                    Table content = (Table) connectionStep.getContent();
                    com.badlogic.gdx.scenes.scene2d.ui.ScrollPane scrollPane = (com.badlogic.gdx.scenes.scene2d.ui.ScrollPane) content.getChildren().get(0);
                    Table scrollTable = (Table) scrollPane.getActor();
                    
                    Button validateBtn = null;
                    Label statusLabel = null;
                    
                    for (com.badlogic.gdx.scenes.scene2d.Actor actor : scrollTable.getChildren()) {
                        if (actor instanceof Table deviceCard) {
                            for (com.badlogic.gdx.scenes.scene2d.Actor cardChild : deviceCard.getChildren()) {
                                if (cardChild instanceof Table actionTable) {
                                    for (com.badlogic.gdx.scenes.scene2d.Actor actionChild : actionTable.getChildren()) {
                                        if (actionChild instanceof com.badlogic.gdx.scenes.scene2d.ui.TextButton btn && "Validate".equals(btn.getText().toString())) {
                                            validateBtn = btn;
                                        } else if (actionChild instanceof Label lbl) {
                                            statusLabel = lbl;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    assertNotNull(validateBtn);
                    assertNotNull(statusLabel);
                    
                    // Trigger validation (it will fail because vendor is unknown)
                    validateBtn.toggle(); // This triggers the ChangeListener
                    
                    assertTrue(statusLabel.getText().toString().contains("Unsupported vendor"));
                    assertTrue(statusLabel.getText().toString().contains("(click for details)"));
                    // ClickListener is added in validateConnection
                    assertEquals(1, statusLabel.getListeners().size);

                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
