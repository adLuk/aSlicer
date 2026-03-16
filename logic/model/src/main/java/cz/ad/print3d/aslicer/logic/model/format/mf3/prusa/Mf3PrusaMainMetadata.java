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
package cz.ad.print3d.aslicer.logic.model.format.mf3.prusa;

import java.util.Map;

/**
 * Represents Prusa-specific metadata found in the main 3MF model file (3dmodel.model).
 * These are standard 3MF metadata entries with the 'slic3rpe:' prefix.
 */
public class Mf3PrusaMainMetadata {

    private String version3mf;
    private String printSettingsId;
    private String printerSettingsId;
    private String filamentSettingsId;

    /**
     * Default constructor.
     */
    public Mf3PrusaMainMetadata() {
    }

    /**
     * Populates the Prusa main metadata from a map of all metadata entries.
     *
     * @param metadata the map of all metadata entries
     * @return a new Mf3PrusaMainMetadata object
     */
    public static Mf3PrusaMainMetadata fromMap(final Map<String, String> metadata) {
        Mf3PrusaMainMetadata prusaMetadata = new Mf3PrusaMainMetadata();
        prusaMetadata.version3mf = metadata.get("slic3rpe:Version3mf");
        prusaMetadata.printSettingsId = metadata.get("slic3rpe:print_settings_id");
        prusaMetadata.printerSettingsId = metadata.get("slic3rpe:printer_settings_id");
        prusaMetadata.filamentSettingsId = metadata.get("slic3rpe:filament_settings_id");
        return prusaMetadata;
    }

    /**
     * Returns the 3MF version used by PrusaSlicer.
     *
     * @return the version, or null if not present
     */
    public String getVersion3mf() {
        return version3mf;
    }

    /**
     * Sets the 3MF version used by PrusaSlicer.
     *
     * @param version3mf the version to set
     */
    public void setVersion3mf(final String version3mf) {
        this.version3mf = version3mf;
    }

    /**
     * Returns the name of the print settings profile used.
     *
     * @return the print settings ID, or null if not present
     */
    public String getPrintSettingsId() {
        return printSettingsId;
    }

    /**
     * Returns the name of the printer settings profile used.
     *
     * @return the printer settings ID, or null if not present
     */
    public String getPrinterSettingsId() {
        return printerSettingsId;
    }

    /**
     * Returns the name of the filament settings profile used.
     *
     * @return the filament settings ID, or null if not present
     */
    public String getFilamentSettingsId() {
        return filamentSettingsId;
    }

    /**
     * Compares this metadata with another for equality.
     *
     * @param o the object to compare with
     * @return true if both have identical values
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3PrusaMainMetadata that = (Mf3PrusaMainMetadata) o;
        return java.util.Objects.equals(version3mf, that.version3mf) &&
                java.util.Objects.equals(printSettingsId, that.printSettingsId) &&
                java.util.Objects.equals(printerSettingsId, that.printerSettingsId) &&
                java.util.Objects.equals(filamentSettingsId, that.filamentSettingsId);
    }

    /**
     * Returns the hash code for this metadata.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(version3mf, printSettingsId, printerSettingsId, filamentSettingsId);
    }

    /**
     * Returns a string representation of this metadata.
     *
     * @return a string containing metadata values
     */
    @Override
    public String toString() {
        return "Mf3PrusaMainMetadata{" +
                "version3mf='" + version3mf + '\'' +
                ", printSettingsId='" + printSettingsId + '\'' +
                ", printerSettingsId='" + printerSettingsId + '\'' +
                ", filamentSettingsId='" + filamentSettingsId + '\'' +
                '}';
    }
}
