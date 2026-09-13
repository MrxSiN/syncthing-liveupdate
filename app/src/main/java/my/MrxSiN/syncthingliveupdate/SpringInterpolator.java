package my.MrxSiN.syncthingliveupdate;

import android.animation.TimeInterpolator;

/**
 * A critically damped spring from the Material 3 Expressive motion scheme.
 *
 * Expressive motion is spring based. The spring is critically damped so a moving
 * progress bar settles on its value without overshooting it, which would briefly
 * report more progress than was made.
 */
final class SpringInterpolator implements TimeInterpolator {

    /** The "slow spatial" stiffness, suited to an element travelling across the screen. */
    static final SpringInterpolator SLOW_SPATIAL = new SpringInterpolator(300f);

    /** Natural periods after which the spring is within 0.1% of rest. */
    private static final float SETTLE_PERIODS = 9.2f;

    private final float naturalFrequency;
    private final float settleSeconds;
    private final float settledValue;

    private SpringInterpolator(float stiffness) {
        naturalFrequency = (float) Math.sqrt(stiffness);
        settleSeconds = SETTLE_PERIODS / naturalFrequency;
        settledValue = position(settleSeconds);
    }

    /** How long the spring takes to come to rest. */
    long durationMillis() {
        return Math.round(settleSeconds * 1000);
    }

    @Override
    public float getInterpolation(float input) {
        return position(input * settleSeconds) / settledValue;
    }

    private float position(float seconds) {
        double decay = naturalFrequency * seconds;
        return (float) (1 - (1 + decay) * Math.exp(-decay));
    }
}
