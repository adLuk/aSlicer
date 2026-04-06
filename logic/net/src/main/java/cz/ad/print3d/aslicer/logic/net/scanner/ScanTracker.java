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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks all active futures during a network scan to allow for reliable cancellation.
 */
public class ScanTracker {

    private final List<CompletableFuture<?>> activeFutures = new CopyOnWriteArrayList<>();
    private volatile boolean cancelled = false;

    /**
     * Registers a future to be tracked.
     * If the tracker is currently in a cancelled state, the future will be cancelled immediately.
     *
     * @param future the future to track
     */
    public void track(CompletableFuture<?> future) {
        if (future == null) return;
        if (cancelled) {
            future.cancel(true);
            return;
        }
        activeFutures.add(future);
        future.whenComplete((res, ex) -> activeFutures.remove(future));
    }

    /**
     * Cancels all tracked futures and clears the tracker.
     * Sets the tracker to a cancelled state to prevent new futures from being tracked.
     */
    public void cancelAll() {
        cancelled = true;
        for (CompletableFuture<?> future : activeFutures) {
            future.cancel(true);
        }
        activeFutures.clear();
    }

    /**
     * Resets the cancelled state of the tracker.
     * Should be called before starting a new scan.
     */
    public void reset() {
        cancelled = false;
    }

    /**
     * Returns whether any futures are currently being tracked.
     *
     * @return true if there are active futures, false otherwise
     */
    public boolean hasActive() {
        return !activeFutures.isEmpty();
    }
}
