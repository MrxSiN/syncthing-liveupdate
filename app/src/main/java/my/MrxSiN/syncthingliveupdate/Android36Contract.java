package my.MrxSiN.syncthingliveupdate;

import android.app.Notification;

/**
 * Android 16 (API 36), the release that introduced Live Updates.
 *
 * Android 16 has no explicit promotion request. It reads the intent from the
 * notification's characteristics instead, and a colorized ongoing notification is
 * what it accepts as a promotable one, so that is how the request is made here.
 */
final class Android36Contract implements PlatformContract {

    static final int SDK = 36;

    @Override
    public int sdk() {
        return SDK;
    }

    @Override
    public String promotionMethod() {
        return "fixNotificationWithChannel";
    }

    @Override
    public Class<?>[] promotionParameters() {
        return PlatformContract.standardPromotionParameters();
    }

    @Override
    public int promotionPackageArgument() {
        return 3;
    }

    @Override
    public int flagPromotedOngoing() {
        return 0x00040000;
    }

    @Override
    public boolean requestPromotion(Notification.Builder builder) {
        builder.setColorized(true);
        return true;
    }

    @Override
    public boolean systemUiPolishSupported() {
        return true;
    }

    @Override
    public String describe() {
        return "Android 16 (API " + SDK + "), promotion requested through setColorized";
    }
}
