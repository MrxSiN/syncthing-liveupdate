package my.MrxSiN.syncthingliveupdate;

/** Names of the host app the module attaches to. */
final class HostApp {

    static final String PACKAGE = "com.github.catfriend1.syncthingfork";

    static final String NOTIFICATION_HANDLER =
            "com.nutomic.syncthingandroid.service.NotificationHandler";
    static final String SYNCTHING_SERVICE =
            "com.nutomic.syncthingandroid.service.SyncthingService";

    /**
     * {@code NotificationHandler#updatePersistentNotification(SyncthingService, Boolean, int, int)}
     * is the single place where the host publishes the sync figures it has just
     * recalculated, so it is the only method the module needs to observe.
     */
    static final String UPDATE_PERSISTENT_NOTIFICATION = "updatePersistentNotification";

    /** Completion of the local folders; below 100 while this device pulls. */
    static final String LOCAL_COMPLETION = "com.nutomic.syncthingandroid.model.LocalCompletion";
    static final String TOTAL_FOLDER_COMPLETION = "getTotalFolderCompletion";

    /** Per-folder status the host keeps in {@link #LOCAL_COMPLETION}. */
    static final String FOLDER_STATUS = "com.nutomic.syncthingandroid.model.FolderStatus";
    static final String UPDATE_FROM_CONFIG = "updateFromConfig";
    static final String SET_FOLDER_STATUS = "setFolderStatus";
    static final String FOLDER_STATE_FIELD = "state";
    static final String FOLDER_ID_FIELD = "id";
    static final String FOLDER_LABEL_FIELD = "label";

    /** Completion of the remote devices; below 100 while they pull from us. */
    static final String REMOTE_COMPLETION = "com.nutomic.syncthingandroid.model.RemoteCompletion";
    static final String TOTAL_DEVICE_COMPLETION = "getTotalDeviceCompletion";

    /** Channel the host posts its foreground-service notification on while running. */
    static final String PERSISTENT_CHANNEL = "01_syncthing_persistent";

    /** Name of the {@code SyncthingService.State} value that means "running". */
    static final String STATE_ACTIVE = "ACTIVE";

    static final String CURRENT_STATE_METHOD = "getCurrentState";

    private HostApp() {
    }
}
