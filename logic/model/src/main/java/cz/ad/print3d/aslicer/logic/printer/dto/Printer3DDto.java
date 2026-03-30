package cz.ad.print3d.aslicer.logic.printer.dto;

import cz.ad.print3d.aslicer.logic.printer.Printer3D;
import cz.ad.print3d.aslicer.logic.printer.system.PrinterSystem;
import cz.ad.print3d.aslicer.logic.printer.system.net.PrinterNetConnection;
import cz.ad.print3d.aslicer.logic.printer.toolhead.Toolhead;
import cz.ad.print3d.aslicer.logic.printer.topology.Topology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO implementation of {@link Printer3D}.
 */
public class Printer3DDto implements Printer3D {

    private PrinterSystem printerSystem;
    private Topology topology;
    private List<Toolhead> toolhead = new ArrayList<>();
    private Map<String, PrinterNetConnection> netConnections = new HashMap<>();

    @Override
    public PrinterSystem getPrinterSystem() {
        return printerSystem;
    }

    public void setPrinterSystem(PrinterSystem printerSystem) {
        this.printerSystem = printerSystem;
    }

    @Override
    public Topology getTopology() {
        return topology;
    }

    public void setTopology(Topology topology) {
        this.topology = topology;
    }

    @Override
    public List<Toolhead> getToolhead() {
        return toolhead;
    }

    public void setToolhead(List<Toolhead> toolhead) {
        this.toolhead = toolhead;
    }

    public void addToolhead(Toolhead toolhead) {
        if (toolhead != null) {
            this.toolhead.add(toolhead);
        }
    }

    @Override
    public Map<String, PrinterNetConnection> getNetConnections() {
        return netConnections;
    }

    public void setNetConnections(Map<String, PrinterNetConnection> netConnections) {
        this.netConnections = netConnections;
    }

    public void addNetConnection(String name, PrinterNetConnection connection) {
        if (name != null && connection != null) {
            this.netConnections.put(name, connection);
        }
    }
}
