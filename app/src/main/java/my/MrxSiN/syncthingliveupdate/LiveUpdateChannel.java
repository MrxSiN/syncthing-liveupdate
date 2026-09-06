package my.MrxSiN.syncthingliveupdate;

import android.app.NotificationChannel;
import android.app.NotificationManager;

/**
 * Owns the notification channel the Live Update is posted on.
 *
 * The host keeps its persistent notification on an {@code IMPORTANCE_MIN}
 * channel, and the platform refuses to promote anything posted there, so the
 * module posts on a channel of its own.
 */
final class LiveUpdateChannel {

    static final String ID = "05_syncthing_live_update";

    private static final String NAME = "Sync progress";
    private static final String DESCRIPTION =
            "Live Update showing Syncthing transfer progress in the status bar";

    private LiveUpdateChannel() {
    }

    static void ensure(NotificationManager notificationManager) {
        if (notificationManager.getNotificationChannel(ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                ID, NAME, NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(DESCRIPTION);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setSound(null, null);
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(channel);
    }
}
