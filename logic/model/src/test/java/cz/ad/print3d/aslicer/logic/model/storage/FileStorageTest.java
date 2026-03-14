package cz.ad.print3d.aslicer.logic.model.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FileStorage}.
 */
public class FileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    public void testDefaultConstructor() {
        FileStorage storage = new FileStorage();
        Path expected = Paths.get(System.getProperty("java.io.tmpdir"), "storage");
        assertEquals(expected, storage.getBasePath());
    }

    @Test
    public void testCustomBasePath() {
        FileStorage storage = new FileStorage(tempDir);
        assertEquals(tempDir, storage.getBasePath());
    }

    @Test
    public void testCreateRandomDirectory() throws IOException {
        FileStorage storage = new FileStorage(tempDir);
        Path randomDir = storage.createRandomDirectory();
        
        assertNotNull(randomDir);
        assertTrue(Files.exists(randomDir));
        assertTrue(Files.isDirectory(randomDir));
        assertEquals(tempDir.toAbsolutePath(), randomDir.getParent().toAbsolutePath());
        assertNotEquals(tempDir.toAbsolutePath(), randomDir.toAbsolutePath());
    }

    @Test
    public void testStoreFile() throws IOException {
        FileStorage storage = new FileStorage(tempDir);
        Path targetDir = storage.createRandomDirectory();
        String fileName = "test.txt";
        String content = "Hello World";
        
        try (ByteArrayInputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            Path storedFile = storage.storeFile(is, fileName, targetDir);
            
            assertNotNull(storedFile);
            assertTrue(Files.exists(storedFile));
            assertEquals(content, Files.readString(storedFile, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testStoreFileInSubdirectory() throws IOException {
        FileStorage storage = new FileStorage(tempDir);
        Path targetDir = storage.createRandomDirectory();
        String fileName = "subdir/test.txt";
        String content = "Hello Subworld";
        
        try (ByteArrayInputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            Path storedFile = storage.storeFile(is, fileName, targetDir);
            
            assertNotNull(storedFile);
            assertTrue(Files.exists(storedFile));
            assertEquals(content, Files.readString(storedFile, StandardCharsets.UTF_8));
            assertTrue(Files.exists(targetDir.resolve("subdir")));
        }
    }
}
