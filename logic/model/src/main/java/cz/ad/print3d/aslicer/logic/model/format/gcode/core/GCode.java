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
package cz.ad.print3d.aslicer.logic.model.format.gcode.core;

import cz.ad.print3d.aslicer.logic.model.Model;
import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base class for G-code information for various flavors.
 * This DTO provides a common structure for storing and accessing G-code commands and metadata.
 * It is designed to be type-safe and content-safe by using structured data instead of raw strings.
 * <p>
 * According to ISO 6983-1 (Numerical control of machines - Program format and definition of address characters),
 * a program consists of blocks, which in turn consist of words. This class follows that structure by
 * storing a list of {@link GCodeBlock} objects.
 * </p>
 */
public abstract class GCode implements Model {
    private final GCodeFlavor flavor;
    private final List<GCodeBlock> commands;
    private final Map<String, String> metadata;

    /**
     * Initializes a new G-code object with the specified flavor.
     *
     * @param flavor the G-code flavor
     */
    protected GCode(GCodeFlavor flavor) {
        this.flavor = flavor;
        this.commands = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    @Override
    public Unit unit() {
        // G-code can contain G20 (inches) or G21 (millimeters) commands.
        // Default is usually millimeter in 3D printing.
        return Unit.MILLIMETER; 
    }

    @Override
    public List<? extends MeshPart> parts() {
        // G-code doesn't naturally have mesh parts in the way STL does.
        // For now, return an empty list or a single part representing the toolpath if needed.
        return Collections.emptyList();
    }

    /**
     * Gets the G-code flavor.
     *
     * @return the G-code flavor
     */
    public GCodeFlavor getFlavor() {
        return flavor;
    }

    /**
     * Gets the list of G-code commands (blocks).
     *
     * @return the list of G-code commands
     */
    public List<GCodeBlock> getCommands() {
        return commands;
    }

    /**
     * Adds a G-code block to the list.
     *
     * @param block the G-code block to add
     */
    public void addCommand(GCodeBlock block) {
        if (block != null) {
            commands.add(block);
        }
    }

    /**
     * Gets the metadata map.
     * Metadata usually contains information from G-code comments, such as slicer settings.
     *
     * @return the metadata map
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Adds a metadata entry.
     *
     * @param key   the metadata key
     * @param value the metadata value
     */
    public void addMetadata(String key, String value) {
        if (key != null && !key.isBlank()) {
            metadata.put(key, value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GCode gCode = (GCode) o;
        return flavor == gCode.flavor &&
                Objects.equals(commands, gCode.commands) &&
                Objects.equals(metadata, gCode.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flavor, commands, metadata);
    }

    @Override
    public String toString() {
        return "GCode{" +
                "flavor=" + flavor +
                ", blockCount=" + commands.size() +
                ", metadataCount=" + metadata.size() +
                '}';
    }
}
