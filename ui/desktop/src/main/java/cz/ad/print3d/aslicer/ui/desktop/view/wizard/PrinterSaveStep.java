package cz.ad.print3d.aslicer.ui.desktop.view.wizard;
import cz.ad.print3d.aslicer.ui.desktop.I18N;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.printer.PrinterRepository;
import cz.ad.print3d.aslicer.logic.printer.dto.Printer3DDto;
import cz.ad.print3d.aslicer.logic.printer.system.net.PrinterNetConnection;
import cz.ad.print3d.aslicer.logic.printer.system.net.dto.BambuPrinterNetConnectionDto;
import cz.ad.print3d.aslicer.logic.printer.system.net.dto.NetworkPrinterNetConnectionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Wizard step for naming validated printers and assigning them to groups.
 * Saved printers are then stored in the {@link PrinterRepository}.
 *
 * @author Senior Architect
 * @since 1.0.0
 */
public class PrinterSaveStep implements WizardStep {

    private static final Logger logger = LoggerFactory.getLogger(PrinterSaveStep.class);

    private final Skin skin;
    private final Table content;
    private final PrinterConnectionStep connectionStep;
    private final PrinterRepository repository;
    private Wizard wizard;

    private final Map<String, TextField> nameFields = new HashMap<>();
    private final Map<String, TextField> groupFields = new HashMap<>();

    /**
     * Creates a new PrinterSaveStep.
     *
     * @param skin           the UI skin to use
     * @param connectionStep the connection step to get validated printers from
     * @param repository     the repository to save printers to
     */
    public PrinterSaveStep(Skin skin, PrinterConnectionStep connectionStep, PrinterRepository repository) {
        this.skin = skin;
        this.connectionStep = connectionStep;
        this.repository = repository;
        this.content = new Table();
        this.content.pad(10);
    }

    @Override
    public String getTitle() {
        return I18N.get("wizard.printer.save.title");
    }

    @Override
    public String getDescription() {
        return I18N.get("wizard.printer.save.description");
    }

    @Override
    public Actor getContent() {
        return content;
    }

    @Override
    public void onEnter(Wizard wizard) {
        this.wizard = wizard;
        buildLayout();
        updateWizardButtons();
    }

    @Override
    public void onExit(Wizard wizard) {
    }

    @Override
    public boolean isValid() {
        Map<String, Printer3DDto> validated = connectionStep.getValidatedPrinters();
        if (validated.isEmpty()) return false;
        
        for (String ip : validated.keySet()) {
            TextField nameField = nameFields.get(ip);
            if (nameField == null || nameField.getText().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isComplete() {
        return isValid();
    }

    private void buildLayout() {
        content.clear();
        nameFields.clear();
        groupFields.clear();

        Map<String, Printer3DDto> validated = connectionStep.getValidatedPrinters();
        Map<String, DiscoveredDevice> ipToDevice = connectionStep.getIpToDevice();

        if (validated.isEmpty()) {
            content.add(new Label(I18N.get("wizard.printer.save.noPrintersValidated"), skin, "default", com.badlogic.gdx.graphics.Color.ORANGE)).pad(20);
            return;
        }

        Table listTable = new Table();
        listTable.top().left();
        
        listTable.add(new Label(I18N.get("wizard.printer.save.ipAddress"), skin, "default", com.badlogic.gdx.graphics.Color.YELLOW)).pad(5).expandX();
        listTable.add(new Label(I18N.get("wizard.printer.save.deviceType"), skin, "default", com.badlogic.gdx.graphics.Color.YELLOW)).pad(5).expandX();
        listTable.add(new Label(I18N.get("wizard.printer.save.printerName"), skin, "default", com.badlogic.gdx.graphics.Color.YELLOW)).pad(5).expandX();
        listTable.add(new Label(I18N.get("wizard.printer.save.group"), skin, "default", com.badlogic.gdx.graphics.Color.YELLOW)).pad(5).expandX();
        listTable.row();

        for (Map.Entry<String, Printer3DDto> entry : validated.entrySet()) {
            String ip = entry.getKey();
            DiscoveredDevice device = ipToDevice.get(ip);

            listTable.add(new Label(ip, skin)).pad(5);
            listTable.add(new Label(device != null ? device.getVendor() : I18N.get("common.unknown"), skin)).pad(5);

            TextField nameField = new TextField(device != null && device.getName() != null ? device.getName() : I18N.format("wizard.printer.save.printerNameFormat", ip), skin);
            nameFields.put(ip, nameField);
            listTable.add(nameField).pad(5).width(150);
            
            nameField.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    updateWizardButtons();
                }
            });

            TextField groupField = new TextField(I18N.get("wizard.printer.save.defaultGroup"), skin);
            groupFields.put(ip, groupField);
            listTable.add(groupField).pad(5).width(120);
            
            listTable.row();
        }

        ScrollPane scrollPane = new ScrollPane(listTable, skin);
        scrollPane.setFadeScrollBars(false);
        content.add(scrollPane).expand().fill();
    }

    private void updateWizardButtons() {
        if (wizard != null) {
            wizard.updateButtons();
        }
    }

    /**
     * Saves all validated printers with their assigned names and groups.
     */
    public void savePrinters() {
        Map<String, Printer3DDto> validated = connectionStep.getValidatedPrinters();
        Map<String, String> codes = connectionStep.getConnectionCodes();

        for (Map.Entry<String, Printer3DDto> entry : validated.entrySet()) {
            String ip = entry.getKey();
            Printer3DDto printerDto = entry.getValue();
            
            String name = nameFields.get(ip).getText().trim();
            String group = groupFields.get(ip).getText().trim();
            if (group.isEmpty()) group = I18N.get("wizard.printer.save.defaultGroup");

            // Update the printer DTO with connection code before saving
            if (codes.containsKey(ip)) {
                updateConnectionCode(printerDto, codes.get(ip));
            }

            logger.info("Saving printer '{}' to group '{}'", name, group);
            repository.savePrinter(group, name, printerDto);
        }
    }

    private void updateConnectionCode(Printer3DDto printerDto, String code) {
        if (printerDto.getNetConnections() != null) {
            for (PrinterNetConnection connection : printerDto.getNetConnections().values()) {
                if (connection instanceof BambuPrinterNetConnectionDto) {
                    ((BambuPrinterNetConnectionDto) connection).setAccessCode(code);
                } else if (connection instanceof NetworkPrinterNetConnectionDto) {
                    ((NetworkPrinterNetConnectionDto) connection).setPairingCode(code);
                }
            }
        }
    }

    @Override
    public boolean processChange(ChangeListener.ChangeEvent event) {
        return false;
    }

    @Override
    public void dispose() {
    }
}
