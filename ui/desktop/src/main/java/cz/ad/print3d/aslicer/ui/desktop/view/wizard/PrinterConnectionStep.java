package cz.ad.print3d.aslicer.ui.desktop.view.wizard;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import cz.ad.print3d.aslicer.logic.net.PrinterClient;
import cz.ad.print3d.aslicer.logic.net.PrinterClientFactory;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.printer.dto.Printer3DDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(PrinterConnectionStep.class);

    /**
     * UI skin used for creating actors and styling.
     */
    private final Skin skin;

    /**
     * Main content table containing the list of discovered devices.
     */
    private final Table content;

    /**
     * Reference to the previous discovery step to access selected devices.
     */
    private final PrinterDiscoveryStep discoveryStep;

    /**
     * Mapping of device IP to a map of input fields (e.g., "accessCode", "apiKey").
     */
    private final Map<String, Map<String, TextField>> printerFields = new HashMap<>();

    /**
     * Mapping of device IP to a map of entered credentials for persistent storage during wizard session.
     */
    private final Map<String, Map<String, String>> connectionCredentials = new HashMap<>();

    /**
     * Mapping of device IP to validated printer details.
     */
    private final Map<String, Printer3DDto> validatedPrinters = new HashMap<>();

    /**
     * Mapping of device IP to status labels for UI feedback.
     */
    private final Map<String, Label> statusLabels = new HashMap<>();

    /**
     * Mapping of device IP to validation buttons.
     */
    private final Map<String, TextButton> validateButtons = new HashMap<>();

    /**
     * Mapping of device IP to detail buttons shown after successful validation.
     */
    private final Map<String, Button> detailButtons = new HashMap<>();

    /**
     * Mapping of device IP to discovered device metadata.
     */
    private final Map<String, DiscoveredDevice> ipToDevice = new HashMap<>();

    /**
     * The parent wizard managing the step lifecycle.
     */
    private Wizard wizard;

    /**
     * Application-scope connection pool for managing printer clients.
     */
    private final cz.ad.print3d.aslicer.logic.net.PrinterConnectionPool connectionPool;

    /**
     * Creates a new PrinterConnectionStep with the specified skin and discovery step.
     *
     * @param skin           the UI skin to use
     * @param discoveryStep  the previous step containing selected devices
     * @param connectionPool the connection pool to use for printer clients
     */
    public PrinterConnectionStep(Skin skin, PrinterDiscoveryStep discoveryStep, cz.ad.print3d.aslicer.logic.net.PrinterConnectionPool connectionPool) {
        this.skin = skin;
        this.discoveryStep = discoveryStep;
        this.connectionPool = connectionPool;
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
        // We no longer disconnect all clients on exit, as they are managed by the connectionPool
        // and might be needed in the next step or main application.
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

    /**
     * Clears and builds the UI layout for this wizard step.
     */
    private void buildLayout() {
        content.clear();
        printerFields.clear();
        statusLabels.clear();
        validateButtons.clear();
        detailButtons.clear();
        ipToDevice.clear();

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

    /**
     * Adds a card-like representation of a device to the layout, including
     * input fields and validation status.
     *
     * @param parent the parent table to add the card to
     * @param device the discovered device to display
     */
    private void addDeviceCard(Table parent, DiscoveredDevice device) {
        String ip = device.getIpAddress();
        ipToDevice.put(ip, device);
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
            String savedAccessCode = getSavedCredential(device.getIpAddress(), "accessCode");
            addTextField(inputTable, fields, "accessCode", "Access Code:", savedAccessCode, device);
        } else {
            String savedApiKey = getSavedCredential(device.getIpAddress(), "apiKey");
            addTextField(inputTable, fields, "apiKey", "API Key:", savedApiKey, device);
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
        updateValidateButtonState(ip);
    }

    /**
     * Helper method to add a labeled text field to a table.
     *
     * @param table        the parent table
     * @param fields       map to store the created text field
     * @param id           unique identifier for the field
     * @param labelText    text for the label
     * @param defaultValue initial value for the text field
     * @param device       the device this field belongs to
     */
    private void addTextField(Table table, Map<String, TextField> fields, String id, String labelText, String defaultValue, DiscoveredDevice device) {
        table.add(new Label(labelText, skin)).padRight(10).left();
        TextField field = new TextField(defaultValue, skin);
        fields.put(id, field);
        table.add(field).width(200).padRight(20).left();
        
        field.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String ip = device.getIpAddress();
                connectionCredentials.computeIfAbsent(ip, ignored -> new HashMap<>()).put(id, field.getText());
                validatedPrinters.remove(ip);
                statusLabels.get(ip).setText("Not validated");
                statusLabels.get(ip).setColor(Color.LIGHT_GRAY);
                detailButtons.get(ip).setVisible(false);
                updateValidateButtonState(ip);
                if (wizard != null) wizard.updateButtons();
            }
        });
    }

    /**
     * Updates the enabled state and color of the validation button based on the
     * presence of required connection information.
     *
     * @param ip the IP address of the device to update
     */
    private void updateValidateButtonState(String ip) {
        DiscoveredDevice device = ipToDevice.get(ip);
        if (device == null) return;
        
        Map<String, TextField> fields = printerFields.get(ip);
        if (fields == null) return;

        boolean ready;
        String vendor = device.getVendor();
        if ("Bambu Lab".equalsIgnoreCase(vendor)) {
            TextField accessCodeField = fields.get("accessCode");
            ready = accessCodeField != null && !accessCodeField.getText().trim().isEmpty();
        } else {
            TextField apiKeyField = fields.get("apiKey");
            ready = apiKeyField != null && !apiKeyField.getText().trim().isEmpty();
        }

        TextButton validateBtn = validateButtons.get(ip);
        if (validateBtn != null) {
            if (ready) {
                // Highlight color when ready to validate
                validateBtn.setColor(Color.ORANGE);
            } else {
                validateBtn.setColor(Color.WHITE);
            }
        }
    }

    /**
     * Retrieves previously entered credentials for a device IP from the current
     * wizard session, or returns an empty string if not found.
     *
     * @param ip the IP address of the device
     * @param id the credential identifier (e.g., "accessCode")
     * @return the saved credential value or an empty string
     */
    private String getSavedCredential(String ip, String id) {
        if (connectionCredentials.containsKey(ip)) {
            return connectionCredentials.get(ip).getOrDefault(id, "");
        }
        return "";
    }

    /**
     * Validates the connection to the specified device using the provided credentials.
     * On success, updates the UI with printer details. On failure, logs the error
     * and allows the user to view/copy details.
     *
     * @param device the device to validate
     * @param nameLabel label to update with the discovered printer name
     */
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
        statusLabel.clearListeners();
        validateBtn.setDisabled(true);

        // Disconnect old client if exists in the pool
        connectionPool.removeClient(ip);

        PrinterClient client = connectionPool.getOrCreateClient(device, credentials);
        if (client == null) {
            String msg = "Unsupported vendor: " + device.getVendor();
            statusLabel.setText("Error: " + msg + " (click for details)");
            statusLabel.setColor(Color.RED);
            statusLabel.clearListeners();
            statusLabel.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    showErrorDialog("Error", msg, null);
                }
            });
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

        client.setDetailsUpdateCallback(details -> Gdx.app.postRunnable(() -> {
            validatedPrinters.put(ip, details);
            if (details.getPrinterSystem() != null) {
                String info = details.getPrinterSystem().getPrinterManufacturer() + " " +
                             (details.getPrinterSystem().getPrinterModel() != null ? details.getPrinterSystem().getPrinterModel() : "");
                nameLabel.setText(info);
                
                if (details.getPrinterSystem().getFullReport() != null) {
                    statusLabel.setText("Success (Report received)");
                    statusLabel.setColor(Color.GREEN);
                } else if (details.getPrinterSystem().getSerialNumber() != null) {
                    statusLabel.setText("Success (Connected)");
                    statusLabel.setColor(Color.GREEN);
                }

                if (details.getPrinterSystem().getSerialNumber() != null) {
                    connectionCredentials.computeIfAbsent(ip, ignored -> new HashMap<>()).put("serial", details.getPrinterSystem().getSerialNumber());
                }
            }
            if (wizard != null) wizard.updateButtons();
        }));

        client.connect()
                .thenCompose(ignored -> client.getDetails())
                .thenAccept(details -> {
                    Gdx.app.postRunnable(() -> {
                        validatedPrinters.put(ip, details);
                        
                        if (details.getPrinterSystem() != null && details.getPrinterSystem().getFullReport() == null) {
                            statusLabel.setText("Success (Waiting for report...)");
                            statusLabel.setColor(Color.YELLOW);
                        } else {
                            statusLabel.setText("Success");
                            statusLabel.setColor(Color.GREEN);
                        }
                        
                        validateBtn.setDisabled(false);
                        validateBtn.setColor(Color.WHITE); // Reset highlight after success
                        detailButtons.get(ip).setVisible(true);
                        
                        if (details.getPrinterSystem() != null) {
                            String info = details.getPrinterSystem().getPrinterManufacturer() + " " +
                                         (details.getPrinterSystem().getPrinterModel() != null ? details.getPrinterSystem().getPrinterModel() : "");
                            nameLabel.setText(info);
                        }
                        
                        if (wizard != null) wizard.updateButtons();
                    });
                    // Do not disconnect immediately for Bambu Lab to receive reports
                    if (!"Bambu Lab".equalsIgnoreCase(device.getVendor())) {
                        connectionPool.removeClient(ip);
                    }
                })
                .exceptionally(ex -> {
                    logger.error("Connection validation failed for {}: {}", ip, ex.getMessage(), ex);
                    Gdx.app.postRunnable(() -> {
                        String msg = ex.getMessage();
                        if (ex instanceof java.util.concurrent.CompletionException && ex.getCause() != null) {
                            msg = ex.getCause().getMessage();
                        }
                        statusLabel.setText("Failed: " + msg + " (click for details)");
                        statusLabel.setColor(Color.RED);
                        statusLabel.clearListeners();
                        String finalMsg = msg;
                        statusLabel.addListener(new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                showErrorDialog("Connection Error", "Failed to connect to " + ip + ":\n\n" + finalMsg, ex);
                            }
                        });
                        validateBtn.setDisabled(false);
                    });
                    connectionPool.removeClient(ip);
                    return null;
                });
    }

    /**
     * Shows a dialog with detailed information about the discovered printer.
     *
     * @param device the discovered device
     * @param details validated printer details
     */
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
            contentTable.add(new Label("", skin)).row();
            contentTable.add(new Image(skin.newDrawable("white", new Color(0.5f, 0.5f, 0.5f, 0.5f)))).fillX().height(1).padTop(5).padBottom(10).row();
            
            contentTable.add(new Label("Validated System Information", skin, "default", Color.GREEN)).left().padBottom(5).row();
            contentTable.add(new Label("Manufacturer: " + details.getPrinterSystem().getPrinterManufacturer(), skin)).left().padLeft(10).row();
            contentTable.add(new Label("Model: " + details.getPrinterSystem().getPrinterModel(), skin)).left().padLeft(10).row();
            contentTable.add(new Label("Software: " + details.getPrinterSystem().getFirmwareVersion(), skin)).left().padLeft(10).row();
            if (details.getPrinterSystem().getHardwareVersion() != null) {
                contentTable.add(new Label("Hardware: " + details.getPrinterSystem().getHardwareVersion(), skin)).left().padLeft(10).row();
            }
            
            contentTable.add(new Label("", skin)).row();
            contentTable.add(new Image(skin.newDrawable("white", new Color(0.5f, 0.5f, 0.5f, 0.5f)))).fillX().height(1).padTop(5).padBottom(10).row();
            contentTable.add(new Label("Full Report:", skin, "default", Color.YELLOW)).left().padBottom(5).row();
            
            if (details.getPrinterSystem().getFullReport() != null && !details.getPrinterSystem().getFullReport().isEmpty()) {
                Label reportLabel = new Label(details.getPrinterSystem().getFullReport(), skin);
                reportLabel.setWrap(true);
                contentTable.add(reportLabel).left().padLeft(10).width(380).row();
            } else {
                contentTable.add(new Label("No raw data received from printer yet.", skin, "default", Color.ORANGE)).left().padLeft(10).row();
            }
        }

        ScrollPane scrollPane = new ScrollPane(contentTable, skin);
        dialog.getContentTable().add(scrollPane).expand().fill().minSize(400, 300);
        dialog.button("Close");
        dialog.show(wizard.getStage());
    }

    /**
     * Shows an error dialog with the provided message and optional exception details.
     * Provides a button to copy the error message and stacktrace to the clipboard.
     *
     * @param title the dialog title
     * @param message the error message to display
     * @param ex optional exception to include stacktrace
     */
    private void showErrorDialog(String title, String message, Throwable ex) {
        Dialog dialog = new Dialog(title, skin);

        Table contentTable = dialog.getContentTable();
        contentTable.pad(20);
        Label label = new Label(message, skin);
        label.setWrap(true);
        label.setAlignment(com.badlogic.gdx.utils.Align.center);
        contentTable.add(label).width(450).row();

        TextButton copyBtn = new TextButton("Copy to Clipboard", skin);
        copyBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                StringBuilder fullError = new StringBuilder(message);
                if (ex != null) {
                    fullError.append("\n\nStacktrace:\n");
                    java.io.StringWriter sw = new java.io.StringWriter();
                    java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                    ex.printStackTrace(pw);
                    fullError.append(sw);
                }
                Gdx.app.getClipboard().setContents(fullError.toString());
                copyBtn.setText("Copied!");
            }
        });

        dialog.getButtonTable().add(copyBtn).pad(10);
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

    /**
     * Allow interaction of Wizard step with events processed and update Wizard internal state based on changes
     * in step.
     *
     * @param event event filtered by Wizard to be this specific type.
     * @return true in case when event was processed and processing is finished, otherwise false.
     */
    @Override
    public boolean processChange(ChangeListener.ChangeEvent event) {
        return false;
    }

    @Override
    public void dispose() {
        // In a real app, we might want to keep the pool alive beyond the wizard
        // but if the whole UI is disposing, we might want to clear it.
        // For now, we leave it to the application scope.
    }
}
