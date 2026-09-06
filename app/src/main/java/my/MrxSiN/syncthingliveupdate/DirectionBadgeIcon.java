package my.MrxSiN.syncthingliveupdate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
        Icon cached = CACHE.get(direction);
        if (cached != null) {
            return cached;
        }

        Icon badged = draw(context, base, direction);
        if (badged == null) {
            return base;
        }
        CACHE.put(direction, badged);
        return badged;
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
        drawBadge(canvas, fill, direction, centre, SIZE * BADGE_RADIUS);

        return Icon.createWithBitmap(bitmap);
    }

    private static void drawBadge(
            Canvas canvas,
            Paint paint,
            SyncDirection direction,
            float centre,
            float radius
    ) {
        switch (direction) {
            case DOWNLOAD -> drawArrow(canvas, paint, centre, centre, radius, true);
            case UPLOAD -> drawArrow(canvas, paint, centre, centre, radius, false);
            case BOTH -> {
                float offset = radius * 0.52f;
                float small = radius * 0.88f;
                drawArrow(canvas, paint, centre - offset, centre, small, false);
                drawArrow(canvas, paint, centre + offset, centre, small, true);
            }
            default -> {
            }
        }
    }

    private static void drawArrow(
            Canvas canvas,
            Paint paint,
            float x,
            float y,
            float radius,
            boolean pointingDown
    ) {
        float headHalfWidth = radius * 0.72f;
        float stemHalfWidth = radius * 0.26f;
        float tip = pointingDown ? y + radius : y - radius;
        float shoulder = pointingDown ? y + radius * 0.1f : y - radius * 0.1f;
        float tail = pointingDown ? y - radius : y + radius;

        canvas.drawRect(x - stemHalfWidth, Math.min(tail, shoulder),
                x + stemHalfWidth, Math.max(tail, shoulder), paint);

        Path head = new Path();
        head.moveTo(x - headHalfWidth, shoulder);
        head.lineTo(x + headHalfWidth, shoulder);
        head.lineTo(x, tip);
        head.close();
        canvas.drawPath(head, paint);
    }
}
