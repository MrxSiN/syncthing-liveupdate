package my.MrxSiN.syncthingliveupdate;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;

import java.util.HashMap;
import java.util.Map;

/**
 * The icons of the progress bar, rendered once per theme and kept for reuse.
 *
 * The tracker is a nine-sided cookie in the primary colour carrying the host's own
 * Syncthing glyph; the two ends are tonal circles showing this device and the
 * remote one.
 */
final class ProgressIcons {

    private static final float TRACKER_GLYPH = 0.6f;
    private static final float ENDPOINT_GLYPH = 0.62f;

    private final Map<Object, Icon> icons = new HashMap<>();

    /** The tracker, or {@code null} when the host glyph cannot be loaded. */
    synchronized Icon tracker(Context hostContext, Icon hostGlyph, SyncthingPalette palette) {
        return icons.computeIfAbsent(palette, key -> ShapedIcon.render(
                CookieShape.NINE_SIDED,
                palette.primary(),
                hostGlyph == null ? null : hostGlyph.loadDrawable(hostContext),
                palette.onPrimary(),
                TRACKER_GLYPH));
    }

    /** The icon for one end of the bar, or {@code null} when it cannot be loaded. */
    synchronized Icon endpoint(
            Context hostContext,
            TransferEndpoint endpoint,
            SyncthingPalette palette
    ) {
        return icons.computeIfAbsent(new EndpointKey(endpoint, palette), key -> {
            Drawable glyph = ModuleDrawables.load(hostContext, endpoint.glyph);
            return ShapedIcon.render(
                    IconShape.CIRCLE,
                    palette.secondaryContainer(),
                    glyph,
                    palette.onSecondaryContainer(),
                    ENDPOINT_GLYPH);
        });
    }

    private record EndpointKey(TransferEndpoint endpoint, SyncthingPalette palette) {
    }
}
