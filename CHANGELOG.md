# Changelog

## Unreleased

## 1.1.1 — 2026-09-22

### Fixed

- The module never declared `autoHotReload`, so Vector could not load an updated build into
  the running `system_server` and every update needed a reboot to take effect. It does now.
- `scripts/check-project.sh` reported nothing when a negative assertion failed. Assertions
  written as `! grep ...` are exempt from `set -e`, so a failed one let the script continue
  and exit 0. Every assertion now goes through a helper that reports the failure and sets an
  exit code, and the script lists every failure rather than stopping at the first. This had
  hidden a contradiction: the script asserted that `signingConfigs` must not appear in
  `app/build.gradle.kts` while that file deliberately created one.
- Android 16 never requested promotion. The module called `setRequestPromotedOngoing`, which
  does not exist before Android 17, and had dropped the colorized request Android 16 reads
  instead, so the notification was rewritten but never promoted there.
- `StatusBarChipColors` shared one argument index across every hooked constructor. A
  SystemUI build exposing several constructors that take a colours model could have had one
  hook rewriting the wrong argument. Each hook now carries its own index.
- `ModuleRuntime.hook(...)` did not catch a failure from the framework, so the module's
  fail-open policy depended on that call never throwing. It is now the single hard boundary
  for hook installation.

### Added

- Unit tests, run by CI, over the parts that can be checked without a device: snapshot
  boundaries at -1, 0, 99 and 100, direction and transfer routes, the spring interpolator's
  monotonicity and lack of overshoot, folder-state transitions, the folder line, the
  platform contract selection, and member resolution by name.
- `scripts/check-host-contract.sh`, which downloads a released Syncthing-Fork APK from
  [researchxxl/syncthing-android](https://github.com/researchxxl/syncthing-android) and checks
  that every hooked class, method and field is still present. It takes an optional release tag
  for pre-releases. CI runs it weekly and on demand. Every hooked member is confirmed present
  in 2.1.3.0, 2.1.5.0 (latest stable) and 2.1.6.0-rc.1 (latest pre-release).
- Versioned platform contracts. `Android36Contract`, `Android37Contract` and
  `UnknownContract` hold every per-release assumption — the promotion method, its signature,
  the argument carrying the package name, the promotion flag, and how promotion is
  requested. An unverified release now keeps the base Live Update and deliberately switches
  the optional SystemUI polish off rather than guessing at private names.
- [DexKit](https://github.com/LuckyPray/DexKit) resolves the hooked host methods by shape
  before falling back to a name lookup, so a method upstream renamed can still be found by
  the string constants it uses. The index is opened while a process's hooks are installed
  and closed immediately afterwards. This adds about 1.2 MB to the APK.
- `DexKitLibrary`, which loads DexKit's native library by absolute path out of the
  module's own installation. DexKit does not load its own library, and inside a host
  process `System.loadLibrary` searches the host's library path rather than the module's,
  so on device DexKit failed with `UnsatisfiedLinkError` until this was added. The module
  also switched to legacy JNI packaging, because the modern packaging leaves the library
  compressed inside the APK where no path can reach it.
- A warning when the module is newer than the running `system_server`. A module cannot be
  injected into a running `system_server`, so in that state the notification is styled but
  promoted by whatever was loaded there, or not at all. The module compares its own APK
  against `sys.system_server.start_elapsed` and says so once, naming `su -c 'stop; start'`
  as the fix, alongside waiting for the framework's own hot reload — the property matters
  rather than boot time, because neither remedy reboots the kernel and a boot-time comparison
  would keep reporting a problem that had already been fixed. An APK path the framework still points at but that no longer exists counts as stale
  on its own, which is the state an in-place update leaves behind. Reading the posted
  notification back was tried first and does not work: the host re-posts its own
  notification between the calls the module rewrites.
- Structured, one-time reflection diagnostics. A missing member, a denied access and a call
  that threw are now distinguished in the log, with the SDK level, the build fingerprint,
  the target member and the expected signature.

### Verified on device

Pixel 8 Pro, Android 17 (SDK 37), build `CP3A.260905.009`, Vector 2.2, Syncthing-Fork
2.1.5.0 — a newer SystemUI and a newer host than the previously documented pair:

- DexKit resolves the hooked host method and indexes in ~50ms (host, 5 dex files) and ~78ms
  (SystemUI, 3 dex files).
- A real upload is promoted through `PromotionPolicyPatch` and `Android37Contract`:
  `channel=05_syncthing_live_update`, `flags=…|PROMOTED_ONGOING`,
  `template=Notification$ProgressStyle`, `color=0xff8ccdff`.
- `ColorsModel$Custom` has no declared constructor left on this build, so the new
  constructor-first allocation reports `Reflection ABSENT` once and falls back to `Unsafe` —
  the fallback path and its diagnostic both exercised.
- A failed DexKit load leaves all three SystemUI hooks installed through the name-lookup
  fallback, so the fail-open boundary holds in the one case that matters.

### Changed

- The wavy progress bar schedules its next frame at most every 16ms instead of asking for
  one from inside each draw, so a 120Hz panel no longer doubles the work, and gives up on
  the custom drawing for the rest of the process after three failed draws rather than
  retrying and logging on every frame.
- `Reflect.allocate` prefers a declared constructor and only falls back to
  `Unsafe.allocateInstance` when none can be called.
- One release signing path. Gradle signs from the environment in CI as well as locally, and
  the third-party signing Action, which was handed the keystore, is gone.
- The CI workflow is split into a read-only build-and-test job and a release job that alone
  holds `contents: write`. Actions are pinned to commit SHAs and updated off the deprecated
  Node 20 runtime, and the release upload uses the workflow's own token instead of a
  separately maintained one.
- `Resolvers.open` reports how many dex files it indexed and how long it took, so the cost
  the module adds to a host's startup is a measured number rather than an assumption. On a
  Pixel 8 Pro: about 50ms for the host's 5 dex files and 78ms for SystemUI's 3.
- The README's architecture, compatibility and troubleshooting sections moved to `docs/`.
  The compatibility page now separates what is claimed from what has been tested on a
  device; Android 16 has not been.

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
