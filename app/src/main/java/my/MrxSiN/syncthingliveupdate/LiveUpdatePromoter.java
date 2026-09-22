package my.MrxSiN.syncthingliveupdate;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedInterface;

/**
 * Turns the host's own foreground-service notification into a Live Update.
 *
 * The host keeps a single persistent notification and updates it as the sync
 * changes, so the module rewrites that notification on its way to the system
 * instead of posting a second one: while a transfer runs it is moved to a
 * promotable channel and given a progress bar, and the rest of the time it is
 * passed through untouched.
 */
final class LiveUpdatePromoter {

    private static final int NOTIFICATION_ARGUMENT = 1;
    private static final String START_FOREGROUND = "startForeground";

    private final SyncProgressSource source;
    private final LiveUpdateAppearance appearance;
    private final PlatformContract contract;
    private final AtomicBoolean installed = new AtomicBoolean(false);

    /** The promotion request is attempted per update, so its failure is logged once. */
    private final AtomicBoolean promotionRequestFailureLogged = new AtomicBoolean(false);

    private final PromotionStatus status = new PromotionStatus();

    private volatile int lastPromotedCompletion = SyncSnapshot.COMPLETION_UNKNOWN;

    LiveUpdatePromoter(
            SyncProgressSource source,
            LiveUpdateAppearance appearance,
            PlatformContract contract
    ) {
        this.source = source;
        this.appearance = appearance;
        this.contract = contract;
    }

    /** Installs the hooks. Returns false when no entry point could be hooked. */
    boolean install() {
        if (!installed.compareAndSet(false, true)) {
            return true;
        }

        int hooks = hook(Reflect.findMethod(
                Service.class, START_FOREGROUND, int.class, Notification.class))
                + hook(Reflect.findMethod(
                Service.class, START_FOREGROUND, int.class, Notification.class, int.class));
        if (hooks == 0) {
            ModuleRuntime.log("No foreground-service entry point hooked; module stays inactive");
            return false;
        }
        ModuleRuntime.log("Promoting the host notification on channel " + LiveUpdateChannel.ID);
        status.report();
        return true;
    }

    private int hook(Method origin) {
        XposedInterface.HookHandle handle = ModuleRuntime.hook(origin, this::intercept);
        return handle == null ? 0 : 1;
    }

    private Object intercept(XposedInterface.Chain chain) throws Throwable {
        Object[] replacement;
        try {
            replacement = rewrittenArguments(chain);
        } catch (Throwable failure) {
            ModuleRuntime.log("Live Update skipped: " + Reflect.describe(failure));
            replacement = null;
        }
        return replacement == null ? chain.proceed() : chain.proceed(replacement);
    }

    /**
     * Returns the argument array to proceed with, or {@code null} to leave the
     * notification the host built exactly as it is.
     */
    private Object[] rewrittenArguments(XposedInterface.Chain chain) {
        SyncSnapshot snapshot = source.current();
        if (!snapshot.transferring()) {
            lastPromotedCompletion = SyncSnapshot.COMPLETION_UNKNOWN;
            return null;
        }

        List<Object> args = chain.getArgs();
        if (args.size() <= NOTIFICATION_ARGUMENT
                || !(args.get(NOTIFICATION_ARGUMENT) instanceof Notification original)
                || !HostApp.PERSISTENT_CHANNEL.equals(original.getChannelId())
                || !(chain.getThisObject() instanceof Context hostContext)) {
            return null;
        }

        NotificationManager notificationManager =
                hostContext.getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return null;
        }
        LiveUpdateChannel.ensure(notificationManager);

        Object[] replacement = args.toArray();
        replacement[NOTIFICATION_ARGUMENT] = promoted(hostContext, original, snapshot);

        if (lastPromotedCompletion != snapshot.completion()) {
            lastPromotedCompletion = snapshot.completion();
            ModuleRuntime.log("Live Update at " + snapshot.completion()
                    + "%, direction=" + snapshot.direction()
                    + ", folders=" + snapshot.folders());
        }
        return replacement;
    }

    /**
     * Rebuilds the host's notification with the characteristics the platform
     * requires for promotion, keeping its title, actions and intents, and hands
     * the look to {@link #appearance}.
     */
    private Notification promoted(
            Context hostContext,
            Notification original,
            SyncSnapshot snapshot
    ) {
        Notification.Builder builder = Notification.Builder
                .recoverBuilder(hostContext, original)
                .setChannelId(LiveUpdateChannel.ID)
                .setShortCriticalText(snapshot.completion() + "%")
                .setOngoing(true)
                .setOnlyAlertOnce(true);

        if (!contract.requestPromotion(builder)
                && promotionRequestFailureLogged.compareAndSet(false, true)) {
            ModuleRuntime.log("Promotion could not be requested on " + contract.describe()
                    + "; the Live Update stays an ordinary notification");
        }

        appearance.apply(hostContext, original, builder, snapshot);
        return builder.build();
    }
}
