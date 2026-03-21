package cz.ad.print3d.aslicer.logic.printer.dto;

import cz.ad.print3d.aslicer.logic.model.basic.*;
import cz.ad.print3d.aslicer.logic.model.basic.dto.*;
import cz.ad.print3d.aslicer.logic.printer.system.action.PrinterSystemActionType;
import cz.ad.print3d.aslicer.logic.printer.topology.area.AreaShape;
import cz.ad.print3d.aslicer.logic.printer.topology.limit.DirectionChangeLimitType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class PrinterDtoTest {

    @Test
    void testPrinter3DDtoHierarchy() {
        // 1. PrinterSystem
        PrinterSystemDto system = new PrinterSystemDto();
        system.setPrinterManufacturer("Prusa Research");
        system.setPrinterName("Original Prusa i3 MK3S+");
        system.setPrinterModel("MK3S+");
        system.setGCodeInterpreter("Marlin");
        system.setFirmwareVersion("3.10.1");

        PrinterActionDto action = new PrinterActionDto();
        action.setType(PrinterSystemActionType.HOMING);
        action.setActionCode("G28");
        system.addPrinterAction(action);

        // 2. Topology - Movement Limits
        MovementLimitsDto limits = new MovementLimitsDto();
        limits.setMaximumSpeed(new SpeedDto(new BigDecimal("200"), SpeedUnitDto.MILLIMETERS_PER_SECOND));
        limits.setMaximumAcceleration(new AccelerationDto(new BigDecimal("1000"), AccelerationUnitDto.MILLIMETERS_PER_SECOND_SQUARED));
        
        DirectionChangeLimitDto jerk = new DirectionChangeLimitDto();
        jerk.setMaximumSpeed(new SpeedDto(new BigDecimal("10"), SpeedUnitDto.MILLIMETERS_PER_SECOND));
        jerk.setType(DirectionChangeLimitType.JERK);
        limits.setDirectionChangeSpeed(jerk);

        // 3. Topology - Areas
        PrintAreaDto printArea = new PrintAreaDto();
        printArea.setShape(AreaShape.RECTANGLE);
        printArea.addDimension(new DimensionDto(new BigDecimal("250"), LengthUnit.MILLIMETER));
        printArea.addDimension(new DimensionDto(new BigDecimal("210"), LengthUnit.MILLIMETER));
        printArea.addDimension(new DimensionDto(new BigDecimal("210"), LengthUnit.MILLIMETER));
        printArea.addGlobalPosition(BigDecimal.ZERO);
        printArea.addGlobalPosition(BigDecimal.ZERO);
        printArea.addGlobalPosition(BigDecimal.ZERO);

        GeometryDto geometry = new GeometryDto();
        geometry.setCoordinateType(Coordinates.CARTESIAN);
        geometry.setUnits(LengthUnit.MILLIMETER);

        TopologyDto topology = new TopologyDto();
        topology.setGeometry(geometry);
        topology.setPrintArea(printArea);
        topology.setMovementLimits(limits);
        topology.setSchematicModelPath(Paths.get("models/prusa_mk3s.stl"));

        // 4. Toolhead
        ToolheadDto toolhead = new ToolheadDto();
        toolhead.setMaxBedTemperature(new TemperatureDto(new BigDecimal("120"), TemperatureUnit.CELSIUS));

        LoaderInputDto loaderInput = new LoaderInputDto();
        loaderInput.addFilamentDiameter(new DimensionDto(new BigDecimal("1.75"), LengthUnit.MILLIMETER));
        loaderInput.addFilamentType("PLA");
        loaderInput.addFilamentType("PETG");

        LoaderDto loader = new LoaderDto();
        loader.addLoaderInput(loaderInput);
        toolhead.addLoader(loader);

        PrinterDto extruder = new PrinterDto();
        extruder.addPrintDiameter(new DimensionDto(new BigDecimal("0.4"), LengthUnit.MILLIMETER));
        extruder.addMaxPrintTemperature(new TemperatureDto(new BigDecimal("300"), TemperatureUnit.CELSIUS));
        extruder.setRetractionSpeed(new SpeedDto(new BigDecimal("35"), SpeedUnitDto.MILLIMETERS_PER_SECOND));
        toolhead.addPrinterHead(extruder);

        // 5. Root Printer3D
        Printer3DDto printer = new Printer3DDto();
        printer.setPrinterSystem(system);
        printer.setTopology(topology);
        printer.addToolhead(toolhead);

        // Assertions
        assertNotNull(printer.getPrinterSystem());
        assertEquals("Prusa Research", printer.getPrinterSystem().getPrinterManufacturer());
        assertEquals("Marlin", printer.getPrinterSystem().getGCodeInterpreter());
        assertEquals(1, printer.getPrinterSystem().getPrinterActions().size());
        assertEquals("G28", printer.getPrinterSystem().getPrinterActions().get(0).getActionCode());

        assertNotNull(printer.getTopology());
        assertEquals(AreaShape.RECTANGLE, printer.getTopology().getPrintArea().getShape());
        assertEquals(3, printer.getTopology().getPrintArea().getDimensions().size());
        assertEquals(new BigDecimal("250"), printer.getTopology().getPrintArea().getDimensions().get(0).getValue());
        
        assertNotNull(printer.getTopology().getMovementLimits());
        assertEquals(new BigDecimal("200"), printer.getTopology().getMovementLimits().getMaximumSpeed().getValue());
        assertEquals(DirectionChangeLimitType.JERK, printer.getTopology().getMovementLimits().getDirectionChangeSpeed().getType());

        assertEquals(1, printer.getToolhead().size());
        assertEquals(new BigDecimal("120"), printer.getToolhead().get(0).getMaxBedTemperature().getValue());
        
        assertEquals(1, printer.getToolhead().get(0).getLoaders().size());
        assertEquals(1, printer.getToolhead().get(0).getLoaders().get(0).getLoaderInputs().size());
        assertEquals(new BigDecimal("1.75"), printer.getToolhead().get(0).getLoaders().get(0).getLoaderInputs().get(0).getFilamentDiameters().get(0).getValue());
        assertTrue(printer.getToolhead().get(0).getLoaders().get(0).getLoaderInputs().get(0).getFilamentTypes().contains("PLA"));

        assertEquals(1, printer.getToolhead().get(0).getPrinterHeads().size());
        assertEquals(new BigDecimal("0.4"), printer.getToolhead().get(0).getPrinterHeads().get(0).getPrintDiameters().get(0).getValue());
    }
}
