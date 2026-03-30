package cz.ad.print3d.aslicer.logic.core.printer;

import cz.ad.print3d.aslicer.logic.printer.Printer3D;
import cz.ad.print3d.aslicer.logic.printer.dto.*;
import cz.ad.print3d.aslicer.logic.printer.system.action.PrinterSystemActionType;
import cz.ad.print3d.aslicer.logic.printer.system.net.dto.BambuPrinterNetConnectionDto;
import cz.ad.print3d.aslicer.logic.printer.system.net.dto.NetworkPrinterNetConnectionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ZipPrinterRepositoryTest {

    @TempDir
    Path tempDir;

    private Path zipPath;
    private ZipPrinterRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        zipPath = tempDir.resolve("printers.zip");
        repository = new ZipPrinterRepository(zipPath);
    }

    @Test
    void testSaveAndGetPrinter() {
        String group = "BambuLab";
        String name = "X1C";
        
        Printer3DDto printer = createSamplePrinter("Bambu Lab", "X1C");
        
        repository.savePrinter(group, name, printer);
        
        Optional<Printer3D> retrieved = repository.getPrinter(group, name);
        assertTrue(retrieved.isPresent());
        assertEquals("Bambu Lab", retrieved.get().getPrinterSystem().getPrinterManufacturer());
        assertEquals("X1C", retrieved.get().getPrinterSystem().getPrinterName());
    }

    @Test
    void testGetGroups() {
        repository.savePrinter("Group1", "Printer1", createSamplePrinter("M1", "P1"));
        repository.savePrinter("Group2", "Printer2", createSamplePrinter("M2", "P2"));
        
        List<String> groups = repository.getGroups();
        assertEquals(2, groups.size());
        assertTrue(groups.contains("Group1"));
        assertTrue(groups.contains("Group2"));
    }

    @Test
    void testGetPrintersByGroup() {
        repository.savePrinter("Group1", "P1", createSamplePrinter("M1", "P1"));
        repository.savePrinter("Group1", "P2", createSamplePrinter("M1", "P2"));
        
        Map<String, Printer3D> printers = repository.getPrintersByGroup("Group1");
        assertEquals(2, printers.size());
        assertTrue(printers.containsKey("P1"));
        assertTrue(printers.containsKey("P2"));
    }

    @Test
    void testDeletePrinter() {
        repository.savePrinter("Group1", "P1", createSamplePrinter("M1", "P1"));
        assertTrue(repository.getPrinter("Group1", "P1").isPresent());
        
        boolean deleted = repository.deletePrinter("Group1", "P1");
        assertTrue(deleted);
        assertFalse(repository.getPrinter("Group1", "P1").isPresent());
    }

    @Test
    void testDeleteGroup() {
        repository.savePrinter("Group1", "P1", createSamplePrinter("M1", "P1"));
        repository.savePrinter("Group1", "P2", createSamplePrinter("M1", "P2"));
        
        boolean deleted = repository.deleteGroup("Group1");
        assertTrue(deleted);
        assertTrue(repository.getPrintersByGroup("Group1").isEmpty());
        assertFalse(repository.getGroups().contains("Group1"));
    }

    @Test
    void testComplexPrinterConfiguration() {
        String group = "Complex";
        String name = "FullPrinter";
        
        Printer3DDto printer = createSamplePrinter("Manufacturer", "Model");
        
        // Add some toolhead info
        ToolheadDto toolhead = new ToolheadDto();
        toolhead.setMaxBedTemperature(new cz.ad.print3d.aslicer.logic.model.basic.dto.TemperatureDto(new BigDecimal("120"), cz.ad.print3d.aslicer.logic.model.basic.TemperatureUnit.CELSIUS));
        
        LoaderDto loader = new LoaderDto();
        LoaderInputDto input = new LoaderInputDto();
        input.addFilamentType("PLA");
        input.addFilamentType("ABS");
        loader.addLoaderInput(input);
        toolhead.addLoader(loader);
        
        printer.addToolhead(toolhead);
        
        // Add some printer system actions
        PrinterActionDto action = new PrinterActionDto();
        action.setType(PrinterSystemActionType.LOAD_MATERIAL);
        action.setActionCode("M109 S210");
        printer.getPrinterSystem().getPrinterActions().add(action);

        repository.savePrinter(group, name, printer);
        
        Optional<Printer3D> retrieved = repository.getPrinter(group, name);
        assertTrue(retrieved.isPresent());
        Printer3D p = retrieved.get();
        
        assertEquals(1, p.getToolhead().size());
        assertEquals(1, p.getToolhead().get(0).getLoaders().size());
        assertEquals(2, p.getToolhead().get(0).getLoaders().get(0).getLoaderInputs().get(0).getFilamentTypes().size());
        assertEquals(1, p.getPrinterSystem().getPrinterActions().size());
        assertEquals(PrinterSystemActionType.LOAD_MATERIAL, p.getPrinterSystem().getPrinterActions().get(0).getType());
    }

    @Test
    void testPrinterWithNetConnections() throws MalformedURLException {
        String group = "Networked";
        String name = "ConnectedPrinter";

        Printer3DDto printer = createSamplePrinter("Manufacturer", "NetworkModel");

        NetworkPrinterNetConnectionDto netConn = new NetworkPrinterNetConnectionDto();
        netConn.setPrinterUrl(new URL("http://192.168.1.100"));
        netConn.setPairingCode("123456");
        printer.addNetConnection("Primary", netConn);

        BambuPrinterNetConnectionDto bambuConn = new BambuPrinterNetConnectionDto();
        bambuConn.setPrinterUrl(new URL("http://192.168.1.101"));
        bambuConn.setSerial("SN001");
        bambuConn.setAccessCode("SECRET");
        printer.addNetConnection("Bambu", bambuConn);

        repository.savePrinter(group, name, printer);

        Optional<Printer3D> retrieved = repository.getPrinter(group, name);
        assertTrue(retrieved.isPresent());
        Printer3D p = retrieved.get();

        assertEquals(2, p.getNetConnections().size());
        assertTrue(p.getNetConnections().containsKey("Primary"));
        assertTrue(p.getNetConnections().containsKey("Bambu"));

        assertTrue(p.getNetConnections().get("Primary") instanceof NetworkPrinterNetConnectionDto);
        assertEquals("123456", ((NetworkPrinterNetConnectionDto) p.getNetConnections().get("Primary")).getPairingCode());

        assertTrue(p.getNetConnections().get("Bambu") instanceof BambuPrinterNetConnectionDto);
        assertEquals("SN001", ((BambuPrinterNetConnectionDto) p.getNetConnections().get("Bambu")).getSerial());
    }

    @Test
    void testPasswordProtectedRepository() throws IOException {
        Path secureZipPath = tempDir.resolve("secure_printers.zip");
        String password = "StrongPassword123";
        ZipPrinterRepository secureRepo = new ZipPrinterRepository(secureZipPath, password);

        String group = "SecureGroup";
        String name = "SecurePrinter";
        Printer3DDto printer = createSamplePrinter("SecureMaker", "SecureModel");

        secureRepo.savePrinter(group, name, printer);

        // Try to read with the correct password
        Optional<Printer3D> retrieved = secureRepo.getPrinter(group, name);
        assertTrue(retrieved.isPresent());
        assertEquals("SecureMaker", retrieved.get().getPrinterSystem().getPrinterManufacturer());

        // Try to read with a different repository instance and wrong password
        ZipPrinterRepository wrongPassRepo = new ZipPrinterRepository(secureZipPath, "WrongPassword");
        // Depending on zip4j behavior, getInputStream might throw exception or read wrong data.
        // ZipFile.getInputStream() usually throws exception if password is wrong during reading.
        assertThrows(RuntimeException.class, () -> wrongPassRepo.getPrinter(group, name));
        
        // Try to read with no password
        ZipPrinterRepository noPassRepo = new ZipPrinterRepository(secureZipPath);
        assertThrows(RuntimeException.class, () -> noPassRepo.getPrinter(group, name));
    }

    private Printer3DDto createSamplePrinter(String manufacturer, String name) {
        Printer3DDto printer = new Printer3DDto();
        PrinterSystemDto system = new PrinterSystemDto();
        system.setPrinterManufacturer(manufacturer);
        system.setPrinterName(name);
        printer.setPrinterSystem(system);
        
        TopologyDto topology = new TopologyDto();
        // Add minimal topology info if needed
        printer.setTopology(topology);
        
        return printer;
    }
}
