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

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.Array;

/**
 * Window that displays a list of currently loaded models in the scene.
 * <p>
 * It shows only the file name and extension of each loaded file.
 * The window includes padding to ensure the list content is properly aligned
 * and does not overlap with the window title.
 * </p>
 * <p>
 * The model list supports both single and multiple selection using standard
 * OS shortcuts (Control/Command for individual items and Shift for range selection).
 * Every item in the list also features two buttons:
 * <ul>
 *   <li>A "duplicate" button with a common symbol to clone the model in the scene.</li>
 *   <li>A "remove" button with a recycler bin symbol to remove the model from the scene and the list.</li>
 * </ul>
 * </p>
 */
public final class ModelListWindow extends Window {

    /**
     * Interface for listening to model list events, such as model removal or duplication requests.
     */
    public interface ModelListListener {
        /**
         * Called when a request to remove a model is made from the UI.
         *
         * @param index the zero-based index of the model to be removed from the list
         */
        void onRemoveModel(int index);

        /**
         * Called when a request to duplicate a model is made from the UI.
         *
         * @param index the zero-based index of the model to be duplicated
         */
        void onDuplicateModel(int index);

        /**
         * Called when the selection of models changes in the UI.
         *
         * @param indices the indices of the currently selected models
         */
        void onSelectModels(Array<Integer> indices);
    }

    /**
     * Internal data structure to represent a loaded model in the list.
     * <p>
     * Each instance is unique, even if they refer to the same filename,
     * allowing for independent selection. The display name is extracted
     * from the file path.
     * </p>
     */
    protected static class ModelListItem {
        /** The filename and extension to be displayed in the list. */
        private final String displayName;

        /**
         * Constructs a new list item with the specified display name.
         *
         * @param displayName the name to display for this item
         */
        public ModelListItem(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /** Underlying list widget used for managing selection state. */
    private final List<ModelListItem> list;
    /** Reference to the array containing paths of loaded models in the application. */
    private final Array<String> modelPaths;
    /** The collection of logic models with detailed data. */
    private final Array<cz.ad.print3d.aslicer.logic.model.Model> logicModels;
    /** Internal cache of list items to maintain stable references for selection. */
    private final Array<ModelListItem> listItems = new Array<>();
    /** The index of the last item that was clicked, used as an anchor for shift selection. */
    private int lastSelectedIndex = -1;
    /** Listener to notify about model list events. */
    private final ModelListListener listener;
    /** The main UI table containing the list of filenames and remove buttons. */
    private final Table listTable;
    /** Label for showing detailed model data when selected. */
    private Label detailLabel;
    /** Flag to prevent recursion during selection updates. */
    private boolean isUpdatingSelection = false;

    /**
     * Creates a new model list window.
     *
     * @param skin         the skin to use for styling UI components
     * @param modelPaths   reference to the array containing paths of loaded models in the application
     * @param logicModels  the collection of logic models with detailed data
     * @param listener     the listener for model list events, such as model removal requests
     */
    public ModelListWindow(Skin skin, Array<String> modelPaths, Array<cz.ad.print3d.aslicer.logic.model.Model> logicModels, ModelListListener listener) {
        super("Loaded Models", skin);
        this.modelPaths = modelPaths;
        this.logicModels = logicModels;
        this.listener = listener;

        setMovable(true);
        setResizable(true);
        setSize(300, 450);

        // Adjust padding to make room for title and have some margin around content
        padTop(30);
        padBottom(10);
        padLeft(10);
        padRight(10);

        // Create the underlying list for selection management
        list = new List<>(skin);
        list.getSelection().setMultiple(true);
        list.getSelection().setRequired(false);
        list.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                notifySelectionChanged();
                updateDetailLabel();
            }
        });

        // We use a Table to display items with buttons, but sync it with the List selection
        listTable = new Table(skin);
        listTable.top().left();

        updateList(false);

