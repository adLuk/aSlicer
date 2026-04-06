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
package cz.ad.print3d.aslicer.logic.net.scanner;

import cz.ad.print3d.aslicer.logic.net.scanner.dto.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles enriching {@link DiscoveredDevice} with information from various discovery sources.
 * Merges data from mDNS and SSDP into the device DTO, resolving naming and vendor information.
 */
public class DeviceEnricher {

    /**
     * Enriches a DiscoveredDevice with information from mDNS discovery.
     * Extracts name, vendor, and model from mDNS service info and TXT records.
     *
     * @param device           the device to enrich
     * @param services         the list of mDNS services associated with this device's IP
     * @param markAsInProgress true to mark ports as verification in progress, false as verified
     */
    public void enrichWithMdns(DiscoveredDevice device, List<MdnsServiceInfo> services, boolean markAsInProgress) {
        if (device == null || services == null) return;

        synchronized (device) {
            for (MdnsServiceInfo service : services) {
                // Store the full mDNS service info for later inspection
                device.addMdnsService(service);

                // Set name if not already set
                if (device.getName() == null || device.getName().isEmpty()) {
                    device.setName(service.getName());
                }

                Map<String, String> attrs = service.getAttributes();

                // Try to identify vendor from common TXT record keys
                if (device.getVendor() == null) {
                    String vendor = attrs.get("mfg");
                    if (vendor == null) vendor = attrs.get("manufacturer");
                    if (vendor == null) vendor = attrs.get("usb_MFG");
                    if (vendor != null) device.setVendor(vendor);
                }

                // Try to identify model from common TXT record keys
                if (device.getModel() == null) {
                    String model = attrs.get("mdl");
                    if (model == null) model = attrs.get("model");
                    if (model == null) model = attrs.get("ty");
                    if (model == null) model = attrs.get("product");
                    if (model == null) model = attrs.get("usb_MDL");
                    if (model != null) device.setModel(model);
                }

                // Add the service discovered by mDNS to the device's services list if not already present
                boolean portFound = device.getServices().stream().anyMatch(s -> s.getPort() == service.getPort());
                if (!portFound && service.getPort() > 0) {
                    String serviceType = service.getType();
                    // Strip leading underscore and .local suffix for better readability
                    if (serviceType.startsWith("_") && serviceType.contains(".")) {
                        serviceType = serviceType.substring(1, serviceType.indexOf('.'));
                    }
                    device.addService(new PortScanResult(service.getPort(), !markAsInProgress, serviceType, "Discovered via mDNS", true));
                }
            }
        }
    }

    /**
     * Enriches a DiscoveredDevice with information from SSDP discovery.
     * Extracts name, vendor, and model from SSDP service info and XML description.
     *
     * @param device           the device to enrich
     * @param services         the list of SSDP services associated with this device's IP
     * @param markAsInProgress true to mark ports as verification in progress, false as verified
     */
    public void enrichWithSsdp(DiscoveredDevice device, List<SsdpServiceInfo> services, boolean markAsInProgress) {
        if (device == null || services == null) return;

        synchronized (device) {
            for (SsdpServiceInfo service : services) {
                // Store the full SSDP service info for later inspection
                device.addSsdpService(service);

                // Set name if not already set
                if (device.getName() == null || device.getName().isEmpty()) {
                    if (service.getFriendlyName() != null) {
                        device.setName(service.getFriendlyName());
                    } else if (service.getUsn() != null) {
                        device.setName(service.getUsn());
                    }
                }

                // Set vendor if not already set
                if (device.getVendor() == null && service.getManufacturer() != null) {
                    device.setVendor(service.getManufacturer());
                }

                // Set model if not already set
                if (device.getModel() == null && service.getModelName() != null) {
                    device.setModel(service.getModelName());
                }

                // Add the service discovered by SSDP to the device's services list if not already present
                boolean portFound = device.getServices().stream().anyMatch(s -> s.getPort() == service.getPort());
                if (!portFound && service.getPort() > 0) {
                    device.addService(new PortScanResult(service.getPort(), !markAsInProgress, "HTTP", "Discovered via SSDP", true));
                }
            }
        }
    }

    /**
     * Enriches a DiscoveredDevice based on the aggregated port scan results and configuration.
     * Identifies the printer vendor/model if a minimal set of required ports is open.
     *
     * @param device the device to enrich
     * @param config the scan configuration containing printer profiles
     */
    public void enrichFromPortScan(DiscoveredDevice device, ScanConfiguration config) {
        if (device == null || config == null) return;

        synchronized (device) {
            Set<Integer> openPorts = device.getServices().stream()
                    .filter(PortScanResult::isOpen)
                    .map(PortScanResult::getPort)
                    .collect(Collectors.toSet());

            for (PrinterDiscoveryProfile profile : config.getProfiles()) {
                if (profile.matches(openPorts)) {
                    // Set vendor if not already set by mDNS or SSDP
                    if (device.getVendor() == null || device.getVendor().isEmpty()) {
                        device.setVendor(profile.getPrinterType());
                    }
                    
                    // If we found a match by required ports, we can also ensure all those ports
                    // have the correct service name if they don't have one yet
                    for (PortDiscoveryConfig portConfig : profile.getPorts()) {
                        int port = portConfig.getPort();
                        if (openPorts.contains(port) && portConfig.getServiceName() != null) {
                            for (int i = 0; i < device.getServices().size(); i++) {
                                PortScanResult res = device.getServices().get(i);
                                if (res.getPort() == port && (res.getService() == null || res.getService().equals("Unknown Service") || res.getService().equals("Text Banner"))) {
                                    device.addService(new PortScanResult(port, true, portConfig.getServiceName(), res.getServiceDetails()));
                                }
                            }
                        }
                    }
                }
            }

            // Secondary pass: Identify by service name if any port was positively identified by pattern
            if (device.getVendor() == null || device.getVendor().isEmpty()) {
                for (PortScanResult res : device.getServices()) {
                    if (res.getService() != null && !res.getService().equals("Unknown Service") && !res.getService().equals("Text Banner")) {
                        for (PrinterDiscoveryProfile profile : config.getProfiles()) {
                            if (res.getService().contains(profile.getPrinterType())) {
                                device.setVendor(profile.getPrinterType());
                                break;
                            }
                        }
                    }
                    if (device.getVendor() != null && !device.getVendor().isEmpty()) break;
                }
            }
        }
    }
}
