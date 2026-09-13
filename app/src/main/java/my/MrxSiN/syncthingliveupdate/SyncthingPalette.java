package my.MrxSiN.syncthingliveupdate;

import android.content.Context;
import android.content.res.Configuration;

/**
 * Material 3 colour roles derived from Syncthing's blue, {@code #0891D1}.
 *
 * The roles follow the fidelity scheme, which keeps the seed recognisable, and are
 * resolved for the current light or dark theme: SystemUI draws the notification on
 * a themed surface, while the icons the module renders are bitmaps and cannot
 * adapt on their own.
 *
 * @param primary                the progress bar and the tracker
 * @param onPrimary              content drawn on {@code primary}
 * @param secondaryContainer     the quieter containers at either end of the bar
 * @param onSecondaryContainer   content drawn on {@code secondaryContainer}
 */
record SyncthingPalette(
        int primary,
        int onPrimary,
        int secondaryContainer,
        int onSecondaryContainer
) {

    static final SyncthingPalette LIGHT =
            new SyncthingPalette(0xFF00628F, 0xFFFFFFFF, 0xFFBEE1FF, 0xFF001E2F);

    static final SyncthingPalette DARK =
            new SyncthingPalette(0xFF8CCDFF, 0xFF00344E, 0xFF2A4C65, 0xFFC9E6FF);

    static SyncthingPalette of(Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES ? DARK : LIGHT;
    }
}
