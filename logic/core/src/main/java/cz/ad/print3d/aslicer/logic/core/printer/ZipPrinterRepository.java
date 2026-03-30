package cz.ad.print3d.aslicer.logic.core.printer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import cz.ad.print3d.aslicer.logic.model.basic.Acceleration;
import cz.ad.print3d.aslicer.logic.model.basic.Dimension;
import cz.ad.print3d.aslicer.logic.model.basic.Speed;
import cz.ad.print3d.aslicer.logic.model.basic.Temperature;
import cz.ad.print3d.aslicer.logic.model.basic.dto.AccelerationDto;
import cz.ad.print3d.aslicer.logic.model.basic.dto.DimensionDto;
import cz.ad.print3d.aslicer.logic.model.basic.dto.SpeedDto;
import cz.ad.print3d.aslicer.logic.model.basic.dto.TemperatureDto;
import cz.ad.print3d.aslicer.logic.printer.Printer3D;
import cz.ad.print3d.aslicer.logic.printer.PrinterRepository;
import cz.ad.print3d.aslicer.logic.printer.dto.*;
import cz.ad.print3d.aslicer.logic.printer.system.PrinterSystem;
import cz.ad.print3d.aslicer.logic.printer.system.action.PrinterAction;
import cz.ad.print3d.aslicer.logic.printer.system.action.PrinterActionType;
import cz.ad.print3d.aslicer.logic.printer.system.action.PrinterSystemActionType;
import cz.ad.print3d.aslicer.logic.printer.toolhead.Toolhead;
import cz.ad.print3d.aslicer.logic.printer.toolhead.loader.Loader;
import cz.ad.print3d.aslicer.logic.printer.toolhead.loader.LoaderInput;
import cz.ad.print3d.aslicer.logic.printer.toolhead.printer.Printer;
import cz.ad.print3d.aslicer.logic.printer.topology.Topology;
import cz.ad.print3d.aslicer.logic.printer.topology.area.GenericArea;
import cz.ad.print3d.aslicer.logic.printer.topology.geometry.Geometry;
import cz.ad.print3d.aslicer.logic.printer.topology.geometry.MovementArea;
import cz.ad.print3d.aslicer.logic.printer.topology.geometry.PrintArea;
import cz.ad.print3d.aslicer.logic.printer.topology.geometry.WorkArea;
import cz.ad.print3d.aslicer.logic.printer.topology.limit.DirectionChangeLimit;
import cz.ad.print3d.aslicer.logic.printer.topology.limit.MovementLimits;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Implementation of {@link PrinterRepository} that stores printer configurations in a ZIP file.
 * <p>
 * The ZIP file structure follows a group-based organization where each group is a folder,
 * and each printer configuration is a JSON file within that folder.
 * </p>
 * <p>
 * This implementation uses Jackson for JSON serialization and the {@code zip4j} library
 * for managing the ZIP file content, supporting optional password protection and AES encryption.
 * </p>
 *
 * @author Senior Architect
 * @since 1.0.0
 */
public class ZipPrinterRepository implements PrinterRepository {

    private static final Logger logger = LoggerFactory.getLogger(ZipPrinterRepository.class);
    private static final String JSON_EXTENSION = ".json";

    private final Path zipPath;
    private final char[] password;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new ZipPrinterRepository using the specified ZIP file path.
     *
     * @param zipPath the path to the ZIP file used for storage
     * @throws IOException if an I/O error occurs during initialization
     */
    public ZipPrinterRepository(Path zipPath) throws IOException {
        this(zipPath, null);
    }

