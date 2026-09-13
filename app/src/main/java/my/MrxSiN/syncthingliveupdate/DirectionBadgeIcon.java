package my.MrxSiN.syncthingliveupdate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws the host's status icon with a small arrow badge for the transfer direction.
 *
 * The status bar chip shows the notification's small icon, so the direction has to
 * be part of that icon rather than a second view. The badge sits in a cleared
 * circle at the bottom right so it stays legible once the icon is scaled down to
 * chip size.
 */
final class DirectionBadgeIcon {

    private static final int SIZE = 96;

    private static final float BADGE_CENTRE = 0.72f;
    private static final float BADGE_CLEARANCE = 0.34f;
    private static final float BADGE_RADIUS = 0.24f;

    private static final Map<SyncDirection, Icon> CACHE = new EnumMap<>(SyncDirection.class);

    private DirectionBadgeIcon() {
    }

    /**
     * Returns {@code base} with a direction badge, or {@code base} itself when the
     * direction is unknown or the host icon cannot be rendered.
     */
    static synchronized Icon badged(Context context, Icon base, SyncDirection direction) {
        if (direction == SyncDirection.NONE || base == null) {
            return base;
        }
        Icon badged = CACHE.computeIfAbsent(direction, key -> draw(context, base, key));
        return badged == null ? base : badged;
    }

    private static Icon draw(Context context, Icon base, SyncDirection direction) {
        Drawable drawable = base.loadDrawable(context);
        if (drawable == null) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        drawable = drawable.mutate();
        drawable.setTint(Color.WHITE);
        drawable.setBounds(0, 0, SIZE, SIZE);
        drawable.draw(canvas);

        float centre = SIZE * BADGE_CENTRE;
        Paint clear = new Paint(Paint.ANTI_ALIAS_FLAG);
        clear.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawCircle(centre, centre, SIZE * BADGE_CLEARANCE, clear);

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(Color.WHITE);
        DirectionGlyph.draw(canvas, fill, direction, centre, centre, SIZE * BADGE_RADIUS);

        return Icon.createWithBitmap(bitmap);
    }
}
