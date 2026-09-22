package my.MrxSiN.syncthingliveupdate;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.View;

import java.lang.ref.WeakReference;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.github.libxposed.api.XposedInterface;

/**
 * Draws the Live Update's progress bar as a Material 3 Expressive wavy linear
 * progress indicator inside SystemUI.
 *
 * The platform lays the bar out into parts, filled before the progress and faded
 * after it, and paints each as a flat rounded rectangle. The layout is kept and
 * only the painting is replaced: the filled part becomes a flowing wave and the
 * faded part a flat track of the same thickness ending in a stop indicator. Bars
 * of every other app are drawn by the platform as before.
 */
final class WavyProgressTrack {

    private static final float STROKE_DP = 4f;
    private static final float AMPLITUDE_DP = 3f;
    private static final float WAVELENGTH_DP = 40f;

    /** The wave travels one wavelength in this time. */
    private static final long WAVE_PERIOD_MS = 1000;

    /** Share of the bar over which the wave grows from flat at either end. */
    private static final float START_RAMP = 0.1f;
    private static final float END_RAMP = 0.05f;

    private static final float SAMPLE_STEP_PX = 2f;

    /**
     * Shortest gap between two self-requested frames, about 60 Hz.
     *
     * Redrawing from inside {@code draw} means the bar follows whatever rate the
     * panel runs at, so a 120 Hz device would rebuild the wave twice as often for
     * a wave that travels one wavelength per second either way. The frame is
     * scheduled instead of requested immediately, which puts a ceiling on the work
     * this module adds to SystemUI regardless of the panel.
     */
    private static final long FRAME_INTERVAL_MS = 16;

    /**
     * Failed draws tolerated before the wave is given up on for this process. A
     * draw that throws once is a surprise; a draw that keeps throwing is a
     * structural mismatch with this SystemUI build, and retrying it on every frame
     * would cost more than the feature is worth.
     */
    private static final int FAILURE_BUDGET = 3;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path wave = new Path();

    /**
     * One reusable frame request per drawable, so a request can be withdrawn and
     * so repeated scheduling cannot pile up. Touched on the main thread only.
     */
    private final Map<Drawable, Runnable> frames = new WeakHashMap<>();

    /** Failed draws so far; touched on the main thread only. */
    private int failures;

    /** Set once the wave is given up on, and never cleared. */
    private volatile boolean disabled;

    private Field parts;
    private Field endDotColor;
    private Field segmentFaded;
    private Method partStart;
    private Method partEnd;
    private Method partColor;
    private Class<?> segmentClass;

    WavyProgressTrack() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    /** Installs the hook. Returns false when the platform drawable is not present. */
    boolean install(ClassLoader systemUiClassLoader, MemberResolver resolver) {
        Class<?> drawable = Reflect.findClass(
                systemUiClassLoader, SystemUi.NOTIFICATION_PROGRESS_DRAWABLE);
        Class<?> part = Reflect.findClass(
                systemUiClassLoader, SystemUi.NOTIFICATION_PROGRESS_DRAWABLE_PART);
        segmentClass = Reflect.findClass(
                systemUiClassLoader, SystemUi.NOTIFICATION_PROGRESS_DRAWABLE_SEGMENT);
        try {
            parts = accessible(drawable.getDeclaredField(SystemUi.DRAWABLE_PARTS_FIELD));
            endDotColor = accessible(drawable.getDeclaredField(SystemUi.DRAWABLE_END_DOT_FIELD));
            segmentFaded = accessible(segmentClass.getDeclaredField(SystemUi.SEGMENT_FADED_FIELD));
            partStart = accessible(part.getDeclaredMethod(SystemUi.PART_START));
            partEnd = accessible(part.getDeclaredMethod(SystemUi.PART_END));
            partColor = accessible(part.getDeclaredMethod(SystemUi.PART_COLOR));
        } catch (ReflectiveOperationException | RuntimeException missing) {
            ModuleRuntime.log("Wavy progress unavailable; the bar stays flat");
            return false;
        }

        Method draw = resolver.method(SystemUi.PROGRESS_DRAW_QUERY);
        if (draw == null || ModuleRuntime.hook(draw, this::intercept) == null) {
            ModuleRuntime.log("Wavy progress unavailable; the bar stays flat");
            return false;
        }
        ModuleRuntime.log("Drawing the host progress bar as a wave in " + SystemUi.PACKAGE);
        return true;
    }

    private Object intercept(XposedInterface.Chain chain) throws Throwable {
        List<Object> args = chain.getArgs();
        if (disabled
                || !(chain.getThisObject() instanceof Drawable drawable)
                || !(hostView(drawable) instanceof View bar)
                || args.isEmpty()
                || !(args.get(0) instanceof Canvas canvas)
                || !HostApp.PACKAGE.equals(bar.getContext().getPackageName())) {
            return chain.proceed();
        }
        try {
            draw(drawable, canvas, bar.getResources().getDisplayMetrics().density);
            return null;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            giveUpAfter(drawable, failure);
            return chain.proceed();
        }
    }

    /**
     * Records a failed draw and, once the budget is spent, stops drawing the wave
     * for the rest of the process so the platform bar is simply used instead.
     */
    private void giveUpAfter(Drawable drawable, Throwable failure) {
        cancelFrame(drawable);
        failures++;
        if (failures < FAILURE_BUDGET) {
            ModuleRuntime.log("Wavy progress skipped: " + Reflect.describe(failure));
            return;
        }
        disabled = true;
        frames.clear();
        ModuleRuntime.log("Wavy progress disabled after " + failures
                + " failed draws; the bar stays flat", failure);
    }

