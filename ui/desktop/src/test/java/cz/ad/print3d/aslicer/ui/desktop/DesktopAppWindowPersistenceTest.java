package cz.ad.print3d.aslicer.ui.desktop;

import cz.ad.print3d.aslicer.ui.desktop.config.AppConfig;
import cz.ad.print3d.aslicer.ui.desktop.config.AppConfigDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for window size persistence.
 */
public class DesktopAppWindowPersistenceTest {

    @TempDir
    Path tempDir;

    private Path originalConfigPath;

    @BeforeEach
    void setUp() {
        originalConfigPath = AppConfig.CONFIG_PATH;
        AppConfig.CONFIG_PATH = tempDir.resolve("aslicer.properties");
    }

    @AfterEach
    void tearDown() {
        AppConfig.CONFIG_PATH = originalConfigPath;
    }

    @Test
    void testWindowSizePersistence() {
        // 1. Setup - save specific dimensions
        AppConfig config = new AppConfig();
        AppConfigDto dto = config.loadToDto();
        dto.setWindowWidth(1000);
        dto.setWindowHeight(800);
        config.saveFromDto(dto);

        // 2. Load and verify dimensions are recovered correctly
        AppConfig config2 = new AppConfig();
        AppConfigDto dto2 = config2.loadToDto();
        assertEquals(1000, dto2.getWindowWidth(), "Window width should be persisted");
        assertEquals(800, dto2.getWindowHeight(), "Window height should be persisted");
    }

    @Test
    void testDefaultWindowSize() {
        // Should use defaults if no config exists
        AppConfig config = new AppConfig();
        AppConfigDto dto = config.loadToDto();
        assertEquals(1280, dto.getWindowWidth(), "Default window width should be 1280");
        assertEquals(720, dto.getWindowHeight(), "Default window height should be 720");
    }
}
