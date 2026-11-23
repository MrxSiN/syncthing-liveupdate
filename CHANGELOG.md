# Changelog

## 1.2.0 — 2026-11-23

The Live Update now shows the transfer rate and the time left, keeps working on a lock screen that
hides sensitive content, starts faster and costs less per update. The module is rewritten in XAX;
package name, entry class, scopes and signing key are unchanged, so it installs over 1.1.2.

### Changed

- **Written in XAX, built by the XAX compiler.** No Java, Gradle or `d8` is involved; the APK
  is built from one XAX store (`source/xax/`) with the pinned compiler in `toolchains.json`.
  Package name, entry class, scopes and signing key are unchanged, so it installs over 1.1.2.
- **Transfer rate and time left.** The second line adds `↓ 4.2 MB/s`, `↑ 1.1 MB/s` and
  `≈ 3 min` after the folder names. They come from the bytes Syncthing reports as still needed
  (folder summaries for downloads, remote completion for uploads), sampled at most every 2 s and
  smoothed. A rate needs two samples and at least 1 kB/s; the estimate needs three samples in
  every direction that still has work, and is hidden past a day. No REST call, timer or extra
  notification update is added: everything rides the host's own posts.
- **Lock screen.** When Android is set to hide sensitive notification content, the lock screen
  now shows the same Live Update with "Syncing files" in place of folder names, instead of
  "Contents hidden", with the chip percentage, rate and estimate. The progress bar and its
  icons are left to the unlocked view.
- **Cheaper per update.** The lock-screen version is a small notification of its own instead of
  a copy of Syncthing's, which halves the rewrite on each host update (median 6.8 ms to 3.7 ms,
  Pixel 8 Pro, Syncthing-Fork 2.1.7.0). Reflected fields are looked up once per class.
- **Faster start.** What DexKit resolves is remembered per host install, in the host's
  `code_cache`, so later starts install the hooks without indexing the host APK: 4-5 ms instead
  of about 60-140 ms on Syncthing-Fork 2.1.7.0. A host update clears it.
- **Hot reload.** The module declares `autoHotReload`, so Syncthing and SystemUI pick up a
  module update without a reboot. `system_server` still loads it at the next framework start.
  `minApiVersion` is now 102.
- **Checks are XAX too.** The shell check scripts are replaced by `xcheck`, a small XAX Linux
  program (`tools/xcheck`), and two checklists in `checks/`. The project checks now assert
  against the canonical store rather than the carriers it was made from, and CI's release notes
  come from `xcheck` instead of `awk`.
- **arm64 only.** The x86_64 DexKit library is no longer packaged.
- The wavy bar's next frame is requested with `View.postInvalidateDelayed(16)` instead of a
  scheduled `Runnable`; frames are still at most every 16 ms and stop when animations are off.
- Failures inside the hooks are caught per call rather than by one surrounding `try`, so a
  call that throws still leaves the host's notification as the host built it.

### Verified

On a Pixel 8 Pro, Android 17 QPR2 (`CP4A.261105.040.A1`), Vector 2.2, Syncthing-Fork 2.1.7.0,
2026-11-23: real 0.5–2 GB uploads and downloads were promoted (`PROMOTED_ONGOING`,
`ProgressStyle`, colour `0xff8ccdff`) with the rate and estimate counting down to completion
(`↓ 25 MB/s · ≈ 32 sec` … `≈ 7 sec`, finished about 4 s later), the lock-screen version attached,
hooks installed from the resolution cache, and no crash. The wavy bar measured 142 µs per frame
with no missed vsync. Android 16 is not tested on a device.

## 1.1.2 — 2026-11-11

Fixes a SystemUI crash loop on the Android 17 November QPR2 update (`CP4A.261105.040.A1`).

### Fixed

- **SystemUI no longer crashes on start.** The module's code finder (DexKit) read SystemUI's
  code straight out of memory, and on QPR2 that read crashed natively, about once a second.
  It now reads the app's APK file instead, and SystemUI is not scanned at all: the parts it
  hooks there are found by name, which works on QPR2.
- **Works with shrunk Syncthing-Fork builds**, such as 2.1.7.0 from F-Droid mirrors, where the
  hooked method and the service state have no readable names. Both are now found by what
  they do instead.
- DexKit updated to 2.3.0.

## 1.1.1 — 2026-09-22

Compatibility and correctness release. Verified against the latest Syncthing-Fork, and tested
on a Pixel 8 Pro on **Android 17**, build `CP3A.260905.009`, with Vector 2.2.

### What's in it

