package my.MrxSiN.syncthingliveupdate;

import android.app.Notification;

/**
 * Android 17 (API 37).
 *
 * Android 17 refuses to promote a colorized notification and asks for the
 * promotion explicitly instead, through {@code setRequestPromotedOngoing}. That
 * method does not exist on Android 16, and the module is compiled against API 36,
 * so it is called reflectively.
 */
final class Android37Contract implements PlatformContract {

    static final int SDK = 37;

    private static final String REQUEST_PROMOTED_ONGOING = "setRequestPromotedOngoing";

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
        return Reflect.invokeIfPresent(
                builder, REQUEST_PROMOTED_ONGOING, boolean.class, true);
    }

    @Override
    public boolean systemUiPolishSupported() {
        return true;
    }

    @Override
    public String describe() {
        return "Android 17 (API " + SDK + "), promotion requested through "
                + REQUEST_PROMOTED_ONGOING;
    }
}
