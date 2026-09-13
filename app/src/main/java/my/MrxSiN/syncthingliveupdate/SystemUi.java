package my.MrxSiN.syncthingliveupdate;

/** Names inside SystemUI, which renders the notification and its status bar chip. */
final class SystemUi {

    static final String PACKAGE = "com.android.systemui";

    /** The framework view SystemUI draws a {@code ProgressStyle} bar with. */
    static final String NOTIFICATION_PROGRESS_BAR =
            "com.android.internal.widget.NotificationProgressBar";

    /**
     * {@code NotificationProgressBar#setProgressModel(Bundle)} is how every update of
     * the notification reaches the bar, with the progress inside the bundle.
     */
    static final String SET_PROGRESS_MODEL = "setProgressModel";
    static final String PROGRESS_MODEL_FIELD = "mProgressModel";
    static final String PROGRESS_MODEL_PROGRESS = "progress";

    /** The drawable that paints the bar's parts, set as the bar's progress drawable. */
    static final String NOTIFICATION_PROGRESS_DRAWABLE =
            "com.android.internal.widget.NotificationProgressDrawable";
    static final String NOTIFICATION_PROGRESS_DRAWABLE_PART =
            NOTIFICATION_PROGRESS_DRAWABLE + "$DrawablePart";
    static final String NOTIFICATION_PROGRESS_DRAWABLE_SEGMENT =
            NOTIFICATION_PROGRESS_DRAWABLE + "$DrawableSegment";
    static final String DRAW = "draw";
    static final String DRAWABLE_PARTS_FIELD = "mParts";
    static final String DRAWABLE_END_DOT_FIELD = "mEndDotColor";
    static final String SEGMENT_FADED_FIELD = "mFaded";
    static final String PART_START = "getStart";
    static final String PART_END = "getEnd";
    static final String PART_COLOR = "getColor";

    /** A status bar chip that is showing; built once per update of its notification. */
    static final String ACTIVE_CHIP =
            "com.android.systemui.statusbar.chips.ui.model.OngoingActivityChipModel$Active";

    static final String CHIP_COLORS = "com.android.systemui.statusbar.chips.ui.model.ColorsModel";

    /** Chip colours given as plain values rather than resolved from the system theme. */
    static final String CUSTOM_CHIP_COLORS = CHIP_COLORS + "$Custom";
    static final String CUSTOM_BACKGROUND_FIELD = "backgroundColorInt";
    static final String CUSTOM_TEXT_FIELD = "primaryTextColorInt";

    private SystemUi() {
    }
}