- **Works with the latest Syncthing-Fork.** Every class, method and field the module hooks is
  verified present in **2.1.5.0** and **2.1.6.0-rc.1**, and 2.1.5.0 is confirmed on a device.
- **Survives upstream renames.** Hooked host methods are now found by their shape — class,
  signature and the string constants they use — before falling back to a name lookup.
- **No reboot to update.** The module declares `autoHotReload`, so a new build is loaded into
  running processes. Until `system_server` holds it, the log says so instead of leaving the
  Live Update quietly unpromoted.
- **Android 16 is promoted again.** It asks for promotion through `setColorized`, which the
  module had stopped sending; each Android release now has its own compatibility contract.
- **An unknown Android release degrades predictably.** It keeps the base Live Update and
  leaves the SystemUI polish off, rather than guessing at private names.
- **The wavy bar costs less and knows when to stop.** It asks for a frame at most every 16ms,
  so a 120Hz panel costs no more than a 60Hz one, and it gives up after three failed draws.
- **Better failure reports.** A member the module cannot reach is logged once, saying whether
  it was missing, refused or threw, with the Android release and build fingerprint.

### Fixed

- The status bar chip could have had one hook rewriting another's argument on a SystemUI build
  that exposes several matching constructors.
- A hook that failed to install could throw instead of being skipped.
- `scripts/check-project.sh` silently passed when a check failed, because `! grep ...` is
  exempt from `set -e`. It had been hiding a real contradiction in the build file.

### Notes

- Upgrading to this version needs one framework restart — `su -c 'stop; start'` — because the
  version being replaced could not hot reload. Updates after this one do not.
- Android 16 is supported but has not been tested on a device.
- The APK is about 1 MB larger: it now carries DexKit's native library.

## 1.1.0 — 2026-09-13

Material 3 Expressive styling, and an optional SystemUI scope for the parts a notification
cannot express on its own.

### What's in it

- **An Expressive progress bar** in the notification shade: a start and an end icon, each a
  tonal circle holding the `smartphone` or `computer` glyph, showing this device and the
  remote one in the order the data moves.
- **A cookie-shaped tracker** — the nine-sided Material 3 Expressive shape — filled with
  Syncthing's primary colour and carrying the host's own glyph.
- **Colours from Syncthing's blue**, as Material 3 roles resolved for the light or dark theme.
- **A wavy progress bar**, drawn where SystemUI draws it: a 4dp stroke with a 3dp amplitude
  and a 40dp wavelength, flowing one wavelength per second and flattening near either end. It
  holds still when animations are off.
- **Progress that glides instead of jumping**, travelling to each new value on a critically
  damped spring, reported in tenths of a percent so the movement is smooth.
- **A status bar chip in Syncthing's blue**, instead of the system surface colour Android 17
  would otherwise use.

### Notes

- The new `com.android.systemui` scope is optional. Without it the Live Update still works;
  the bar is simply the flat one the platform draws.
- The visual layer now sits behind a `LiveUpdateAppearance` interface, so the design can
  change without touching the promotion logic.

## 1.0.0 — 2026-09-06

First release. Tested on a Pixel 8 Pro on **Android 17**, with Vector 2.2 and Syncthing-Fork
2.1.3.0.

### What's in it

- **Syncthing's sync progress as an Android Live Update**: a promoted ongoing notification
  with a progress bar, a percentage in the status bar chip, and presence on the lock screen
  and Always-on Display.
- **One notification, not two.** The notification Syncthing already posts is rewritten on its
  way to the system, keeping its title, icon, Exit action and tap target.
- **The folders currently transferring**, named on a second line by their configured labels.
  Past two, the remainder is shown as a count.
- **A direction badge** on the status icon: down while this device pulls from a remote, up
  while a remote pulls from this device, both when each is happening at once.
- **No polling.** Every figure is read from the host's own callbacks as it is recalculated. No
  REST calls, no API key, no timers.
- **Untouched when idle.** With nothing transferring, the notification is passed straight
  through and behaves exactly as Syncthing intended.

### Notes

- The module restores `FLAG_PROMOTED_ONGOING` in `system_server` for its own channel in
  Syncthing-Fork alone, because Syncthing-Fork cannot declare `POST_PROMOTED_NOTIFICATIONS`
  itself. Every other notification keeps the platform's verdict.
- A hook that cannot install is logged and skipped, and Syncthing is left exactly as it was.
- Requires Android 16 or newer, root with a Zygisk-based Xposed framework exposing libxposed
  API 102, and Syncthing-Fork (`com.github.catfriend1.syncthingfork`).
