package my.MrxSiN.syncthingliveupdate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Whether the {@code system_server} half of the module is older than this APK.
 *
 * The case that matters most is the one the first version of this check got
 * wrong: restarting the framework restarts {@code system_server} without
 * rebooting, so a check that only knew about boot time went on reporting a
 * problem the restart had already fixed.
 */
class PromotionStatusTest {

    /** An arbitrary boot, and times expressed as milliseconds after it. */
    private static final long BOOTED_AT = 1_790_036_359_000L;

    private static long afterBoot(long millis) {
        return BOOTED_AT + millis;
    }

    @Nested
    @DisplayName("with sys.system_server.start_elapsed available")
    class WithProperty {

        @Test
        @DisplayName("installed before system_server started: current")
        void installedBefore() {
            assertFalse(PromotionStatus.stale(true, afterBoot(39_906_000), BOOTED_AT, 40_093_833));
        }

        @Test
        @DisplayName("installed after system_server started: stale")
        void installedAfter() {
            assertTrue(PromotionStatus.stale(true, afterBoot(40_200_000), BOOTED_AT, 40_093_833));
        }

        @Test
        @DisplayName("a framework restart clears a warning that boot time alone would keep")
        void restartClearsTheWarning() {
            long installedAt = afterBoot(39_906_000);

            // Before the restart, system_server still dates from the original boot.
            assertTrue(PromotionStatus.stale(true, installedAt, BOOTED_AT, 14_679));

            // After it, system_server is newer than the APK, without a reboot.
            assertFalse(PromotionStatus.stale(true, installedAt, BOOTED_AT, 40_093_833));
        }

        @Test
        @DisplayName("installed at the same moment counts as current")
        void simultaneous() {
            assertFalse(PromotionStatus.stale(true, afterBoot(40_093_833), BOOTED_AT, 40_093_833));
        }
    }

    @Nested
    @DisplayName("without the property")
    class WithoutProperty {

        @Test
        @DisplayName("falls back to boot time: installed after boot is stale")
        void installedAfterBoot() {
            assertTrue(PromotionStatus.stale(true, afterBoot(39_906_000), BOOTED_AT, 0));
        }

        @Test
        @DisplayName("falls back to boot time: installed before boot is current")
        void installedBeforeBoot() {
            assertFalse(PromotionStatus.stale(true, BOOTED_AT - 60_000, BOOTED_AT, 0));
        }

        @Test
        @DisplayName("a negative property value is treated as unknown")
        void negativeProperty() {
            assertTrue(PromotionStatus.stale(true, afterBoot(1_000), BOOTED_AT, -1));
        }
    }

    @Test
    @DisplayName("an unreadable install time never raises a false alarm")
    void unknownInstallTime() {
        assertFalse(PromotionStatus.stale(true, 0, BOOTED_AT, 40_093_833));
        assertFalse(PromotionStatus.stale(true, -1, BOOTED_AT, 0));
    }

    @Test
    @DisplayName("an APK path that no longer exists is stale on its own")
    void replacedInstall() {
        // The framework kept the ApplicationInfo of an install that has since been
        // replaced, so nothing about its timestamps can be read or trusted.
        assertTrue(PromotionStatus.stale(false, 0, BOOTED_AT, 40_093_833));
        assertTrue(PromotionStatus.stale(false, afterBoot(1_000), BOOTED_AT, 40_093_833));
        assertTrue(PromotionStatus.stale(false, 0, BOOTED_AT, 0));
    }
}
