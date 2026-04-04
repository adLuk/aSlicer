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
package cz.ad.print3d.aslicer.logic.net.scanner.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Data transfer object representing a device discovered on the network.
 * It contains the IP address of the device and a list of services (open ports) found.
 */
public class DiscoveredDevice {

    private final String ipAddress;
    private final List<PortScanResult> services;
    private final List<MdnsServiceInfo> mdnsServices = new ArrayList<>();
    private final List<SsdpServiceInfo> ssdpServices = new ArrayList<>();
    private boolean selected;
    private String name;
    private String vendor;
    private String model;

    /**
     * Constructs a new DiscoveredDevice.
     *
     * @param ipAddress the IP address of the discovered device
     */
    public DiscoveredDevice(String ipAddress) {
        this(ipAddress, new ArrayList<>());
    }

    /**
     * Constructs a new DiscoveredDevice with already discovered services.
     *
     * @param ipAddress the IP address of the discovered device
     * @param services  the list of services found on the device
     */
    public DiscoveredDevice(String ipAddress, List<PortScanResult> services) {
        this.ipAddress = ipAddress;
        this.services = new ArrayList<>(services);
    }

    /**
     * Returns the IP address of the device.
     *
     * @return the IP address
     */
    public synchronized String getIpAddress() {
        return ipAddress;
    }

    /**
     * Returns an unmodifiable list of services (open ports) discovered on the device.
     *
     * @return the list of services
     */
    public synchronized List<PortScanResult> getServices() {
        return Collections.unmodifiableList(services);
    }

    /**
     * Returns an unmodifiable list of mDNS services associated with this device.
     *
     * @return the list of mDNS services
     */
    public synchronized List<MdnsServiceInfo> getMdnsServices() {
        return Collections.unmodifiableList(mdnsServices);
    }

    /**
     * Returns an unmodifiable list of SSDP services associated with this device.
     *
     * @return the list of SSDP services
     */
    public synchronized List<SsdpServiceInfo> getSsdpServices() {
        return Collections.unmodifiableList(ssdpServices);
    }

    /**
     * Adds an mDNS service info to this device.
     *
     * @param service the mDNS service info to add
     */
    public synchronized void addMdnsService(MdnsServiceInfo service) {
        if (service != null && !mdnsServices.contains(service)) {
            mdnsServices.add(service);
        }
    }

    /**
     * Adds an SSDP service info to this device.
     *
     * @param service the SSDP service info to add
     */
    public synchronized void addSsdpService(SsdpServiceInfo service) {
        if (service != null && !ssdpServices.contains(service)) {
            ssdpServices.add(service);
        }
    }

    /**
     * Returns whether the device is selected in the UI.
     *
     * @return true if selected, false otherwise
     */
    public synchronized boolean isSelected() {
        return selected;
    }

    /**
     * Sets whether the device is selected in the UI.
     *
     * @param selected true if selected, false otherwise
     */
    public synchronized void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Returns the name of the device if identified.
     *
     * @return the device name, or null if unknown
     */
    public synchronized String getName() {
        return name;
    }

    /**
     * Sets the name of the device.
     *
     * @param name the device name
     */
    public synchronized void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the vendor of the device if identified.
     *
     * @return the device vendor, or null if unknown
     */
    public synchronized String getVendor() {
        return vendor;
    }

    /**
     * Sets the vendor of the device.
     *
     * @param vendor the device vendor
     */
    public synchronized void setVendor(String vendor) {
        this.vendor = vendor;
    }

    /**
     * Returns the model of the device if identified.
     *
     * @return the device model, or null if unknown
     */
    public synchronized String getModel() {
        return model;
    }

    /**
     * Sets the model of the device.
     *
     * @param model the device model
     */
    public synchronized void setModel(String model) {
        this.model = model;
    }

    /**
     * Adds a service discovery result to this device. If a service with the same port already exists,
     * it will be updated if the new result provides more information (e.g., is open or has service name).
     *
     * @param result the port scan result to add
     */
    public synchronized void addService(PortScanResult result) {
        if (result == null) return;

        for (int i = 0; i < services.size(); i++) {
            PortScanResult existing = services.get(i);
            if (existing.getPort() == result.getPort()) {
                // If the new result is open and the existing one isn't, or if the new one has service info
                // that the existing one lacks, we update it.
                if ((result.isOpen() && !existing.isOpen()) ||
                        (result.getService() != null && existing.getService() == null)) {
                    services.set(i, result);
                }
                return;
            }
        }
        services.add(result);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscoveredDevice that = (DiscoveredDevice) o;
        return selected == that.selected &&
                Objects.equals(ipAddress, that.ipAddress) && 
                Objects.equals(services, that.services) &&
                Objects.equals(mdnsServices, that.mdnsServices) &&
                Objects.equals(ssdpServices, that.ssdpServices) &&
                Objects.equals(name, that.name) &&
                Objects.equals(vendor, that.vendor) &&
                Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, services, mdnsServices, ssdpServices, selected, name, vendor, model);
    }

    @Override
    public String toString() {
        return "DiscoveredDevice{" +
                "ipAddress='" + ipAddress + '\'' +
                ", name='" + name + '\'' +
                ", vendor='" + vendor + '\'' +
                ", model='" + model + '\'' +
                ", selected=" + selected +
                ", services=" + services +
                ", mdnsServices=" + mdnsServices +
                ", ssdpServices=" + ssdpServices +
                '}';
    }
}
