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
package cz.ad.print3d.aslicer.logic.model.serializer;

import cz.ad.print3d.aslicer.logic.model.Model;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * Interface for serializing 3D model data to a binary output channel.
 *
 * @param <T> the type of model to serialize
 */
public interface ModelSerializer<T extends Model> {
    /**
     * Serializes the given model into the specified binary output channel.
     *
     * @param model the model to serialize
     * @param channel the output binary channel
     * @throws IOException if an I/O error occurs during serialization
     */
    void serialize(T model, WritableByteChannel channel) throws IOException;
}
