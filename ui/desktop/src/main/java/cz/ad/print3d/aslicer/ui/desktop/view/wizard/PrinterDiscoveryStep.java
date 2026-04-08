package cz.ad.print3d.aslicer.ui.desktop.view.wizard;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
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
import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.ScanConfiguration;
import cz.ad.print3d.aslicer.logic.net.util.IpUtils;
import cz.ad.print3d.aslicer.ui.desktop.view.DeviceRow;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wizard step for discovering 3D printers on the network.
 * This step allows the user to define an IP range and perform a network scan to identify
 * available 3D printers and their services.
 */
public class PrinterDiscoveryStep implements WizardStep {

    private final Skin skin;
    private final Table content;
    private final TextField startIpField;
    private final TextField endIpField;
    private final CheckBox deepScanCheckBox;
    private final CheckBox includeSelfIpCheckBox;
    private final TextField timeoutField;
    private final Label progressLabel;
    private final Table resultsTable;
    private final NetworkScanner scanner;
    private final NetworkInformationCollector collector;
    private final ScanConfiguration scanConfig;
    private final ImageButton searchButton;
    private final Drawable searchIcon;
    private final Drawable stopIcon;
    private boolean isScanning = false;
    private Wizard wizard;

    public PrinterDiscoveryStep(Skin skin) {
        this(skin, new NettyNetworkScanner(), new NetworkInformationCollector(), ScanConfigurationLoader.loadDefault());
    }

    public PrinterDiscoveryStep(Skin skin, NetworkScanner scanner, NetworkInformationCollector collector, ScanConfiguration config) {
        this.skin = skin;
        this.scanner = scanner;
        this.collector = collector;
        this.scanConfig = config;

        content = new Table();
        content.pad(10);

        // IP Range and Settings section
        Table settingsTable = new Table();
        settingsTable.left();

        // Row 1: IP Range and Search Button
        Table ipRow = new Table();
        ipRow.add(new Label("Start IP:", skin)).padRight(5);
        startIpField = new TextField("", skin);
        ipRow.add(startIpField).width(110).padRight(10);

        ipRow.add(new Label("End IP:", skin)).padRight(5);
        endIpField = new TextField("", skin);
        ipRow.add(endIpField).width(110).padRight(10);

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
        ipRow.add(searchButton).size(32);
        settingsTable.add(ipRow).left().row();

        // Row 2: Checkboxes and Timeout
        Table optionsRow = new Table();
        deepScanCheckBox = new CheckBox("Deep Scan", skin);
        optionsRow.add(deepScanCheckBox).padRight(10);

        includeSelfIpCheckBox = new CheckBox("Include self IP", skin);
        includeSelfIpCheckBox.setChecked(false);
        optionsRow.add(includeSelfIpCheckBox).padRight(10);

        optionsRow.add(new Label("Timeout (ms):", skin)).padRight(5);
        timeoutField = new TextField("500", skin);
        timeoutField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
        optionsRow.add(timeoutField).width(60);
        settingsTable.add(optionsRow).left().padTop(5).row();

        content.add(settingsTable).fillX().row();

        // Progress section
        progressLabel = new Label("", skin);
        progressLabel.setColor(Color.YELLOW);
        content.add(progressLabel).left().padTop(5).row();

        // Results section
        resultsTable = new Table();
        resultsTable.top().left();
        ScrollPane scrollPane = new ScrollPane(resultsTable, skin);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        content.add(scrollPane).expand().fill().padTop(10).row();

        initializeIpRange();
    }

    @Override
    public String getTitle() {
        return "Discover Printers";
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

    private void updateWizardButtons() {
        if (wizard != null) {
            wizard.updateButtons();
        }
    }

    private void initializeIpRange() {
        progressLabel.setText("Getting network information...");
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
            Gdx.app.postRunnable(() -> progressLabel.setText("Failed to get network info: " + ex.getMessage()));
            return null;
        });
    }

    private void startScan() {
        if (isScanning) return;

        resultsTable.clear();
        resultsTable.add(new Label("Scanning...", skin)).expandX().center().row();
        progressLabel.setText("Starting scan...");

        String startIp = startIpField.getText();
        String endIp = endIpField.getText();
        String timeoutStr = timeoutField.getText();

        if (startIp.isEmpty() || endIp.isEmpty()) {
            resultsTable.clear();
            resultsTable.add(new Label("Please enter a valid IP range.", skin)).expandX().center().row();
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
                        progressLabel.setText(String.format("Scanning: %.0f%% (%s)", progress * 100, currentIp));
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
                progressLabel.setText("Scan complete.");
                
                // Final update of all discovered devices to ensure everything is correctly identified and sorted
                for (DiscoveredDevice device : devices) {
                    addDiscoveredDevice(device);
                }
                
                updateWizardButtons();
            }
        })).exceptionally(ex -> {
            Gdx.app.postRunnable(() -> {
                isScanning = false;
                searchButton.setStyle(createButtonStyle(searchIcon));
                if (ex.getCause() instanceof java.util.concurrent.CancellationException || ex instanceof java.util.concurrent.CancellationException) {
                    progressLabel.setText("Scan stopped.");
                } else {
                    resultsTable.clear();
                    resultsTable.add(new Label("Scan failed: " + ex.getMessage(), skin)).expandX().center().row();
                    progressLabel.setText("");
                }
            });
            return null;
        });
    }

    private void stopScan() {
        if (!isScanning) return;
        progressLabel.setText("Stopping scan...");
        scanner.stopScan();
    }

    private void addDiscoveredDevice(DiscoveredDevice device) {
        Gdx.app.postRunnable(() -> {
            // Remove initial labels if present
            if (resultsTable.getChildren().size == 1 && resultsTable.getChildren().get(0) instanceof Label) {
                resultsTable.clear();
            }

            DeviceRow row = null;
            for (Actor actor : resultsTable.getChildren()) {
                if (actor instanceof DeviceRow && device.getIpAddress().equals(actor.getName())) {
                    row = (DeviceRow) actor;
                    break;
                }
            }

            if (row == null) {
                row = new DeviceRow(device, skin, d -> {}); // Detail click ignored for now
                row.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        updateWizardButtons();
                    }
                });
                resultsTable.add(row).expandX().fillX().padBottom(10).row();
            } else {
                row.update(device);
            }
            sortResults();
            updateWizardButtons();
        });
    }

    private void sortResults() {
        // Find all device rows
        List<DeviceRow> rows = new ArrayList<>();
        for (Actor actor : resultsTable.getChildren()) {
            if (actor instanceof DeviceRow) {
                rows.add((DeviceRow) actor);
            }
        }

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
        for (Actor actor : resultsTable.getChildren()) {
            if (actor instanceof DeviceRow) {
                DeviceRow row = (DeviceRow) actor;
                if (row.getDevice().isSelected()) {
                    selected.add(row.getDevice());
                }
            }
        }
        return selected;
    }

    private ImageButton.ImageButtonStyle createButtonStyle(Drawable icon) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = skin.newDrawable("white", Color.GRAY);
        style.down = skin.newDrawable("white", Color.DARK_GRAY);
        style.imageUp = icon;
        return style;
    }

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

    private Drawable createStopIcon() {
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.RED);
        pixmap.fillRectangle(8, 8, 16, 16);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }
}
