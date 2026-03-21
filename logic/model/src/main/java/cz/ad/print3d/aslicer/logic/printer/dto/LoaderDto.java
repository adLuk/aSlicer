package cz.ad.print3d.aslicer.logic.printer.dto;

import cz.ad.print3d.aslicer.logic.printer.toolhead.loader.Loader;
import cz.ad.print3d.aslicer.logic.printer.toolhead.loader.LoaderInput;

import java.util.ArrayList;
import java.util.List;

public class LoaderDto implements Loader {

    private List<LoaderInput> loaderInputs = new ArrayList<>();

    @Override
    public List<LoaderInput> getLoaderInputs() {
        return loaderInputs;
    }

    public void setLoaderInputs(List<LoaderInput> loaderInputs) {
        this.loaderInputs = loaderInputs;
    }

    public void addLoaderInput(LoaderInput input) {
        if (input != null) {
            this.loaderInputs.add(input);
        }
    }
}
