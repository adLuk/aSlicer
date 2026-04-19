package cz.ad.print3d.aslicer.ui.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for I18N localization system.
 */
public class I18NTest {

    private HeadlessApplication app;

    @BeforeEach
    void setUp() {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        app = new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                GdxTestUtils.mockGdxGL();
            }
        }, config);
    }

    @AfterEach
    void tearDown() {
        if (app != null) {
            app.exit();
        }
    }

    @Test
    void testInitWithLocale() {
        final AtomicBoolean finished = new AtomicBoolean(false);
        Gdx.app.postRunnable(() -> {
            try {
                Locale csLocale = Locale.forLanguageTag("cs-CZ");
                I18N.init(csLocale);
                assertEquals(csLocale, I18N.getCurrentLocale());
                
                // app.title in messages_cs.properties is "aSlicer - nástroj pro zpracování 3D modelů"
                String title = I18N.get("app.title");
                assertTrue(title.contains("nástroj") || title.contains("3D"), "Should load Czech translation");

                Locale enLocale = Locale.ENGLISH;
                I18N.init(enLocale);
                assertEquals(enLocale, I18N.getCurrentLocale());
                
                // app.title in messages.properties is "aSlicer - 3D model processing tool"
                title = I18N.get("app.title");
                assertTrue(title.contains("tool") || title.contains("processing"), "Should load English translation");
            } finally {
                finished.set(true);
            }
        });

        while (!finished.get()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    void testDefaultLocale() {
        final AtomicBoolean finished = new AtomicBoolean(false);
        Gdx.app.postRunnable(() -> {
            try {
                I18N.init(Locale.US);
                assertEquals(Locale.US, I18N.getCurrentLocale());
            } finally {
                finished.set(true);
            }
        });

        while (!finished.get()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
