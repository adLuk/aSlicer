package cz.ad.print3d.aslicer.ui.desktop.view.wizard;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Pools;
import cz.ad.print3d.aslicer.ui.desktop.GdxTestUtils;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class WizardEventTest {

    @Test
    void testNextButtonClickEvent() throws InterruptedException {
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
                    com.badlogic.gdx.scenes.scene2d.Stage stage = new com.badlogic.gdx.scenes.scene2d.Stage();
                    stage.addActor(wizard);

                    TestStep step1 = new TestStep("Step 1", "Content 1");
                    TestStep step2 = new TrackingTestStep("Step 2", "Content 2", step2Entered);

                    wizard.addStep(step1);
                    wizard.addStep(step2);

                    step1.complete = true;
                    wizard.updateButtons();

                    // Find the next button. Since it's private and we haven't named it yet,
                    // we'll have to find it by text or by searching the hierarchy.
                    TextButton nextButton = findButtonByText(wizard, "Next >");
                    
                    if (nextButton != null) {
                        // Simulate a click: touchDown then touchUp
                        InputEvent touchDown = Pools.obtain(InputEvent.class);
                        touchDown.setType(InputEvent.Type.touchDown);
                        touchDown.setStage(wizard.getStage());
                        nextButton.fire(touchDown);

                        InputEvent touchUp = Pools.obtain(InputEvent.class);
                        touchUp.setType(InputEvent.Type.touchUp);
                        touchUp.setStage(wizard.getStage());
                        nextButton.fire(touchUp);
                    }

                    assertTrue(wizard.wasTouchDownReceived(), "Wizard should have received touchDown event before it was filtered");
                    assertTrue(step2Entered.get(), "Step 2 should be entered after clicking Next button");

                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    private TextButton findButtonByText(Table table, String text) {
        for (Actor child : table.getChildren()) {
            if (child instanceof TextButton && ((TextButton) child).getText().toString().equals(text)) {
                return (TextButton) child;
            }
            if (child instanceof Table) {
                TextButton found = findButtonByText((Table) child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static class TestStep implements WizardStep {
        private final String title;
        private final Table content;
        boolean complete = true;

        TestStep(String title, String label) {
            this.title = title;
            this.content = new Table();
            this.content.add(new Label(label, GdxTestUtils.createTestSkin()));
        }

        @Override public String getTitle() { return title; }
        @Override public Actor getContent() { return content; }
        @Override public void onEnter(Wizard wizard) {}
        @Override public void onExit(Wizard wizard) {}
        @Override public boolean isValid() { return true; }
        @Override public boolean isComplete() { return complete; }
        @Override public boolean processChange(ChangeListener.ChangeEvent event) {return false;}
    }

    private static class TrackingTestStep extends TestStep {
        private final AtomicBoolean entered;
        TrackingTestStep(String title, String label, AtomicBoolean entered) {
            super(title, label);
            this.entered = entered;
        }
        @Override public void onEnter(Wizard wizard) { entered.set(true); }
    }
}
