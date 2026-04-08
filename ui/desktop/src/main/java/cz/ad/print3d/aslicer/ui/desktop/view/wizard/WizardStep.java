package cz.ad.print3d.aslicer.ui.desktop.view.wizard;

import com.badlogic.gdx.scenes.scene2d.Actor;

/**
 * Interface representing a single step in a {@link Wizard}.
 */
public interface WizardStep {

    /**
     * Returns the title of the step.
     *
     * @return the title of the step
     */
    String getTitle();

    /**
     * Returns the content actor for the step.
     *
     * @return the content actor for the step
     */
    Actor getContent();

    /**
     * Called when the step becomes active.
     *
     * @param wizard the wizard containing the step
     */
    void onEnter(Wizard wizard);

    /**
     * Called when the step is no longer active.
     *
     * @param wizard the wizard containing the step
     */
    void onExit(Wizard wizard);

    /**
     * Validates the step before moving to the next one.
     *
     * @return {@code true} if the step is valid, {@code false} otherwise
     */
    boolean isValid();

    /**
     * Determines if the step is complete and allows moving to the next step.
     *
     * @return {@code true} if the step is complete, {@code false} otherwise
     */
    boolean isComplete();
}
