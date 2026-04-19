package cz.ad.print3d.aslicer.ui.desktop.view;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import cz.ad.print3d.aslicer.ui.desktop.GdxTestUtils;
import cz.ad.print3d.aslicer.ui.desktop.I18N;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class LanguageSelectBoxTest {

    @Test
    void testLanguageSelection() throws InterruptedException {
        final AtomicBoolean finished = new AtomicBoolean(false);
        final AtomicBoolean success = new AtomicBoolean(false);
        final AtomicBoolean eventFired = new AtomicBoolean(false);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = GdxTestUtils.createTestSkin();
                    I18N.init(Locale.ENGLISH);

                    LanguageSelectBox selectBox = new LanguageSelectBox(skin);
                    Stage stage = new Stage(new ScreenViewport());
                    stage.addActor(selectBox);

                    assertEquals("English", selectBox.getSelected().toString());

                    selectBox.addListener(event -> {
                        eventFired.set(true);
                        return true;
                    });

                    // Change selection to Czech
                    selectBox.setSelectedByLocale(Locale.forLanguageTag("cs-CZ"));
                    assertEquals("Čeština", selectBox.getSelected().toString());
                    assertEquals("cs", selectBox.getSelected().getLocale().getLanguage());

                    success.set(true);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finished.set(true);
                    Gdx.app.exit();
                }
            }
        }, new HeadlessApplicationConfiguration());

        long start = System.currentTimeMillis();
        while (!finished.get() && System.currentTimeMillis() - start < 5000) {
            Thread.sleep(100);
        }

        assertTrue(success.get(), "Test should complete successfully");
    }
}
