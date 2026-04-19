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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import cz.ad.print3d.aslicer.ui.desktop.I18N;

import java.util.Locale;

/**
 * A custom component for choosing the application language, represented by country flags.
 * It uses a button to toggle a list of languages.
 */
public class LanguageSelectBox extends Table {

    /**
     * Represents a language option.
     */
    public static class LanguageItem {
        private final Locale locale;
        private final Drawable flag;
        private final String name;

        public LanguageItem(Locale locale, String name, Drawable flag) {
            this.locale = locale;
            this.name = name;
            this.flag = flag;
        }

        public Locale getLocale() {
            return locale;
        }

        public Drawable getFlag() {
            return flag;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Skin skin;
    private final TextButton selectionButton;
    private final List<LanguageItem> languageList;
    private final ScrollPane scrollPane;
    private final Array<LanguageItem> items = new Array<>();
    private InputListener stageHideListener;

    /**
     * Creates a new language select box.
     *
     * @param skin the skin to use for styling
     */
    public LanguageSelectBox(Skin skin) {
        this.skin = skin;
        this.selectionButton = new TextButton("", skin);
        
        setupItems();
        
        this.languageList = new List<LanguageItem>(skin) {
            private float itemHeight = 24;

            @Override
            public float getItemHeight() {
                return itemHeight;
            }

            @Override
            public void draw(Batch batch, float parentAlpha) {
                validate();

                com.badlogic.gdx.graphics.g2d.BitmapFont font = getStyle().font;
                Drawable selectedDrawable = getStyle().selection;
                Color fontColorSelected = getStyle().fontColorSelected;
                Color fontColorUnselected = getStyle().fontColorUnselected;

                float x = getX(), y = getY(), width = getWidth(), height = getHeight();
                float itemHeight = getItemHeight();

                Color color = batch.getColor();
                float oldAlpha = color.a;
                batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

                if (getStyle().background != null) {
                    getStyle().background.draw(batch, x, y, width, height);
                }

                float itemY = height;
                for (int i = 0; i < getItems().size; i++) {
                    LanguageItem item = getItems().get(i);
                    boolean selected = getSelection().contains(item);

                    if (selected) {
                        selectedDrawable.draw(batch, x, y + itemY - itemHeight, width, itemHeight);
                    }

                    float iconWidth = 24;
                    float iconHeight = 16;
                    float pad = 5;

                    item.getFlag().draw(batch, x + pad, y + itemY - itemHeight + (itemHeight - iconHeight) / 2, iconWidth, iconHeight);

                    font.setColor(selected ? fontColorSelected : fontColorUnselected);
                    font.draw(batch, item.getName(), x + pad + iconWidth + pad, y + itemY - itemHeight + (itemHeight + font.getCapHeight()) / 2);

                    itemY -= itemHeight;
                }
                
                batch.setColor(color.r, color.g, color.b, oldAlpha);
            }
        };
        this.languageList.setItems(items);
        this.languageList.getSelection().setProgrammaticChangeEvents(false);
        
        this.scrollPane = new ScrollPane(languageList, skin);
        this.scrollPane.setVisible(false);
        this.scrollPane.setFadeScrollBars(false);

        selectionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (scrollPane.getParent() != null) {
                    hideDropdown();
                } else {
                    showDropdown();
                }
            }
        });

