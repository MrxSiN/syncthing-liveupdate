package my.MrxSiN.syncthingliveupdate;

import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.lang.reflect.Executable;

import io.github.libxposed.api.XposedInterface;

/**
 * Process-wide access to the framework interface handed to the module entry.
 *
 * The modern Xposed API exposes hooking and logging through the
 * {@link XposedInterface} instance attached to the module entry class instead of
 * the static {@code XposedBridge} of the legacy API, so the rest of the module
 * reaches it through this holder. Messages also go to logcat, which is the only
 * log a user can read without the framework manager.
 */
final class ModuleRuntime {

    private static final String TAG = "SyncthingLiveUpdate";

    private static volatile XposedInterface api;

    private ModuleRuntime() {
    }

    static void attach(XposedInterface runtime) {
        api = runtime;
    }

    /** The module's own application info, or {@code null} before the framework attached. */
    static ApplicationInfo moduleApplicationInfo() {
        XposedInterface runtime = api;
        return runtime == null ? null : runtime.getModuleApplicationInfo();
    }

    static void log(String message) {
        Log.i(TAG, message);
        XposedInterface runtime = api;
        if (runtime != null) {
            runtime.log(Log.INFO, TAG, message);
        }
    }

    static void log(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
        XposedInterface runtime = api;
        if (runtime != null) {
            runtime.log(Log.ERROR, TAG, message, throwable);
        }
    }

    /**
     * Installs a hook, or returns {@code null} when the framework interface is
     * missing, the framework rejected the hook, or installing it threw.
     *
     * This is the module's single hard boundary for hook installation: every
     * feature reaches the framework through here, so catching the failure here is
     * what makes "a broken hook disables one feature, never the process" a
     * property of the module rather than a habit of each call site. Errors that
     * report a damaged runtime rather than a missing member ({@link Error} other
     * than {@link LinkageError}) are left to propagate, because swallowing those
     * would hide a problem the module cannot recover from anyway.
     */
    static XposedInterface.HookHandle hook(Executable origin, XposedInterface.Hooker hooker) {
        XposedInterface runtime = api;
        if (runtime == null || origin == null) {
            return null;
        }
        try {
            return runtime.hook(origin).intercept(hooker);
        } catch (RuntimeException | LinkageError failure) {
            log("Hook installation failed: " + origin, failure);
            return null;
        }
    }
}
