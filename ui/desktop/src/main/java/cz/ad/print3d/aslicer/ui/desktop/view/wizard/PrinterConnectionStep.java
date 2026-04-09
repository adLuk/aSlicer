package cz.ad.print3d.aslicer.ui.desktop.view.wizard;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import cz.ad.print3d.aslicer.logic.net.PrinterClient;
import cz.ad.print3d.aslicer.logic.net.PrinterClientFactory;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.printer.dto.Printer3DDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Wizard step for entering connection details for selected printers.
 * This step displays all selected printers from the previous step and provides
 * input fields for authentication or connection codes (e.g., Bambu access code).
 */
public class PrinterConnectionStep implements WizardStep {

    private final Skin skin;
    private final Table content;
    private final PrinterDiscoveryStep discoveryStep;
    private final Map<String, Map<String, TextField>> printerFields = new HashMap<>();
    private final Map<String, Map<String, String>> connectionCredentials = new HashMap<>();
    private final Map<String, Printer3DDto> validatedPrinters = new HashMap<>();
    private final Map<String, Label> statusLabels = new HashMap<>();
    private final Map<String, TextButton> validateButtons = new HashMap<>();
    private final Map<String, Button> detailButtons = new HashMap<>();
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
        List<DiscoveredDevice> selectedDevices = discoveryStep.getSelectedDevices();
        if (selectedDevices.isEmpty()) return false;
        
