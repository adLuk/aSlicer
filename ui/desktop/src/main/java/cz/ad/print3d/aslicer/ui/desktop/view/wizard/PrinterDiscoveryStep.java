package cz.ad.print3d.aslicer.ui.desktop.view.wizard;
import cz.ad.print3d.aslicer.ui.desktop.I18N;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import cz.ad.print3d.aslicer.logic.net.info.NetworkAddressInfo;
import cz.ad.print3d.aslicer.logic.net.info.NetworkInformationCollector;
import cz.ad.print3d.aslicer.logic.net.info.NetworkInterfaceInfo;
import cz.ad.print3d.aslicer.logic.net.scanner.NettyNetworkScanner;
import cz.ad.print3d.aslicer.logic.net.scanner.NetworkScanner;
import cz.ad.print3d.aslicer.logic.net.scanner.ScanConfigurationLoader;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.ScanConfiguration;
import cz.ad.print3d.aslicer.logic.net.util.IpUtils;
import cz.ad.print3d.aslicer.ui.desktop.DesktopApp;
import cz.ad.print3d.aslicer.ui.desktop.view.DeviceRow;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Wizard step for discovering 3D printers on the network.
 * This step allows the user to define an IP range and perform a network scan to identify
 * available 3D printers and their services.
 */
public class PrinterDiscoveryStep implements WizardStep {

    /**
     * UI skin used for creating actors and styling.
     */
    private final Skin skin;

    /**
     * Main content table containing the UI elements.
     */
    private final Table content;

    /**
     * Text field for the starting IP address of the scan range.
     */
    private final TextField startIpField;

    /**
     * Text field for the ending IP address of the scan range.
     */
    private final TextField endIpField;

    /**
     * Checkbox to enable deep scanning (checking all potential printer ports).
     */
    private final CheckBox deepScanCheckBox;

    /**
     * Checkbox to include the local machine's IP address in the scan.
     */
    private final CheckBox includeSelfIpCheckBox;

    /**
     * Text field for the scan timeout in milliseconds.
     */
    private final TextField timeoutField;

    /**
     * Label to display the current scan status and progress.
     */
    private final Label progressLabel;

    /**
     * List of UI rows representing discovered devices.
     */
    private final List<DeviceRow> rows = new ArrayList<>();

    /**
     * Table containing the list of discovered devices.
     */
    private final Table resultsTable;

    /**
     * Network scanner implementation for identifying printers.
     */
    private final NetworkScanner scanner;

    /**
     * Collector for local network interface information.
     */
    private final NetworkInformationCollector collector;

    /**
     * Current scan configuration loaded from the environment or defaults.
     */
    private final ScanConfiguration scanConfig;

    /**
     * Button to start or stop the network scan.
     */
    private final ImageButton searchButton;

    /**
     * Icon displayed when no scan is active.
     */
    private final Drawable searchIcon;

    /**
     * Icon displayed when a scan is currently running.
     */
    private final Drawable stopIcon;

    /**
     * Flag indicating whether a network scan is currently in progress.
     */
    private boolean isScanning = false;

    /**
     * The parent wizard managing the step lifecycle.
     */
    private Wizard wizard;

    /**
     * Creates a new PrinterDiscoveryStep with the specified skin and default components.
     *
     * @param skin the UI skin to use
     */
    public PrinterDiscoveryStep(Skin skin) {
        this(skin, new NettyNetworkScanner(), new NetworkInformationCollector(), ScanConfigurationLoader.loadDefault());
    }

