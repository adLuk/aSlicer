package cz.ad.print3d.aslicer.ui.desktop.view;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import cz.ad.print3d.aslicer.logic.net.PrinterConnectionPool;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.printer.Printer3D;
import cz.ad.print3d.aslicer.logic.printer.PrinterRepository;
import cz.ad.print3d.aslicer.logic.printer.dto.Printer3DDto;
import cz.ad.print3d.aslicer.ui.desktop.GdxTestUtils;
import cz.ad.print3d.aslicer.ui.desktop.model.ModelManager;
import cz.ad.print3d.aslicer.ui.desktop.view.wizard.PrinterConnectionStep;
import cz.ad.print3d.aslicer.ui.desktop.view.wizard.Wizard;
import cz.ad.print3d.aslicer.ui.desktop.view.wizard.WizardStep;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PrinterSelectBoxRefreshTest {

    @Test
    public void testPrinterSelectBoxRefreshesAfterWizardFinishes() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    DesktopUI ui = new DesktopUI();
                    Skin skin = ui.getSkin();

                    final Map<String, Map<String, Printer3D>> storage = new HashMap<>();
                    PrinterRepository repository = new PrinterRepository() {
                        @Override public List<String> getGroups() { return new ArrayList<>(storage.keySet()); }
                        @Override public Map<String, Printer3D> getPrintersByGroup(String groupName) { return storage.getOrDefault(groupName, Collections.emptyMap()); }
                        @Override public Optional<Printer3D> getPrinter(String groupName, String printerName) { return Optional.ofNullable(getPrintersByGroup(groupName).get(printerName)); }
                        @Override public void savePrinter(String groupName, String printerName, Printer3D printer) {
                            storage.computeIfAbsent(groupName, k -> new HashMap<>()).put(printerName, printer);
                        }
                        @Override public boolean deletePrinter(String groupName, String printerName) { return false; }
                        @Override public boolean deleteGroup(String groupName) { return false; }
                    };
                    
                    PrinterRepository spyRepository = Mockito.spy(repository);

                    AppToolbar.ToolbarListener listener = mock(AppToolbar.ToolbarListener.class);
                    AppToolbar toolbar = new AppToolbar(skin, listener, spyRepository);
                    
                    ui.setupLayout(toolbar, mock(AppStageToolbar.class), mock(AppSideToolbar.class), mock(SceneManager.class), mock(ModelManager.class));

                    // Verify initial call from PrinterSelectBox constructor
                    verify(spyRepository, atLeastOnce()).getGroups();
                    Mockito.reset(spyRepository);

                    ui.togglePrinterDiscoveryWindow(new PrinterConnectionPool(), spyRepository, 800, 600, null);
                    Wizard wizard = ui.getPrinterWizard();
                    assertNotNull(wizard);

                    List<WizardStep> steps = wizard.getSteps();
                    PrinterConnectionStep connectionStep = (PrinterConnectionStep) steps.get(1);
                    
                    Printer3DDto dto = new Printer3DDto();
                    connectionStep.getValidatedPrinters().put("192.168.1.100", dto);
                    DiscoveredDevice device = new DiscoveredDevice("192.168.1.100");
                    device.setName("TestPrinter");
                    connectionStep.getIpToDevice().put("192.168.1.100", device);
                    
                    wizard.setStep(2);
                    assertTrue(wizard.getCurrentStep().isValid(), "Save step should be valid");

                    // Now finish the wizard
                    wizard.finish();
                    
                    // Check if printer is in storage
                    assertTrue(storage.get("Default").containsKey("TestPrinter"), "Printer should be saved in repository");
                    
                    // Check if toolbar was refreshed (it should call getGroups() again)
                    verify(spyRepository, atLeastOnce()).getGroups();
                    
                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
