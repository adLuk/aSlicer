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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class WizardTest {

    @Test
    void testWizardNavigation() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger finishCalled = new AtomicInteger(0);
        AtomicInteger cancelCalled = new AtomicInteger(0);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = GdxTestUtils.createTestSkin();
                    Wizard wizard = new Wizard("Test Wizard", skin);

                    TestStep step1 = new TestStep("Step 1", "Content 1");
                    TestStep step2 = new TestStep("Step 2", "Content 2");

                    wizard.addStep(step1);
                    wizard.addStep(step2);

                    wizard.setListener(new Wizard.WizardListener() {
                        @Override
                        public void onFinish(Wizard wizard) {
                            finishCalled.incrementAndGet();
                        }

                        @Override
                        public void onCancel(Wizard wizard) {
                            cancelCalled.incrementAndGet();
                        }
                    });

                    // Test initial state
                    assertEquals("Step 1", step1.getTitle());
                    assertTrue(step1.isEntered);

                    // Test next navigation (should be disabled if not complete)
                    step1.complete = false;
                    wizard.updateButtons();
                    wizard.next();
                    assertTrue(step1.isEntered);
                    assertFalse(step2.isEntered);

                    // Test next navigation (should work if complete)
                    step1.complete = true;
                    wizard.updateButtons();
                    assertEquals("Step 1 of 2", wizard.getStepProgressLabel().getText().toString());

                    wizard.next();
                    assertFalse(step1.isEntered);
                    assertTrue(step2.isEntered);
                    assertEquals("Step 2 of 2", wizard.getStepProgressLabel().getText().toString());

                    // Test back navigation
                    wizard.back();
                    assertTrue(step1.isEntered);
                    assertFalse(step2.isEntered);

                    // Test finish (should only work on last step and if complete)
                    wizard.finish();
                    assertEquals(0, finishCalled.get());

                    wizard.next();
                    step2.complete = true;
                    wizard.updateButtons();
                    wizard.finish();
                    assertEquals(1, finishCalled.get());

                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(1, finishCalled.get());
    }

    private static class TestStep implements WizardStep {
        private final String title;
        private final Table content;
        boolean isEntered = false;
        boolean complete = true;

        TestStep(String title, String label) {
            this.title = title;
            this.content = new Table();
            this.content.add(new Label(label, GdxTestUtils.createTestSkin()));
        }

        @Override public String getTitle() { return title; }
        @Override public Actor getContent() { return content; }
        @Override public void onEnter(Wizard wizard) { isEntered = true; }
        @Override public void onExit(Wizard wizard) { isEntered = false; }
        @Override public boolean isValid() { return true; }
        @Override public boolean isComplete() { return complete; }
        @Override public boolean processChange(ChangeListener.ChangeEvent event) {return false;}
    }
}