    /**
     * Creates a new PrinterDiscoveryStep with full control over its components.
     *
     * @param skin      the UI skin to use
     * @param scanner   the network scanner implementation
     * @param collector the network information collector
     * @param config    the scan configuration
     */
    public PrinterDiscoveryStep(Skin skin, NetworkScanner scanner, NetworkInformationCollector collector, ScanConfiguration config) {
        this.skin = skin;
        this.scanner = scanner;
        this.collector = collector;
        this.scanConfig = config;

        content = new Table();
        content.pad(15);

        // IP Range and Settings section
        Table settingsTable = new Table();
        settingsTable.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.5f)));
        settingsTable.pad(15);

        // Row 1: IP Range and Search Button
        Table ipRow = new Table();
        ipRow.add(new Label(I18N.get("wizard.printer.discovery.scanRange"), skin, skin.has("default-bold", Label.LabelStyle.class) ? "default-bold" : "default")).left().colspan(5).padBottom(10).row();
        
        ipRow.add(new Label(I18N.get("printerdiscovery.startIp"), skin)).padRight(10);
        startIpField = new TextField("", skin);
        ipRow.add(startIpField).width(130).padRight(20);

        ipRow.add(new Label(I18N.get("printerdiscovery.endIp"), skin)).padRight(10);
        endIpField = new TextField("", skin);
        ipRow.add(endIpField).width(130).padRight(20);

        searchIcon = createSearchIcon();
        stopIcon = createStopIcon();
        searchButton = new ImageButton(createButtonStyle(searchIcon));
        searchButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (isScanning) {
                    stopScan();
                } else {
                    startScan();
                }
            }
        });
        ipRow.add(searchButton).size(40);
        settingsTable.add(ipRow).left().fillX().row();

        // Row 2: Checkboxes and Timeout
        Table optionsRow = new Table();
        optionsRow.padTop(10);
        
        deepScanCheckBox = new CheckBox(I18N.get("printerdiscovery.deepScan"), skin);
        optionsRow.add(deepScanCheckBox).padRight(20);

        includeSelfIpCheckBox = new CheckBox(I18N.get("printerdiscovery.includeSelfIp"), skin);
        includeSelfIpCheckBox.setChecked(false);
        optionsRow.add(includeSelfIpCheckBox).padRight(20);

        optionsRow.add(new Label(I18N.get("printerdiscovery.timeout"), skin)).padRight(10);
        timeoutField = new TextField("500", skin);
        timeoutField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
        optionsRow.add(timeoutField).width(80);
        
        settingsTable.add(optionsRow).left().row();

        // Row 3: Manual IP Entry
        Table manualRow = new Table();
        manualRow.padTop(10);
        manualRow.add(new Label(I18N.get("wizard.printer.discovery.manualIp"), skin)).padRight(10);
        TextField manualIpField = new TextField("", skin);
        manualRow.add(manualIpField).width(130).padRight(10);
        TextButton manualAddButton = new TextButton(I18N.get("wizard.printer.discovery.add"), skin);
        manualAddButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String ip = manualIpField.getText().trim();
                if (!ip.isEmpty() && IpUtils.isValidIp(ip)) {
                    DiscoveredDevice device = new DiscoveredDevice(ip);
                    // Add standard Bambu port if not found by scan
                    device.addService(new PortScanResult(8883, true));
                    addDiscoveredDevice(device);
                    manualIpField.setText("");
                }
            }
        });
        manualRow.add(manualAddButton).width(60);
        settingsTable.add(manualRow).left().row();

        content.add(settingsTable).fillX().padBottom(15).row();

        // Progress section
        Table progressTable = new Table();
        progressLabel = new Label(I18N.get("wizard.printer.discovery.readyToScan"), skin);
        progressLabel.setColor(Color.LIGHT_GRAY);
        progressTable.add(new Label(I18N.get("wizard.printer.discovery.status"), skin, skin.has("default-bold", Label.LabelStyle.class) ? "default-bold" : "default")).left();
        progressTable.add(progressLabel).left().expandX();
        content.add(progressTable).fillX().padBottom(10).row();

        // Results section
        resultsTable = new Table();
        resultsTable.top().left();
        
        Table resultsHeader = new Table();
        resultsHeader.setBackground(skin.newDrawable("white", new Color(0.3f, 0.3f, 0.3f, 1f)));
        resultsHeader.add(new Label(I18N.get("wizard.printer.discovery.header"), skin, skin.has("default-bold", Label.LabelStyle.class) ? "default-bold" : "default")).left().expandX().pad(5);
        content.add(resultsHeader).fillX().row();
        
        ScrollPane scrollPane = new ScrollPane(resultsTable, skin);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        
        // Add a background to the scrollpane to distinguish it
        scrollPane.getStyle().background = skin.newDrawable("white", new Color(0.15f, 0.15f, 0.15f, 1f));
        
        content.add(scrollPane).expand().fill().row();

        initializeIpRange();
    }

    @Override
    public String getTitle() {
        return I18N.get("printerdiscovery.title");
    }

    @Override
    public String getDescription() {
        return I18N.get("wizard.printer.discovery.description");
    }

    @Override
    public Actor getContent() {
        return content;
    }

    @Override
    public void onEnter(Wizard wizard) {
        this.wizard = wizard;
        updateWizardButtons();
    }

    @Override
    public void onExit(Wizard wizard) {
        stopScan();
    }

    @Override
    public boolean isValid() {
        return !getSelectedDevices().isEmpty();
    }

    @Override
    public boolean isComplete() {
        return !getSelectedDevices().isEmpty();
    }

    /**
     * Notifies the parent wizard that the step state has changed and buttons
     * should be updated (e.g., Enable/Disable 'Next' button).
     */
    private void updateWizardButtons() {
        if (wizard != null) {
            wizard.updateButtons();
        }
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
        boolean result = false;
        if(event!=null){
            Actor actor = event.getTarget();
            if(actor instanceof DeviceRow) {
                updateWizardButtons();
                result = true;
            }
        }
        return result;
    }

    /**
     * Disposes of resources used by this step, including the search icons.
     */
    @Override
    public void dispose() {
        stopScan();
        if (scanner != null) {
            scanner.close();
        }
    }

    /**
     * Initializes the IP range fields based on the local network interface.
     * Attempts to find the first non-loopback IPv4 address and suggests a
     * default scan range for that subnet.
     */
    private void initializeIpRange() {
        progressLabel.setText(I18N.get("printerdiscovery.gettingNetworkInfo"));
        collector.collectAsync().thenAccept(interfaces -> Gdx.app.postRunnable(() -> {
            progressLabel.setText("");
            for (NetworkInterfaceInfo ni : interfaces) {
                if (ni.isUp() && !ni.isLoopback()) {
                    for (NetworkAddressInfo addr : ni.getAddresses()) {
                        if (addr.isIpv4()) {
                            try {
                                IpUtils.IpRange range = IpUtils.calculateIpRange(addr.getAddress(), addr.getPrefixLength());
                                startIpField.setText(range.getStartIp());
                                endIpField.setText(range.getEndIp());
                                return;
                            } catch (UnknownHostException | IllegalArgumentException e) {
                                // Ignore and try next
                            }
                        }
                    }
                }
            }
        })).exceptionally(ex -> {
            Gdx.app.postRunnable(() -> progressLabel.setText(I18N.format("printerdiscovery.failedToGetNetworkInfo", ex.getMessage())));
            return null;
        });
    }

    /**
     * Starts a new network scan based on the user-defined IP range and settings.
     * This method runs asynchronously and updates the UI as devices are discovered.
     */
    private void startScan() {
        if (isScanning) return;

        rows.clear();
        resultsTable.clear();
        resultsTable.add(new Label(I18N.get("printerdiscovery.scanning"), skin)).expandX().center().row();
        progressLabel.setText(I18N.get("printerdiscovery.startingScan"));

        String startIp = startIpField.getText();
        String endIp = endIpField.getText();
        String timeoutStr = timeoutField.getText();

        if (startIp.isEmpty() || endIp.isEmpty()) {
            resultsTable.clear();
            resultsTable.add(new Label(I18N.get("printerdiscovery.enterValidIpRange"), skin)).expandX().center().row();
            progressLabel.setText("");
            return;
        }

        int timeout = 500;
        try {
            if (!timeoutStr.isEmpty()) {
                timeout = Integer.parseInt(timeoutStr);
            }
        } catch (NumberFormatException e) {
            // Use default
        }
        scanner.setTimeout(timeout);
        scanner.setIncludeSelfIp(includeSelfIpCheckBox.isChecked());

        isScanning = true;
        searchButton.setStyle(createButtonStyle(stopIcon));

        String baseIp = IpUtils.getBaseIp(startIp);
        int startHost = IpUtils.getHostPart(startIp);
        int endHost = IpUtils.getHostPart(endIp);

        ScanConfiguration currentConfig;
        if (deepScanCheckBox.isChecked()) {
            currentConfig = new ScanConfiguration(scanConfig.getProfiles(), scanConfig.getCommonPorts(), true);
        } else {
            currentConfig = scanConfig;
        }

        scanner.scanRange(baseIp, startHost, endHost, currentConfig, true, new NetworkScanner.ScanProgressListener() {
            @Override
            public void onProgress(double progress, String currentIp) {
                Gdx.app.postRunnable(() -> {
                    if (isScanning) {
                        progressLabel.setText(I18N.format("printerdiscovery.scanProgressFormat", progress * 100, currentIp));
                    }
                });
            }

            @Override
            public void onDeviceDiscovered(DiscoveredDevice device) {
                addDiscoveredDevice(device);
            }

            @Override
            public void onDeviceUpdated(DiscoveredDevice device) {
                addDiscoveredDevice(device);
            }

            @Override
            public void onPortDiscovered(String host, PortScanResult portResult) {
                Gdx.app.postRunnable(() -> {
                    DeviceRow row = null;
                    for (Actor actor : resultsTable.getChildren()) {
                        if (actor instanceof DeviceRow && host.equals(actor.getName())) {
                            row = (DeviceRow) actor;
                            break;
                        }
                    }

                    if (row != null) {
                        DiscoveredDevice updateDevice = new DiscoveredDevice(host);
                        updateDevice.addService(portResult);
                        row.update(updateDevice);
                        sortResults();
                    }
                });
            }
        }).thenAccept(devices -> Gdx.app.postRunnable(() -> {
            if (isScanning) {
                isScanning = false;
                searchButton.setStyle(createButtonStyle(searchIcon));
                progressLabel.setText(I18N.get("printerdiscovery.scanComplete"));
                
                // Final update of all discovered devices to ensure everything is correctly identified and sorted
                for (DiscoveredDevice device : devices) {
                    addDiscoveredDevice(device);
                }
            }
        })).exceptionally(ex -> {
            Gdx.app.postRunnable(() -> {
                isScanning = false;
                searchButton.setStyle(createButtonStyle(searchIcon));
                if (ex.getCause() instanceof java.util.concurrent.CancellationException || ex instanceof java.util.concurrent.CancellationException) {
                    progressLabel.setText(I18N.get("printerdiscovery.scanStopped"));
                } else {
                    resultsTable.clear();
                    resultsTable.add(new Label(I18N.format("printerdiscovery.scanFailed", ex.getMessage()), skin)).expandX().center().row();
                    progressLabel.setText("");
                }
            });
            return null;
        });
    }

    /**
     * Stops the currently running network scan.
     */
    private void stopScan() {
        if (!isScanning) return;
        progressLabel.setText(I18N.get("printerdiscovery.stoppingScan"));
        scanner.stopScan();
    }

    /**
     * Adds a newly discovered device to the results table.
     * If the device was already discovered (same IP), its information is updated.
     *
     * @param device the discovered device metadata
     */
    public void addDiscoveredDevice(DiscoveredDevice device) {
        Gdx.app.postRunnable(() -> {
        if (resultsTable.getChildren().size == 1 && resultsTable.getChildren().get(0) instanceof Label) {
            resultsTable.clear();
        }

            DeviceRow row = null;
            for (DeviceRow existingRow : rows) {
                if (device.getIpAddress().equals(existingRow.getName())) {
                    row = existingRow;
                    break;
                }
            }

            if (row == null) {
                row = new DeviceRow(device, skin, this::showDeviceDetails);
                row.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        updateWizardButtons();
                    }
                });
                rows.add(row);
                resultsTable.add(row).expandX().fillX().padBottom(10).row();
            } else {
                row.update(device);
            }
            sortResults();
        });
    }

    /**
     * Shows a dialog with detailed information about a discovered device,
     * including open ports, services, and mDNS attributes.
     *
     * @param device the device to show details for
     */
    private void showDeviceDetails(DiscoveredDevice device) {
        Dialog dialog = new Dialog(I18N.format("printerdiscovery.deviceDetailsTitle", device.getIpAddress()), skin) {
            @Override
            protected void result(Object object) {
                hide();
            }
        };

        Table content = new Table();
        content.pad(10);
        content.left();

        // Device Header
        content.add(new Label(I18N.get("printerdiscovery.deviceInfo"), skin, "default", Color.YELLOW)).left().padBottom(5).row();
        content.add(new Label(I18N.format("printerdiscovery.ipAddress", device.getIpAddress()), skin)).left().padLeft(10).row();
        if (device.getVendor() != null)
            content.add(new Label(I18N.format("printerdiscovery.vendor", device.getVendor()), skin)).left().padLeft(10).row();
        if (device.getModel() != null)
            content.add(new Label(I18N.format("printerdiscovery.model", device.getModel()), skin)).left().padLeft(10).row();
        if (device.getName() != null) content.add(new Label(I18N.format("printerdiscovery.name", device.getName()), skin)).left().padLeft(10).row();
        content.add(new Label("", skin)).row(); // Spacer

        // Port Scan Results
        content.add(new Label(I18N.get("printerdiscovery.openPortsHeader"), skin, "default", Color.GREEN)).left().padBottom(5).row();
        if (device.getServices().isEmpty()) {
            content.add(new Label(I18N.get("printerdiscovery.noOpenPorts"), skin)).left().padLeft(10).row();
        } else {
            for (PortScanResult service : device.getServices()) {
                String serviceName = service.getService() != null ? service.getService() : "Unknown";
                content.add(new Label(I18N.format("printerdiscovery.portInfoFormat", service.getPort(), serviceName), skin, "default", Color.WHITE)).left().padLeft(10).row();
                if (service.getServiceDetails() != null && !service.getServiceDetails().isEmpty()) {
                    Label detailsLabel = new Label(service.getServiceDetails(), skin);
                    detailsLabel.setWrap(true);
                    detailsLabel.setColor(Color.LIGHT_GRAY);
                    content.add(detailsLabel).left().padLeft(20).expandX().fillX().row();
                }
            }
        }
        content.add(new Label("", skin)).row(); // Spacer

        // mDNS Data
        content.add(new Label(I18N.get("printerdiscovery.mdnsHeader"), skin, "default", Color.CYAN)).left().padBottom(5).row();
        if (device.getMdnsServices().isEmpty()) {
            content.add(new Label(I18N.get("printerdiscovery.noMdnsData"), skin)).left().padLeft(10).row();
        } else {
            for (MdnsServiceInfo service : device.getMdnsServices()) {
                content.add(new Label(I18N.format("printerdiscovery.mdnsService", service.getName()), skin, "default", Color.WHITE)).left().padLeft(10).row();
                content.add(new Label(I18N.format("printerdiscovery.mdnsType", service.getType()), skin)).left().padLeft(20).row();
                content.add(new Label(I18N.format("printerdiscovery.mdnsHostname", service.getHostname()), skin)).left().padLeft(20).row();

                if (!service.getAttributes().isEmpty()) {
                    for (Map.Entry<String, String> entry : service.getAttributes().entrySet()) {
                        content.add(new Label("  " + entry.getKey() + " = " + entry.getValue(), skin)).left().padLeft(30).row();
                    }
                }
            }
        }

        ScrollPane scrollPane = new ScrollPane(content, skin);
        scrollPane.setFadeScrollBars(false);
        dialog.getContentTable().add(scrollPane).expand().fill().minSize(400, 300);

        dialog.button(I18N.get("printerdiscovery.close"));
        Stage stage = wizard != null ? wizard.getStage() : null;
        if (stage == null && Gdx.app.getApplicationListener() instanceof DesktopApp) {
            stage = ((DesktopApp) Gdx.app.getApplicationListener()).desktopUI.getDialogStage();
        }
        dialog.show(stage);
    }

    /**
     * Sorts the discovery results, putting identified printers first and then
     * sorting by IP address.
     */
    private void sortResults() {
        if (rows.isEmpty()) return;

        // Sort rows: identified printers first, then by IP
        rows.sort((r1, r2) -> {
            boolean p1 = r1.getDevice().getVendor() != null && !r1.getDevice().getVendor().isEmpty();
            boolean p2 = r2.getDevice().getVendor() != null && !r2.getDevice().getVendor().isEmpty();
            if (p1 && !p2) return -1;
            if (!p1 && p2) return 1;
            return r1.getName().compareTo(r2.getName());
        });

        // Re-add to table in sorted order
        resultsTable.clear();
        for (DeviceRow row : rows) {
            resultsTable.add(row).expandX().fillX().padBottom(10).row();
        }
    }

    public List<DiscoveredDevice> getSelectedDevices() {
        List<DiscoveredDevice> selected = new ArrayList<>();
        for (DeviceRow row : rows) {
            if (row.getDevice().isSelected()) {
                selected.add(row.getDevice());
            }
        }
        return selected;
    }

    /**
     * Helper method to create an image button style with specified icons.
     *
     * @param icon the icon to display on the button
     * @return the created button style
     */
    private ImageButton.ImageButtonStyle createButtonStyle(Drawable icon) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = skin.newDrawable("white", Color.GRAY);
        style.down = skin.newDrawable("white", Color.DARK_GRAY);
        style.imageUp = icon;
        return style;
    }

    /**
     * Creates a procedurally generated magnifying glass icon.
     *
     * @return the search icon drawable
     */
    private Drawable createSearchIcon() {
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        // Magnifying glass circle
        pixmap.drawCircle(12, 12, 8);
        pixmap.drawCircle(12, 12, 7);
        // Handle
        pixmap.drawLine(18, 18, 28, 28);
        pixmap.drawLine(19, 18, 29, 28);
        pixmap.drawLine(18, 19, 28, 29);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    /**
     * Creates a procedurally generated stop (red square) icon.
     *
     * @return the stop icon drawable
     */
    private Drawable createStopIcon() {
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.RED);
        pixmap.fillRectangle(8, 8, 16, 16);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }
}
