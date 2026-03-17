package cz.ad.print3d.aslicer.ui.desktop.config;
import cz.ad.print3d.aslicer.ui.desktop.DesktopApp;
import cz.ad.print3d.aslicer.ui.desktop.config.*;
import cz.ad.print3d.aslicer.ui.desktop.persistence.*;
import cz.ad.print3d.aslicer.ui.desktop.view.*;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AppConfigPathTest {

    @Test
    void testConfigPath() {
        Path expectedPath = Paths.get(System.getProperty("user.home"), ".aslicer", "aslicer.properties");
        assertEquals(expectedPath, AppConfig.CONFIG_PATH, "Config path should be in ~/.aslicer/aslicer.properties");
    }
}
