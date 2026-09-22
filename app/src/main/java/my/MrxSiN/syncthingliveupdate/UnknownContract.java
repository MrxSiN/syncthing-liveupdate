package my.MrxSiN.syncthingliveupdate;

import android.app.Notification;

/**
 * A release the module has not been read against.
 *
 * The notification side is public, documented API plus one private method name
 * that has been stable since Live Updates existed, so the newest known contract is
 * a defensible guess there and the base Live Update is still attempted. The
 * SystemUI polish is not: it reaches into private classes, fields and methods
 * whose names nothing guarantees, and discovering them opportunistically would
 * turn an unknown release into an unpredictable one. It is therefore switched off
 * deliberately rather than left to chance, which is also what makes the resulting
 * behaviour easy to describe in a bug report: base Live Update, no polish.
 */
final class UnknownContract implements PlatformContract {

    private final PlatformContract newestKnown;
    private final int sdk;

    UnknownContract(PlatformContract newestKnown, int sdk) {
        this.newestKnown = newestKnown;
        this.sdk = sdk;
    }

    @Override
    public int sdk() {
        return sdk;
    }

    @Override
    public String promotionMethod() {
        return newestKnown.promotionMethod();
    }

    @Override
    public Class<?>[] promotionParameters() {
        return newestKnown.promotionParameters();
    }

    @Override
    public int promotionPackageArgument() {
        return newestKnown.promotionPackageArgument();
    }

    @Override
    public int flagPromotedOngoing() {
        return newestKnown.flagPromotedOngoing();
    }

    @Override
    public boolean requestPromotion(Notification.Builder builder) {
        return newestKnown.requestPromotion(builder);
    }

    @Override
    public boolean systemUiPolishSupported() {
        return false;
    }

    @Override
    public String describe() {
        return "unverified API " + sdk + ", following API " + newestKnown.sdk()
                + " for promotion and leaving the SystemUI polish off";
    }
}
