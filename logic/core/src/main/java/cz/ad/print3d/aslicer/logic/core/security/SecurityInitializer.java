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

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Security;

/**
 * Utility class for initializing security providers and ensuring FIPS compliance.
 */
public final class SecurityInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityInitializer.class);

    private SecurityInitializer() {
        // Private constructor to prevent instantiation
    }

    /**
     * Initializes the Bouncy Castle FIPS provider and sets it as the main security provider.
     * This method should be called early in the application lifecycle.
     */
    public static void init() {
        if (Security.getProvider(BouncyCastleFipsProvider.PROVIDER_NAME) == null) {
            LOGGER.info("Initializing Bouncy Castle FIPS provider");
            Security.insertProviderAt(new BouncyCastleFipsProvider(), 1);
        } else {
            LOGGER.debug("Bouncy Castle FIPS provider already initialized");
        }
    }
}
