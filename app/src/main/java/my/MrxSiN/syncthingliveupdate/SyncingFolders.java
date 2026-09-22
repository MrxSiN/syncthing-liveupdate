package my.MrxSiN.syncthingliveupdate;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Watches the host's folder updates and feeds them to {@link FolderStates}.
 *
 * The host stores one status per folder and updates it from the event stream, so
 * the set is kept by watching those updates rather than by asking the REST API.
 * Labels come from the config the host caches alongside them, because a folder id
 * is not what the user named the folder.
 */
final class SyncingFolders {

    private final FolderStates states = new FolderStates();

    /**
     * Installs the hooks. Returns false when the host model is not present, in
     * which case the Live Update simply omits the folder line.
     */
    boolean install(MemberResolver resolver) {
        Method config = resolver.method(HostApp.FOLDER_CONFIG_QUERY);
        Method status = resolver.method(HostApp.FOLDER_STATUS_QUERY);
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
        return states.current();
    }

    private void rememberLabels(List<Object> args) {
        if (!args.isEmpty() && args.get(0) instanceof List<?> folders) {
            states.rememberLabels(folders);
        }
    }

    private void rememberState(List<Object> args) {
        if (args.size() < 3 || !(args.get(0) instanceof String folderId)) {
            return;
        }
        states.rememberState(folderId, Boolean.TRUE.equals(args.get(1)), args.get(2));
    }
}