        for (DiscoveredDevice device : selectedDevices) {
            if (!validatedPrinters.containsKey(device.getIpAddress())) {
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
        printerFields.clear();
        statusLabels.clear();
        validateButtons.clear();
        detailButtons.clear();

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
            addDeviceCard(scrollTable, device);
        }

        ScrollPane scrollPane = new ScrollPane(scrollTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.getStyle().background = skin.newDrawable("white", new Color(0.15f, 0.15f, 0.15f, 1f));
        content.add(scrollPane).expand().fill();
    }

    private void addDeviceCard(Table parent, DiscoveredDevice device) {
        Table deviceCard = new Table();
        deviceCard.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.5f)));
        deviceCard.pad(15);
        
        String vendor = (device.getVendor() != null ? device.getVendor() : "Unknown");
        String displayName = vendor + " Printer";
        Label nameLabel = new Label(displayName, skin, skin.has("default-bold", Label.LabelStyle.class) ? "default-bold" : "default");
        deviceCard.add(nameLabel).left().expandX();
        deviceCard.add(new Label(device.getIpAddress(), skin)).right().row();

        deviceCard.add(new Image(skin.getDrawable("white"))).colspan(2).fillX().height(1).padTop(5).padBottom(10).row();

        Table inputTable = new Table();
        inputTable.left();
        Map<String, TextField> fields = new HashMap<>();

        if ("Bambu Lab".equalsIgnoreCase(vendor)) {
            String savedSerial = getSavedCredential(device.getIpAddress(), "serial", device.getName() != null ? device.getName() : "");
            String savedAccessCode = getSavedCredential(device.getIpAddress(), "accessCode", "");
            addTextField(inputTable, fields, "serial", "Serial Number:", savedSerial);
            addTextField(inputTable, fields, "accessCode", "Access Code:", savedAccessCode);
        } else {
            String savedApiKey = getSavedCredential(device.getIpAddress(), "apiKey", "");
            addTextField(inputTable, fields, "apiKey", "API Key:", savedApiKey);
        }
        printerFields.put(device.getIpAddress(), fields);
        deviceCard.add(inputTable).colspan(2).fillX().row();
        
        Table actionTable = new Table();
        actionTable.left();
        
        TextButton validateBtn = new TextButton("Validate", skin);
        validateButtons.put(device.getIpAddress(), validateBtn);
        actionTable.add(validateBtn).padRight(10);
        
        boolean isValidated = validatedPrinters.containsKey(device.getIpAddress());
        Label statusLabel = new Label(isValidated ? "Success" : "Not validated", skin);
        statusLabel.setColor(isValidated ? Color.GREEN : Color.LIGHT_GRAY);
        statusLabels.put(device.getIpAddress(), statusLabel);
        actionTable.add(statusLabel).expandX().left();
        
        Button detailBtn = new Button(skin);
        detailBtn.add(new Label("?", skin));
        detailBtn.setVisible(isValidated);
        
        if (isValidated) {
            Printer3DDto details = validatedPrinters.get(device.getIpAddress());
            if (details != null && details.getPrinterSystem() != null) {
                String info = details.getPrinterSystem().getPrinterManufacturer() + " " +
                             (details.getPrinterSystem().getPrinterModel() != null ? details.getPrinterSystem().getPrinterModel() : "");
                nameLabel.setText(info);
            }
        }
        detailButtons.put(device.getIpAddress(), detailBtn);
        actionTable.add(detailBtn).right();
        
        deviceCard.add(actionTable).colspan(2).fillX().padTop(10).row();
        
        validateBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                validateConnection(device, nameLabel);
            }
        });
        
        detailBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showDeviceDetails(device, validatedPrinters.get(device.getIpAddress()));
            }
        });

        parent.add(deviceCard).fillX().padBottom(15).row();
    }

    private void addTextField(Table table, Map<String, TextField> fields, String id, String labelText, String defaultValue) {
        table.add(new Label(labelText, skin)).padRight(10).left();
        TextField field = new TextField(defaultValue, skin);
        fields.put(id, field);
        table.add(field).width(200).padRight(20).left();
        
        field.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String ip = null;
                for (Map.Entry<String, Map<String, TextField>> entry : printerFields.entrySet()) {
                    if (entry.getValue().values().contains(field)) {
                        ip = entry.getKey();
                        break;
                    }
                }
                if (ip != null) {
                    connectionCredentials.computeIfAbsent(ip, k -> new HashMap<>()).put(id, field.getText());
                    validatedPrinters.remove(ip);
                    statusLabels.get(ip).setText("Not validated");
                    statusLabels.get(ip).setColor(Color.LIGHT_GRAY);
                    detailButtons.get(ip).setVisible(false);
                    if (wizard != null) wizard.updateButtons();
                }
            }
        });
    }

    private String getSavedCredential(String ip, String id, String defaultValue) {
        if (connectionCredentials.containsKey(ip)) {
            return connectionCredentials.get(ip).getOrDefault(id, defaultValue);
        }
        return defaultValue;
    }

    private void validateConnection(DiscoveredDevice device, Label nameLabel) {
        String ip = device.getIpAddress();
        Map<String, String> credentials = new HashMap<>();
        for (Map.Entry<String, TextField> entry : printerFields.get(ip).entrySet()) {
            credentials.put(entry.getKey(), entry.getValue().getText());
        }

        Label statusLabel = statusLabels.get(ip);
        TextButton validateBtn = validateButtons.get(ip);

        statusLabel.setText("Validating...");
        statusLabel.setColor(Color.YELLOW);
        validateBtn.setDisabled(true);

        PrinterClient client = PrinterClientFactory.createClient(device, credentials);
        if (client == null) {
            statusLabel.setText("Error: Unsupported vendor");
            statusLabel.setColor(Color.RED);
            validateBtn.setDisabled(false);
            return;
        }

        client.setCertificateValidationCallback(details -> {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            Gdx.app.postRunnable(() -> {
                Dialog dialog = new Dialog("Untrusted Certificate", skin) {
                    @Override
                    protected void result(Object object) {
                        future.complete((Boolean) object);
                    }
                };
                dialog.text("An untrusted (self-signed) certificate was discovered for " + ip + ":\n\n" + details + "\n\nDo you want to trust this certificate?");
                dialog.button("Yes", true);
                dialog.button("No", false);
                dialog.show(wizard.getStage());
            });
            return future;
        });

        client.connect()
                .thenCompose(v -> client.getDetails())
                .thenAccept(details -> {
                    Gdx.app.postRunnable(() -> {
                        validatedPrinters.put(ip, details);
                        statusLabel.setText("Success");
                        statusLabel.setColor(Color.GREEN);
                        validateBtn.setDisabled(false);
                        detailButtons.get(ip).setVisible(true);
                        
                        if (details.getPrinterSystem() != null) {
                            String info = details.getPrinterSystem().getPrinterManufacturer() + " " +
                                         (details.getPrinterSystem().getPrinterModel() != null ? details.getPrinterSystem().getPrinterModel() : "");
                            nameLabel.setText(info);
                        }
                        
                        if (wizard != null) wizard.updateButtons();
                    });
                    client.disconnect();
                })
                .exceptionally(ex -> {
                    Gdx.app.postRunnable(() -> {
                        statusLabel.setText("Failed: " + ex.getMessage());
                        statusLabel.setColor(Color.RED);
                        validateBtn.setDisabled(false);
                    });
                    client.disconnect();
                    return null;
                });
    }

    private void showDeviceDetails(DiscoveredDevice device, Printer3DDto details) {
        Dialog dialog = new Dialog("Device Details - " + device.getIpAddress(), skin) {
            @Override
            protected void result(Object object) {
                hide();
            }
        };

        Table contentTable = new Table();
        contentTable.pad(10).left();

        contentTable.add(new Label("Discovered Information", skin, "default", Color.YELLOW)).left().padBottom(5).row();
        contentTable.add(new Label("IP Address: " + device.getIpAddress(), skin)).left().padLeft(10).row();
        contentTable.add(new Label("Vendor: " + device.getVendor(), skin)).left().padLeft(10).row();
        contentTable.add(new Label("", skin)).row();

        if (details != null && details.getPrinterSystem() != null) {
            contentTable.add(new Label("Validated System Information", skin, "default", Color.GREEN)).left().padBottom(5).row();
            contentTable.add(new Label("Manufacturer: " + details.getPrinterSystem().getPrinterManufacturer(), skin)).left().padLeft(10).row();
            contentTable.add(new Label("Model: " + details.getPrinterSystem().getPrinterModel(), skin)).left().padLeft(10).row();
            contentTable.add(new Label("Firmware: " + details.getPrinterSystem().getFirmwareVersion(), skin)).left().padLeft(10).row();
        }

        ScrollPane scrollPane = new ScrollPane(contentTable, skin);
        dialog.getContentTable().add(scrollPane).expand().fill().minSize(400, 300);
        dialog.button("Close");
        dialog.show(wizard.getStage());
    }

    /**
     * Returns a map of connection codes for each selected printer IP.
     *
     * @return map of IP to code (API key or access code)
     */
    public Map<String, String> getConnectionCodes() {
        Map<String, String> codes = new HashMap<>();
        for (Map.Entry<String, Map<String, TextField>> entry : printerFields.entrySet()) {
            Map<String, TextField> fields = entry.getValue();
            if (fields.containsKey("apiKey")) {
                codes.put(entry.getKey(), fields.get("apiKey").getText().trim());
            } else if (fields.containsKey("accessCode")) {
                codes.put(entry.getKey(), fields.get("accessCode").getText().trim());
            }
        }
        return codes;
    }
}
