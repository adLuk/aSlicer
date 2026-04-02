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
import com.badlogic.gdx.utils.Align;
import cz.ad.print3d.aslicer.logic.net.info.NetworkAddressInfo;
import cz.ad.print3d.aslicer.logic.net.info.NetworkInformationCollector;
import cz.ad.print3d.aslicer.logic.net.info.NetworkInterfaceInfo;
import cz.ad.print3d.aslicer.logic.net.scanner.NettyNetworkScanner;
import cz.ad.print3d.aslicer.logic.net.scanner.NetworkScanner;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;
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
        super("Discover Printers", skin);
        this.skin = skin;
        this.scanner = scanner;
        this.collector = collector;

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

        List<Integer> ports;
        if (deepScanCheckBox.isChecked()) {
            // Scan all possible ports (1-65535)
            ports = new java.util.ArrayList<>(65535);
            for (int p = 1; p <= 65535; p++) {
                ports.add(p);
            }
        } else {
            // Common ports for 3D printers and related services
            ports = List.of(22, 80, 443, 3344, 5000, 7080, 7125, 8080, 8883);
        }

        scanner.scanRange(baseIp, startHost, endHost, ports, true, new NetworkScanner.ScanProgressListener() {
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
            addDiscoveredDevice(device);
        }
    }

    /**
     * Adds a single discovered device to the results table or updates an existing one.
     *
     * @param device the discovered device to add or update
     */
    private void addDiscoveredDevice(DiscoveredDevice device) {
        // Remove initial labels if present
        if (resultsTable.getChildren().size == 1 && resultsTable.getChildren().get(0) instanceof Label) {
            Label l = (Label) resultsTable.getChildren().get(0);
            String text = l.getText().toString();
            if (text.equals("No printers found.") || text.equals("Scanning...") || text.startsWith("Please enter")) {
                resultsTable.clear();
            }
        }

        // Check if device already added (prevent duplicates, allow updates)
        Table deviceTable = null;
        for (Actor actor : resultsTable.getChildren()) {
            if (actor instanceof Table && device.getIpAddress().equals(actor.getName())) {
                deviceTable = (Table) actor;
                break;
            }
        }

        DiscoveredDevice currentDevice;
        if (deviceTable == null) {
            deviceTable = new Table();
            deviceTable.setName(device.getIpAddress());
            deviceTable.left();
            currentDevice = new DiscoveredDevice(device.getIpAddress());
            deviceTable.setUserObject(currentDevice);
            resultsTable.add(deviceTable).expandX().fillX().padBottom(10).row();
        } else {
            currentDevice = (DiscoveredDevice) deviceTable.getUserObject();
            deviceTable.clear();
        }

        // Merge device information
        if (device.getName() != null && (currentDevice.getName() == null || currentDevice.getName().isEmpty())) {
            currentDevice.setName(device.getName());
        }
        if (device.getVendor() != null && (currentDevice.getVendor() == null || currentDevice.getVendor().isEmpty())) {
            currentDevice.setVendor(device.getVendor());
        }
        if (device.getModel() != null && (currentDevice.getModel() == null || currentDevice.getModel().isEmpty())) {
            currentDevice.setModel(device.getModel());
        }

        // Merge services (ports)
        for (PortScanResult service : device.getServices()) {
            boolean found = false;
            for (PortScanResult existing : currentDevice.getServices()) {
                if (existing.getPort() == service.getPort()) {
                    // Update verification status if it changed (e.g., from in-progress to verified)
                    if (existing.isVerificationInProgress() && !service.isVerificationInProgress()) {
                        existing.setVerificationInProgress(false);
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                currentDevice.addService(service);
            }
        }

        // Merge mDNS services
        for (MdnsServiceInfo mdnsService : device.getMdnsServices()) {
            currentDevice.addMdnsService(mdnsService);
        }

        StringBuilder deviceLabelText = new StringBuilder(currentDevice.getIpAddress());
        if (currentDevice.getName() != null && !currentDevice.getName().isEmpty()) {
            deviceLabelText.append(" - ").append(currentDevice.getName());
        }
        if ((currentDevice.getVendor() != null && !currentDevice.getVendor().isEmpty()) || (currentDevice.getModel() != null && !currentDevice.getModel().isEmpty())) {
            deviceLabelText.append(" [");
            if (currentDevice.getVendor() != null) deviceLabelText.append(currentDevice.getVendor());
            if (currentDevice.getVendor() != null && !currentDevice.getVendor().isEmpty() && currentDevice.getModel() != null && !currentDevice.getModel().isEmpty()) {
                deviceLabelText.append(" ");
            }
            if (currentDevice.getModel() != null) deviceLabelText.append(currentDevice.getModel());
            deviceLabelText.append("]");
        }

        Label deviceLabel = new Label(deviceLabelText.toString(), skin);
        deviceLabel.setColor(Color.WHITE);
        deviceLabel.setAlignment(Align.center);

        CheckBox selectionCheckBox = new CheckBox("", skin);
        selectionCheckBox.setChecked(currentDevice.isSelected());
        selectionCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentDevice.setSelected(selectionCheckBox.isChecked());
            }
        });

        TextButton hintButton = new TextButton("?", skin);
        // Button is always enabled to allow showing a hint if data is missing
        hintButton.setDisabled(false);
        hintButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showMdnsDetails(currentDevice);
            }
        });

        // Expert UIX layout for each device row:
        // Col 1: Selection (left) - fixed width to balance with hint button
        // Col 2: Device Info (centered in middle) - expands to fill space
        // Col 3: Details Button (right) - fixed width to balance with checkbox
        deviceTable.add(selectionCheckBox).width(40).left();
        deviceTable.add(deviceLabel).expandX().center();
        deviceTable.add(hintButton).width(40).right();
        deviceTable.row();

        for (PortScanResult service : currentDevice.getServices()) {
            addPortToTable(deviceTable, service);
        }
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
            if (actor instanceof Table && actor.getUserObject() instanceof DiscoveredDevice) {
                devices.add((DiscoveredDevice) actor.getUserObject());
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
     * Displays a dialog with all mDNS data received for a device.
     *
     * @param device the device to show mDNS details for
     */
    private void showMdnsDetails(DiscoveredDevice device) {
        Dialog dialog = new Dialog("mDNS Details", skin) {
            @Override
            protected void result(Object object) {
                hide();
            }
        };
        
        Table content = new Table();
        content.pad(10);
        content.left();

        if (device.getMdnsServices().isEmpty()) {
            Label hintLabel = new Label("No mDNS data was received for this device.\nThis could be because the device does not support mDNS\nor it is blocked by the network firewall.", skin);
            hintLabel.setColor(Color.LIGHT_GRAY);
            content.add(hintLabel).left().pad(10).row();
        } else {
            for (MdnsServiceInfo service : device.getMdnsServices()) {
                content.add(new Label("Service: " + service.getName(), skin, "default", Color.CYAN)).left().row();
                content.add(new Label("Type: " + service.getType(), skin)).left().padLeft(10).row();
                content.add(new Label("Hostname: " + service.getHostname(), skin)).left().padLeft(10).row();
                content.add(new Label("Address: " + service.getIpAddress() + ":" + service.getPort(), skin)).left().padLeft(10).row();

                if (!service.getAttributes().isEmpty()) {
                    content.add(new Label("Attributes:", skin)).left().padLeft(10).row();
                    for (Map.Entry<String, String> entry : service.getAttributes().entrySet()) {
                        content.add(new Label("  " + entry.getKey() + " = " + entry.getValue(), skin)).left().padLeft(20).row();
                    }
                }
                content.add(new Label("", skin)).row(); // Spacer
            }
        }

        ScrollPane scrollPane = new ScrollPane(content, skin);
        scrollPane.setFadeScrollBars(false);
        dialog.getContentTable().add(scrollPane).expand().fill().minSize(400, 300);

        dialog.button("Close");
        dialog.show(getStage());
    }

    /**
     * Adds a single port result to the specified device table with appropriate styling.
     *
     * @param deviceTable the table representing the device
     * @param service     the port scan result to add
     */
    private void addPortToTable(Table deviceTable, PortScanResult service) {
        String serviceName = service.getService() != null ? service.getService() : "Open";
        String details = service.getServiceDetails() != null ? " (" + service.getServiceDetails() + ")" : "";
        String status = service.isVerificationInProgress() ? " [Verification in progress]" : "";
        String serviceInfo = "Port " + service.getPort() + ": " + serviceName + details + status;
        
        Label serviceLabel = new Label("  " + serviceInfo, skin);
        serviceLabel.setName("port-" + service.getPort());
        
        // Coloring based on source and status
        if (service.isVerificationInProgress()) {
            serviceLabel.setColor(Color.ORANGE);
        } else if (service.isFromMdns()) {
            serviceLabel.setColor(Color.CYAN);
        } else {
            serviceLabel.setColor(Color.GREEN);
        }
        
        // Ports are displayed on the left of the report, spanning across the row for better visibility
        deviceTable.add(serviceLabel).colspan(3).left().padLeft(10).row();
    }

    /**
     * Adds or updates a single port result for a host in the results table.
     *
     * @param host       the host IP address
     * @param portResult the port scan result
     */
    private void addOrUpdatePort(String host, PortScanResult portResult) {
        Table deviceTable = null;
        for (Actor actor : resultsTable.getChildren()) {
            if (actor instanceof Table && host.equals(actor.getName())) {
                deviceTable = (Table) actor;
                break;
            }
        }

        if (deviceTable == null) {
            if (portResult.isOpen() || portResult.isVerificationInProgress()) {
                // Device not found, create it with just this port
                DiscoveredDevice device = new DiscoveredDevice(host);
                device.addService(portResult);
                addDiscoveredDevice(device);
            }
            return;
        }

        // Update the underlying data object
        DiscoveredDevice currentDevice = (DiscoveredDevice) deviceTable.getUserObject();
        if (currentDevice != null) {
            boolean found = false;
            for (PortScanResult existing : currentDevice.getServices()) {
                if (existing.getPort() == portResult.getPort()) {
                    if (existing.isVerificationInProgress() && !portResult.isVerificationInProgress()) {
                        existing.setVerificationInProgress(false);
                    }
                    found = true;
                    break;
                }
            }
            if (!found && (portResult.isOpen() || portResult.isVerificationInProgress())) {
                currentDevice.addService(portResult);
            }
        }

        // Check if port already displayed
        Label portLabel = null;
        for (Actor actor : deviceTable.getChildren()) {
            if (actor instanceof Label && ("port-" + portResult.getPort()).equals(actor.getName())) {
                portLabel = (Label) actor;
                break;
            }
        }

        if (portLabel != null) {
            if (!portResult.isOpen() && !portResult.isVerificationInProgress()) {
                // Remove closed port label (e.g., if mDNS said it's there but scan said it's closed)
                portLabel.remove();
                
                // If the device table is now empty (no more ports and no name), we might want to keep it or remove it.
                // Keeping it for now as it shows the IP.
                return;
            }

            // Update existing port label
            String serviceName = portResult.getService() != null ? portResult.getService() : "Open";
            String details = portResult.getServiceDetails() != null ? " (" + portResult.getServiceDetails() + ")" : "";
            String status = portResult.isVerificationInProgress() ? " [Verification in progress]" : "";
            String serviceInfo = "Port " + portResult.getPort() + ": " + serviceName + details + status;
            portLabel.setText("  " + serviceInfo);
            
            if (portResult.isVerificationInProgress()) {
                portLabel.setColor(Color.ORANGE);
            } else if (portResult.isFromMdns()) {
                portLabel.setColor(Color.CYAN);
            } else {
                portLabel.setColor(Color.GREEN);
            }
        } else {
            // Add new port label if it's open or in progress
            if (portResult.isOpen() || portResult.isVerificationInProgress()) {
                addPortToTable(deviceTable, portResult);
            }
        }
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