    /** Runs on the main thread, the only thread SystemUI draws notifications on. */
    private void draw(Drawable drawable, Canvas canvas, float density)
            throws ReflectiveOperationException {
        Rect bounds = drawable.getBounds();
        float centreY = bounds.exactCenterY();
        float stroke = STROKE_DP * density;
        paint.setStrokeWidth(stroke);

        boolean flowing = ValueAnimator.areAnimatorsEnabled();
        float wavelength = WAVELENGTH_DP * density;
        float phase = flowing
                ? wavelength * (SystemClock.uptimeMillis() % WAVE_PERIOD_MS) / WAVE_PERIOD_MS
                : 0f;

        for (Object part : (List<?>) parts.get(drawable)) {
            float start = bounds.left + (float) partStart.invoke(part);
            float end = bounds.left + (float) partEnd.invoke(part);
            if (start > end) {
                continue;
            }
            paint.setColor((int) partColor.invoke(part));
            if (!segmentClass.isInstance(part)) {
                drawDot(canvas, (start + end) / 2, centreY, (end - start) / 2);
            } else if ((boolean) segmentFaded.get(part)) {
                drawLine(canvas, start, end, centreY, stroke);
            } else {
                float amplitude = AMPLITUDE_DP * density
                        * waveGrowth((end - bounds.left) / Math.max(1, bounds.width()));
                drawWave(canvas, start, end, centreY, stroke, amplitude, wavelength,
                        phase + bounds.left);
            }
        }

        int stopColor = (int) endDotColor.get(drawable);
        if (stopColor != 0) {
            paint.setColor(stopColor);
            drawDot(canvas, bounds.right - stroke / 2, centreY, stroke / 2);
        }

        if (flowing) {
            scheduleFrame(drawable);
        }
    }

    /** Asks for the next frame no sooner than {@link #FRAME_INTERVAL_MS} from now. */
    private void scheduleFrame(Drawable drawable) {
        Runnable frame = frames.computeIfAbsent(drawable, Frame::new);
        drawable.unscheduleSelf(frame);
        drawable.scheduleSelf(frame, SystemClock.uptimeMillis() + FRAME_INTERVAL_MS);
    }

    private void cancelFrame(Drawable drawable) {
        Runnable frame = frames.remove(drawable);
        if (frame != null) {
            drawable.unscheduleSelf(frame);
        }
    }

    /**
     * The view the drawable is shown in. The bar wraps it in a layer list, so the
     * callback chain is followed through any enclosing drawables.
     */
    private static Object hostView(Drawable drawable) {
        Drawable.Callback callback = drawable.getCallback();
        while (callback instanceof Drawable parent) {
            callback = parent.getCallback();
        }
        return callback;
    }

    /** Flattens the wave while the bar is nearly empty or nearly full. */
    private static float waveGrowth(float progress) {
        float rising = Math.min(1f, progress / START_RAMP);
        float falling = Math.min(1f, (1f - progress) / END_RAMP);
        return Math.max(0f, Math.min(rising, falling));
    }

    private void drawWave(
            Canvas canvas,
            float start,
            float end,
            float centreY,
            float stroke,
            float amplitude,
            float wavelength,
            float origin
    ) {
        float from = start + stroke / 2;
        float to = end - stroke / 2;
        if (to <= from) {
            drawDot(canvas, (start + end) / 2, centreY, stroke / 2);
            return;
        }
        wave.rewind();
        for (float x = from; ; x = Math.min(to, x + SAMPLE_STEP_PX)) {
            double angle = 2 * Math.PI * (x - origin) / wavelength;
            float y = centreY + amplitude * (float) Math.sin(angle);
            if (x == from) {
                wave.moveTo(x, y);
            } else {
                wave.lineTo(x, y);
            }
            if (x >= to) {
                break;
            }
        }
        canvas.drawPath(wave, paint);
    }

    private void drawLine(Canvas canvas, float start, float end, float centreY, float stroke) {
        float from = start + stroke / 2;
        float to = end - stroke / 2;
        if (to <= from) {
            drawDot(canvas, (start + end) / 2, centreY, stroke / 2);
            return;
        }
        canvas.drawLine(from, centreY, to, centreY, paint);
    }

    private void drawDot(Canvas canvas, float x, float y, float radius) {
        Paint.Style style = paint.getStyle();
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x, y, radius, paint);
        paint.setStyle(style);
    }

    private static <T extends AccessibleObject> T accessible(T member) {
        member.setAccessible(true);
        return member;
    }

    /**
     * A pending frame request.
     *
     * The drawable is held weakly on purpose: the request is the value of a
     * {@link WeakHashMap} keyed by that same drawable, so holding it strongly
     * would keep every bar the module ever drew alive for the life of SystemUI.
     */
    private static final class Frame implements Runnable {

        private final WeakReference<Drawable> target;

        Frame(Drawable drawable) {
            target = new WeakReference<>(drawable);
        }

        @Override
        public void run() {
            Drawable drawable = target.get();
            if (drawable != null) {
                drawable.invalidateSelf();
            }
        }
    }
}
