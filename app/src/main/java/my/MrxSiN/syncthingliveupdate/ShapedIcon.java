package my.MrxSiN.syncthingliveupdate;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;

/**
 * Renders a glyph centred in a filled shape, the way Material 3 Expressive pairs an
 * icon with its container.
 *
 * SystemUI draws the icons of a {@code ProgressStyle} as they are, without tinting
 * them to the theme, so both colours are baked into a bitmap.
 */
final class ShapedIcon {

    private static final int SIZE = 96;

    private ShapedIcon() {
    }

    /**
     * @param glyphFraction share of the container's width the glyph takes up
     * @return the icon, or {@code null} when there is no glyph to draw
     */
    static Icon render(
            IconShape shape,
            int containerColor,
            Drawable glyph,
            int glyphColor,
            float glyphFraction
    ) {
        if (glyph == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint container = new Paint(Paint.ANTI_ALIAS_FLAG);
        container.setColor(containerColor);
        canvas.drawPath(shape.outline(SIZE), container);

        int inset = Math.round(SIZE * (1 - glyphFraction) / 2);
        Drawable tinted = glyph.mutate();
        tinted.setTint(glyphColor);
        tinted.setBounds(inset, inset, SIZE - inset, SIZE - inset);
        tinted.draw(canvas);

        return Icon.createWithBitmap(bitmap);
    }
}
