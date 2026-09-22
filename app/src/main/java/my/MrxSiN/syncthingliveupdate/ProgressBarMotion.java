package my.MrxSiN.syncthingliveupdate;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.github.libxposed.api.XposedInterface;

/**
 * Animates the Live Update's progress bar between updates inside SystemUI.
 *
 * The platform applies each new progress instantly, so a jump from 40% to 80%
 * teleports the bar and its tracker. The update is let through at the value the bar
 * is showing, and the bar then travels to the new value on a spring, one frame at a
 * time, through the same method the update arrived by. Only bars inflated for the
 * host app are touched.
 */
final class ProgressBarMotion {

    private static final SpringInterpolator SPRING = SpringInterpolator.SLOW_SPATIAL;

    /**
     * Touched on the main thread only, where views are bound and animators run. An
     * entry is dropped when its animation ends, so no bar is kept alive by it.
     */
    private final Map<View, ValueAnimator> running = new WeakHashMap<>();

    private Method setProgressModel;

    /** Set while a frame is being applied, so the hook lets its own call through. */
    private boolean applyingFrame;

    /** Installs the hook. Returns false when the platform bar is not present. */
    boolean install(MemberResolver resolver) {
        setProgressModel = resolver.method(SystemUi.PROGRESS_MODEL_QUERY);
        if (setProgressModel == null
                || ModuleRuntime.hook(setProgressModel, this::intercept) == null) {
            ModuleRuntime.log(
                    "Progress bar animation unavailable; the bar jumps between updates");
            return false;
        }
        ModuleRuntime.log("Animating the host progress bar in " + SystemUi.PACKAGE);
        return true;
    }

    private Object intercept(XposedInterface.Chain chain) throws Throwable {
        List<Object> args = chain.getArgs();
        if (applyingFrame
                || !(chain.getThisObject() instanceof ProgressBar bar)
                || args.isEmpty()
                || !(args.get(0) instanceof Bundle model)
                || !HostApp.PACKAGE.equals(bar.getContext().getPackageName())) {
            return chain.proceed();
        }

        ValueAnimator previous = running.remove(bar);
        if (previous != null) {
            previous.cancel();
        }

        int from = bar.getProgress();
        int to = model.getInt(SystemUi.PROGRESS_MODEL_PROGRESS);
        boolean shown = Reflect.readField(bar, SystemUi.PROGRESS_MODEL_FIELD) != null
                && bar.isAttachedToWindow();
        if (!shown || from == to) {
            return chain.proceed();
        }

        Object result = chain.proceed(new Object[]{withProgress(model, from)});
        animate(bar, model, from, to);
        return result;
    }

    private void animate(ProgressBar bar, Bundle model, int from, int to) {
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setInterpolator(SPRING);
        animator.setDuration(SPRING.durationMillis());
        animator.addUpdateListener(frame ->
                applyFrame(bar, withProgress(model, (int) frame.getAnimatedValue())));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                running.remove(bar, animation);
            }
        });
        running.put(bar, animator);
        animator.start();
    }

    private void applyFrame(ProgressBar bar, Bundle frame) {
        applyingFrame = true;
        try {
            setProgressModel.invoke(bar, frame);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            ValueAnimator animator = running.remove(bar);
            if (animator != null) {
                animator.cancel();
            }
            ModuleRuntime.log("Progress animation stopped: " + Reflect.describe(failure));
        } finally {
            applyingFrame = false;
        }
    }

    private static Bundle withProgress(Bundle model, int progress) {
        Bundle frame = new Bundle(model);
        frame.putInt(SystemUi.PROGRESS_MODEL_PROGRESS, progress);
        return frame;
    }
}