    /**
     * Constructs a new ZipPrinterRepository using the specified ZIP file path and password.
     *
     * @param zipPath  the path to the ZIP file used for storage
     * @param password the password for the ZIP file, or {@code null} if no password is required
     * @throws IOException if an I/O error occurs during initialization
     */
    public ZipPrinterRepository(Path zipPath, String password) throws IOException {
        this.zipPath = zipPath;
        this.password = (password != null) ? password.toCharArray() : null;
        this.objectMapper = createObjectMapper();
        ensureZipFileExists();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        SimpleModule module = new SimpleModule();
        module.addAbstractTypeMapping(Printer3D.class, Printer3DDto.class);
        module.addAbstractTypeMapping(PrinterSystem.class, PrinterSystemDto.class);
        module.addAbstractTypeMapping(PrinterAction.class, PrinterActionDto.class);
        module.addAbstractTypeMapping(PrinterActionType.class, PrinterSystemActionType.class);
        
        // Handling PrinterNetConnection with Jackson's built-in type info instead of SimpleModule
        // module.addAbstractTypeMapping(PrinterNetConnection.class, PrinterNetConnectionDto.class);
        // module.addAbstractTypeMapping(NetworkPrinterNetConnection.class, NetworkPrinterNetConnectionDto.class);
        // module.addAbstractTypeMapping(BambuPrinterNetConnection.class, BambuPrinterNetConnectionDto.class);
        
        module.addAbstractTypeMapping(Topology.class, TopologyDto.class);
        module.addAbstractTypeMapping(Toolhead.class, ToolheadDto.class);
        module.addAbstractTypeMapping(Loader.class, LoaderDto.class);
        module.addAbstractTypeMapping(LoaderInput.class, LoaderInputDto.class);
        module.addAbstractTypeMapping(Printer.class, PrinterDto.class);
        module.addAbstractTypeMapping(Geometry.class, GeometryDto.class);
        module.addAbstractTypeMapping(GenericArea.class, GenericAreaDto.class);
        module.addAbstractTypeMapping(PrintArea.class, PrintAreaDto.class);
        module.addAbstractTypeMapping(WorkArea.class, WorkAreaDto.class);
        module.addAbstractTypeMapping(MovementArea.class, MovementAreaDto.class);
        module.addAbstractTypeMapping(MovementLimits.class, MovementLimitsDto.class);
        module.addAbstractTypeMapping(DirectionChangeLimit.class, DirectionChangeLimitDto.class);
        module.addAbstractTypeMapping(Dimension.class, DimensionDto.class);
        module.addAbstractTypeMapping(Temperature.class, TemperatureDto.class);
        module.addAbstractTypeMapping(Speed.class, SpeedDto.class);
        module.addAbstractTypeMapping(Acceleration.class, AccelerationDto.class);

        mapper.registerModule(module);
        return mapper;
    }

    private void ensureZipFileExists() throws IOException {
        if (Files.notExists(zipPath)) {
            Files.createDirectories(zipPath.getParent());
            // Create the ZIP file if it doesn't exist by initializing it.
            // zip4j handles creation when the first entry is added.
            logger.info("Initialized storage path for ZIP repository at: {}", zipPath);
        }
    }

    private ZipFile createZipFile() {
        ZipFile zipFile = new ZipFile(zipPath.toFile());
        if (password != null) {
            zipFile.setPassword(password);
        }
        return zipFile;
    }

    private ZipParameters createZipParameters() {
        ZipParameters parameters = new ZipParameters();
        if (password != null) {
            parameters.setEncryptFiles(true);
            parameters.setEncryptionMethod(EncryptionMethod.AES);
            parameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
        }
        return parameters;
    }

