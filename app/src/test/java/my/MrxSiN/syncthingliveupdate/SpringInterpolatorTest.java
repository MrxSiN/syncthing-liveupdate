package my.MrxSiN.syncthingliveupdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The spring is critically damped on purpose: an overshoot would show more
 * progress than was actually made, so "never exceeds 1" is a correctness property
 * of the progress bar rather than a matter of taste.
 */
class SpringInterpolatorTest {

    private static final float TOLERANCE = 1e-4f;

    private final SpringInterpolator spring = SpringInterpolator.SLOW_SPATIAL;

    @Test
    @DisplayName("starts at rest")
    void start() {
        assertEquals(0f, spring.getInterpolation(0f), TOLERANCE);
    }

    @Test
    @DisplayName("arrives exactly at its target")
    void end() {
        assertEquals(1f, spring.getInterpolation(1f), TOLERANCE);
    }

    @Test
    @DisplayName("never overshoots, so the bar never reports progress it did not make")
    void neverOvershoots() {
        for (int step = 0; step <= 1000; step++) {
            float value = spring.getInterpolation(step / 1000f);
            assertTrue(value <= 1f + TOLERANCE,
                    "overshot to " + value + " at " + step / 1000f);
        }
    }

    @Test
    @DisplayName("only ever moves forwards")
    void monotonic() {
        float previous = -1f;
        for (int step = 0; step <= 1000; step++) {
            float value = spring.getInterpolation(step / 1000f);
            assertTrue(value >= previous, "went backwards at " + step / 1000f);
            previous = value;
        }
    }

    @Test
    @DisplayName("settles within an animation-sized duration")
    void duration() {
        long millis = spring.durationMillis();

        assertTrue(millis > 0, "duration was " + millis);
        assertTrue(millis < 2000, "duration was " + millis);
    }

    @Test
    @DisplayName("is most of the way there by halfway, as a spatial spring should be")
    void shape() {
        assertTrue(spring.getInterpolation(0.5f) > 0.9f);
    }
}
