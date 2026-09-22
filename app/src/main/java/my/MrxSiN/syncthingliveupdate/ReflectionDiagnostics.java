package my.MrxSiN.syncthingliveupdate;

import android.os.Build;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records why a reflective lookup failed, once per distinct failure.
 *
 * {@link Reflect} keeps a soft API: a missing member, a denied access and a call
 * that threw all come back as {@code null} or {@code false}, because every caller
 * reacts the same way — drop the feature and leave the host alone. Those three
 * are not the same problem to whoever has to fix the module, though: an absent
 * member means the host renamed something, a denied access means the platform
 * tightened its hidden-API policy, and a call that threw means the member is
 * still there but no longer behaves as the module assumed.
 *
 * The distinction is therefore kept here rather than thrown away, together with
 * the build the module is running on, so one logcat line is enough to say which
 * kind of breakage this is. Each distinct failure is reported once so that a
 * miss on a per-frame path cannot flood the log.
 */
final class ReflectionDiagnostics {

    /** What went wrong, as far as reflection can tell the three apart. */
    enum Failure {

        /** The class, method or field is not declared where the module expects it. */
        ABSENT,

        /** The member exists but the runtime refused to read, write or call it. */
        DENIED,

        /** The member exists and was called, and the call itself threw. */
        THREW
    }

    private static final Set<String> reported = ConcurrentHashMap.newKeySet();

    private ReflectionDiagnostics() {
    }

    /**
     * Reports one failure.
     *
     * @param target    the member the module was looking for, as
     *                  {@code owner#member}
     * @param signature the signature the module expected, for a member whose name
     *                  alone does not identify it
     * @param cause     what reflection threw, or {@code null} when the member was
     *                  simply not found
     */
    static void record(String target, String signature, Throwable cause) {
        Throwable reason = cause instanceof InvocationTargetException wrapper
                && wrapper.getCause() != null
                ? wrapper.getCause()
                : cause;
        Failure failure = classify(reason);
        String key = failure + " " + target + " " + signature;
        if (!reported.add(key)) {
            return;
        }
        ModuleRuntime.log("Reflection " + failure
                + ": " + target
                + " expected=" + signature
                + ", sdk=" + Build.VERSION.SDK_INT
                + ", fingerprint=" + Build.FINGERPRINT
                + ", cause=" + Reflect.describe(reason));
    }

    private static Failure classify(Throwable cause) {
        if (cause == null
                || cause instanceof ClassNotFoundException
                || cause instanceof NoSuchMethodException
                || cause instanceof NoSuchFieldException) {
            return Failure.ABSENT;
        }
        if (cause instanceof IllegalAccessException || cause instanceof SecurityException) {
            return Failure.DENIED;
        }
        return Failure.THREW;
    }

    /** Forgets what has been reported, so one test cannot silence the next. */
    static void reset() {
        reported.clear();
    }
}
