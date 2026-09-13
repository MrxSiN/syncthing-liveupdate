package my.MrxSiN.syncthingliveupdate;

import android.graphics.Path;

/**
 * The scalloped "cookie" from the Material 3 Expressive shape set.
 *
 * The outline is a circle whose radius dips a fixed number of times. Tracing it as
 * a smooth curve keeps every lobe round at any size, which a polygon with rounded
 * corners would not once it is scaled down to a 20dp progress tracker.
 *
 * @param lobes number of scallops around the outline
 * @param depth how far each dip reaches inwards, as a fraction of the radius
 */
record CookieShape(int lobes, float depth) implements IconShape {

    /** The nine-sided cookie Material 3 Expressive uses for loading indicators. */
    static final CookieShape NINE_SIDED = new CookieShape(9, 0.12f);

    private static final int SAMPLES_PER_LOBE = 16;

    @Override
    public Path outline(float size) {
        float radius = size / 2f;
        Path path = new Path();
        int samples = lobes * SAMPLES_PER_LOBE;
        for (int i = 0; i < samples; i++) {
            double angle = 2 * Math.PI * i / samples;
            double dip = (1 - Math.cos(lobes * angle)) / 2;
            double r = radius * (1 - depth * dip);
            float x = (float) (radius + r * Math.sin(angle));
            float y = (float) (radius - r * Math.cos(angle));
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
        return path;
    }
}
