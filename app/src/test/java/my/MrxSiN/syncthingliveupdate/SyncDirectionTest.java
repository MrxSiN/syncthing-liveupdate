package my.MrxSiN.syncthingliveupdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The direction decides which way the progress bar reads and which badge the
 * status icon carries, so every combination of the two completion figures is
 * pinned down here.
 */
class SyncDirectionTest {

    @ParameterizedTest(name = "downloading={0}, uploading={1} -> {2}")
    @CsvSource({
            "false, false, NONE",
            "true,  false, DOWNLOAD",
            "false, true,  UPLOAD",
            "true,  true,  BOTH"
    })
    @DisplayName("of() covers every combination")
    void of(boolean downloading, boolean uploading, SyncDirection expected) {
        assertEquals(expected, SyncDirection.of(downloading, uploading));
    }

    @Test
    @DisplayName("a two-way transfer is drawn outbound")
    void bothIsDrawnOutbound() {
        assertSame(TransferRoute.of(SyncDirection.UPLOAD), TransferRoute.of(SyncDirection.BOTH));
    }

    @Test
    @DisplayName("an upload runs from this device to the remote one")
    void upload() {
        TransferRoute route = TransferRoute.of(SyncDirection.UPLOAD);

        assertEquals(TransferEndpoint.THIS_DEVICE, route.source());
        assertEquals(TransferEndpoint.REMOTE_DEVICE, route.destination());
    }

    @Test
    @DisplayName("a download runs from the remote device to this one")
    void download() {
        TransferRoute route = TransferRoute.of(SyncDirection.DOWNLOAD);

        assertEquals(TransferEndpoint.REMOTE_DEVICE, route.source());
        assertEquals(TransferEndpoint.THIS_DEVICE, route.destination());
    }

    @Test
    @DisplayName("no direction means no route, and so no end icons")
    void none() {
        assertNull(TransferRoute.of(SyncDirection.NONE));
    }
}
