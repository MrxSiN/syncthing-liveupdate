package my.MrxSiN.syncthingliveupdate;

/**
 * Which way the data is moving.
 *
 * Syncthing tracks two independent figures: how complete the local folders are,
 * which falls behind while this device pulls from a remote, and how complete the
 * remotes are, which falls behind while they pull from this device.
 */
enum SyncDirection {

    NONE,
    DOWNLOAD,
    UPLOAD,
    BOTH;

    static SyncDirection of(boolean downloading, boolean uploading) {
        if (downloading && uploading) {
            return BOTH;
        }
        if (downloading) {
            return DOWNLOAD;
        }
        return uploading ? UPLOAD : NONE;
    }
}
