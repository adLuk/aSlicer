/*
 * aSlicer - 3D model processing tool.
 * Copyright (C) 2026 cz.ad.print3d.aslicer contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.ad.print3d.aslicer.ui.desktop.view;

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
import cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.ScanConfiguration;
import cz.ad.print3d.aslicer.logic.net.util.IpUtils;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Dialog window for discovering 3D printers on the network.
 * It allows users to specify an IP range and scan for active devices and services.
 */
public class PrinterDiscoveryDialog extends Window {

    final Skin skin;
    final TextField startIpField;
    final TextField endIpField;
    final CheckBox deepScanCheckBox;
    final CheckBox includeSelfIpCheckBox;
    final TextField timeoutField;
    final Label progressLabel;
    final Table resultsTable;
    private final NetworkScanner scanner;
    private final NetworkInformationCollector collector;
    private final ScanConfiguration scanConfig;
    private final ImageButton searchButton;
    private final Drawable searchIcon;
    private final Drawable stopIcon;
    private boolean isScanning = false;

    /**
     * Constructs a new PrinterDiscoveryDialog.
     *
     * @param skin the skin to use for styling
     */
    public PrinterDiscoveryDialog(Skin skin) {
        this(skin, new NettyNetworkScanner(), new NetworkInformationCollector());
    }

    /**
     * Constructs a new PrinterDiscoveryDialog with specified scanner and collector.
     *
     * @param skin      the skin to use for styling
     * @param scanner   the network scanner to use
     * @param collector the network information collector to use
     */
    public PrinterDiscoveryDialog(Skin skin, NetworkScanner scanner, NetworkInformationCollector collector) {
        this(skin, scanner, collector, ScanConfigurationLoader.loadDefault());
    }