        ScrollPane scrollPane = new ScrollPane(listTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        detailLabel = new Label("", skin);
        detailLabel.setWrap(true);

        add(scrollPane).expand().fill().row();
        add(new Label("Model Details:", skin)).left().padTop(10).row();
        add(detailLabel).expandX().fillX().minHeight(100).padTop(5);

        updateDetailLabel();
    }

    /**
     * Updates the displayed list of models from the application state.
     * <p>
     * Extracts only the filename and extension for display.
     * It maintains the existing {@link ModelListItem} instances to preserve selection
     * when the number of items and their paths remain the same.
     * Rebuilds the UI table to include the filenames and their respective remove buttons.
     * </p>
     */
    public void updateList() {
        updateList(true);
    }

    /**
     * Updates the displayed list of models with an option to suppress selection change notification.
     *
     * @param notify true to notify the listener about selection changes, false otherwise
     */
    public void updateList(boolean notify) {
        // Capture current selected indices to restore them after rebuild
        Array<Integer> selectedIndices = new Array<>();
        Selection<ModelListItem> selection = list.getSelection();
        for (ModelListItem item : selection) {
            int index = listItems.indexOf(item, true);
            if (index != -1) {
                selectedIndices.add(index);
            }
        }

        boolean needsRebuild = false;
        if (listItems.size == modelPaths.size) {
            for (int i = 0; i < listItems.size; i++) {
                String fileName = java.nio.file.Paths.get(modelPaths.get(i)).getFileName().toString();
                if (!listItems.get(i).displayName.equals(fileName)) {
                    needsRebuild = true;
                    break;
                }
            }
        } else {
            needsRebuild = true;
        }

        if (needsRebuild) {
            listItems.clear();
            for (String path : modelPaths) {
                listItems.add(new ModelListItem(java.nio.file.Paths.get(path).getFileName().toString()));
            }
            list.setItems(listItems);
            
            // Restore selection
            selection.clear();
            for (int index : selectedIndices) {
                if (index < listItems.size) {
                    selection.add(listItems.get(index));
                }
            }
        } else {
            // Even if names match, sync list items if needed
            if (list.getItems() != listItems) {
                list.setItems(listItems);
            }
        }

        rebuildTable();
        if (notify) {
            notifySelectionChanged();
        }
    }

