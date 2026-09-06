package my.MrxSiN.syncthingliveupdate;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Knows which folders are transferring right now, and what they are called.
 *
 * The host stores one status per folder and updates it from the event stream, so
 * the set is kept by watching those updates rather than by asking the REST API.
 * Labels come from the config the host caches alongside them, because a folder id
 * is not what the user named the folder.
 */
final class SyncingFolders {

    /**
     * Values of {@code FolderStatus.state} that mean data is moving. The host also
     * reports "scanning", "scan-waiting", "starting" and "idle", none of which are
     * a transfer.
     */
    private static final Set<String> TRANSFER_STATES = Set.of("syncing", "sync-preparing");

    private final Map<String, String> labels = new ConcurrentHashMap<>();
    private final Set<String> syncing = new LinkedHashSet<>();

    /**
     * Installs the hooks. Returns false when the host model is not present, in
     * which case the Live Update simply omits the folder line.
     */
    boolean install(ClassLoader hostClassLoader) {
        Class<?> localCompletion = Reflect.findClass(hostClassLoader, HostApp.LOCAL_COMPLETION);
        Class<?> folderStatus = Reflect.findClass(hostClassLoader, HostApp.FOLDER_STATUS);

        Method config = Reflect.findMethod(
                localCompletion, HostApp.UPDATE_FROM_CONFIG, List.class);
        Method status = Reflect.findMethod(
                localCompletion, HostApp.SET_FOLDER_STATUS,
                String.class, Boolean.class, folderStatus);
        if (config == null || status == null) {
            return false;
        }

        boolean hooked = ModuleRuntime.hook(config, chain -> {
            rememberLabels(chain.getArgs());
            return chain.proceed();
        }) != null;
        hooked &= ModuleRuntime.hook(status, chain -> {
            rememberState(chain.getArgs());
            return chain.proceed();
        }) != null;
        return hooked;
    }

    /** Labels of the folders currently transferring, in configuration order. */
    List<String> current() {
        synchronized (syncing) {
            List<String> current = new ArrayList<>(syncing.size());
            for (String folderId : syncing) {
                current.add(labels.getOrDefault(folderId, folderId));
            }
            return current;
        }
    }

    private void rememberLabels(List<Object> args) {
        if (args.isEmpty() || !(args.get(0) instanceof List<?> folders)) {
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

    private void rememberState(List<Object> args) {
        if (args.size() < 3 || !(args.get(0) instanceof String folderId)) {
            return;
        }
        boolean paused = Boolean.TRUE.equals(args.get(1));
        Object state = Reflect.readField(args.get(2), HostApp.FOLDER_STATE_FIELD);
        boolean transferring = !paused && state instanceof String name
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
