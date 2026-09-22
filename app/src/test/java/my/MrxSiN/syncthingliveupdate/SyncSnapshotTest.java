package my.MrxSiN.syncthingliveupdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link SyncSnapshot} decides when a progress bar is shown at all, so its
 * boundaries are the module's most consequential arithmetic.
 */
class SyncSnapshotTest {

    private static SyncSnapshot running(int completion) {
        return new SyncSnapshot(true, completion, SyncDirection.DOWNLOAD, List.of());
    }

    @Nested
    @DisplayName("transferring()")
    class Transferring {

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 50, 98, 99})
        @DisplayName("is true from 0 up to but not including 100")
        void insideTheRange(int completion) {
            assertTrue(running(completion).transferring());
        }

        @Test
        @DisplayName("is false at 100, where the transfer is done")
        void complete() {
            assertFalse(running(SyncSnapshot.COMPLETION_COMPLETE).transferring());
        }

        @Test
        @DisplayName("is false at -1, which means no remote device is connected")
        void unknown() {
            assertFalse(running(SyncSnapshot.COMPLETION_UNKNOWN).transferring());
        }

        @Test
        @DisplayName("is false for any other negative figure")
        void negative() {
            assertFalse(running(-2).transferring());
        }

        @Test
        @DisplayName("is false above 100, which the host should never report")
        void beyondComplete() {
            assertFalse(running(101).transferring());
        }

        @Test
        @DisplayName("is false while the service is not running, whatever the figure")
        void notRunning() {
            assertFalse(new SyncSnapshot(false, 50, SyncDirection.DOWNLOAD, List.of())
                    .transferring());
        }
    }

    @Test
    @DisplayName("IDLE shows nothing")
    void idle() {
        assertFalse(SyncSnapshot.IDLE.transferring());
        assertFalse(SyncSnapshot.IDLE.running());
        assertEquals(SyncSnapshot.COMPLETION_UNKNOWN, SyncSnapshot.IDLE.completion());
        assertEquals(SyncDirection.NONE, SyncSnapshot.IDLE.direction());
        assertTrue(SyncSnapshot.IDLE.folders().isEmpty());
    }

    @Test
    @DisplayName("a null direction becomes NONE rather than a null field")
    void nullDirection() {
        assertEquals(SyncDirection.NONE, new SyncSnapshot(true, 10, null, List.of()).direction());
    }

    @Test
    @DisplayName("a null folder list becomes an empty one")
    void nullFolders() {
        assertTrue(new SyncSnapshot(true, 10, SyncDirection.NONE, null).folders().isEmpty());
    }

    @Test
    @DisplayName("the folder list is copied, so the caller cannot change it afterwards")
    void foldersAreCopied() {
        List<String> mutable = new ArrayList<>(List.of("Photos"));
        SyncSnapshot snapshot = new SyncSnapshot(true, 10, SyncDirection.NONE, mutable);

        mutable.add("Documents");

        assertEquals(List.of("Photos"), snapshot.folders());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.folders().add("Music"));
    }
}
