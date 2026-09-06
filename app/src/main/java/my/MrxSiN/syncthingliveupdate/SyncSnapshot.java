package my.MrxSiN.syncthingliveupdate;

import java.util.List;

/**
 * Immutable view of what the host knows about the current sync.
 *
 * @param running    whether the Syncthing service is in its active state
 * @param completion overall completion in percent, or {@link #COMPLETION_UNKNOWN}
 *                   when no remote device is connected
 * @param direction  which way the data is currently moving
 * @param folders    labels of the folders currently transferring
 */
record SyncSnapshot(
        boolean running,
        int completion,
        SyncDirection direction,
        List<String> folders
) {

    static final int COMPLETION_UNKNOWN = -1;
    static final int COMPLETION_COMPLETE = 100;

    static final SyncSnapshot IDLE =
            new SyncSnapshot(false, COMPLETION_UNKNOWN, SyncDirection.NONE, List.of());

    SyncSnapshot {
        direction = direction == null ? SyncDirection.NONE : direction;
        folders = folders == null ? List.of() : List.copyOf(folders);
    }

    /** True while a transfer is in progress and a progress bar carries meaning. */
    boolean transferring() {
        return running && completion >= 0 && completion < COMPLETION_COMPLETE;
    }
}
