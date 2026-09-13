package my.MrxSiN.syncthingliveupdate;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Draws the arrows that stand for a {@link SyncDirection}.
 *
 * Kept apart from {@link DirectionBadgeIcon} so the badge only decides where the
 * glyph goes and in which colour.
 */
final class DirectionGlyph {

    private static final float BOTH_OFFSET = 0.52f;
    private static final float BOTH_SCALE = 0.88f;

    private static final float HEAD_HALF_WIDTH = 0.72f;
    private static final float STEM_HALF_WIDTH = 0.26f;
    private static final float SHOULDER = 0.1f;

    private DirectionGlyph() {
    }

    /**
     * Draws the glyph for {@code direction} centred on ({@code x}, {@code y}) and
     * fitting within {@code radius}. Nothing is drawn for {@link SyncDirection#NONE}.
     */
    static void draw(
            Canvas canvas,
            Paint paint,
            SyncDirection direction,
            float x,
            float y,
            float radius
    ) {
        switch (direction) {
            case DOWNLOAD -> drawArrow(canvas, paint, x, y, radius, true);
            case UPLOAD -> drawArrow(canvas, paint, x, y, radius, false);
            case BOTH -> {
                float offset = radius * BOTH_OFFSET;
                float small = radius * BOTH_SCALE;
                drawArrow(canvas, paint, x - offset, y, small, false);
                drawArrow(canvas, paint, x + offset, y, small, true);
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
        float headHalfWidth = radius * HEAD_HALF_WIDTH;
        float stemHalfWidth = radius * STEM_HALF_WIDTH;
        float tip = pointingDown ? y + radius : y - radius;
        float shoulder = pointingDown ? y + radius * SHOULDER : y - radius * SHOULDER;
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
