package my.MrxSiN.syncthingliveupdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import android.app.Notification;
import android.app.NotificationChannel;

/**
 * The contracts are what a future Android release is supposed to be added to, so
 * the selection rule and the shared parts of the promotion signature are pinned
 * down here rather than rediscovered on a device.
 */
class PlatformContractsTest {

    @Test
    @DisplayName("Android 16 gets the contract that asks through setColorized")
    void android16() {
        assertInstanceOf(Android36Contract.class, PlatformContracts.select(36));
    }

    @Test
    @DisplayName("Android 17 gets the contract that asks explicitly")
    void android17() {
        assertInstanceOf(Android37Contract.class, PlatformContracts.select(37));
    }

    @ParameterizedTest
    @ValueSource(ints = {35, 38, 40, 99})
    @DisplayName("anything else is treated as unverified")
    void unknown(int sdk) {
        PlatformContract contract = PlatformContracts.select(sdk);

        assertInstanceOf(UnknownContract.class, contract);
        assertEquals(sdk, contract.sdk());
    }

    @ParameterizedTest
    @ValueSource(ints = {36, 37})
    @DisplayName("a verified release may attempt the SystemUI polish")
    void polishOnVerifiedReleases(int sdk) {
        assertTrue(PlatformContracts.select(sdk).systemUiPolishSupported());
    }

    @ParameterizedTest
    @ValueSource(ints = {35, 38, 99})
    @DisplayName("an unverified release falls back to the base Live Update")
    void noPolishOnUnverifiedReleases(int sdk) {
        assertFalse(PlatformContracts.select(sdk).systemUiPolishSupported());
    }

    @Test
    @DisplayName("an unverified release still follows the newest known promotion path")
    void unknownFollowsNewestKnown() {
        PlatformContract unknown = PlatformContracts.select(99);
        PlatformContract newest = PlatformContracts.select(Android37Contract.SDK);

        assertEquals(newest.promotionMethod(), unknown.promotionMethod());
        assertEquals(newest.promotionPackageArgument(), unknown.promotionPackageArgument());
        assertEquals(newest.flagPromotedOngoing(), unknown.flagPromotedOngoing());
    }

    @ParameterizedTest
    @ValueSource(ints = {36, 37, 99})
    @DisplayName("every contract describes the same promotion signature")
    void promotionSignature(int sdk) {
        PlatformContract contract = PlatformContracts.select(sdk);
        Class<?>[] parameters = contract.promotionParameters();

        assertEquals("fixNotificationWithChannel", contract.promotionMethod());
        assertEquals(4, parameters.length);
        assertEquals(Notification.class, parameters[0]);
        assertEquals(NotificationChannel.class, parameters[1]);
        assertEquals(int.class, parameters[2]);
        assertEquals(String.class, parameters[3]);
        assertEquals(String.class, parameters[contract.promotionPackageArgument()]);
    }

    @ParameterizedTest
    @ValueSource(ints = {36, 37, 99})
    @DisplayName("FLAG_PROMOTED_ONGOING is a single bit and not one the platform already uses")
    void promotionFlag(int sdk) {
        int flag = PlatformContracts.select(sdk).flagPromotedOngoing();

        assertEquals(0x00040000, flag);
        assertEquals(1, Integer.bitCount(flag));
        assertEquals(0, flag & Notification.FLAG_ONGOING_EVENT);
        assertEquals(0, flag & Notification.FLAG_FOREGROUND_SERVICE);
    }

    @ParameterizedTest
    @ValueSource(ints = {36, 37, 99})
    @DisplayName("every contract can say what it is in a log line")
    void describable(int sdk) {
        assertFalse(PlatformContracts.select(sdk).describe().isBlank());
    }
}
