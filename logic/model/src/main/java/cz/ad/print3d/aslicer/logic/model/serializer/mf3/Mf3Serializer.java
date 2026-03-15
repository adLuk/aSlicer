package cz.ad.print3d.aslicer.logic.model.serializer.mf3;

import cz.ad.print3d.aslicer.logic.model.format.mf3.bambu.Mf3BambuConfig;
import cz.ad.print3d.aslicer.logic.model.format.mf3.bambu.Mf3BambuCustomGCode;
import cz.ad.print3d.aslicer.logic.model.format.mf3.contenttype.Mf3ContentTypes;
import cz.ad.print3d.aslicer.logic.model.format.mf3.contenttype.Mf3Default;
import cz.ad.print3d.aslicer.logic.model.format.mf3.contenttype.Mf3Override;
import cz.ad.print3d.aslicer.logic.model.format.mf3.core.Mf3Model;
import cz.ad.print3d.aslicer.logic.model.format.mf3.relationship.Mf3Relationship;
import cz.ad.print3d.aslicer.logic.model.format.mf3.relationship.Mf3Relationships;
import cz.ad.print3d.aslicer.logic.model.serializer.ModelSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Implementation of {@link ModelSerializer} for 3MF (3D Manufacturing Format) files.
 * This serializer is responsible for packaging model data into the ZIP-based 3MF structure.
 */
