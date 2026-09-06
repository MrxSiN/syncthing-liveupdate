package my.MrxSiN.syncthingliveupdate;

import android.app.Notification;
import android.app.NotificationChannel;

import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedInterface;

/**
 * Promotes the module's own notification inside system_server.
 *
 * The platform only sets {@code FLAG_PROMOTED_ONGOING} when the posting package
 * holds {@code android.permission.POST_PROMOTED_NOTIFICATIONS}, which the host
 * cannot declare without being rebuilt. The patch restores that one flag, and
 * only for the module's own channel in the host package: every other
 * notification, including the host's own, keeps the platform's verdict.
 */
final class PromotionPolicyPatch {

    private static final String NOTIFICATION_MANAGER_SERVICE =
            "com.android.server.notification.NotificationManagerService";

    /** Applies the promotion verdict to a notification about to be recorded. */
    private static final String FIX_NOTIFICATION = "fixNotificationWithChannel";

    /** {@code Notification.FLAG_PROMOTED_ONGOING}, not public API on every release. */
    private static final int FLAG_PROMOTED_ONGOING = 0x00040000;

    private static final int PACKAGE_ARGUMENT = 3;

    private PromotionPolicyPatch() {
    }

    /** Installs the patch. Returns false when the platform method is not present. */
    static boolean install(ClassLoader systemServerClassLoader) {
        Class<?> service = findService(systemServerClassLoader);
        Method origin = Reflect.findMethod(
                service,
                FIX_NOTIFICATION,
                Notification.class,
                NotificationChannel.class,
                int.class,
                String.class
        );
        if (origin == null) {
            ModuleRuntime.log("Promotion entry point not found; Live Update stays unpromoted");
            return false;
        }

        XposedInterface.HookHandle handle =
                ModuleRuntime.hook(origin, PromotionPolicyPatch::promoteLiveUpdate);
        if (handle == null) {
            ModuleRuntime.log("Promotion hook rejected; Live Update stays unpromoted");
            return false;
        }
        ModuleRuntime.log("Promotion enabled for " + HostApp.PACKAGE
                + " channel " + LiveUpdateChannel.ID);
        return true;
    }

    /**
     * The service lives on the system server class path, which is not always the
     * loader the module itself was loaded with, so every reachable loader is
     * tried before giving up.
     */
    private static Class<?> findService(ClassLoader preferred) {
        ClassLoader[] candidates = {
                preferred,
                PromotionPolicyPatch.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader(),
                ClassLoader.getSystemClassLoader()
        };
        for (ClassLoader candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            Class<?> found = Reflect.findClass(candidate, NOTIFICATION_MANAGER_SERVICE);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static Object promoteLiveUpdate(XposedInterface.Chain chain) throws Throwable {
        Object result = chain.proceed();
        List<Object> args = chain.getArgs();
        if (args.size() <= PACKAGE_ARGUMENT
                || !HostApp.PACKAGE.equals(args.get(PACKAGE_ARGUMENT))) {
            return result;
        }
        if (args.get(0) instanceof Notification notification
                && args.get(1) instanceof NotificationChannel channel
                && LiveUpdateChannel.ID.equals(channel.getId())) {
            notification.flags |= FLAG_PROMOTED_ONGOING;
        }
        return result;
    }
}
