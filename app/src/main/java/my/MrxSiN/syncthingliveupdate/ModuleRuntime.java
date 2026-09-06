package my.MrxSiN.syncthingliveupdate;

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
     * missing or the framework rejected the hook.
     */
    static XposedInterface.HookHandle hook(Executable origin, XposedInterface.Hooker hooker) {
        XposedInterface runtime = api;
        if (runtime == null || origin == null) {
            return null;
        }
        return runtime.hook(origin).intercept(hooker);
    }
}
