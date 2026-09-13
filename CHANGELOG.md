# Changelog

## Unreleased

## 1.1.0 — 2026-09-13

- Material 3 Expressive styling for the notification in the shade, built entirely from
  `Notification.ProgressStyle` because a promoted notification may not carry custom views.
  - The bar reads from source to destination: a start and an end icon, each a tonal circle
    holding the Material Symbols `smartphone` or `computer` glyph, show this device and the
    remote one in the order the data moves. A two-way transfer is drawn outbound; the status
    icon badge still shows both arrows.
  - The tracker is the nine-sided "cookie" from the Material 3 Expressive shape set, filled
    with the primary colour and carrying the host's own Syncthing glyph.
  - Colours are Material 3 roles derived from Syncthing's blue with the fidelity scheme,
    resolved for the light or dark theme. SystemUI draws the progress icons untinted, so
    each is rendered into a bitmap once per theme.
- A new, optional `com.android.systemui` scope adds what `ProgressStyle` cannot express:
  - The filled part of the bar is drawn as a Material 3 Expressive wavy progress indicator:
    a 4dp stroke with a 3dp amplitude and a 40dp wavelength that flows one wavelength per
    second and flattens near either end, followed by a flat 4dp track and a stop dot. The
    platform's layout of the bar is kept; only `NotificationProgressDrawable.draw(Canvas)` is
    replaced, and only for Syncthing-Fork's notification. The wave holds still when
    animations are off.
  - Progress no longer jumps. `NotificationProgressBar.setProgressModel(Bundle)` applies
    each update at the value on screen and then moves the bar to the new value on a
    critically damped spring. Progress is reported in tenths of a percent so the movement is
    smooth.
  - The status bar chip is filled with Syncthing's primary colour and its content drawn in
    on-primary, matching the progress tracker. Android 17 otherwise always paints a
    notification chip in the system surface colour.
- The visual layer now sits behind a `LiveUpdateAppearance` interface.
  `LiveUpdatePromoter` keeps only what promotion requires — channel, chip text, ongoing
  flag and the promotion request — while `ExpressiveAppearance` owns the colour, progress
  style, folder line and status icon badge.
- The status icon badge draws its arrows through `DirectionGlyph`.
- `scripts/check-project.sh` covers the new classes and resources, and fails if a custom
  content view is introduced, which would stop the notification from being promoted.

## 1.0.0 — 2026-09-06

First release.

- Promotes Syncthing's sync progress to an Android Live Update: a promoted ongoing
  notification with a `Notification.ProgressStyle` bar, a percentage in the status bar chip
  through `setShortCriticalText`, and presence on the lock screen and Always-on Display.
- Rewrites the notification Syncthing already posts rather than adding a second one. The
  host's title, icon, Exit action and tap target are kept, and it remains the
  foreground-service notification the app depends on. `Notification.Builder.recoverBuilder`
  reopens the notification the host just built, so only the parts promotion requires are
  changed.
- Restores `FLAG_PROMOTED_ONGOING` inside
  `NotificationManagerService.fixNotificationWithChannel(...)` in `system_server`, scoped to
  the module's own channel in the host package. Android sets that flag only for a package
  holding `android.permission.POST_PROMOTED_NOTIFICATIONS`; Syncthing-Fork does not declare
  it, and an install-time permission cannot be granted afterwards. Every other notification
  keeps the platform's verdict.
- Moves the notification to `05_syncthing_live_update` at `IMPORTANCE_LOW` while a transfer
  runs, because the host's own persistent channel is `IMPORTANCE_MIN` and the platform
  refuses to promote anything posted there. When the sync completes the notification is
  passed through untouched on `01_syncthing_persistent`, so the idle behaviour is the one
  Syncthing ships with.
- Reads the completion percentage from
  `NotificationHandler.updatePersistentNotification(SyncthingService, Boolean, int, int)`,
  which is where the host hands over the figures it has just recalculated. No REST polling,
  no API key and no timer of the module's own. The values are read before the call proceeds,
  because the host posts its notification inside it.
- Names the folders currently transferring on a second line, from
  `LocalCompletion.setFolderStatus(...)` paired with the labels cached in
  `updateFromConfig(List<Folder>)`, so the configured label is shown rather than the folder
  id. A folder counts as transferring in the `syncing` and `sync-preparing` states only; the
  host also reports `scanning`, `scan-waiting` and `starting`, and a rescan is not a
  transfer. Past two folders the remainder is shown as a count, with no English phrasing,
  because every other string in the notification is the host's own localized text.
- Badges the status icon with the transfer direction, composited into the host's own
  `ic_stat_notify` so the chip keeps Syncthing's identity. Direction comes from the two
  figures the percentage is derived from: `LocalCompletion.getTotalFolderCompletion()` falls
  below 100 while this device pulls, `RemoteCompletion.getTotalDeviceCompletion()` while
  remotes pull from it.
- Never requests colorized, and sets `setRequestPromotedOngoing(true)` reflectively. The
  promotion rule differs by release and the two forms are mutually exclusive: Android 16
  required `setColorized(true)`, while Android 17 rejects a colorized notification and reads
  the explicit request instead. Requesting colorized on Android 17 both blocks promotion and
  renders the notification as a filled, light-coloured card.
- Fails open throughout. Each hook reports whether it installed, a missing host method is
  logged and skipped, and the direction badge and folder line are optional layers: without
  them the progress bar still works and Syncthing is left exactly as it was.
- Added `scripts/check-project.sh`, which validates the build configuration, the module
  declaration and the host contract the hooks depend on, and runs first in CI.

Validated on a Pixel 8 Pro running Android 17 (SDK 37) with KernelSU, Vector 2.2 and
Syncthing-Fork 2.1.3.0. During a live transfer the notification was confirmed as a single
record on `05_syncthing_live_update` carrying
`ONGOING_EVENT|ONLY_ALERT_ONCE|NO_CLEAR|FOREGROUND_SERVICE|PROMOTED_ONGOING`, and reverting
to `01_syncthing_persistent` without the promotion flag once the sync completed.
