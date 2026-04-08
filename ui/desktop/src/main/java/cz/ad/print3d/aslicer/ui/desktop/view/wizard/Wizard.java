package cz.ad.print3d.aslicer.ui.desktop.view.wizard;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A wizard component that manages multiple {@link WizardStep}s in a dialog window.
 */
public final class Wizard extends Window {

    private final List<WizardStep> steps = new ArrayList<>();
    private int currentStepIndex = -1;
    private final Table contentTable;
    private final TextButton backButton;
    private final TextButton nextButton;
    private final TextButton finishButton;
    private final TextButton cancelButton;
    private final Label stepTitleLabel;
    private final Label stepProgressLabel;
    private final Label stepDescriptionLabel;

    private WizardListener listener;

    /**
     * Constructs a new Wizard with default size.
     *
     * @param title the title of the wizard window
     * @param skin  the skin to use for styling
     */
    public Wizard(String title, Skin skin) {
        this(title, skin, 800, 600);
    }

    /**
     * Constructs a new Wizard with specified size.
     *
     * @param title  the title of the wizard window
     * @param skin   the skin to use for styling
     * @param width  the width of the wizard
     * @param height the height of the wizard
     */
    public Wizard(String title, Skin skin, int width, int height) {
        super(title, skin);
        setMovable(true);
        setResizable(true);
        setResizeBorder(10);
        setSize(width, height);

        if (skin.has("title-white", Label.LabelStyle.class)) {
            stepTitleLabel = new Label("", skin, "title-white");
        } else {
            stepTitleLabel = new Label("", skin);
        }
        stepProgressLabel = new Label("", skin);
        stepDescriptionLabel = new Label("", skin);
        stepDescriptionLabel.setWrap(true);
        contentTable = new Table();

        backButton = new TextButton("< Back", skin);
        nextButton = new TextButton("Next >", skin);
        finishButton = new TextButton("Finish", skin);
        cancelButton = new TextButton("Cancel", skin);

        setupLayout();
        setupListeners();
        updateButtons();
    }

    private void setupLayout() {
        Table mainTable = new Table();
        mainTable.pad(15);

        // Header section
        Table headerTable = new Table();
        headerTable.add(stepTitleLabel).left().expandX();
        headerTable.add(stepProgressLabel).right().padLeft(10);
        mainTable.add(headerTable).fillX().padBottom(5).row();
        
        mainTable.add(new Image(getSkin().getDrawable("white"))).fillX().height(1).padBottom(10).row();
        
        mainTable.add(stepDescriptionLabel).fillX().padBottom(10).row();
        mainTable.add(contentTable).expand().fill().row();

        // Separator before buttons
        mainTable.add(new Image(getSkin().getDrawable("white"))).fillX().height(1).padTop(10).padBottom(10).row();

        Table buttonTable = new Table();
        buttonTable.add(cancelButton).padRight(10).minWidth(80);
        buttonTable.add().expandX();
        buttonTable.add(backButton).padRight(5).minWidth(80);
        buttonTable.add(nextButton).padRight(5).minWidth(80);
        buttonTable.add(finishButton).minWidth(80);

        mainTable.add(buttonTable).fillX();
        add(mainTable).expand().fill();
    }

    private void setupListeners() {
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                back();
            }
        });

        nextButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                next();
            }
        });

        finishButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                finish();
            }
        });

        cancelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                cancel();
            }
        });
    }

    /**
     * Adds a step to the wizard.
     *
     * @param step the step to add
     */
    public void addStep(WizardStep step) {
        steps.add(step);
        if (currentStepIndex == -1) {
            setStep(0);
        } else {
            updateButtons();
        }
    }

    /**
     * Sets the current step of the wizard.
     *
     * @param index the index of the step
     */
    public void setStep(int index) {
        if (index < 0 || index >= steps.size()) {
            return;
        }

        if (currentStepIndex != -1) {
            steps.get(currentStepIndex).onExit(this);
        }

        currentStepIndex = index;
        WizardStep currentStep = steps.get(currentStepIndex);
        
        stepTitleLabel.setText(currentStep.getTitle());
        stepDescriptionLabel.setText(currentStep.getDescription());
        stepProgressLabel.setText(String.format("Step %d of %d", currentStepIndex + 1, steps.size()));
        
        contentTable.clear();
        contentTable.add(currentStep.getContent()).expand().fill();
        
        // Ensure layout refreshes when step content changes
        contentTable.invalidateHierarchy();
        invalidateHierarchy();
        
        currentStep.onEnter(this);
        updateButtons();
    }

    public void next() {
        if (currentStepIndex < steps.size() - 1) {
            WizardStep currentStep = steps.get(currentStepIndex);
            if (currentStep.isValid() && currentStep.isComplete()) {
                setStep(currentStepIndex + 1);
            } else {
                // If this is hit, it means the button was enabled but the step wasn't actually valid/complete.
                // This shouldn't happen, but let's re-update buttons.
                updateButtons();
            }
        }
    }

    /**
     * Navigates to the previous step if possible.
     */
    public void back() {
        if (currentStepIndex > 0) {
            setStep(currentStepIndex - 1);
        }
    }

    /**
     * Completes the wizard.
     */
    public void finish() {
        if (currentStepIndex != steps.size() - 1) {
            return;
        }
        WizardStep currentStep = steps.get(currentStepIndex);
        if (currentStep.isValid() && currentStep.isComplete()) {
            if (listener != null) {
                listener.onFinish(this);
            }
            remove();
        }
    }

    /**
     * Cancels the wizard.
     */
    public void cancel() {
        if (listener != null) {
            listener.onCancel(this);
        }
        remove();
    }

    /**
     * Updates the state and visibility of navigation buttons.
     */
    public void updateButtons() {
        if (currentStepIndex == -1) {
            backButton.setDisabled(true);
            nextButton.setDisabled(true);
            finishButton.setDisabled(true);
            return;
        }

        backButton.setDisabled(currentStepIndex == 0);
        
        boolean isLastStep = currentStepIndex == steps.size() - 1;
        WizardStep currentStep = steps.get(currentStepIndex);
        
        boolean canProceed = currentStep.isComplete() && currentStep.isValid();
        
        nextButton.setVisible(!isLastStep);
        nextButton.setDisabled(!canProceed);
        
        finishButton.setVisible(isLastStep);
        finishButton.setDisabled(!canProceed);
        
        // Ensure UI correctly reflects disabled state
        nextButton.invalidate();
        finishButton.invalidate();
    }

    /**
     * Sets the wizard listener.
     *
     * @param listener the listener
     */
    public void setListener(WizardListener listener) {
        this.listener = listener;
    }

    /**
     * Interface for listening to wizard events.
     */
    public interface WizardListener {
        /**
         * Called when the wizard is finished.
         *
         * @param wizard the wizard
         */
        void onFinish(Wizard wizard);

        /**
         * Called when the wizard is cancelled.
         *
         * @param wizard the wizard
         */
        void onCancel(Wizard wizard);
    }
}