public class Mf3Serializer implements ModelSerializer<Mf3Model> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mf3Serializer.class);

    private static final String DEFAULT_MODEL_ENTRY = "3D/3dmodel.model";
    private static final String ROOT_RELS_ENTRY = "_rels/.rels";
    private static final String CONTENT_TYPES_ENTRY = "[Content_Types].xml";
    private static final String PRUSA_MODEL_CONFIG_ENTRY = "Metadata/Slic3r_PE_model.config";
    private static final String PRUSA_SETTINGS_ENTRY = "Metadata/Slic3r_PE.config";
    private static final String BAMBU_MODEL_CONFIG_ENTRY = "Metadata/model_settings.config";
    private static final String BAMBU_CUSTOM_GCODE_ENTRY = "Metadata/Bambu_Custom_GCode";
    private static final String MAIN_MODEL_REL_TYPE_01 = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel/mainmodel";
    private static final String MAIN_MODEL_REL_TYPE_11 = "http://schemas.microsoft.com/3dmanufacturing/2013/11/3dmodel/mainmodel";
    private static final String MAIN_MODEL_REL_TYPE_CORE = "http://schemas.microsoft.com/3dmanufacturing/core/2015/02/mainmodel";

    /**
     * Serializes the given {@link Mf3Model} into the specified binary output channel.
     * Since 3MF is a ZIP-based format, this method wraps the channel in a ZipOutputStream
     * to package the model XML data and potentially other resources into the 3MF container.
     *
     * @param model the 3MF model to serialize
     * @param channel the output 3MF binary channel
     * @throws IOException if an I/O error occurs during serialization
     */
    @Override
    public void serialize(Mf3Model model, WritableByteChannel channel) throws IOException {
        LOGGER.info("Starting 3MF package serialization");
        try (ZipOutputStream zos = new ZipOutputStream(Channels.newOutputStream(channel))) {
            // 1. Write [Content_Types].xml
            serializeContentTypes(model, zos);

            // 2. Identify the model path from root relationships
            String modelPath = DEFAULT_MODEL_ENTRY;
            Mf3Relationships rootRels = model.relationshipParts().get(ROOT_RELS_ENTRY);
            if (rootRels != null && rootRels.getRelationships() != null) {
                for (Mf3Relationship rel : rootRels.getRelationships()) {
                    String type = rel.getType();
                    if (MAIN_MODEL_REL_TYPE_01.equals(type) || MAIN_MODEL_REL_TYPE_11.equals(type) || MAIN_MODEL_REL_TYPE_CORE.equals(type)) {
                        modelPath = rel.getTarget();
                        if (modelPath.startsWith("/")) {
                            modelPath = modelPath.substring(1);
                        }
                    }
                }
            }
            LOGGER.debug("Target model path in 3MF: {}", modelPath);

            // Ensure we have a root relationship to the main model if none exists
            if (rootRels == null) {
                LOGGER.debug("Root relationships missing, creating default rels pointing to {}", modelPath);
                rootRels = new Mf3Relationships();
                Mf3Relationship modelRel = new Mf3Relationship();
                modelRel.setId("rel1");
                modelRel.setTarget("/" + modelPath);
                modelRel.setType(MAIN_MODEL_REL_TYPE_01);
                rootRels.getRelationships().add(modelRel);
                model.setRelationshipPart(ROOT_RELS_ENTRY, rootRels);
            }

            // 3. Write all relationship parts from the model DTO
            for (Map.Entry<String, Mf3Relationships> entry : model.relationshipParts().entrySet()) {
                LOGGER.trace("Serializing relationship part: {}", entry.getKey());
                serializeRelationships(entry.getValue(), entry.getKey(), zos);
            }

            // 4. Write the model XML itself
            LOGGER.debug("Serializing model XML to {}", modelPath);
            serializeModelXml(model, modelPath, zos);

            // 5. Write Prusa-specific configuration if present
            serializePrusaModelConfig(model, zos);
            serializePrusaSettings(model, zos);

            // 6. Write Bambu-specific configuration if present
            serializeBambuModelConfig(model, zos);
            serializeBambuCustomGCode(model, zos);

            // 7. Copy files from storage if present
            serializeStorageFiles(model.storagePath(), zos);
        }
        LOGGER.info("Finished 3MF package serialization successfully");
    }

    private void serializeContentTypes(Mf3Model model, ZipOutputStream zos) throws IOException {
        Mf3ContentTypes contentTypes = model.contentTypes();
        if (contentTypes == null) {
            LOGGER.debug("Content types missing, creating default ones");
            contentTypes = createDefaultContentTypes();
        }
        LOGGER.trace("Serializing content types to {}", CONTENT_TYPES_ENTRY);
        zos.putNextEntry(new ZipEntry(CONTENT_TYPES_ENTRY));
        marshal(contentTypes, zos);
        zos.closeEntry();
    }

    private Mf3ContentTypes createDefaultContentTypes() {
        Mf3ContentTypes contentTypes = new Mf3ContentTypes();
        
        Mf3Default relsDefault = new Mf3Default();
        relsDefault.setExtension("rels");
        relsDefault.setContentType("application/vnd.openxmlformats-package.relationships+xml");
        contentTypes.getDefaults().add(relsDefault);

        Mf3Default modelDefault = new Mf3Default();
        modelDefault.setExtension("model");
        modelDefault.setContentType("application/vnd.ms-package.3dmanufacturing-3dmodel+xml");
        contentTypes.getDefaults().add(modelDefault);

        return contentTypes;
    }

    private void serializeRelationships(Mf3Relationships relationships, String path, ZipOutputStream zos) throws IOException {
        zos.putNextEntry(new ZipEntry(path));
        marshal(relationships, zos);
        zos.closeEntry();
    }

    private void serializeModelXml(Mf3Model model, String path, ZipOutputStream zos) throws IOException {
        zos.putNextEntry(new ZipEntry(path));
        marshal(model, zos);
        zos.closeEntry();
    }

    private void serializePrusaModelConfig(Mf3Model model, ZipOutputStream zos) throws IOException {
        if (model.getPrusaSlicerModelConfig() != null) {
            LOGGER.debug("Serializing Prusa model config to {}", PRUSA_MODEL_CONFIG_ENTRY);
            zos.putNextEntry(new ZipEntry(PRUSA_MODEL_CONFIG_ENTRY));
            marshal(model.getPrusaSlicerModelConfig(), zos);
            zos.closeEntry();
        }
    }

    private void serializePrusaSettings(Mf3Model model, ZipOutputStream zos) throws IOException {
        if (model.getPrusaSettings() != null) {
            LOGGER.debug("Serializing Prusa settings to {}", PRUSA_SETTINGS_ENTRY);
            zos.putNextEntry(new ZipEntry(PRUSA_SETTINGS_ENTRY));
            zos.write(model.getPrusaSettings().serialize().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    private void serializeBambuModelConfig(Mf3Model model, ZipOutputStream zos) throws IOException {
        if (model.getBambuConfig() != null) {
            LOGGER.debug("Serializing Bambu model config to {}", BAMBU_MODEL_CONFIG_ENTRY);
            zos.putNextEntry(new ZipEntry(BAMBU_MODEL_CONFIG_ENTRY));
            marshal(model.getBambuConfig(), zos);
            zos.closeEntry();
        }
    }

    private void serializeBambuCustomGCode(Mf3Model model, ZipOutputStream zos) throws IOException {
        if (model.getBambuCustomGCode() != null) {
            LOGGER.debug("Serializing Bambu custom G-code to {}", BAMBU_CUSTOM_GCODE_ENTRY);
            zos.putNextEntry(new ZipEntry(BAMBU_CUSTOM_GCODE_ENTRY));
            final ObjectMapper mapper = new ObjectMapper();
            // Don't use mapper.writeValue(zos, ...) because it closes the stream
            byte[] bytes = mapper.writeValueAsBytes(model.getBambuCustomGCode());
            zos.write(bytes);
            zos.closeEntry();
        }
    }

    private void serializeStorageFiles(Path storagePath, ZipOutputStream zos) throws IOException {
        if (storagePath == null || !Files.exists(storagePath)) {
            return;
        }
        LOGGER.info("Including supplementary files from storage: {}", storagePath);
        try (Stream<Path> paths = Files.walk(storagePath)) {
            List<Path> filePaths = paths.filter(Files::isRegularFile).toList();
            for (Path filePath : filePaths) {
                String entryName = storagePath.relativize(filePath).toString().replace('\\', '/');
                LOGGER.trace("Adding supplementary entry to ZIP: {}", entryName);
                zos.putNextEntry(new ZipEntry(entryName));
                try (InputStream is = Files.newInputStream(filePath)) {
                    is.transferTo(zos);
                }
                zos.closeEntry();
            }
        }
    }

    private String getRelsPathFor(String partPath) {
        int lastSlash = partPath.lastIndexOf('/');
        if (lastSlash == -1) {
            return "_rels/" + partPath + ".rels";
        }
        return partPath.substring(0, lastSlash) + "/_rels/" + partPath.substring(lastSlash + 1) + ".rels";
    }

    private void marshal(Object object, OutputStream os) throws IOException {
        try {
            JAXBContext context = JAXBContext.newInstance(object.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE);
            marshaller.marshal(object, os);
        } catch (JAXBException e) {
            throw new IOException("Failed to serialize XML content", e);
        }
    }
}
