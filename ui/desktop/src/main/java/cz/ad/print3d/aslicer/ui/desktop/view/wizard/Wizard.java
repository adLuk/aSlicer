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

    private WizardListener listener;

    /**
     * Constructs a new Wizard.
     *
     * @param title the title of the wizard window
     * @param skin  the skin to use for styling
     */
    public Wizard(String title, Skin skin) {
        super(title, skin);
        setMovable(true);
        setResizable(true);
        setSize(600, 500);

        stepTitleLabel = new Label("", skin);
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
        mainTable.pad(10);
        mainTable.add(stepTitleLabel).left().padBottom(10).row();
        mainTable.add(contentTable).expand().fill().row();

        Table buttonTable = new Table();
        buttonTable.add(cancelButton).padRight(10);
        buttonTable.add().expandX();
        buttonTable.add(backButton).padRight(5);
        buttonTable.add(nextButton).padRight(5);
        buttonTable.add(finishButton);

        mainTable.add(buttonTable).fillX().padTop(10);
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
        contentTable.clear();
        contentTable.add(currentStep.getContent()).expand().fill();
        
        currentStep.onEnter(this);
        updateButtons();
    }

    /**
     * Navigates to the next step if possible.
     */
    public void next() {
        if (currentStepIndex < steps.size() - 1) {
            WizardStep currentStep = steps.get(currentStepIndex);
            if (currentStep.isValid() && currentStep.isComplete()) {
                setStep(currentStepIndex + 1);
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
        
        nextButton.setVisible(!isLastStep);
        nextButton.setDisabled(!currentStep.isComplete());
        
        finishButton.setVisible(isLastStep);
        finishButton.setDisabled(!currentStep.isComplete());
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
