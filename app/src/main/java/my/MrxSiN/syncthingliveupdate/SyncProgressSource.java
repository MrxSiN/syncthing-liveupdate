package my.MrxSiN.syncthingliveupdate;

/** Supplies the sync state the host published most recently. */
interface SyncProgressSource {

    SyncSnapshot current();
}
