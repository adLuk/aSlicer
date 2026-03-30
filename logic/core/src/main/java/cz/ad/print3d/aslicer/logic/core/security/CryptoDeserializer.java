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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Jackson deserializer that decrypts strings using {@link CryptoService}.
 */
public class CryptoDeserializer extends JsonDeserializer<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CryptoDeserializer.class);
    private final CryptoService cryptoService;

    public CryptoDeserializer() {
        this(CryptoService.getInstance());
    }

    public CryptoDeserializer(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value != null && !value.isEmpty()) {
            try {
                return cryptoService.decrypt(value);
            } catch (Exception e) {
                LOGGER.debug("Could not decrypt value, assuming it's in plain text: {}", e.getMessage());
                return value;
            }
        }
        return value;
    }
}
