package my.MrxSiN.syncthingliveupdate;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.graphics.drawable.Icon;

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

    /** Syncthing's own blue, used for the progress bar and the icon tint. */
    private static final int ACCENT_COLOR = 0xFF0891D1;

    /** How many folder names fit on the notification's second line. */
    private static final int FOLDERS_NAMED = 2;

    private static final int PROGRESS_SEGMENT_LENGTH = 100;
    private static final int NOTIFICATION_ARGUMENT = 1;
    private static final String START_FOREGROUND = "startForeground";

    /**
     * Android 17 asks for the promotion explicitly instead of reading it out of
     * {@code setColorized}, which it now rejects. The method does not exist on
     * Android 16, where the colorized request carried the same meaning.
     */
    private static final String REQUEST_PROMOTED_ONGOING = "setRequestPromotedOngoing";

    private final SyncProgressSource source;
    private final AtomicBoolean installed = new AtomicBoolean(false);

    private volatile int lastPromotedCompletion = SyncSnapshot.COMPLETION_UNKNOWN;

    LiveUpdatePromoter(SyncProgressSource source) {
        this.source = source;
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
     * requires for promotion, keeping its title, actions and intents.
     */
    private Notification promoted(
            Context hostContext,
            Notification original,
            SyncSnapshot snapshot
    ) {
        Notification.Builder builder = Notification.Builder
                .recoverBuilder(hostContext, original)
                .setChannelId(LiveUpdateChannel.ID)
                .setStyle(progressStyle(snapshot))
                .setShortCriticalText(snapshot.completion() + "%")
                .setColor(ACCENT_COLOR)
                .setOngoing(true)
                .setOnlyAlertOnce(true);

        Reflect.invokeIfPresent(builder, REQUEST_PROMOTED_ONGOING, boolean.class, true);

        String folders = folderLine(snapshot.folders());
        if (folders != null) {
            builder.setContentText(folders);
        }

        Icon badged = DirectionBadgeIcon.badged(
                hostContext, original.getSmallIcon(), snapshot.direction());
        if (badged != null) {
            builder.setSmallIcon(badged);
        }
        return builder.build();
    }

    /**
     * The line naming what is being transferred, or {@code null} when the host has
     * not reported a folder. Long lists are trimmed with a plain count rather than
     * a sentence, because the module carries no translations of its own.
     */
    private static String folderLine(List<String> folders) {
        if (folders.isEmpty()) {
            return null;
        }
        if (folders.size() <= FOLDERS_NAMED) {
            return String.join(", ", folders);
        }
        return String.join(", ", folders.subList(0, FOLDERS_NAMED))
                + " +" + (folders.size() - FOLDERS_NAMED);
    }

    private static Notification.ProgressStyle progressStyle(SyncSnapshot snapshot) {
        Notification.ProgressStyle.Segment segment =
                new Notification.ProgressStyle.Segment(PROGRESS_SEGMENT_LENGTH);
        segment.setColor(ACCENT_COLOR);
        return new Notification.ProgressStyle()
                .setProgressSegments(List.of(segment))
                .setProgress(snapshot.completion());
    }
}
