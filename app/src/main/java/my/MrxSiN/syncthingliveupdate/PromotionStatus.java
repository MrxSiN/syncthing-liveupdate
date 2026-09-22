package my.MrxSiN.syncthingliveupdate;

import android.content.pm.ApplicationInfo;
import android.os.SystemClock;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Warns when the {@code system_server} half of the module cannot be the current one.
 *
 * The module has two halves in two processes, and they are not replaced at the
 * same moment. {@link PromotionPolicyPatch} is installed into
 * {@code system_server} as that process starts; the host half is replaced as soon
 * as the host app restarts. A framework that honours {@code autoHotReload} does
 * load an updated module into a running {@code system_server}, but not
 * instantaneously, and a framework or setup that does not needs a restart. Either
 * way there is a window — and on a first install it lasts until one happens — in
 * which the host runs this version while {@code system_server} runs the previous
 * one, or none at all.
 *
 * The symptom is silence: the notification is rewritten, styled and moved to the
 * module's own channel, and then simply never promoted — indistinguishable from a
 * bug unless something says so.
 *
 * Saying so is a matter of two timestamps rather than of asking the other
 * process. The module's own APK was last written when it was installed, and
 * {@code sys.system_server.start_elapsed} records when {@code system_server} last
 * started; if the first is later than the second, whatever is running there
 * predates this APK.
 *
 * The property matters rather than the boot time, because both remedies — a hot
 * reload and a framework restart — replace what runs in {@code system_server}
 * without rebooting the kernel. Comparing against boot time alone would therefore go on
 * reporting a problem that the restart had already fixed. Reading the posted
 * notification back would have been the obvious alternative and does not work
 * either: the host re-posts its own notification between the calls the module
 * rewrites, so what {@code getActiveNotifications()} returns at any moment is
 * usually the host's version and carries no verdict to read.
 */
final class PromotionStatus {

    private static final String SYSTEM_PROPERTIES = "android.os.SystemProperties";

    /** Milliseconds of elapsed real time at which {@code system_server} last started. */
    private static final String START_ELAPSED = "sys.system_server.start_elapsed";

    /**
     * What to do about it. A framework that honours {@code autoHotReload} loads
     * the new module into the running {@code system_server} by itself, shortly
     * after the update, and this warning stops being true without anyone acting
     * on it. Where it does not, restarting the framework re-runs
     * {@code system_server} startup and costs about as long as a boot animation
     * rather than a full reboot.
     */
    private static final String REMEDY =
            "wait for the framework to hot reload the module, or force it with"
                    + " \"su -c 'stop; start'\"";

    private final AtomicBoolean reported = new AtomicBoolean(false);

    /** Reports the warning at most once per process. */
    void report() {
        if (!reported.compareAndSet(false, true) || !stale()) {
            return;
        }
        ModuleRuntime.log("This module is newer than the running system_server,"
                + " which still holds the previous version of it, or none at all."
                + " Promotion therefore follows whatever was loaded there — on a first"
                + " install nothing promotes the Live Update, and after an update it is"
                + " the old rules that apply. To load this version's, " + REMEDY);
    }

    /**
     * Whether the module's own APK is newer than the running {@code system_server}.
     *
     * @param apkPresent               whether the APK the framework believes this
     *                                 module lives in is still on disk
     * @param installedAt              wall-clock time that APK was written
     * @param bootedAt                 wall-clock time the device booted
     * @param systemServerStartElapsed milliseconds after boot at which
     *                                 {@code system_server} last started, or a
     *                                 value of zero or less when unknown
     */
    static boolean stale(
            boolean apkPresent,
            long installedAt,
            long bootedAt,
            long systemServerStartElapsed
    ) {
        /*
         * The framework hands out the ApplicationInfo it recorded when it loaded
         * the module, and an install replaces the whole directory rather than the
         * file inside it. A path that no longer exists therefore says directly
         * what this class is trying to work out: what is running in
         * system_server belongs to an install that has since been replaced.
         */
        if (!apkPresent) {
            return true;
        }
        if (installedAt <= 0) {
            return false;
        }
        long installedAtElapsed = installedAt - bootedAt;
        return systemServerStartElapsed > 0
                ? installedAtElapsed > systemServerStartElapsed
                : installedAtElapsed > 0;
    }

    private static boolean stale() {
        ApplicationInfo module = ModuleRuntime.moduleApplicationInfo();
        if (module == null || module.sourceDir == null) {
            return false;
        }
        File apk = new File(module.sourceDir);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        return stale(
                apk.exists(),
                apk.lastModified(),
                System.currentTimeMillis() - elapsedRealtime,
                systemServerStartElapsed());
    }

    /**
     * The property's value, or {@code 0} when it cannot be read — which leaves the
     * check comparing against boot time, the answer this class gave before the
     * property was known about.
     */
    private static long systemServerStartElapsed() {
        Object value = Reflect.invokeStatic(
                Reflect.findClass(PromotionStatus.class.getClassLoader(), SYSTEM_PROPERTIES),
                "get",
                new Class<?>[]{String.class, String.class},
                START_ELAPSED,
                "");
        if (!(value instanceof String text) || text.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException unreadable) {
            return 0;
        }
    }
}
