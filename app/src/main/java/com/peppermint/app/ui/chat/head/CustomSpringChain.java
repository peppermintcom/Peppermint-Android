package com.peppermint.app.ui.chat.head;


import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringConfigRegistry;
import com.facebook.rebound.SpringListener;
import com.facebook.rebound.SpringSystem;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SpringChain is a helper class for creating spring animations with multiple springs in a chain.
 * Chains of springs can be used to create cascading animations that maintain individual physics
 * state for each member of the chain. One spring in the chain is chosen to be the control spring.
 * Springs before and after the control spring in the chain are pulled along by their predecessor.
 * You can change which spring is the control spring at any point by calling
 * {@link CustomSpringChain#setControlSpringIndex(int)}.
 *
 * <p><strong>Custom SpringChain that allows the {@link SpringSystem} to be supplied.</strong></p>
 */
public class CustomSpringChain implements SpringListener {

    /**
     * Add these spring configs to the registry to support live tuning through the
     * {@link com.facebook.rebound.ui.SpringConfiguratorView}
     */
    private static final SpringConfigRegistry registry = SpringConfigRegistry.getInstance();
    private static final int DEFAULT_MAIN_TENSION = 40;
    private static final int DEFAULT_MAIN_FRICTION = 6;
    private static final int DEFAULT_ATTACHMENT_TENSION = 400;
    private static final int DEFAULT_ATTACHMENT_FRICTION = 15;
    private static int id = 0;


    /**
     * Factory method for creating a new SpringChain with default SpringConfig.
     * @return the newly created SpringChain
     */
    public static CustomSpringChain create(SpringSystem springSystem) {
        return new CustomSpringChain(springSystem);
    }

    /**
     * Factory method for creating a new SpringChain with the provided SpringConfig.
     * @param mainTension tension for the main spring
     * @param mainFriction friction for the main spring
     * @param attachmentTension tension for the attachment spring
     * @param attachmentFriction friction for the attachment spring
     * @return the newly created SpringChain
     */
    public static CustomSpringChain create(SpringSystem springSystem,
            int mainTension,
            int mainFriction,
            int attachmentTension,
            int attachmentFriction) {
        return new CustomSpringChain(springSystem, mainTension, mainFriction, attachmentTension, attachmentFriction);
    }

    private final SpringSystem mSpringSystem;
    private final CopyOnWriteArrayList<SpringListener> mListeners =
            new CopyOnWriteArrayList<SpringListener>();
    private final CopyOnWriteArrayList<Spring> mSprings = new CopyOnWriteArrayList<Spring>();
    private int mControlSpringIndex = -1;
    private boolean mChainEnabled = true;

    // The main spring config defines the tension and friction for the control spring. Keeping these
    // values separate allows the behavior of the trailing springs to be different than that of the
    // control point.
    private final SpringConfig mMainSpringConfig;

    // The attachment spring config defines the tension and friction for the rest of the springs in
    // the chain.
    private final SpringConfig mAttachmentSpringConfig;

    private CustomSpringChain(SpringSystem springSystem) {
        this(   springSystem,
                DEFAULT_MAIN_TENSION,
                DEFAULT_MAIN_FRICTION,
                DEFAULT_ATTACHMENT_TENSION,
                DEFAULT_ATTACHMENT_FRICTION);
    }

    private CustomSpringChain(SpringSystem springSystem,
            int mainTension,
            int mainFriction,
            int attachmentTension,
            int attachmentFriction) {
        mSpringSystem = springSystem;
        mMainSpringConfig = SpringConfig.fromOrigamiTensionAndFriction(mainTension, mainFriction);
        mAttachmentSpringConfig =
                SpringConfig.fromOrigamiTensionAndFriction(attachmentTension, attachmentFriction);
        registry.addSpringConfig(mMainSpringConfig, "main spring " + id++);
        registry.addSpringConfig(mAttachmentSpringConfig, "attachment spring " + id++);
    }

    public SpringConfig getMainSpringConfig() {
        return mMainSpringConfig;
    }

    public SpringConfig getAttachmentSpringConfig() {
        return mAttachmentSpringConfig;
    }

    public SpringSystem getSpringSystem() {
        return mSpringSystem;
    }

    /**
     * Add a spring to the chain that will callback to the provided listener.
     * @param listener the listener to notify for this Spring in the chain
     * @return this SpringChain for chaining
     */
    public CustomSpringChain addSpring(final SpringListener listener) {
        // We listen to each spring added to the SpringChain and dynamically chain the springs together
        // whenever the control spring state is modified.
        Spring spring = mSpringSystem
                .createSpring()
                .addListener(this)
                .setSpringConfig(mAttachmentSpringConfig);
        mSprings.add(spring);
        mListeners.add(listener);
        return this;
    }

    public Spring removeSpring(int i) {
        mListeners.remove(i);
        return mSprings.remove(i);
    }

    public void removeAllSprings() {
        mListeners.clear();
        mSprings.clear();
    }

    /**
     * Set the index of the control spring. This spring will drive the positions of all the springs
     * before and after it in the list when moved.
     * @param i the index to use for the control spring
     * @return this SpringChain
     */
    public CustomSpringChain setControlSpringIndex(int i) {
        mControlSpringIndex = i;
        Spring controlSpring = mSprings.get(mControlSpringIndex);
        if (controlSpring == null) {
            return null;
        }
        for (Spring spring : mSpringSystem.getAllSprings()) {
            spring.setSpringConfig(mAttachmentSpringConfig);
        }
        getControlSpring().setSpringConfig(mMainSpringConfig);
        return this;
    }

    public void enable() {
        mChainEnabled = true;
        setControlSpringIndex(mControlSpringIndex);
    }

    public void disable() {
        mChainEnabled = false;
    }

    /**
     * Retrieve the control spring so you can manipulate it to drive the positions of the other
     * springs.
     * @return the control spring.
     */
    public Spring getControlSpring() {
        return mSprings.get(mControlSpringIndex);
    }

    /**
     * Retrieve the list of springs in the chain.
     * @return the list of springs
     */
    public List<Spring> getAllSprings() {
        return mSprings;
    }

    @Override
    public void onSpringUpdate(Spring spring) {
        // Get the control spring index and update the endValue of each spring above and below it in the
        // spring collection triggering a cascading effect.
        int idx = mSprings.indexOf(spring);
        SpringListener listener = mListeners.get(idx);

        if(mChainEnabled) {
            int above = -1;
            int below = -1;
            if (idx == mControlSpringIndex) {
                below = idx - 1;
                above = idx + 1;
            } else if (idx < mControlSpringIndex) {
                below = idx - 1;
            } else if (idx > mControlSpringIndex) {
                above = idx + 1;
            }
            if (above > -1 && above < mSprings.size()) {
                mSprings.get(above).setEndValue(spring.getCurrentValue());
            }
            if (below > -1 && below < mSprings.size()) {
                mSprings.get(below).setEndValue(spring.getCurrentValue());
            }
        }

        listener.onSpringUpdate(spring);
    }

    @Override
    public void onSpringAtRest(Spring spring) {
        int idx = mSprings.indexOf(spring);
        mListeners.get(idx).onSpringAtRest(spring);
    }

    @Override
    public void onSpringActivate(Spring spring) {
        int idx = mSprings.indexOf(spring);
        mListeners.get(idx).onSpringActivate(spring);
    }

    @Override
    public void onSpringEndStateChange(Spring spring) {
        int idx = mSprings.indexOf(spring);
        mListeners.get(idx).onSpringEndStateChange(spring);
    }
}
