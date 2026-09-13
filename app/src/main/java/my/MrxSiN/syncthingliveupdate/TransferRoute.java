package my.MrxSiN.syncthingliveupdate;

/**
 * Where the data comes from and where it goes, read left to right along the
 * progress bar so the bar itself shows the direction.
 */
record TransferRoute(TransferEndpoint source, TransferEndpoint destination) {

    private static final TransferRoute OUTBOUND =
            new TransferRoute(TransferEndpoint.THIS_DEVICE, TransferEndpoint.REMOTE_DEVICE);
    private static final TransferRoute INBOUND =
            new TransferRoute(TransferEndpoint.REMOTE_DEVICE, TransferEndpoint.THIS_DEVICE);

    /**
     * The route for {@code direction}, or {@code null} when the direction is unknown.
     * A two-way transfer is drawn outbound; the status icon badge still shows both
     * arrows.
     */
    static TransferRoute of(SyncDirection direction) {
        return switch (direction) {
            case UPLOAD, BOTH -> OUTBOUND;
            case DOWNLOAD -> INBOUND;
            case NONE -> null;
        };
    }
}
