package my.MrxSiN.syncthingliveupdate;

import android.app.Notification;
import android.app.NotificationChannel;

/**
 * Everything the module assumes about one Android release.
 *
 * A module that patches private platform code is only ever correct for the
 * releases it was read against. Spreading those assumptions through the code that
 * uses them makes a broken release expensive to diagnose, because nothing says
 * which assumption was version-specific in the first place. They are therefore
 * declared here, once per release, so that supporting a new Android version is a
 * new implementation of this interface rather than an edit spread over the module.
 *
 * @see Android36Contract
 * @see Android37Contract
 * @see UnknownContract
 */
interface PlatformContract {

    /** The SDK level this contract was read against, for logs and diagnostics. */
    int sdk();

    /**
     * Name of the private {@code NotificationManagerService} method that applies
     * the promotion verdict to a notification about to be recorded.
     */
    String promotionMethod();

    /** Parameter types of {@link #promotionMethod()}, in order. */
    Class<?>[] promotionParameters();

    /** Index of the posting package name among {@link #promotionParameters()}. */
    int promotionPackageArgument();

    /** {@code Notification.FLAG_PROMOTED_ONGOING}, not public API on every release. */
    int flagPromotedOngoing();

    /**
     * Asks the platform to promote the notification being built, in the way this
     * release expects to be asked. Returns whether the request was accepted.
     */
    boolean requestPromotion(Notification.Builder builder);

    /**
     * Whether the optional SystemUI polish — the wavy bar, the animated progress
     * and the coloured chip — may be attempted on this release.
     *
     * That polish reaches into private SystemUI classes, so it is only attempted
     * on a release whose SystemUI the module has been read against. On anything
     * else the module stays with the base Live Update, which needs nothing but
     * public notification API, rather than guessing at private names.
     */
    boolean systemUiPolishSupported();

    /** One line naming this contract, for the load message. */
    String describe();

    /** The standard signature of {@link #promotionMethod()} on every known release. */
    static Class<?>[] standardPromotionParameters() {
        return new Class<?>[]{
                Notification.class,
                NotificationChannel.class,
                int.class,
                String.class
        };
    }
}
