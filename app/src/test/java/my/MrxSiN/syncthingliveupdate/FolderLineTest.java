package my.MrxSiN.syncthingliveupdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

/** The second line of the notification, and the only text the module writes. */
class FolderLineTest {

    @Test
    @DisplayName("no folders means no line at all, so the host's own text is kept")
    void empty() {
        assertNull(FolderLine.of(List.of()));
        assertNull(FolderLine.of(null));
    }

    @Test
    @DisplayName("one folder is named on its own")
    void one() {
        assertEquals("Camera roll", FolderLine.of(List.of("Camera roll")));
    }

    @Test
    @DisplayName("two folders are both named")
    void two() {
        assertEquals("Camera roll, Documents",
                FolderLine.of(List.of("Camera roll", "Documents")));
    }

    @Test
    @DisplayName("a third folder becomes a count rather than a third name")
    void three() {
        assertEquals("Camera roll, Documents +1",
                FolderLine.of(List.of("Camera roll", "Documents", "Music")));
    }

    @Test
    @DisplayName("the count covers everything past the second name")
    void many() {
        assertEquals("a, b +5", FolderLine.of(List.of("a", "b", "c", "d", "e", "f", "g")));
    }

    @Test
    @DisplayName("the trimming boundary follows FOLDERS_NAMED rather than a literal two")
    void boundary() {
        List<String> exactly = List.of("a", "b");
        List<String> oneMore = List.of("a", "b", "c");

        assertEquals(FolderLine.FOLDERS_NAMED, exactly.size());
        assertEquals("a, b", FolderLine.of(exactly));
        assertEquals("a, b +1", FolderLine.of(oneMore));
    }
}
