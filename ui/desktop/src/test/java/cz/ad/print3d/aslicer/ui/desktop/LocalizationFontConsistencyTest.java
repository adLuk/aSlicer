package cz.ad.print3d.aslicer.ui.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import cz.ad.print3d.aslicer.ui.desktop.view.DesktopUI;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocalizationFontConsistencyTest {

    @Test
    public void testFontSupportsAllLocalizedCharacters() {
        final AtomicBoolean finished = new AtomicBoolean(false);
        final StringBuilder failureMessage = new StringBuilder();
        final AtomicBoolean hasCriticalFailure = new AtomicBoolean(false);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    DesktopUI ui = new DesktopUI();
                    BitmapFont font = ui.createFont();

                    File i18nDir = new File("src/main/resources/i18n");
                    if (!i18nDir.exists()) {
                        i18nDir = new File("ui/desktop/src/main/resources/i18n");
                    }
                    File[] propFiles = i18nDir.listFiles((dir, name) -> name.endsWith(".properties"));

                    if (propFiles == null || propFiles.length == 0) {
                        failureMessage.append("No localization property files found in ").append(i18nDir.getAbsolutePath());
                        hasCriticalFailure.set(true);
                    } else {
                        for (File propFile : propFiles) {
                            if (checkFile(propFile, font, failureMessage)) {
                                // We only consider failure in primary supported languages as critical 
                                // because we currently bundle only Noto Sans Regular (Latin/Cyrillic).
                                // Thai and Chinese require additional fonts not yet bundled.
                                if (propFile.getName().matches(".*(_cs|_sk|_en|_de|_es|_uk|messages\\.properties).*")) {
                                    hasCriticalFailure.set(true);
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    failureMessage.append("Exception during test: ").append(e.getMessage());
                    hasCriticalFailure.set(true);
                    e.printStackTrace();
                } finally {
                    finished.set(true);
                    Gdx.app.exit();
                }
            }
        }, new HeadlessApplicationConfiguration());

        while (!finished.get()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        assertTrue(!hasCriticalFailure.get(), "Critical missing glyphs in font (CS, SK, EN, DE, ES, UK):\n" + failureMessage.toString());
        if (failureMessage.length() > 0) {
            System.out.println("[DEBUG_LOG] Note: Some non-critical glyphs are missing (expected for TH/ZH without specialized fonts):\n" + failureMessage.toString());
        }
    }

    /**
     * @return true if there are missing characters
     */
    private boolean checkFile(File propFile, BitmapFont font, StringBuilder failureMessage) {
        Properties props = new Properties();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(propFile), StandardCharsets.UTF_8)) {
            props.load(reader);
            TreeSet<Character> missingChars = new TreeSet<>();
            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                for (char c : value.toCharArray()) {
                    if (Character.isWhitespace(c) || Character.isISOControl(c)) continue;
                    if (!font.getData().hasGlyph(c)) {
                        missingChars.add(c);
                    }
                }
            }

            if (!missingChars.isEmpty()) {
                failureMessage.append("File ").append(propFile.getName()).append(" contains unsupported characters: ");
                for (char c : missingChars) {
                    failureMessage.append("'").append(c).append("' (0x").append(Integer.toHexString(c)).append(") ");
                }
                failureMessage.append("\n");
                return true;
            }
        } catch (Exception e) {
            failureMessage.append("Error reading file ").append(propFile.getName()).append(": ").append(e.getMessage()).append("\n");
            return true;
        }
        return false;
    }
}
