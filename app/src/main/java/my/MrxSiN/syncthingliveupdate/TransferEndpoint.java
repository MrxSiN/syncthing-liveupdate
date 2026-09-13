package my.MrxSiN.syncthingliveupdate;

/** One end of a transfer, drawn at the matching end of the progress bar. */
enum TransferEndpoint {

    THIS_DEVICE(R.drawable.ic_endpoint_this_device),
    REMOTE_DEVICE(R.drawable.ic_endpoint_remote_device);

    /** Glyph in the module's own resources. */
    final int glyph;

    TransferEndpoint(int glyph) {
        this.glyph = glyph;
    }
}
