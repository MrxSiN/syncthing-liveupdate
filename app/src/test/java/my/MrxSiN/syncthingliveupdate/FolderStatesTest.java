package my.MrxSiN.syncthingliveupdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

/**
 * The transferring set is fed by host callbacks that arrive in any order, so what
 * matters is that every state the host reports maps to the right verdict and that
 * a folder leaving a transfer state leaves the set again.
 *
 * The host's model objects are stand-ins with the same field names, which is all
 * the module reads from them.
 */
class FolderStatesTest {

    /** Stands in for the host's {@code Folder} configuration entry. */
    private record Folder(String id, String label) {
    }

    /** Stands in for the host's {@code FolderStatus}. */
    private record Status(String state) {
    }

    private final FolderStates states = new FolderStates();

    @ParameterizedTest
    @ValueSource(strings = {"syncing", "sync-preparing"})
    @DisplayName("a transfer state puts the folder in the set")
    void transferStates(String state) {
        states.rememberState("photos", false, new Status(state));

        assertEquals(List.of("photos"), states.current());
    }

    @ParameterizedTest
    @ValueSource(strings = {"idle", "scanning", "scan-waiting", "starting", "error", "unknown"})
    @DisplayName("every other state keeps it out")
    void otherStates(String state) {
        states.rememberState("photos", false, new Status(state));

        assertTrue(states.current().isEmpty());
    }

    @Test
    @DisplayName("a paused folder is never transferring, whatever state it is in")
    void paused() {
        states.rememberState("photos", true, new Status("syncing"));

        assertTrue(states.current().isEmpty());
    }

    @Test
    @DisplayName("a folder that stops transferring leaves the set")
    void leavesTheSet() {
        states.rememberState("photos", false, new Status("syncing"));
        states.rememberState("photos", false, new Status("idle"));

        assertTrue(states.current().isEmpty());
    }

    @Test
    @DisplayName("a folder that is paused mid-transfer leaves the set")
    void pausedMidTransfer() {
        states.rememberState("photos", false, new Status("syncing"));
        states.rememberState("photos", true, new Status("syncing"));

        assertTrue(states.current().isEmpty());
    }

    @Test
    @DisplayName("repeating the same state does not list the folder twice")
    void idempotent() {
        states.rememberState("photos", false, new Status("syncing"));
        states.rememberState("photos", false, new Status("syncing"));

        assertEquals(List.of("photos"), states.current());
    }

    @Test
    @DisplayName("folders are listed in the order they started transferring")
    void order() {
        states.rememberState("photos", false, new Status("syncing"));
        states.rememberState("documents", false, new Status("syncing"));
        states.rememberState("music", false, new Status("sync-preparing"));

        assertEquals(List.of("photos", "documents", "music"), states.current());
    }

    @Test
    @DisplayName("a configured label replaces the folder id")
    void labels() {
        states.rememberLabels(List.of(new Folder("photos", "Camera roll")));
        states.rememberState("photos", false, new Status("syncing"));

        assertEquals(List.of("Camera roll"), states.current());
    }

    @Test
    @DisplayName("a folder with no label of its own keeps being named by its id")
    void missingLabel() {
        states.rememberLabels(List.of(new Folder("photos", ""), new Folder("music", null)));
        states.rememberState("photos", false, new Status("syncing"));
        states.rememberState("music", false, new Status("syncing"));

        assertEquals(List.of("photos", "music"), states.current());
    }

    @Test
    @DisplayName("a later configuration replaces an earlier label")
    void relabel() {
        states.rememberLabels(List.of(new Folder("photos", "Camera roll")));
        states.rememberLabels(List.of(new Folder("photos", "Photos")));
        states.rememberState("photos", false, new Status("syncing"));

        assertEquals(List.of("Photos"), states.current());
    }

    @Test
    @DisplayName("a status the module cannot read is treated as not transferring")
    void unreadableStatus() {
        states.rememberState("photos", false, "not a status object");
        states.rememberState("music", false, null);

        assertTrue(states.current().isEmpty());
    }

    @Test
    @DisplayName("entries with no usable id are ignored rather than stored empty")
    void unusableIds() {
        states.rememberLabels(List.of(new Folder("", "Nameless"), new Folder(null, "Missing")));
        states.rememberState("", false, new Status("syncing"));
        states.rememberState(null, false, new Status("syncing"));

        assertTrue(states.current().isEmpty());
    }

    @Test
    @DisplayName("a missing configuration is not an error")
    void noConfiguration() {
        states.rememberLabels(null);

        assertTrue(states.current().isEmpty());
    }

    @Test
    @DisplayName("the returned list is a copy, not the live set")
    void currentIsACopy() {
        states.rememberState("photos", false, new Status("syncing"));
        List<String> first = states.current();

        states.rememberState("documents", false, new Status("syncing"));

        assertEquals(List.of("photos"), first);
        assertEquals(List.of("photos", "documents"), states.current());
    }
}