    @Override
    public List<String> getGroups() {
        try (ZipFile zipFile = createZipFile()) {
            if (!zipFile.getFile().exists()) {
                return Collections.emptyList();
            }
            Set<String> groups = new HashSet<>();
            List<FileHeader> fileHeaders = zipFile.getFileHeaders();
            for (FileHeader header : fileHeaders) {
                String fileName = header.getFileName();
                int slashIndex = fileName.indexOf('/');
                if (slashIndex > 0) {
                    groups.add(fileName.substring(0, slashIndex));
                }
            }
            return new ArrayList<>(groups);
        } catch (IOException e) {
            logger.error("Error reading groups from ZIP repository", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Printer3D> getPrintersByGroup(String groupName) {
        Map<String, Printer3D> printers = new HashMap<>();
        String prefix = groupName + "/";
        try (ZipFile zipFile = createZipFile()) {
            if (!zipFile.getFile().exists()) {
                return printers;
            }
            List<FileHeader> fileHeaders = zipFile.getFileHeaders();
            for (FileHeader header : fileHeaders) {
                String fileName = header.getFileName();
                if (fileName.startsWith(prefix) && fileName.endsWith(JSON_EXTENSION)) {
                    String printerName = fileName.substring(prefix.length(), fileName.length() - JSON_EXTENSION.length());
                    try (InputStream is = zipFile.getInputStream(header)) {
                        printers.put(printerName, objectMapper.readValue(is, Printer3D.class));
                    } catch (IOException e) {
                        logger.error("Error reading printer {} from group {}", printerName, groupName, e);
                        throw new RuntimeException("Error reading printer " + printerName, e);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error accessing ZIP repository for group: {}", groupName, e);
        }
        return printers;
    }

    @Override
    public Optional<Printer3D> getPrinter(String groupName, String printerName) {
        String fileName = groupName + "/" + printerName + JSON_EXTENSION;
        try (ZipFile zipFile = createZipFile()) {
            if (!zipFile.getFile().exists()) {
                return Optional.empty();
            }
            FileHeader header = zipFile.getFileHeader(fileName);
            if (header != null) {
                try (InputStream is = zipFile.getInputStream(header)) {
                    return Optional.of(objectMapper.readValue(is, Printer3D.class));
                }
            } else {
                logger.warn("Printer file does not exist in ZIP: {}", fileName);
            }
        } catch (IOException e) {
            logger.error("Error reading printer {} from group {}", printerName, groupName, e);
            throw new RuntimeException("Error reading printer " + printerName, e);
        }
        return Optional.empty();
    }

    @Override
    public void savePrinter(String groupName, String printerName, Printer3D printer) {
        String fileName = groupName + "/" + printerName + JSON_EXTENSION;
        try (ZipFile zipFile = createZipFile()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            objectMapper.writeValue(baos, printer);
            
            ZipParameters parameters = createZipParameters();
            parameters.setFileNameInZip(fileName);
            
            zipFile.addStream(new ByteArrayInputStream(baos.toByteArray()), parameters);
        } catch (IOException e) {
            logger.error("Error saving printer {} to group {}", printerName, groupName, e);
            throw new RuntimeException("Failed to save printer to ZIP repository", e);
        }
    }

    @Override
    public boolean deletePrinter(String groupName, String printerName) {
        String fileName = groupName + "/" + printerName + JSON_EXTENSION;
        try (ZipFile zipFile = createZipFile()) {
            if (!zipFile.getFile().exists()) {
                return false;
            }
            FileHeader header = zipFile.getFileHeader(fileName);
            if (header != null) {
                zipFile.removeFile(header);
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error("Error deleting printer {} from group {}", printerName, groupName, e);
            return false;
        }
    }

    @Override
    public boolean deleteGroup(String groupName) {
        String prefix = groupName + "/";
        try (ZipFile zipFile = createZipFile()) {
            if (!zipFile.getFile().exists()) {
                return false;
            }
            List<FileHeader> fileHeaders = zipFile.getFileHeaders();
            List<String> filesToRemove = new ArrayList<>();
            for (FileHeader header : fileHeaders) {
                if (header.getFileName().startsWith(prefix)) {
                    filesToRemove.add(header.getFileName());
                }
            }
            if (!filesToRemove.isEmpty()) {
                zipFile.removeFiles(filesToRemove);
                return true;
            }
        } catch (IOException e) {
            logger.error("Error deleting group {}", groupName, e);
        }
        return false;
    }
}
