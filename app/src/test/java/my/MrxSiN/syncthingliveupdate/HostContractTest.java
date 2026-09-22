package my.MrxSiN.syncthingliveupdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

/**
 * The declared host contract, checked for the mistakes that a device cannot catch
 * quickly: a query that no longer agrees with the constant it was written from, a
 * signature with a typo in a type name, or a string hint that was dropped.
 *
 * Whether a released Syncthing-Fork actually still contains these members is a
 * different question, and one this test deliberately does not try to answer from
 * a JVM with no host APK: {@code scripts/check-host-contract.sh} does that against
 * the pinned host release.
 */
class HostContractTest {

    private static Stream<MemberQuery> hostQueries() {
        return Stream.of(
                HostApp.PERSISTENT_NOTIFICATION_QUERY,
                HostApp.FOLDER_COMPLETION_QUERY,
                HostApp.DEVICE_COMPLETION_QUERY,
                HostApp.FOLDER_CONFIG_QUERY,
                HostApp.FOLDER_STATUS_QUERY);
    }

    private static Stream<MemberQuery> everyQuery() {
        return Stream.concat(
                hostQueries(),
                Stream.of(SystemUi.PROGRESS_MODEL_QUERY, SystemUi.PROGRESS_DRAW_QUERY));
    }

    @ParameterizedTest
    @MethodSource("everyQuery")
    @DisplayName("every query names a class, a method and well-formed parameter types")
    void wellFormed(MemberQuery query) {
        assertFalse(query.ownerName().isBlank(), query.describe());
        assertFalse(query.name().isBlank(), query.describe());
        for (String parameterType : query.parameterTypeNames()) {
            assertFalse(parameterType.isBlank(), query.describe());
            assertFalse(parameterType.endsWith("."), query.describe());
            assertFalse(parameterType.contains("/"),
                    "parameter types are written as Java names, not descriptors: "
                            + query.describe());
        }
    }

    @ParameterizedTest
    @MethodSource("hostQueries")
    @DisplayName("every host query points inside the host's own packages")
    void insideTheHost(MemberQuery query) {
        assertTrue(query.ownerName().startsWith("com.nutomic.syncthingandroid."),
                query.describe());
    }

    @Test
    @DisplayName("the notification query still matches the constants it was written from")
    void persistentNotificationQuery() {
        MemberQuery query = HostApp.PERSISTENT_NOTIFICATION_QUERY;

        assertEquals(HostApp.NOTIFICATION_HANDLER, query.ownerName());
        assertEquals(HostApp.UPDATE_PERSISTENT_NOTIFICATION, query.name());
        assertEquals(
                List.of(HostApp.SYNCTHING_SERVICE, "java.lang.Boolean", "int", "int"),
                query.parameterTypeNames());
    }

    @Test
    @DisplayName("the one method the module cannot do without can be found after a rename")
    void persistentNotificationIsRenameProof() {
        assertEquals(List.of(HostApp.PERSISTENT_CHANNEL),
                HostApp.PERSISTENT_NOTIFICATION_QUERY.usingStrings());
    }

    @Test
    @DisplayName("the folder queries still match the constants they were written from")
    void folderQueries() {
        assertEquals(HostApp.LOCAL_COMPLETION, HostApp.FOLDER_CONFIG_QUERY.ownerName());
        assertEquals(HostApp.UPDATE_FROM_CONFIG, HostApp.FOLDER_CONFIG_QUERY.name());
        assertEquals(List.of("java.util.List"), HostApp.FOLDER_CONFIG_QUERY.parameterTypeNames());

        assertEquals(HostApp.LOCAL_COMPLETION, HostApp.FOLDER_STATUS_QUERY.ownerName());
        assertEquals(HostApp.SET_FOLDER_STATUS, HostApp.FOLDER_STATUS_QUERY.name());
        assertEquals(
                List.of("java.lang.String", "java.lang.Boolean", HostApp.FOLDER_STATUS),
                HostApp.FOLDER_STATUS_QUERY.parameterTypeNames());
    }

    @Test
    @DisplayName("the completion queries take no arguments, which is how they are hooked")
    void completionQueries() {
        assertEquals(HostApp.LOCAL_COMPLETION, HostApp.FOLDER_COMPLETION_QUERY.ownerName());
        assertEquals(HostApp.TOTAL_FOLDER_COMPLETION, HostApp.FOLDER_COMPLETION_QUERY.name());
        assertTrue(HostApp.FOLDER_COMPLETION_QUERY.parameterTypeNames().isEmpty());

        assertEquals(HostApp.REMOTE_COMPLETION, HostApp.DEVICE_COMPLETION_QUERY.ownerName());
        assertEquals(HostApp.TOTAL_DEVICE_COMPLETION, HostApp.DEVICE_COMPLETION_QUERY.name());
        assertTrue(HostApp.DEVICE_COMPLETION_QUERY.parameterTypeNames().isEmpty());
    }

    @Test
    @DisplayName("the SystemUI queries point at the framework widgets they were read from")
    void systemUiQueries() {
        assertEquals(SystemUi.NOTIFICATION_PROGRESS_BAR,
                SystemUi.PROGRESS_MODEL_QUERY.ownerName());
        assertEquals(SystemUi.SET_PROGRESS_MODEL, SystemUi.PROGRESS_MODEL_QUERY.name());
        assertEquals(List.of("android.os.Bundle"),
                SystemUi.PROGRESS_MODEL_QUERY.parameterTypeNames());

        assertEquals(SystemUi.NOTIFICATION_PROGRESS_DRAWABLE,
                SystemUi.PROGRESS_DRAW_QUERY.ownerName());
        assertEquals(SystemUi.DRAW, SystemUi.PROGRESS_DRAW_QUERY.name());
        assertEquals(List.of("android.graphics.Canvas"),
                SystemUi.PROGRESS_DRAW_QUERY.parameterTypeNames());
    }

    @Test
    @DisplayName("a query keeps its string hints when it is copied")
    void queriesAreImmutable() {
        MemberQuery query = MemberQuery.of("a.B", "c", "int").using("hint");

        assertEquals(List.of("int"), query.parameterTypeNames());
        assertEquals(List.of("hint"), query.usingStrings());
        assertEquals("a.B#c(int)", query.describe());
    }
}