    /**
     * Rebuilds the visual representation of the model list.
     * <p>
     * Each row consists of a filename label, a duplication button, and a removal button on the right.
     * The row's background is updated to reflect its selection state in the
     * underlying {@link List}.
     * </p>
     */
    private void rebuildTable() {
        listTable.clearChildren();
        Skin skin = getSkin();

        Drawable trashIcon = getTrashIcon(skin);
        Drawable duplicateIcon = getDuplicateIcon(skin);

        for (int i = 0; i < listItems.size; i++) {
            final int index = i;
            ModelListItem item = listItems.get(i);

            // Row Container Table to allow for selection background
            Table row = new Table(skin);
            row.left();

            // Set selection background if item is selected in the underlying list
            if (list.getSelection().contains(item)) {
                row.setBackground(skin.newDrawable("white", Color.GRAY));
            }

            // Filename label
            Label label = new Label(item.displayName, skin);
            label.setEllipsis(true);
            row.add(label).expandX().left().fillX().padLeft(5).minWidth(0);

            // Duplicate button
            ImageButton.ImageButtonStyle duplicateStyle = new ImageButton.ImageButtonStyle();
            duplicateStyle.imageUp = duplicateIcon;
            duplicateStyle.up = skin.newDrawable("white", new Color(0, 0, 0, 0));
            duplicateStyle.over = skin.newDrawable("white", new Color(0, 1, 0, 0.2f));

            ImageButton duplicateBtn = new ImageButton(duplicateStyle);
            duplicateBtn.setName("duplicateButton");
            duplicateBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (listener != null) {
                        listener.onDuplicateModel(index);
                    }
                    event.cancel(); // Prevent selection when clicking duplicate
                }
            });
            row.add(duplicateBtn).size(24).padRight(5);

            // Trash button
            ImageButton.ImageButtonStyle removeStyle = new ImageButton.ImageButtonStyle();
            removeStyle.imageUp = trashIcon;
            removeStyle.up = skin.newDrawable("white", new Color(0, 0, 0, 0));
            removeStyle.over = skin.newDrawable("white", new Color(1, 0, 0, 0.2f));

            ImageButton removeBtn = new ImageButton(removeStyle);
            removeBtn.setName("removeButton");
            removeBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (listener != null) {
                        listener.onRemoveModel(index);
                    }
                    event.cancel(); // Prevent selection when clicking remove
                }
            });
            row.add(removeBtn).size(24).padRight(5);

            // Row click listener for selection management
            row.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    handleRowClick(index, item);
                }
            });

            listTable.add(row).expandX().fillX().row();
        }
    }

    /**
     * Handles a click on a list row, updating the selection state based on
     * standard modifier keys (Shift for range selection, Ctrl/Command for multi-selection).
     * <p>
     * After updating the selection state of the underlying {@link List}, the
     * table is rebuilt to reflect the new selection visually.
     * </p>
     *
     * @param index the index of the clicked row
     * @param item  the data item associated with the clicked row
     */
    protected void handleRowClick(int index, ModelListItem item) {
        Selection<ModelListItem> selection = list.getSelection();
        boolean shift = com.badlogic.gdx.Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ||
                        com.badlogic.gdx.Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        boolean ctrl = com.badlogic.gdx.Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ||
                       com.badlogic.gdx.Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT) ||
                       com.badlogic.gdx.Gdx.input.isKeyPressed(Input.Keys.SYM); // Command on Mac

        if (!ctrl && !shift) {
            selection.clear();
            selection.add(item);
            lastSelectedIndex = index;
        } else if (ctrl) {
            if (selection.contains(item)) {
                selection.remove(item);
            } else {
                selection.add(item);
            }
            lastSelectedIndex = index;
        } else if (shift && lastSelectedIndex != -1) {
            selection.clear();
            int min = Math.min(lastSelectedIndex, index);
            int max = Math.max(lastSelectedIndex, index);
            for (int i = min; i <= max; i++) {
                selection.add(listItems.get(i));
            }
        } else {
            selection.add(item);
            lastSelectedIndex = index;
        }
        rebuildTable();
        notifySelectionChanged();
    }

    /**
     * Updates the selection in the list to match the specified indices.
     * This method is used when models are selected from outside the list, e.g., via 3D picking.
     *
     * @param indices the indices of models to select
     */
    public void setSelectedIndices(Array<Integer> indices) {
        isUpdatingSelection = true;
        try {
            list.getSelection().clear();
            for (Integer index : indices) {
                if (index >= 0 && index < listItems.size) {
                    list.getSelection().add(listItems.get(index));
                }
            }
            rebuildTable();
            updateDetailLabel();
        } finally {
            isUpdatingSelection = false;
        }
    }

    /**
     * Updates the detail label with information about the currently selected model.
     * <p>
     * Displays model name, part count, triangle count, and units if a single model is selected.
     * Shows generic messages for multiple or no selection.
     * </p>
     */
    private void updateDetailLabel() {
        if (detailLabel == null) return;

        Selection<ModelListItem> selection = list.getSelection();
        if (selection.size() != 1) {
            detailLabel.setText(selection.isEmpty() ? "No model selected" : "Multiple models selected");
            return;
        }

        ModelListItem item = selection.first();
        int index = listItems.indexOf(item, true);
        if (index >= 0 && index < logicModels.size) {
            cz.ad.print3d.aslicer.logic.model.Model model = logicModels.get(index);
            int partCount = model.parts().size();
            int triangleCount = 0;
            for (cz.ad.print3d.aslicer.logic.model.Model.MeshPart part : model.parts()) {
                triangleCount += part.triangles().size();
            }

            String unitStr = model.lengthUnit() != null ? model.lengthUnit().getValue() : "unknown";
            String info = String.format("File: %s\nParts: %d\nTriangles: %d\nUnits: %s",
                item.displayName, partCount, triangleCount, unitStr);
            detailLabel.setText(info);
        } else {
            detailLabel.setText("Data not available");
        }
    }

    /**
     * Notifies the listener about the current selection of models.
     * <p>
     * Collects all selected indices from the underlying {@link List} and
     * passes them to the {@link ModelListListener#onSelectModels(Array)} method.
     * </p>
     */
    private void notifySelectionChanged() {
        if (isUpdatingSelection) return;
        if (listener != null) {
            Array<Integer> selectedIndices = new Array<>();
            Selection<ModelListItem> selection = list.getSelection();
            for (ModelListItem item : selection) {
                int index = listItems.indexOf(item, true);
                if (index != -1) {
                    selectedIndices.add(index);
                }
            }
            listener.onSelectModels(selectedIndices);
        }
    }

    /**
     * Retrieves or creates the duplication icon drawable.
     * <p>
     * If the icon does not exist in the skin, it is created procedurally using
     * {@link Pixmap} and added to the skin for reuse. The icon represents
     * a common duplication symbol (two overlapping squares).
     * </p>
     *
     * @param skin the skin to use for storing and retrieving the icon
     * @return the duplication icon drawable
     */
    private Drawable getDuplicateIcon(Skin skin) {
        if (skin.has("duplicateIcon", Texture.class)) {
            return skin.getDrawable("duplicateIcon");
        }

        Pixmap pixmap = new Pixmap(24, 24, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0, 0, 0, 0));
        pixmap.fill();

        pixmap.setColor(Color.LIGHT_GRAY);
        // Draw two overlapping squares
        // Background square
        pixmap.drawRectangle(4, 4, 12, 12);
        // Foreground square
        pixmap.setColor(Color.WHITE);
        pixmap.fillRectangle(8, 8, 12, 12);
        pixmap.setColor(Color.BLACK);
        pixmap.drawRectangle(8, 8, 12, 12);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        skin.add("duplicateIcon", texture);
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    /**
     * Retrieves or creates the recycler bin icon drawable.
     * <p>
     * If the icon does not exist in the skin, it is created procedurally using
     * {@link Pixmap} and added to the skin for reuse. The icon represents
     * a simple red recycler bin.
     * </p>
     *
     * @param skin the skin to use for storing and retrieving the icon
     * @return the recycler bin icon drawable
     */
    private Drawable getTrashIcon(Skin skin) {
        if (skin.has("trashIcon", Texture.class)) {
            return skin.getDrawable("trashIcon");
        }
        
        Pixmap pixmap = new Pixmap(24, 24, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0, 0, 0, 0));
        pixmap.fill();
        
        pixmap.setColor(Color.RED);
        // Draw a simple recycler bin symbol
        // Base
        pixmap.drawRectangle(6, 8, 12, 12);
        // Lid
        pixmap.drawLine(4, 6, 20, 6);
        pixmap.drawLine(9, 4, 15, 4);
        // Vertical lines
        pixmap.drawLine(9, 10, 9, 18);
        pixmap.drawLine(12, 10, 12, 18);
        pixmap.drawLine(15, 10, 15, 18);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        skin.add("trashIcon", texture);
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    /**
     * Provides access to the underlying list for testing purposes.
     *
     * @return the LibGDX list used for selection management
     */
    protected List<ModelListItem> getInternalList() {
        return list;
    }

    /**
     * Retrieves the internal list of items displayed in the window.
     *
     * @return the array of list items
     */
    protected Array<ModelListItem> getListItems() {
        return listItems;
    }
}
