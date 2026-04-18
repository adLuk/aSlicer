package cz.ad.print3d.aslicer.ui.desktop.view.wizard;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.printer.PrinterRepository;
import cz.ad.print3d.aslicer.logic.printer.dto.Printer3DDto;
import cz.ad.print3d.aslicer.ui.desktop.GdxTestUtils;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PrinterSaveStepTest {

    @Test
    public void testPrinterSaveStepFlow() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = GdxTestUtils.createTestSkin();

                    PrinterDiscoveryStep discoveryStep = new PrinterDiscoveryStep(skin);
                    cz.ad.print3d.aslicer.logic.net.PrinterConnectionPool pool = new cz.ad.print3d.aslicer.logic.net.PrinterConnectionPool();
                    PrinterConnectionStep connectionStep = new PrinterConnectionStep(skin, discoveryStep, pool);
                    
                    final Map<String, cz.ad.print3d.aslicer.logic.printer.Printer3D> savedPrinters = new HashMap<>();
                    PrinterRepository repository = new PrinterRepository() {
                        @Override public java.util.List<String> getGroups() { return Collections.emptyList(); }
                        @Override public Map<String, cz.ad.print3d.aslicer.logic.printer.Printer3D> getPrintersByGroup(String groupName) { return Collections.emptyMap(); }
                        @Override public java.util.Optional<cz.ad.print3d.aslicer.logic.printer.Printer3D> getPrinter(String groupName, String printerName) { return java.util.Optional.empty(); }
                        @Override public void savePrinter(String groupName, String printerName, cz.ad.print3d.aslicer.logic.printer.Printer3D printer) { savedPrinters.put(groupName + ":" + printerName, printer); }
                        @Override public boolean deletePrinter(String groupName, String printerName) { return false; }
                        @Override public boolean deleteGroup(String groupName) { return false; }
                    };
                    
                    PrinterSaveStep saveStep = new PrinterSaveStep(skin, connectionStep, repository);

                    assertEquals("Save Printers", saveStep.getTitle());
                    assertNotNull(saveStep.getDescription());

                    // Manually populate data in connection step
                    Printer3DDto dto = new Printer3DDto();
                    connectionStep.getValidatedPrinters().put("192.168.1.100", dto);
                    
                    DiscoveredDevice device = new DiscoveredDevice("192.168.1.100");
                    device.setName("MyPrinter");
                    connectionStep.getIpToDevice().put("192.168.1.100", device);

                    Wizard wizard = new Wizard("Test Wizard", skin);
                    wizard.addStep(discoveryStep);
                    wizard.addStep(connectionStep);
                    wizard.addStep(saveStep);
                    
                    wizard.setStep(2); // Enter Save Step
                    
                    // Should be valid with default values
                    assertTrue(saveStep.isValid());

                    saveStep.savePrinters();

                    assertTrue(savedPrinters.containsKey("Default:MyPrinter"));
                    assertSame(dto, savedPrinters.get("Default:MyPrinter"));
                    
                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
