package my.MrxSiN.syncthingliveupdate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which folders are transferring right now, and what they are called.
 *
 * This is the bookkeeping only. {@link SyncingFolders} owns the hooks that feed
 * it, which keeps the part that depends on a live host separate from the part
 * whose behaviour can be stated exactly: a folder is transferring when the host
 * last reported it unpaused and in a transfer state, and it is named by its
 * configured label when the host gave one.
 */
final class FolderStates {

    /**
     * Values of {@code FolderStatus.state} that mean data is moving. The host also
     * reports "scanning", "scan-waiting", "starting" and "idle", none of which are
     * a transfer.
     */
    private static final Set<String> TRANSFER_STATES = Set.of("syncing", "sync-preparing");

    private final Map<String, String> labels = new ConcurrentHashMap<>();
    private final Set<String> syncing = new LinkedHashSet<>();

    /** Labels of the folders currently transferring, in the order they started. */
    List<String> current() {
        synchronized (syncing) {
            List<String> current = new ArrayList<>(syncing.size());
            for (String folderId : syncing) {
                current.add(labels.getOrDefault(folderId, folderId));
            }
            return current;
        }
    }

    /**
     * Records the label of every folder in the host's configuration. A folder with
     * no label of its own keeps being named by its id, which is what the host's own
     * interface shows in that case.
     */
    void rememberLabels(List<?> folders) {
        if (folders == null) {
            return;
        }
        for (Object folder : folders) {
            Object id = Reflect.readField(folder, HostApp.FOLDER_ID_FIELD);
            if (!(id instanceof String folderId) || folderId.isEmpty()) {
                continue;
            }
            Object label = Reflect.readField(folder, HostApp.FOLDER_LABEL_FIELD);
            labels.put(folderId, label instanceof String text && !text.isEmpty()
                    ? text
                    : folderId);
        }
    }

    /**
     * Records one folder's status. A paused folder is never transferring, whatever
     * state it was left in.
     */
    void rememberState(String folderId, boolean paused, Object status) {
        if (folderId == null || folderId.isEmpty()) {
            return;
        }
        Object state = Reflect.readField(status, HostApp.FOLDER_STATE_FIELD);
        boolean transferring = !paused
                && state instanceof String name
                && TRANSFER_STATES.contains(name);

        synchronized (syncing) {
            if (transferring) {
                syncing.add(folderId);
            } else {
                syncing.remove(folderId);
            }
        }
    }
}