        languageList.addListener(new ChangeListener() {
            /**
             * Handles selection changes in the language list.
             * The dropdown is hidden only when a user explicitly selects a language.
             * Programmatic changes (e.g., during initialization) do not trigger this.
             *
             * @param event the change event
             * @param actor the list actor
             */
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Ensure the event originated from the list itself and not a bubble from elsewhere
                if (event.getTarget() != languageList) {
                    return;
                }
                
                LanguageItem selected = languageList.getSelected();
                if (selected != null) {
                    updateButton(selected);
                    hideDropdown();
                    
                    // Trigger a change event on this Table to notify external listeners (like AppToolbar)
                    ChangeEvent ce = com.badlogic.gdx.utils.Pools.obtain(ChangeEvent.class);
                    fire(ce);
                    com.badlogic.gdx.utils.Pools.free(ce);
                }
            }
        });

        add(selectionButton).fill().expand();
        setSelectedByLocale(I18N.getCurrentLocale(), false);
    }

    private void showDropdown() {
        Stage stage = getStage();
        if (stage == null) return;

        stage.addActor(scrollPane);
        scrollPane.setVisible(true);
        updateScrollPaneSizeAndPosition();
        scrollPane.validate(); // Ensure layout is calculated
        scrollPane.toFront();
        stage.setScrollFocus(scrollPane);

        // Add a listener to the stage to hide the dropdown when clicking elsewhere
        stageHideListener = new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (!scrollPane.isAscendantOf(event.getTarget()) && event.getTarget() != selectionButton) {
                    hideDropdown();
                    return true;
                }
                return false;
            }
        };
        stage.addListener(stageHideListener);
    }

    private void hideDropdown() {
        if (getStage() != null && stageHideListener != null) {
            getStage().removeListener(stageHideListener);
            stageHideListener = null;
        }
        scrollPane.remove();
        scrollPane.setVisible(false);
    }

    private void updateScrollPaneSizeAndPosition() {
        if (getStage() == null) return;

        float width = Math.max(150, getWidth());
        float itemHeight = languageList.getItemHeight();
        float height = Math.min(200, items.size * itemHeight);
        
        scrollPane.setSize(width, height);
        
        com.badlogic.gdx.math.Vector2 stagePos = localToStageCoordinates(new com.badlogic.gdx.math.Vector2(0, 0));
        float stageHeight = getStage().getHeight();
        
        // If there's not enough space below, show it above
        if (stagePos.y - height < 0 && stageHeight - (stagePos.y + getHeight()) > height) {
            scrollPane.setPosition(stagePos.x, stagePos.y + getHeight());
        } else {
            scrollPane.setPosition(stagePos.x, stagePos.y - height);
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (scrollPane.getParent() != null) {
            updateScrollPaneSizeAndPosition();
            scrollPane.toFront();
        }
    }

    private void setupItems() {
        for (Locale locale : I18N.getSupportedLocales()) {
            String name = locale.getDisplayLanguage(locale);
            if (name.length() > 0) {
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
            }
            items.add(new LanguageItem(locale, name, createFlagIcon(locale.getCountry())));
        }
    }

    /**
     * Sets the selection based on the provided locale.
     * Fires a ChangeEvent if the selection actually changes.
     *
     * @param locale the locale to select
     */
    public void setSelectedByLocale(Locale locale) {
        setSelectedByLocale(locale, true);
    }

    /**
     * Internal method to set selection with optional event firing.
     *
     * @param locale the locale to select
     * @param fireEvent true to fire a ChangeEvent if selection changes
     */
    private void setSelectedByLocale(Locale locale, boolean fireEvent) {
        for (LanguageItem item : items) {
            if (item.getLocale().getLanguage().equals(locale.getLanguage())) {
                if (languageList.getSelected() != item) {
                    languageList.setSelected(item);
                    updateButton(item);
                    
                    if (fireEvent) {
                        // Fire a change event to notify listeners that the selection has changed programmatically
                        com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent ce = 
                            com.badlogic.gdx.utils.Pools.obtain(com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent.class);
                        fire(ce);
                        com.badlogic.gdx.utils.Pools.free(ce);
                    }
                }
                break;
            }
        }
    }

    /**
     * @return the currently selected language item
     */
    public LanguageItem getSelected() {
        return languageList.getSelected();
    }

    /**
     * @return the number of available language options
     */
    public int getItemsCount() {
        return items.size;
    }

    private void updateButton(LanguageItem item) {
        selectionButton.clearChildren();
        com.badlogic.gdx.scenes.scene2d.ui.Image flagImage = new com.badlogic.gdx.scenes.scene2d.ui.Image(item.flag);
        selectionButton.add(flagImage).size(24, 16).padRight(5);
        selectionButton.add(new com.badlogic.gdx.scenes.scene2d.ui.Label(item.name, skin));
    }

    private Drawable createFlagIcon(String countryCode) {
        Pixmap pixmap = new Pixmap(24, 16, Pixmap.Format.RGBA8888);
        switch (countryCode) {
            case "CZ":
                drawCzechFlag(pixmap);
                break;
            case "US":
                drawUSFlag(pixmap);
                break;
            case "SK":
                drawSlovakFlag(pixmap);
                break;
            case "DE":
                drawGermanFlag(pixmap);
                break;
            case "ES":
                drawSpanishFlag(pixmap);
                break;
            case "UA":
                drawUkrainianFlag(pixmap);
                break;
            case "TH":
                drawThaiFlag(pixmap);
                break;
            case "CN":
                drawChineseFlag(pixmap);
                break;
            default:
                pixmap.setColor(Color.GRAY);
                pixmap.fill();
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    private void drawCzechFlag(Pixmap pixmap) {
        pixmap.setColor(Color.WHITE);
        pixmap.fillRectangle(0, 0, 24, 8);
        pixmap.setColor(Color.RED);
        pixmap.fillRectangle(0, 8, 24, 8);
        pixmap.setColor(Color.BLUE);
        pixmap.fillTriangle(0, 0, 0, 16, 12, 8);
    }

    private void drawUSFlag(Pixmap pixmap) {
        // Simple representation of US flag
        for (int i = 0; i < 7; i++) {
            pixmap.setColor(i % 2 == 0 ? Color.RED : Color.WHITE);
            pixmap.fillRectangle(0, i * 2, 24, 2);
        }
        pixmap.setColor(Color.BLUE);
        pixmap.fillRectangle(0, 0, 12, 8);
    }

    private void drawSlovakFlag(Pixmap pixmap) {
        pixmap.setColor(Color.WHITE);
        pixmap.fillRectangle(0, 0, 24, 5);
        pixmap.setColor(Color.BLUE);
        pixmap.fillRectangle(0, 5, 24, 6);
        pixmap.setColor(Color.RED);
        pixmap.fillRectangle(0, 11, 24, 5);
        // Small coat of arms representation
        pixmap.setColor(Color.RED);
        pixmap.fillRectangle(4, 4, 4, 6);
    }

    private void drawGermanFlag(Pixmap pixmap) {
        pixmap.setColor(Color.BLACK);
        pixmap.fillRectangle(0, 0, 24, 5);
        pixmap.setColor(Color.RED);
        pixmap.fillRectangle(0, 5, 24, 6);
        pixmap.setColor(Color.GOLD);
        pixmap.fillRectangle(0, 11, 24, 5);
    }

    private void drawSpanishFlag(Pixmap pixmap) {
        pixmap.setColor(Color.RED);
        pixmap.fillRectangle(0, 0, 24, 4);
        pixmap.setColor(Color.YELLOW);
        pixmap.fillRectangle(0, 4, 24, 8);
        pixmap.setColor(Color.RED);
        pixmap.fillRectangle(0, 12, 24, 4);
    }

    private void drawUkrainianFlag(Pixmap pixmap) {
        pixmap.setColor(Color.BLUE);
        pixmap.fillRectangle(0, 0, 24, 8);
        pixmap.setColor(Color.YELLOW);
        pixmap.fillRectangle(0, 8, 24, 8);
    }

    private void drawThaiFlag(Pixmap pixmap) {
        pixmap.setColor(Color.RED);
        pixmap.fillRectangle(0, 0, 24, 3);
        pixmap.setColor(Color.WHITE);
        pixmap.fillRectangle(0, 3, 24, 3);
        pixmap.setColor(Color.BLUE);
        pixmap.fillRectangle(0, 6, 24, 4);
        pixmap.setColor(Color.WHITE);
        pixmap.fillRectangle(0, 10, 24, 3);
        pixmap.setColor(Color.RED);
        pixmap.fillRectangle(0, 13, 24, 3);
    }

    private void drawChineseFlag(Pixmap pixmap) {
        pixmap.setColor(Color.RED);
        pixmap.fill();
        pixmap.setColor(Color.YELLOW);
        pixmap.fillRectangle(2, 2, 4, 4);
    }
}
