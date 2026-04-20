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

    @Test
    void testSaveAllConfigDoesNotSaveZeros() {
        // 1. Initial state with valid dimensions
        AppConfig config = new AppConfig();
        AppConfigDto dto = config.loadToDto();
        dto.setWindowWidth(1024);
        dto.setWindowHeight(768);
        config.saveFromDto(dto);

        // 2. Create app and simulate zero dimensions (e.g. during minimization)
        DesktopApp app = new DesktopApp();
        app.currentWidth = 0;
        app.currentHeight = 0;

        // 3. Save config - should NOT overwrite with zeros
        app.saveAllConfig();

        // 4. Verify that dimensions are still valid in config
        AppConfig config2 = new AppConfig();
        AppConfigDto dto2 = config2.loadToDto();
        assertEquals(1024, dto2.getWindowWidth(), "Should preserve last valid width");
        assertEquals(768, dto2.getWindowHeight(), "Should preserve last valid height");
    }

    @Test
    void testResizeIgnoresZeros() {
        DesktopApp app = new DesktopApp();
        app.currentWidth = 800;
        app.currentHeight = 600;

        app.resize(0, 0);

        assertEquals(800, app.currentWidth, "Resize to 0 should be ignored for width");
        assertEquals(600, app.currentHeight, "Resize to 0 should be ignored for height");

        app.resize(1024, 768);
        assertEquals(1024, app.currentWidth, "Valid resize should be accepted");
        assertEquals(768, app.currentHeight, "Valid resize should be accepted");
    }

    @Test
    void testAppConstructorInitializesFromConfig() {
        AppConfig config = new AppConfig();
        AppConfigDto dto = config.loadToDto();
        dto.setWindowWidth(1111);
        dto.setWindowHeight(2222);
        config.saveFromDto(dto);

        DesktopApp app = new DesktopApp();
        assertEquals(1111, app.currentWidth, "Constructor should load width from config");
        assertEquals(2222, app.currentHeight, "Constructor should load height from config");
    }
}
