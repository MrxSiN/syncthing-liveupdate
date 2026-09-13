package my.MrxSiN.syncthingliveupdate;

import android.app.Notification;
import android.content.Context;

/**
 * Decides how the Live Update looks.
 *
 * {@link LiveUpdatePromoter} owns what the platform requires for promotion; the
 * visual layer is applied through this interface so the design can change without
 * touching the promotion logic.
 */
interface LiveUpdateAppearance {

    /**
     * Styles {@code builder}, which was recovered from {@code original}, for the
     * transfer described by {@code snapshot}.
     */
    void apply(
            Context hostContext,
            Notification original,
            Notification.Builder builder,
            SyncSnapshot snapshot
    );
}