    /**
     * Constructs a new PrinterDiscoveryDialog with specified scanner, collector and configuration.
     *
     * @param skin      the skin to use for styling
     * @param scanner   the network scanner to use
     * @param collector the network information collector to use
     * @param config    the scan configuration to use
     */
    public PrinterDiscoveryDialog(Skin skin, NetworkScanner scanner, NetworkInformationCollector collector, ScanConfiguration config) {
        super("Discover Printers", skin);
        this.skin = skin;
        this.scanner = scanner;
        this.collector = collector;
        this.scanConfig = config;

        setMovable(true);
        setResizable(true);
        setSize(500, 400);

        Table content = new Table();
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

        // Close button
        Table bottomTable = new Table();
        TextButton closeButton = new TextButton("Close", skin);
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                close();
            }
        });
        bottomTable.add(closeButton).right();
        content.add(bottomTable).expandX().fillX().padTop(10);

        add(content).expand().fill();

        initializeIpRange();
    }

    /**
     * Centers the window on the screen.
     */
    public void centerWindow() {
        setPosition(Gdx.graphics.getWidth() / 2f - getWidth() / 2f, Gdx.graphics.getHeight() / 2f - getHeight() / 2f);
    }

    /**
     * Initializes the IP range based on the first active non-loopback network interface.
     * This operation is performed asynchronously and displays progress information.
     */
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

    /**
     * Starts the network scan based on the specified IP range.
     * Displays progress percentage and current IP being scanned.
     */
    void startScan() {
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
        searchButton.getStyle().imageUp = stopIcon;

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
                Gdx.app.postRunnable(() -> addDiscoveredDevice(device));
            }

            @Override
            public void onPortDiscovered(String host, PortScanResult portResult) {
                Gdx.app.postRunnable(() -> addOrUpdatePort(host, portResult));
            }
        }).thenAccept(devices -> Gdx.app.postRunnable(() -> {
            if (isScanning) {
                isScanning = false;
                searchButton.getStyle().imageUp = searchIcon;
                searchButton.setDisabled(false);
                progressLabel.setText("Scan complete.");
                updateResults(devices);
            }
        })).exceptionally(ex -> {
            Gdx.app.postRunnable(() -> {
                isScanning = false;
                searchButton.getStyle().imageUp = searchIcon;
                searchButton.setDisabled(false);
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

    /**
     * Stops the current ongoing scan.
     * It disables the search button, updates the progress label to indicate
     * that termination is in progress, and calls the scanner's stop method.
     * The button will be re-enabled and the icon reverted once all
     * scanning futures have completed their cancellation.
     */
    void stopScan() {
        if (!isScanning) return;
        searchButton.setDisabled(true);
        progressLabel.setText("Stopping scan...");
        scanner.stopScan();
    }

    /**
     * Updates the results table with discovered devices and their services.
     * <p>This method clears the current results and populates the table with the provided list of devices.
     * Each device is displayed with a checkbox to allow multi-selection, its IP address, identified name,
     * vendor, model, and discovered services.</p>
     *
     * @param devices list of discovered devices to display
     */
    public void updateResults(List<DiscoveredDevice> devices) {
        resultsTable.clear();
        if (devices.isEmpty()) {
            resultsTable.add(new Label("No printers found.", skin)).expandX().center().row();
            return;
        }

        for (DiscoveredDevice device : devices) {
            addDiscoveredDevice(device, false);
        }
        sortAndRefreshResults();
    }

    /**
     * Adds a single discovered device to the results table or updates an existing one.
     *
     * @param device the discovered device to add or update
     */
    private void addDiscoveredDevice(DiscoveredDevice device) {
        addDiscoveredDevice(device, true);
    }

    /**
     * Adds a single discovered device to the results table or updates an existing one,
     * optionally triggering a refresh and sort.
     *
     * @param device the discovered device to add or update
     * @param shouldSort if true, will refresh and sort results immediately
     */
    void addDiscoveredDevice(DiscoveredDevice device, boolean shouldSort) {
        // Remove initial labels if present
        if (resultsTable.getChildren().size == 1 && resultsTable.getChildren().get(0) instanceof Label) {
            Label l = (Label) resultsTable.getChildren().get(0);
            String text = l.getText().toString();
            if (text.equals("No printers found.") || text.equals("Scanning...") || text.startsWith("Please enter")) {
                resultsTable.clear();
            }
        }

        // Check if device already added (prevent duplicates, allow updates)
        DeviceRow row = null;
        for (Actor actor : resultsTable.getChildren()) {
            if (actor instanceof DeviceRow && device.getIpAddress().equals(actor.getName())) {
                row = (DeviceRow) actor;
                break;
            }
        }

        if (row == null) {
            row = new DeviceRow(device, skin, this::showDeviceDetails);
            resultsTable.add(row).expandX().fillX().padBottom(10).row();
            if (shouldSort) {
                sortAndRefreshResults();
            }
        } else {
            boolean wasIdentified = row.getDevice().getVendor() != null && !row.getDevice().getVendor().isEmpty();
            row.update(device);
            boolean isIdentified = row.getDevice().getVendor() != null && !row.getDevice().getVendor().isEmpty();
            
            if (!wasIdentified && isIdentified && shouldSort) {
                sortAndRefreshResults();
            }
        }
    }

    void sortAndRefreshResults() {
        List<DeviceRow> rows = new ArrayList<>();
        for (Actor actor : resultsTable.getChildren()) {
            if (actor instanceof DeviceRow) {
                rows.add((DeviceRow) actor);
            }
        }
        
        rows.sort((r1, r2) -> {
            boolean p1 = r1.getDevice().getVendor() != null && !r1.getDevice().getVendor().isEmpty();
            boolean p2 = r2.getDevice().getVendor() != null && !r2.getDevice().getVendor().isEmpty();
            if (p1 && !p2) return -1;
            if (!p1 && p2) return 1;
            
            return compareIps(r1.getDevice().getIpAddress(), r2.getDevice().getIpAddress());
        });
        
        resultsTable.clear();
        for (DeviceRow row : rows) {
            resultsTable.add(row).expandX().fillX().padBottom(10).row();
        }
    }

    private int compareIps(String ip1, String ip2) {
        try {
            String[] parts1 = ip1.split("\\.");
            String[] parts2 = ip2.split("\\.");
            if (parts1.length == 4 && parts2.length == 4) {
                for (int i = 0; i < 4; i++) {
                    int n1 = Integer.parseInt(parts1[i]);
                    int n2 = Integer.parseInt(parts2[i]);
                    if (n1 != n2) return n1 - n2;
                }
            }
        } catch (Exception e) {
            // Fallback to string comparison
        }
        return ip1.compareTo(ip2);
    }

    /**
     * Returns an unmodifiable list of all discovered devices currently displayed in the results table.
     * <p>This method iterates through the children of the results table and extracts the
     * {@link DiscoveredDevice} objects stored as user objects in the device-specific tables.</p>
     *
     * @return an unmodifiable list of discovered devices
     */
    public List<DiscoveredDevice> getDiscoveredDevices() {
        List<DiscoveredDevice> devices = new ArrayList<>();
        for (Actor actor : resultsTable.getChildren()) {
            if (actor instanceof DeviceRow) {
                devices.add(((DeviceRow) actor).getDevice());
            }
        }
        return Collections.unmodifiableList(devices);
    }

    /**
     * Returns a list of all discovered devices that have been selected by the user.
     * <p>The selection state is maintained within each {@link DiscoveredDevice} object
     * and can be toggled via the checkbox in the UI results table.</p>
     *
     * @return a list of selected discovered devices
     */
    public List<DiscoveredDevice> getSelectedDevices() {
        List<DiscoveredDevice> selected = new ArrayList<>();
        for (DiscoveredDevice device : getDiscoveredDevices()) {
            if (device.isSelected()) {
                selected.add(device);
            }
        }
        return selected;
    }

    /**
     * Displays a dialog with all discovered data for a device, including mDNS and services.
     *
     * @param device the device to show details for
     */
    private void showDeviceDetails(DiscoveredDevice device) {
        Dialog dialog = new Dialog("Device Details - " + device.getIpAddress(), skin) {
            @Override
            protected void result(Object object) {
                hide();
            }
        };
        
        Table content = new Table();
        content.pad(10);
        content.left();

        // Device Header
        content.add(new Label("Device Information", skin, "default", Color.YELLOW)).left().padBottom(5).row();
        content.add(new Label("IP Address: " + device.getIpAddress(), skin)).left().padLeft(10).row();
        if (device.getVendor() != null) content.add(new Label("Vendor: " + device.getVendor(), skin)).left().padLeft(10).row();
        if (device.getModel() != null) content.add(new Label("Model: " + device.getModel(), skin)).left().padLeft(10).row();
        if (device.getName() != null) content.add(new Label("Name: " + device.getName(), skin)).left().padLeft(10).row();
        content.add(new Label("", skin)).row(); // Spacer

        // Port Scan Results
        content.add(new Label("Open Ports & Services", skin, "default", Color.GREEN)).left().padBottom(5).row();
        if (device.getServices().isEmpty()) {
            content.add(new Label("No open ports discovered.", skin)).left().padLeft(10).row();
        } else {
            for (PortScanResult service : device.getServices()) {
                String serviceName = service.getService() != null ? service.getService() : "Unknown";
                content.add(new Label("Port " + service.getPort() + ": " + serviceName, skin, "default", Color.WHITE)).left().padLeft(10).row();
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
        content.add(new Label("mDNS Services", skin, "default", Color.CYAN)).left().padBottom(5).row();
        if (device.getMdnsServices().isEmpty()) {
            content.add(new Label("No mDNS data received.", skin)).left().padLeft(10).row();
        } else {
            for (MdnsServiceInfo service : device.getMdnsServices()) {
                content.add(new Label("Service: " + service.getName(), skin, "default", Color.WHITE)).left().padLeft(10).row();
                content.add(new Label("Type: " + service.getType(), skin)).left().padLeft(20).row();
                content.add(new Label("Hostname: " + service.getHostname(), skin)).left().padLeft(20).row();
                
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

        dialog.button("Close");
        dialog.show(getStage());
    }


    /**
     * Adds or updates a single port result for a host in the results table.
     *
     * @param host       the host IP address
     * @param portResult the port scan result
     */
    private void addOrUpdatePort(String host, PortScanResult portResult) {
        DeviceRow row = null;
        for (Actor actor : resultsTable.getChildren()) {
            if (actor instanceof DeviceRow && host.equals(actor.getName())) {
                row = (DeviceRow) actor;
                break;
            }
        }

        if (row == null) {
            if (portResult.isOpen() || portResult.isVerificationInProgress()) {
                // Device not found, create it with just this port
                DiscoveredDevice device = new DiscoveredDevice(host);
                device.addService(portResult);
                addDiscoveredDevice(device);
            }
            return;
        }

        // Update the row by merging the new port result into existing device
        DiscoveredDevice device = row.getDevice();
        device.addService(portResult);
        row.update(device);
    }

    /**
     * Closes the dialog by stopping any ongoing scan and hiding the window.
     */
    private void close() {
        stopScan();
        setVisible(false);
    }

    @Override
    public boolean remove() {
        if (scanner != null) {
            scanner.close();
        }
        return super.remove();
    }

    /**
     * Creates a style for an image button with the specified icon.
     *
     * @param icon the icon drawable
     * @return the image button style
     */
    private ImageButton.ImageButtonStyle createButtonStyle(Drawable icon) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = skin.newDrawable("white", Color.GRAY);
        style.down = skin.newDrawable("white", Color.DARK_GRAY);
        style.imageUp = icon;
        return style;
    }

    /**
     * Creates a search icon drawable.
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
     * Creates a stop icon drawable.
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
