package my.MrxSiN.syncthingliveupdate;

import java.util.List;

/**
 * Formats the line naming what is being transferred.
 *
 * Kept apart from {@link ExpressiveAppearance} because deciding how a list of
 * folder names becomes one line of text is a different job from deciding how a
 * notification looks, changes for different reasons, and is the only part of the
 * appearance that can be checked without a device.
 */
final class FolderLine {

    /** How many folder names fit on the notification's second line. */
    static final int FOLDERS_NAMED = 2;

    private static final String SEPARATOR = ", ";

    private FolderLine() {
    }

    /**
     * The line for {@code folders}, or {@code null} when the host has not reported
     * one. Long lists are trimmed with a plain count rather than a sentence,
     * because the module carries no translations of its own.
     */
    static String of(List<String> folders) {
        if (folders == null || folders.isEmpty()) {
            return null;
        }
        if (folders.size() <= FOLDERS_NAMED) {
            return String.join(SEPARATOR, folders);
        }
        return String.join(SEPARATOR, folders.subList(0, FOLDERS_NAMED))
                + " +" + (folders.size() - FOLDERS_NAMED);
    }
}
