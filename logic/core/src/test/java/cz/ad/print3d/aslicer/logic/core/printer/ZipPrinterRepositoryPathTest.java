package cz.ad.print3d.aslicer.logic.core.printer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Test for {@link ZipPrinterRepository} with various path scenarios.
 * <p>Specifically tests handling of paths that might not have a parent directory
 * to ensure robust initialization.</p>
 */
public class ZipPrinterRepositoryPathTest {

    @TempDir
    Path tempDir;

    @Test
    void testInitWithNoParentPath() throws IOException {
        // Change current directory or use a path without parent
        Path currentDir = Paths.get("").toAbsolutePath();
        Path testZip = currentDir.resolve("test_printers.zip");
        
        // Using a path that has no parent (relative to current dir)
        Path relativePath = Paths.get("test_printers_no_parent.zip");
        
        try {
            assertDoesNotThrow(() -> {
                new ZipPrinterRepository(relativePath);
            });
        } finally {
            Files.deleteIfExists(relativePath);
        }
    }
}
