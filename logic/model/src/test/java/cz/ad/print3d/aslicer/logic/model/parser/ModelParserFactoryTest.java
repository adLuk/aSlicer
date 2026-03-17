package cz.ad.print3d.aslicer.logic.model.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ModelParserFactoryTest {

    @TempDir
    Path tempDir;

    @Test
    public void testParseStl() throws IOException {
        Path file = tempDir.resolve("test.stl");
        Files.write(file, "solid test\nendsolid test".getBytes());
        assertNotNull(ModelParserFactory.parse(file));
    }

    @Test
    public void testParseAst() throws IOException {
        Path file = tempDir.resolve("test.ast");
        Files.write(file, "solid test\nendsolid test".getBytes());
        // This is expected to fail currently
        assertNotNull(ModelParserFactory.parse(file));
    }

    @Test
    public void testParse3mf() throws IOException {
        Path file = tempDir.resolve("test.3mf");
        Files.write(file, new byte[100]); 
        // 3MF parser might fail on invalid zip, but here we just check if it matches extension
        // If it throws anything other than IOException related to "Unsupported format", it means it matched
        try {
            ModelParserFactory.parse(file);
        } catch (IOException e) {
            if (e.getMessage().contains("Unsupported file format")) {
                throw e;
            }
        }
    }

    @Test
    public void testParseGcode() throws IOException {
        Path file = tempDir.resolve("test.gcode");
        Files.write(file, "G1 X0 Y0".getBytes());
        assertNotNull(ModelParserFactory.parse(file));
    }

    @Test
    public void testUnsupportedFormat() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.write(file, "some content".getBytes());
        assertThrows(IOException.class, () -> ModelParserFactory.parse(file));
    }
}
