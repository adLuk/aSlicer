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
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import cz.ad.print3d.aslicer.logic.net.info.NetworkAddressInfo;
import cz.ad.print3d.aslicer.logic.net.info.NetworkInformationCollector;
import cz.ad.print3d.aslicer.logic.net.info.NetworkInterfaceInfo;
import cz.ad.print3d.aslicer.logic.net.scanner.NettyNetworkScanner;
import cz.ad.print3d.aslicer.logic.net.scanner.NetworkScanner;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;
import cz.ad.print3d.aslicer.logic.net.util.IpUtils;

import java.net.UnknownHostException;
import java.util.List;

/**
 * Dialog window for discovering 3D printers on the network.
 * It allows users to specify an IP range and scan for active devices and services.
 */
public class PrinterDiscoveryDialog extends Window {

    final Skin skin;
    final TextField startIpField;
    final TextField endIpField;
    final CheckBox deepScanCheckBox;
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

        // IP Range section
        Table rangeTable = new Table();
        rangeTable.add(new Label("Start IP:", skin)).padRight(5);
        startIpField = new TextField("", skin);
        rangeTable.add(startIpField).width(120).padRight(10);

        rangeTable.add(new Label("End IP:", skin)).padRight(5);
        endIpField = new TextField("", skin);
        rangeTable.add(endIpField).width(120).padRight(10);

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
        rangeTable.add(searchButton).size(32).row();

        deepScanCheckBox = new CheckBox("Deep Scan (All IPs/Ports)", skin);
        rangeTable.add(deepScanCheckBox).colspan(5).left().padTop(5);

        content.add(rangeTable).fillX().row();

        // Progress section
        progressLabel = new Label("", skin);
        progressLabel.setColor(Color.YELLOW);
        content.add(progressLabel).left().padTop(5).row();

        // Results section
        resultsTable = new Table();
        resultsTable.top().left();
        ScrollPane scrollPane = new ScrollPane(resultsTable, skin);
        scrollPane.setFadeScrollBars(false);
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
        resultsTable.add(new Label("Scanning...", skin)).row();
        progressLabel.setText("Starting scan...");

        String startIp = startIpField.getText();
        String endIp = endIpField.getText();

        if (startIp.isEmpty() || endIp.isEmpty()) {
            resultsTable.clear();
            resultsTable.add(new Label("Please enter a valid IP range.", skin));
            progressLabel.setText("");
            return;
        }

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
            ports = List.of(80, 443, 3000, 5000, 7080, 8080, 8883);
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
                resultsTable.clear();
                if (ex.getCause() instanceof java.util.concurrent.CancellationException || ex instanceof java.util.concurrent.CancellationException) {
                    progressLabel.setText("Scan stopped.");
                } else {
                    resultsTable.add(new Label("Scan failed: " + ex.getMessage(), skin));
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
     *
     * @param devices list of discovered devices
     */
    private void updateResults(List<DiscoveredDevice> devices) {
        resultsTable.clear();
        if (devices.isEmpty()) {
            resultsTable.add(new Label("No printers found.", skin));
            return;
        }

        for (DiscoveredDevice device : devices) {
            addDiscoveredDevice(device);
        }
    }

    /**
     * Adds a single discovered device to the results table.
     *
     * @param device the discovered device to add
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

        // Check if device already added (prevent duplicates)
        for (Actor actor : resultsTable.getChildren()) {
            if (actor instanceof Table && device.getIpAddress().equals(actor.getName())) {
                return;
            }
        }

        Table deviceTable = new Table();
        deviceTable.setName(device.getIpAddress());
        deviceTable.left();
        deviceTable.add(new Label(device.getIpAddress(), skin)).left().expandX();
        deviceTable.row();
        for (PortScanResult service : device.getServices()) {
            String serviceName = service.getService() != null ? service.getService() : "Open";
            String details = service.getServiceDetails() != null ? " (" + service.getServiceDetails() + ")" : "";
            String serviceInfo = "Port " + service.getPort() + ": " + serviceName + details;
            Label serviceLabel = new Label("  " + serviceInfo, skin);
            serviceLabel.setColor(Color.LIGHT_GRAY);
            deviceTable.add(serviceLabel).left().padLeft(20).row();
        }
        resultsTable.add(deviceTable).fillX().padBottom(10).row();
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
