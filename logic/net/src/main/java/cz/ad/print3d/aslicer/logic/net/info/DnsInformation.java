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
package cz.ad.print3d.aslicer.logic.net.info;

import io.netty.resolver.dns.DefaultDnsServerAddressStreamProvider;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DnsInformation provides information about configured DNS servers on the system.
 * It utilizes Netty to discover the default name servers configured in the operating system.
 */
public class DnsInformation {

    /**
     * Returns a list of configured DNS server addresses.
     * The addresses are returned in their textual representation.
     *
     * @return an unmodifiable list of DNS server IP addresses
     */
    public List<String> getConfiguredDnsServers() {
        // This is a common way in Netty to get the default name servers.
        return DefaultDnsServerAddressStreamProvider.defaultAddressList().stream()
                .map(addr -> addr.getAddress().getHostAddress())
                .distinct()
                .collect(Collectors.toList());
    }
}
