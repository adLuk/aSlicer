package cz.ad.print3d.aslicer.ui.desktop.view.wizard;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wizard step for entering connection details for selected printers.
 * This step displays all selected printers from the previous step and provides
 * input fields for authentication or connection codes (e.g., Bambu access code).
 */
public class PrinterConnectionStep implements WizardStep {

    private final Skin skin;
    private final Table content;
    private final PrinterDiscoveryStep discoveryStep;
    private final Map<String, TextField> codeFields = new HashMap<>();
    private Wizard wizard;

    public PrinterConnectionStep(Skin skin, PrinterDiscoveryStep discoveryStep) {
        this.skin = skin;
        this.discoveryStep = discoveryStep;
        this.content = new Table();
        this.content.pad(10);
    }

    @Override
    public String getTitle() {
        return "Connection Details";
    }

    @Override
    public String getDescription() {
        return "Enter the access codes for your selected printers. This information is necessary to establish a secure communication channel.";
    }

    @Override
    public Actor getContent() {
        return content;
    }

    @Override
    public void onEnter(Wizard wizard) {
        this.wizard = wizard;
        buildLayout();
    }

    @Override
    public void onExit(Wizard wizard) {
    }

    @Override
    public boolean isValid() {
        for (TextField field : codeFields.values()) {
            if (field.getText().trim().isEmpty()) {
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
        codeFields.clear();

        List<DiscoveredDevice> selectedDevices = discoveryStep.getSelectedDevices();
        if (selectedDevices.isEmpty()) {
            Table emptyTable = new Table();
            emptyTable.add(new Label("No printers selected in the previous step.", skin)).row();
            content.add(emptyTable).center().expand();
            return;
        }

        Table scrollTable = new Table();
        scrollTable.top().left();
        scrollTable.pad(10);

        for (DiscoveredDevice device : selectedDevices) {
            Table deviceCard = new Table();
            deviceCard.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.5f)));
            deviceCard.pad(15);
            
            String displayName = (device.getVendor() != null ? device.getVendor() : "Unknown") + " Printer";
            deviceCard.add(new Label(displayName, skin, skin.has("default-bold", Label.LabelStyle.class) ? "default-bold" : "default")).left().expandX();
            deviceCard.add(new Label(device.getIpAddress(), skin)).right().row();

            deviceCard.add(new Image(skin.getDrawable("white"))).colspan(2).fillX().height(1).padTop(5).padBottom(10).row();

            Table inputTable = new Table();
            inputTable.add(new Label("Access Code:", skin)).padRight(15);
            
            TextField codeField = new TextField("", skin);
            codeField.setMessageText("Enter code (e.g. for Bambu)");
            codeField.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (wizard != null) {
                        wizard.updateButtons();
                    }
                }
            });
            inputTable.add(codeField).width(250).expandX().fillX();
            
            codeFields.put(device.getIpAddress(), codeField);
            deviceCard.add(inputTable).colspan(2).fillX().row();
            
            scrollTable.add(deviceCard).fillX().padBottom(15).row();
        }

        ScrollPane scrollPane = new ScrollPane(scrollTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.getStyle().background = skin.newDrawable("white", new Color(0.15f, 0.15f, 0.15f, 1f));
        content.add(scrollPane).expand().fill();
    }

    /**
     * Returns a map of connection codes for each selected printer IP.
     *
     * @return map of IP to code
     */
    public Map<String, String> getConnectionCodes() {
        Map<String, String> codes = new HashMap<>();
        for (Map.Entry<String, TextField> entry : codeFields.entrySet()) {
            codes.put(entry.getKey(), entry.getValue().getText().trim());
        }
        return codes;
    }
}
