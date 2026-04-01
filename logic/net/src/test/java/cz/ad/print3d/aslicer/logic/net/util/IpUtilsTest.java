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
 */
package cz.ad.print3d.aslicer.logic.net.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.net.UnknownHostException;

/**
 * Unit tests for IpUtils.
 */
public class IpUtilsTest {

    @Test
    public void testCalculateIpRangeV4_24() throws UnknownHostException {
        IpUtils.IpRange range = IpUtils.calculateIpRange("192.168.1.50", 24);
        assertEquals("192.168.1.1", range.getStartIp());
        assertEquals("192.168.1.254", range.getEndIp());
        assertEquals("192.168.1.", range.getBaseIp());
        assertEquals(1, range.getStartHost());
        assertEquals(254, range.getEndHost());
    }

    @Test
    public void testCalculateIpRangeV4_25() throws UnknownHostException {
        IpUtils.IpRange range = IpUtils.calculateIpRange("192.168.1.50", 25);
        assertEquals("192.168.1.1", range.getStartIp());
        assertEquals("192.168.1.126", range.getEndIp());
        assertEquals("192.168.1.", range.getBaseIp());
        assertEquals(1, range.getStartHost());
        assertEquals(126, range.getEndHost());
    }

    @Test
    public void testGetHostPart() {
        assertEquals(50, IpUtils.getHostPart("192.168.1.50"));
        assertEquals(254, IpUtils.getHostPart("10.0.0.254"));
    }

    @Test
    public void testGetBaseIp() {
        assertEquals("192.168.1.", IpUtils.getBaseIp("192.168.1.50"));
        assertEquals("10.0.0.", IpUtils.getBaseIp("10.0.0.1"));
    }

    @Test
    public void testInvalidPrefixLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            IpUtils.calculateIpRange("192.168.1.1", 33);
        });
    }

    @Test
    public void testIPv6NotSupported() {
        assertThrows(IllegalArgumentException.class, () -> {
            IpUtils.calculateIpRange("::1", 128);
        });
    }
}
