package cz.ad.print3d.aslicer.ui.desktop.view.wizard;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import cz.ad.print3d.aslicer.ui.desktop.GdxTestUtils;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class WizardValidityTest {

    @Test
    void testNextDisabledWhenInvalidEvenIfComplete() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean step2Entered = new AtomicBoolean(false);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = GdxTestUtils.createTestSkin();

                    Wizard wizard = new Wizard("Test Wizard", skin);

                    ValidatableTestStep step1 = new ValidatableTestStep("Step 1", "Content 1");
                    TrackingTestStep step2 = new TrackingTestStep("Step 2", "Content 2", step2Entered);

                    wizard.addStep(step1);
                    wizard.addStep(step2);

                    // Scenario: complete = true but valid = false -> Next should be disabled and navigation should not occur
                    step1.complete = true;
                    step1.valid = false;
                    wizard.updateButtons();
                    wizard.next();
                    assertFalse(step2Entered.get(), "Navigation should not occur when step is invalid even if complete");

                    // Now make it valid and try again -> navigation should proceed
                    step1.valid = true;
                    wizard.updateButtons();
                    wizard.next();
                    assertTrue(step2Entered.get(), "Navigation should occur when step becomes valid and complete");
                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    private static class ValidatableTestStep implements WizardStep {
        private final String title;
        private final Table content;
        boolean valid = false;
        boolean complete = false;

        ValidatableTestStep(String title, String label) {
            this.title = title;
            this.content = new Table();
            this.content.add(new Label(label, GdxTestUtils.createTestSkin()));
        }

        @Override public String getTitle() { return title; }
        @Override public Actor getContent() { return content; }
        @Override public void onEnter(Wizard wizard) { }
        @Override public void onExit(Wizard wizard) { }
        @Override public boolean isValid() { return valid; }
        @Override public boolean isComplete() { return complete; }
        @Override public boolean processChange(ChangeListener.ChangeEvent event) {return false;}

    }

    private static class TrackingTestStep implements WizardStep {
        private final String title;
        private final Table content;
        private final AtomicBoolean entered;

        TrackingTestStep(String title, String label, AtomicBoolean entered) {
            this.title = title;
            this.entered = entered;
            this.content = new Table();
            this.content.add(new Label(label, GdxTestUtils.createTestSkin()));
        }

        @Override public String getTitle() { return title; }
        @Override public Actor getContent() { return content; }
        @Override public void onEnter(Wizard wizard) { entered.set(true); }
        @Override public void onExit(Wizard wizard) { entered.set(false); }
        @Override public boolean isValid() { return true; }
        @Override public boolean isComplete() { return true; }
        @Override public boolean processChange(ChangeListener.ChangeEvent event) {return false;}
    }
}
