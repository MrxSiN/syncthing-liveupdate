package my.MrxSiN.syncthingliveupdate;

import android.graphics.Path;

/** A container outline from the Material 3 Expressive shape set. */
interface IconShape {

    IconShape CIRCLE = size -> {
        Path path = new Path();
        path.addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW);
        return path;
    };

    /** The outline filling a square of {@code size} pixels. */
    Path outline(float size);
}
