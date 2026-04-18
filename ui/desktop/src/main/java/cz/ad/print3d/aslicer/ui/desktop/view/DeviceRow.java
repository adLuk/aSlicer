package cz.ad.print3d.aslicer.ui.desktop.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;

import java.util.function.Consumer;

/**
 * A specialized UI component representing a single discovered device row in the results table.
 * It manages merging device information, services, and mDNS data, and handles the display
 * of port status and details button.
 */
public final class DeviceRow extends Table {

    /**
     * The internal representation of the device data being managed by this row.
     */
    private final DiscoveredDevice currentDevice;

    /**
     * UI skin used for styling.
     */
    private final Skin skin;

    /**
     * Callback invoked when the user clicks on the details button.
     */
    private final Consumer<DiscoveredDevice> onDetailsClick;

    /**
     * Flag indicating if the port details section is expanded.
     */
    private boolean isExpanded = true;

    /**
     * Constructs a new DeviceRow.
     *
     * @param device         the initial discovered device data
     * @param skin           the skin to use for styling
     * @param onDetailsClick callback for when the details button is clicked
     */
    public DeviceRow(DiscoveredDevice device, Skin skin, Consumer<DiscoveredDevice> onDetailsClick) {
        this.currentDevice = new DiscoveredDevice(device.getIpAddress());
        this.skin = skin;
        this.onDetailsClick = onDetailsClick;
        
        setName(device.getIpAddress());
        setUserObject(currentDevice);
        left();
        update(device);
    }

    /**
     * Updates the row with new device information.
     * Merges names, vendors, models, and services into the current state.
     *
     * @param device updated device information
     */
    public void update(DiscoveredDevice device) {
        boolean wasIdentified = currentDevice.getVendor() != null && !currentDevice.getVendor().isEmpty();

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

        boolean isIdentified = currentDevice.getVendor() != null && !currentDevice.getVendor().isEmpty();
        if (!wasIdentified && isIdentified) {
            isExpanded = false;
        }

        buildLayout();
    }

    /**
     * Rebuilds the UI layout based on the current state of the device.
     */
    private void buildLayout() {
        clear();
        pad(8);
        
        String ip = currentDevice.getIpAddress();
        String name = currentDevice.getName();
        String vendor = currentDevice.getVendor();
        String model = currentDevice.getModel();

        boolean positivelyIdentified = vendor != null && !vendor.isEmpty();
        
        if (positivelyIdentified) {
            setBackground(skin.newDrawable("white", new Color(0.1f, 0.3f, 0.1f, 0.4f)));
        } else {
            setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.4f)));
        }

        StringBuilder deviceLabelText = new StringBuilder();
        if (positivelyIdentified) {
            deviceLabelText.append(vendor).append(" Printer @ ");
        }
        deviceLabelText.append(ip);

        if (name != null && !name.isEmpty() && !name.equals(vendor)) {
            deviceLabelText.append(" - ").append(name);
        }

        if (model != null && !model.isEmpty()) {
            deviceLabelText.append(" [").append(model).append("]");
        }

        Label deviceLabel = new Label(deviceLabelText.toString(), skin);
        // Enable markup if the skin supports it, otherwise use fallback
        if (!(skin.has("default", Label.LabelStyle.class) && deviceLabel.getStyle().font.getData().markupEnabled)) {
            // fallback if markup is not enabled
            if (positivelyIdentified) {
                deviceLabel.setColor(Color.YELLOW);
            }
        }
        deviceLabel.setAlignment(Align.left);

        CheckBox selectionCheckBox = new CheckBox("", skin);
        selectionCheckBox.setChecked(currentDevice.isSelected());
        selectionCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentDevice.setSelected(selectionCheckBox.isChecked());
                DeviceRow.this.fire(new ChangeListener.ChangeEvent());
            }
        });

        TextButton hintButton = new TextButton("?", skin);
        hintButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onDetailsClick.accept(currentDevice);
            }
        });

        TextButton expandButton = new TextButton(isExpanded ? "-" : "+", skin);
        expandButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                isExpanded = !isExpanded;
                buildLayout();
            }
        });

        add(selectionCheckBox).width(30).left();
        add(expandButton).width(30).left().padLeft(5);
        add(deviceLabel).expandX().fillX().left().padLeft(10);
        add(hintButton).width(35).right().padLeft(10);
        row();

        // Add ports/services
        if (isExpanded) {
            Table portsTable = new Table();
            portsTable.left();
            for (PortScanResult result : currentDevice.getServices()) {
                addPortInfo(portsTable, result);
            }
            add(portsTable).colspan(4).expandX().fillX().padLeft(75).padTop(8).row();
        }
    }

    /**
     * Adds port-specific information to the ports table.
     *
     * @param portsTable the table to add information to
     * @param result     the scan result for a specific port
     */
    private void addPortInfo(Table portsTable, PortScanResult result) {
        String serviceName = result.getService() != null ? result.getService() : "Open";
        String status = result.isVerificationInProgress() ? " [Verification in progress]" : "";
        String serviceInfo = "Port " + result.getPort() + ": " + serviceName + status;
        
        Label serviceLabel = new Label("  " + serviceInfo, skin);
        serviceLabel.setName("port-" + result.getPort());
        
        // Coloring based on source and status
        if (result.isVerificationInProgress()) {
            serviceLabel.setColor(Color.ORANGE);
        } else if (result.isFromMdns()) {
            serviceLabel.setColor(Color.CYAN);
        } else {
            serviceLabel.setColor(Color.GREEN);
        }
        
        portsTable.add(serviceLabel).colspan(3).left().padLeft(10).row();
    }

    /**
     * Returns the underlying discovered device data.
     *
     * @return the current device
     */
    public DiscoveredDevice getDevice() {
        return currentDevice;
    }

    /**
     * Returns whether the port details section is expanded.
     *
     * @return true if expanded, false if collapsed
     */
    public boolean isExpanded() {
        return isExpanded;
    }
}
