package cz.ad.print3d.aslicer.logic.printer.system.action;

public enum PrinterSystemActionType implements PrinterActionType {

    START_HEATING_BED,
    START_HEATING_NOZZLE,
    CALIBRATE_NOZZLE,
    CALIBRATE_BED,
    HOMING,
    BED_LEVELING,
    PURGE_LINE,
    HEAD_HOME,
    STOP_HEATING_BED,
    STOP_HEATING_NOZZLE,
    START_FAN,
    STOP_FAN,
    LOAD_MATERIAL,
    START_EXTRUDER,
    STOP_EXTRUDER,
    PAUSE_EXTRUDER,
    RESUME_EXTRUDER,
    RELEASE_ENGINE_CONTROL,
    TAKE_ENGINE_CONTROL;

    private final String name;
    private final  String description;

    private PrinterSystemActionType() {
        //TODO fix it by hadcoded name
        this.name = this.name().toLowerCase();
        this.description = this.name().toLowerCase();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
