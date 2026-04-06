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
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ServiceValidator}.
 * Uses {@link ServiceIdentifier} for base identification and then matches banners
 * against {@link PrinterDiscoveryProfile}s in the {@link ScanConfiguration}.
 */
public class DefaultServiceValidator implements ServiceValidator {

    @Override
    public PortScanResult validate(int port, String banner, ScanConfiguration config) {
        // 1. Identify printer type from banner if profiles are provided
        if (config != null && banner != null && !banner.isEmpty()) {
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

        // 2. Fallback to configured service name for this port if it's not ambiguous among profiles,
        // it doesn't have a specific validation pattern (which would have matched in step 1),
        // and it's not a common port that could belong to any device.
        if (config != null) {
            List<PortDiscoveryConfig> matchingConfigs = config.getProfiles().stream()
                    .flatMap(p -> p.getPorts().stream())
                    .filter(pc -> pc.getPort() == port && pc.getServiceName() != null)
                    .filter(pc -> pc.getValidationPattern() == null)
                    .collect(Collectors.toList());

            if (matchingConfigs.size() == 1 && !config.getCommonPorts().contains(port)) {
                return new PortScanResult(port, true, matchingConfigs.get(0).getServiceName(), (banner != null && !banner.isEmpty()) ? banner : "No banner received");
            }
        }

        if (banner == null || banner.isEmpty()) {
            return new PortScanResult(port, true, "Unknown Service", "No banner received");
        }

        // 3. Fallback to generic identifier
        return new PortScanResult(port, true, "Text Banner", banner);
    }

    @Override
    public PortScanResult validate(int port, ByteBuf msg, ScanConfiguration config) {
        if (msg == null || msg.readableBytes() == 0) {
            // Check for configured service name even without data
            if (config != null) {
                Collection<PortDiscoveryConfig> configs = config.getPortConfigs(port);
                for (PortDiscoveryConfig portConfig : configs) {
                    if (portConfig.getServiceName() != null) {
                        return new PortScanResult(port, true, portConfig.getServiceName(), "No data received");
                    }
                }
            }
            return new PortScanResult(port, true, "Unknown Service", "No data received");
        }

        // 1. Base identification
        ServiceIdentifier.ServiceInfo info = ServiceIdentifier.identify(msg);
        String banner = msg.toString(StandardCharsets.UTF_8).trim();

        // 2. Printer type matching via pattern
        if (config != null) {
            Collection<PortDiscoveryConfig> configs = config.getPortConfigs(port);
            for (PortDiscoveryConfig portConfig : configs) {
                Pattern pattern = portConfig.getValidationPattern();
                if (pattern != null && banner != null && !banner.isEmpty() && pattern.matcher(banner).find()) {
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

        // 3. Fallback to configured service name for this port if it's not ambiguous,
        // it doesn't have a specific validation pattern, and it's not a common port.
        if (config != null) {
            List<PortDiscoveryConfig> matchingConfigs = config.getProfiles().stream()
                    .flatMap(p -> p.getPorts().stream())
                    .filter(pc -> pc.getPort() == port && pc.getServiceName() != null)
                    .filter(pc -> pc.getValidationPattern() == null)
                    .collect(Collectors.toList());

            if (matchingConfigs.size() == 1 && !config.getCommonPorts().contains(port)) {
                return new PortScanResult(port, true, matchingConfigs.get(0).getServiceName(), info.getDetails());
            }
        }

        return new PortScanResult(port, true, info.getName(), info.getDetails());
    }
}
