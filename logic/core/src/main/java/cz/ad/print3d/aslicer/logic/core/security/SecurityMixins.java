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
package cz.ad.print3d.aslicer.logic.core.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Jackson Mix-ins for applying security-related annotations to DTOs without modifying the model module.
 */
public final class SecurityMixins {

    private SecurityMixins() {
        // Prevent instantiation
    }

    /**
     * Mix-in for BambuPrinterNetConnectionDto to encrypt accessCode.
     */
    public abstract static class BambuPrinterNetConnectionMixin {
        @JsonSerialize(using = CryptoSerializer.class)
        @JsonDeserialize(using = CryptoDeserializer.class)
        @JsonProperty("accessCode")
        private String accessCode;
    }

    /**
     * Mix-in for NetworkPrinterNetConnectionDto to encrypt pairingCode.
     */
    public abstract static class NetworkPrinterNetConnectionMixin {
        @JsonSerialize(using = CryptoSerializer.class)
        @JsonDeserialize(using = CryptoDeserializer.class)
        @JsonProperty("pairingCode")
        private String pairingCode;
    }
}
