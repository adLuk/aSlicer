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
package cz.ad.print3d.aslicer.ui.desktop.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;

/**
 * A group of buttons in a toolbar with a separator and resizing capability.
 * This class follows the best practices of OOP design and provides a clean architecture for toolbar groups.
 * It provides a way to organize buttons into logical groups and visually separate them.
 */
public final class ToolbarGroup extends Table {

    private final Table buttonContainer;
    private final Image separator;
    private final Table separatorContainer;
    private final boolean vertical;
    private final Color defaultColor = Color.GRAY;
    private final Color hoverColor = Color.WHITE;

    private static final float HIT_AREA_SIZE = 24f;
    private static final float VISUAL_SIZE = 4f;

    /**
     * Creates a new toolbar group with horizontal orientation.
     *
     * @param skin the skin used for styling the separator
     */
    public ToolbarGroup(Skin skin) {
        this(skin, false);
    }

    /**
     * Creates a new toolbar group with specified orientation.
     *
     * @param skin     the skin used for styling the separator
     * @param vertical true if the buttons should be arranged vertically
     */
    public ToolbarGroup(Skin skin, boolean vertical) {
        this.vertical = vertical;
        this.buttonContainer = new Table();
        this.separator = new Image(skin.newDrawable("white", Color.WHITE));
        this.separator.setColor(defaultColor);
        this.separatorContainer = new Table();

        // Setup button container
        add(buttonContainer).fill();

        if (vertical) {
            row();
            // Setup separator line (4px high) within a larger interactive container (24px high)
            separatorContainer.add(separator).height(VISUAL_SIZE).fillX().center();
            add(separatorContainer).height(HIT_AREA_SIZE).fillX();
        } else {
            // Setup separator line (4px wide) within a larger interactive container (24px wide)
            separatorContainer.add(separator).width(VISUAL_SIZE).fillY().center();
            add(separatorContainer).width(HIT_AREA_SIZE).fillY();
        }

        // Setup hover effect and cursor change
        separatorContainer.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1) { // Mouse hover
                    separator.setColor(hoverColor);
                    try {
                        Gdx.graphics.setSystemCursor(vertical ? Cursor.SystemCursor.VerticalResize : Cursor.SystemCursor.HorizontalResize);
                    } catch (Exception ignored) {
                        // May fail in some environments (e.g. headless)
                    }
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer == -1) { // Mouse exit
                    separator.setColor(defaultColor);
                    try {
                        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
                    } catch (Exception ignored) {
                        // May fail in some environments
                    }
                }
            }
        });

        // Setup resizing logic via drag listener on the separator container
        separatorContainer.addListener(new DragListener() {
            private float lastStageX;
            private float lastStageY;

            @Override
            public void dragStart(InputEvent event, float x, float y, int pointer) {
                lastStageX = event.getStageX();
                lastStageY = event.getStageY();
                event.stop(); // Prevent event from reaching other actors
            }

            @Override
            public void drag(InputEvent event, float x, float y, int pointer) {
                Cell<?> cell = getCell(buttonContainer);
                if (vertical) {
                    float deltaY = event.getStageY() - lastStageY;
                    lastStageY = event.getStageY();
                    // Use prefHeight if it's been set, otherwise use current height
                    float currentHeight = cell.getPrefHeight();
                    if (currentHeight <= 0) currentHeight = buttonContainer.getHeight();
                    float newHeight = currentHeight - deltaY;
                    if (newHeight > 10) {
                        cell.height(newHeight);
                        invalidateHierarchy();
                    }
                } else {
                    float deltaX = event.getStageX() - lastStageX;
                    lastStageX = event.getStageX();
                    // Use prefWidth if it's been set, otherwise use current width
                    float currentWidth = cell.getPrefWidth();
                    if (currentWidth <= 0) currentWidth = buttonContainer.getWidth();
                    float newWidth = currentWidth + deltaX;
                    if (newWidth > 10) {
                        cell.width(newWidth);
                        invalidateHierarchy();
                    }
                }
                event.stop(); // Prevent event from reaching other actors
            }

            @Override
            public void dragStop(InputEvent event, float x, float y, int pointer) {
                event.stop();
            }
        });
    }

    /**
     * Adds a button to the group.
     *
     * @param button the button actor to be added
     */
    public void addButton(Actor button) {
        if (vertical) {
            buttonContainer.add(button).pad(5).row();
        } else {
            buttonContainer.add(button).pad(5);
        }
    }

    /**
     * Sets whether the separator should be visible.
     *
     * @param visible true if the separator should be shown
     */
    public void setSeparatorVisible(boolean visible) {
        separatorContainer.setVisible(visible);
        Cell<?> cell = getCell(separatorContainer);
        if (cell != null) {
            if (vertical) {
                cell.height(visible ? HIT_AREA_SIZE : 0);
            } else {
                cell.width(visible ? HIT_AREA_SIZE : 0);
            }
        }
        invalidateHierarchy();
    }

    /**
     * Returns the button container for manual configuration if needed.
     *
     * @return the table containing the group buttons
     */
    public Table getButtonContainer() {
        return buttonContainer;
    }

    /**
     * Returns the separator actor (the visual line).
     *
     * @return the image used as a separator line
     */
    public Image getSeparator() {
        return separator;
    }

    /**
     * Returns the separator container (the interactive area).
     *
     * @return the table containing the separator line
     */
    public Table getSeparatorContainer() {
        return separatorContainer;
    }
}
