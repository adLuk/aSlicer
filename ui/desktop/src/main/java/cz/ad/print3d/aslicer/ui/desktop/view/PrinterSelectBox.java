package cz.ad.print3d.aslicer.ui.desktop.view;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import cz.ad.print3d.aslicer.logic.printer.Printer3D;
import cz.ad.print3d.aslicer.logic.printer.PrinterRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A component for selecting printers from a repository.
 * It supports single, multiple, or no selection, mimicking a combobox.
 */
public final class PrinterSelectBox extends Table {
    private final PrinterRepository repository;
    private final Skin skin;
    private final TextButton selectionButton;
    private final Set<Printer3D> selectedPrinters = new HashSet<>();
    private final Map<Printer3D, CheckBox> printerCheckBoxMap = new java.util.HashMap<>();
    private final Table printerListTable;
    private final ScrollPane scrollPane;

    public PrinterSelectBox(Skin skin, PrinterRepository repository) {
        this.skin = skin;
        this.repository = repository;
        this.selectionButton = new TextButton("", skin);
        
        add(selectionButton).fillX().expandX();
        
        this.printerListTable = new Table();
        this.scrollPane = new ScrollPane(printerListTable, skin);
        this.scrollPane.setVisible(false);
        
        selectionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                scrollPane.setVisible(!scrollPane.isVisible());
            }
        });
        
        row();
        add(scrollPane).fillX().expandX().maxHeight(200);
        
        refresh();
    }

    public void refresh() {
        printerListTable.clear();
        printerCheckBoxMap.clear();
        selectedPrinters.clear();
        List<String> groups = repository.getGroups();
        if (groups.isEmpty()) {
            selectionButton.setText("No printers");
            selectionButton.setDisabled(true);
        } else {
            selectionButton.setDisabled(false);
            for (String group : groups) {
                Map<String, Printer3D> printers = repository.getPrintersByGroup(group);
                for (Map.Entry<String, Printer3D> entry : printers.entrySet()) {
                    Printer3D printer = entry.getValue();
                    CheckBox cb = new CheckBox(group + " / " + entry.getKey(), skin);
                    printerCheckBoxMap.put(printer, cb);
                    cb.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            if (cb.isChecked()) {
                                selectedPrinters.add(printer);
                            } else {
                                selectedPrinters.remove(printer);
                            }
                            updateButtonText();
                        }
                    });
                    printerListTable.add(cb).left().row();
                }
            }
            updateButtonText();
        }
    }

    public void selectPrinter(Printer3D printer, boolean selected) {
        if (selected) {
            selectedPrinters.add(printer);
        } else {
            selectedPrinters.remove(printer);
        }
        updateCheckBoxes();
        updateButtonText();
    }

    public void clearSelection() {
        selectedPrinters.clear();
        updateCheckBoxes();
        updateButtonText();
    }

    private void updateCheckBoxes() {
        for (Map.Entry<Printer3D, CheckBox> entry : printerCheckBoxMap.entrySet()) {
            entry.getValue().setChecked(selectedPrinters.contains(entry.getKey()));
        }
    }

    public Set<Printer3D> getSelectedPrinters() {
        return new HashSet<>(selectedPrinters);
    }

    public String getSelectionText() {
        return selectionButton.getText().toString();
    }

    private void updateButtonText() {
        if (selectedPrinters.isEmpty()) {
            selectionButton.setText("None");
        } else if (selectedPrinters.size() == 1) {
            selectionButton.setText(selectedPrinters.iterator().next().getPrinterSystem().getPrinterName());
        } else {
            selectionButton.setText(selectedPrinters.size() + " printers");
        }
    }
}
