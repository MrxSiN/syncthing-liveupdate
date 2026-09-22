package my.MrxSiN.syncthingliveupdate;

import android.app.Notification;
import android.content.Context;
import android.graphics.drawable.Icon;

import java.util.List;

/**
 * Material 3 Expressive styling for the Live Update.
 *
 * SystemUI renders the notification itself, and a promoted notification may not
 * carry custom views, so the design is expressed through what
 * {@link Notification.ProgressStyle} exposes. The bar runs from where the data
 * comes from to where it goes, each end marked by a tonal circle, and is led by a
 * cookie-shaped tracker carrying the Syncthing glyph. Every colour is a Material 3
 * role derived from Syncthing's blue for the current theme.
 */
final class ExpressiveAppearance implements LiveUpdateAppearance {

    /**
     * Steps per percent. The host reports whole percents, but SystemUI draws the bar
     * from integer steps, so a finer scale lets an animated bar glide instead of
     * stepping a percent at a time.
     */
    private static final int PROGRESS_STEPS_PER_PERCENT = 10;

    private final ProgressIcons icons = new ProgressIcons();

    @Override
    public void apply(
            Context hostContext,
            Notification original,
            Notification.Builder builder,
            SyncSnapshot snapshot
    ) {
        SyncthingPalette palette = SyncthingPalette.of(hostContext);
        builder.setColor(palette.primary())
                .setStyle(progressStyle(hostContext, original, snapshot, palette));

        String folders = FolderLine.of(snapshot.folders());
        if (folders != null) {
            builder.setContentText(folders);
        }

        Icon badged = DirectionBadgeIcon.badged(
                hostContext, original.getSmallIcon(), snapshot.direction());
        if (badged != null) {
            builder.setSmallIcon(badged);
        }
    }

    private Notification.ProgressStyle progressStyle(
            Context hostContext,
            Notification original,
            SyncSnapshot snapshot,
            SyncthingPalette palette
    ) {
        Notification.ProgressStyle.Segment segment =
                new Notification.ProgressStyle.Segment(
                        SyncSnapshot.COMPLETION_COMPLETE * PROGRESS_STEPS_PER_PERCENT)
                        .setColor(palette.primary());
        Notification.ProgressStyle style = new Notification.ProgressStyle()
                .setProgressSegments(List.of(segment))
                .setProgress(snapshot.completion() * PROGRESS_STEPS_PER_PERCENT)
                .setProgressTrackerIcon(
                        icons.tracker(hostContext, original.getSmallIcon(), palette));

        TransferRoute route = TransferRoute.of(snapshot.direction());
        if (route != null) {
            style.setProgressStartIcon(icons.endpoint(hostContext, route.source(), palette))
                    .setProgressEndIcon(icons.endpoint(hostContext, route.destination(), palette));
        }
        return style;
    }
}
