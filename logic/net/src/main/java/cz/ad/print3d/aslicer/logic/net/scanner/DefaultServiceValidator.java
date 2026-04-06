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

import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortDiscoveryConfig;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.PrinterDiscoveryProfile;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.ScanConfiguration;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Default implementation of {@link ServiceValidator}.
 * Uses {@link ServiceIdentifier} for base identification and then matches banners
 * against {@link PrinterDiscoveryProfile}s in the {@link ScanConfiguration}.
 */
public class DefaultServiceValidator implements ServiceValidator {

    @Override
    public PortScanResult validate(int port, String banner, ScanConfiguration config) {
        if (banner == null || banner.isEmpty()) {
            return new PortScanResult(port, true, "Unknown Service", "No banner received");
        }

        // 1. Identify printer type from banner if profiles are provided
        if (config != null) {
            for (PrinterDiscoveryProfile profile : config.getProfiles()) {
                for (PortDiscoveryConfig portConfig : profile.getPorts()) {
                    if (portConfig.getPort() == port) {
                        Pattern pattern = portConfig.getValidationPattern();
                        if (pattern != null && pattern.matcher(banner).find()) {
                            String serviceName = portConfig.getServiceName() != null ? portConfig.getServiceName() : profile.getPrinterType();
                            return new PortScanResult(port, true, serviceName, banner);
                        }
                    }
                }
            }
        }

        // 2. Fallback to generic identifier
        // Since we don't have a direct string-based identify in ServiceIdentifier,
        // we might just return what we have if we can't match it to a printer.
        return new PortScanResult(port, true, "Text Banner", banner);
    }

    @Override
    public PortScanResult validate(int port, ByteBuf msg, ScanConfiguration config) {
        if (msg == null || msg.readableBytes() == 0) {
            return new PortScanResult(port, true, "Unknown Service", "No data received");
        }

        // 1. Base identification
        ServiceIdentifier.ServiceInfo info = ServiceIdentifier.identify(msg);
        String banner = msg.toString(StandardCharsets.UTF_8).trim();

        // 2. Printer type matching
        if (config != null) {
            Collection<PortDiscoveryConfig> configs = config.getPortConfigs(port);
            for (PortDiscoveryConfig portConfig : configs) {
                Pattern pattern = portConfig.getValidationPattern();
                if (pattern != null && banner != null && pattern.matcher(banner).find()) {
                    // Find the printer type for this portConfig
                    String printerType = config.getProfiles().stream()
                            .filter(p -> p.getPorts().contains(portConfig))
                            .map(PrinterDiscoveryProfile::getPrinterType)
                            .findFirst()
                            .orElse("3D Printer");

                    String serviceName = portConfig.getServiceName() != null ? portConfig.getServiceName() : printerType;
                    return new PortScanResult(port, true, serviceName, banner);
                }
            }
        }

        return new PortScanResult(port, true, info.getName(), info.getDetails());
    }
}
