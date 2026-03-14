package cz.ad.print3d.aslicer.logic.model.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Utility class for storing files and creating random directories in a predefined storage location.
 * The default location is a folder named 'storage' within the system's temporary directory.
 */
public class FileStorage {

    private final Path basePath;

    /**
     * Creates a new FileStorage with the default base path.
     * The default path is 'storage' located in the system temporary folder.
     */
    public FileStorage() {
        this(Paths.get(System.getProperty("java.io.tmpdir"), "storage"));
    }

    /**
     * Creates a new FileStorage with a specific base path.
     *
     * @param basePath the base directory to use for storage
     */
    public FileStorage(final Path basePath) {
        this.basePath = basePath;
    }

    /**
     * Creates a random named directory within the predefined base folder.
     *
     * @return the path to the newly created random directory
     * @throws IOException if an I/O error occurs during directory creation
     */
    public Path createRandomDirectory() throws IOException {
        final Path randomPath = basePath.resolve(UUID.randomUUID().toString());
        Files.createDirectories(randomPath);
        return randomPath;
    }

    /**
     * Stores a file from an input stream into the specified directory with the given name.
     *
     * @param input    the input stream to read the file content from
     * @param fileName the name of the file to be stored
     * @param targetDir the directory where the file should be saved
     * @return the path to the stored file
     * @throws IOException if an I/O error occurs during file storage
     */
    public Path storeFile(final InputStream input, final String fileName, final Path targetDir) throws IOException {
        final Path filePath = targetDir.resolve(fileName);
        // Ensure any subdirectories in fileName also exist
        Files.createDirectories(filePath.getParent());
        Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath;
    }

    /**
     * Returns the base path of this storage.
     *
     * @return the predefined base folder path
     */
    public Path getBasePath() {
        return basePath;
    }
}
