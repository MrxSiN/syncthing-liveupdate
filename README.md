<div align="center">

# <img src="branding/syncthing-live-update-icon.png" width="36" height="36"> Syncthing Live Update

### An Xposed module that turns Syncthing's sync progress into an Android Live Update

[![Android](https://img.shields.io/badge/Android-Vector-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/JingMatrix/Vector)
[![API](https://img.shields.io/badge/libxposed%20API-102-brightgreen?style=for-the-badge)](https://github.com/libxposed/api)
[![Target](https://img.shields.io/badge/Target-Syncthing--Fork-0891D1?style=for-the-badge&logo=syncthing&logoColor=white)](https://github.com/researchxxl/syncthing-android)
[![Platform](https://img.shields.io/badge/Android-16%2B%20(API%2036)-blue?style=for-the-badge)](https://developer.android.com/about/versions/16)
[![JDK](https://img.shields.io/badge/JDK-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)

</div>

<p align="center"><img src="branding/syncthing-live-update-icon.png" width="160" alt="Syncthing Live Update app icon"></p>

Android 16 introduced **Live Updates**: promoted ongoing notifications that surface in the
status bar chip, on the lock screen and on Always-on Display. Syncthing already knows how
far along a sync is — it simply has no way to say so in that format. This module supplies
the missing part, without adding a notification of its own.

## 📸 Screenshots

<p align="center"><img src="branding/notification.png" width="620" alt="Syncthing's own notification, rewritten with a progress bar and the folder being transferred"></p>

Syncthing's own persistent notification during a transfer: the host's title and Exit action
are untouched, with the progress bar and the name of the folder being transferred added.

<p align="center"><img src="branding/status-bar-chip.png" width="380" alt="Status bar chip showing the Syncthing icon with a download badge and 73 percent"></p>

The same notification as a status bar chip. The arrow badge shows the direction of the
transfer — here, data arriving on this device.

## ✨ Features

- Promotes Syncthing's sync progress to an Android Live Update, visible in the status bar
  chip, on the lock screen and on Always-on Display.
- Rewrites the notification Syncthing already posts instead of adding a second one. Its
  title, icon, Exit action and tap target are kept, and it stays the foreground-service
  notification the app depends on.
- Draws a `Notification.ProgressStyle` bar for overall sync completion, and a percentage in
  the status bar chip through `setShortCriticalText`.
- Names the folders currently transferring on a second line, using the labels configured in
  Syncthing rather than folder ids. Past two, the remainder is shown as a count.
- Badges the status icon with the transfer direction: down while this device pulls from a
  remote, up while a remote pulls from this device, and both when each is happening at once.
- Reads every figure from the host's own callbacks. No REST polling, no API key, no timers.
- Passes the notification through untouched when nothing is transferring, so the idle
  behaviour is exactly the one Syncthing ships with.
- Restores `FLAG_PROMOTED_ONGOING` for its own notification channel only. Every other
  notification on the device, including Syncthing's own, keeps the platform's verdict.
- Fails open at every step: a hook that cannot install is logged and skipped, and Syncthing
  is left exactly as it was.
- Carries no native library, no DEX-search dependency and no background service.

## 🎯 Scope

The module hooks two processes:

```text
system
com.github.catfriend1.syncthingfork
```

Both are required. Without `system` the notification is rewritten but never promoted, and
no chip appears. Do not enable additional applications in the module scope.

## 🔍 How it works

Two problems have to be solved, and they live in different processes.

### In the Syncthing process

1. The module entry class extends `io.github.libxposed.api.XposedModule` and installs its
   hooks from `onPackageReady()`.
2. `NotificationHandler.updatePersistentNotification(SyncthingService, Boolean, int, int)`
   is where the host hands over the figures it has just recalculated in
   `RestApi.onTotalSyncCompletionChange()`. Observing that one method yields the overall
   completion percentage without a single REST call. The values are read *before* the call
   proceeds, because the host posts its notification inside it.
3. Direction comes from the two figures that percentage is derived from.
   `LocalCompletion.getTotalFolderCompletion()` falls below 100 while this device pulls;
   `RemoteCompletion.getTotalDeviceCompletion()` falls below 100 while remotes pull from it.
   Both below 100 means both directions at once.
4. Folder names come from `LocalCompletion.setFolderStatus(String, Boolean, FolderStatus)`,
   which the host calls on every folder state change, paired with the labels it caches in
   `updateFromConfig(List<Folder>)`. A folder counts as transferring in the `syncing` and
   `sync-preparing` states — not while it is merely `scanning`, `scan-waiting` or `starting`,
   which is why a plain rescan never puts a folder name on the notification.
5. The rewrite itself happens on `Service.startForeground(...)`, filtered to the host's
   persistent channel. `Notification.Builder.recoverBuilder(...)` reopens the notification
   the host just built, so nothing has to be reconstructed from scratch, and the module only
   adds what promotion requires.

### In `system_server`

Android sets `FLAG_PROMOTED_ONGOING` only for a package holding
`android.permission.POST_PROMOTED_NOTIFICATIONS`. Syncthing-Fork does not declare it, and an
install-time permission cannot be granted after the fact — `pm grant` will not do it. So the
module restores that single flag inside
`NotificationManagerService.fixNotificationWithChannel(Notification, NotificationChannel, int, String)`,
and only when the package is Syncthing-Fork **and** the channel is the module's own:

```java
if (args.get(0) instanceof Notification notification
        && args.get(1) instanceof NotificationChannel channel
        && LiveUpdateChannel.ID.equals(channel.getId())) {
    notification.flags |= FLAG_PROMOTED_ONGOING;
}
```

Nothing else about the platform's promotion policy is touched.

### The channel

The host posts its persistent notification on an `IMPORTANCE_MIN` channel, and the platform
refuses to promote anything posted there. During a transfer the notification is moved to
`05_syncthing_live_update` at `IMPORTANCE_LOW`; when the sync completes it goes back to
`01_syncthing_persistent` and behaves exactly as before.

### A note on Android versions

The rule for what may be promoted changed between releases, and the two are mutually
exclusive:

| Release | `hasPromotableCharacteristics()` requires |
| --- | --- |
| Android 16 | `setColorized(true)` |
| Android 17 | `setRequestPromotedOngoing(true)`, and **not** colorized |

The module never requests colorized, and sets the promotion request reflectively so the call
is simply skipped on Android 16. A colorized notification is also why a Live Update can come
out looking like a filled, light-coloured card rather than an ordinary one.

## ✅ Requirements

### Runtime

- **Android 16 (API 36) or newer.** Live Updates do not exist before that; developed and
  validated on Android 17.
- **Root.** A framework implementing the modern Xposed API, version 101 or newer:
  [Vector](https://github.com/JingMatrix/Vector) with Magisk or KernelSU and Zygisk enabled.
  LSPatch cannot run this module — a rootless patch injects into the target app only, and
  the promotion flag has to be restored inside `system_server`.
- [Syncthing-Fork](https://github.com/researchxxl/syncthing-android)
  (`com.github.catfriend1.syncthingfork`).
- The **Syncthing Live Update** APK installed and enabled in the framework manager, with
  both scope entries granted.

### Build environment

- Android Studio with JDK 17 or newer, **or** a standalone JDK 17+ setup.
- Gradle 9.6.1 when building without an existing wrapper.
- Android SDK Platform 36.
- Git or a downloaded copy of the project source.

## 🛠️ Build

From the project directory, build the release APK with the included wrapper:

#### Linux / macOS

```bash
./gradlew :app:assembleRelease
```

#### Windows PowerShell

```powershell
.\gradlew.bat :app:assembleRelease
```

The generated APK will be located under:

```text
app/build/outputs/apk/release/
```

`scripts/check-project.sh` validates the build configuration, the module declaration and the
host contract the hooks depend on. It runs first in CI, and is worth running before a commit.

## 📦 Installation

1. Build and install the release APK.
2. Open the framework manager and enable **Syncthing Live Update**.
3. Grant both scope entries: the system server **and** Syncthing-Fork. The module declares
   them in `META-INF/xposed/scope.list`, but the system server scope generally has to be
   confirmed by hand.
4. **Reboot.** The `system_server` hook is installed while the system server starts, so it
   cannot take effect until the next boot.
5. Review the framework logs, or logcat, for entries tagged:

```text
SyncthingLiveUpdate
```

A healthy start logs the framework it attached to, the host method it is observing, and the
channel it will promote on.

With the Vector CLI, steps 2 and 3 are:

```sh
su -c '/data/adb/lspd/cli modules enable my.MrxSiN.syncthingliveupdate'
su -c '/data/adb/lspd/cli scope set my.MrxSiN.syncthingliveupdate system/0 com.github.catfriend1.syncthingfork/0'
```

## 🧪 Validation status

The release version is `1.0.0` (`versionCode 1`).

Validated on a Pixel 8 Pro running Android 17 (SDK 37) with KernelSU, Vector 2.2 and
Syncthing-Fork 2.1.3.0. During a live transfer the notification was confirmed as a single
record carrying:

```text
channel=05_syncthing_live_update
flags=ONGOING_EVENT|ONLY_ALERT_ONCE|NO_CLEAR|FOREGROUND_SERVICE|PROMOTED_ONGOING
```

and reverting to `01_syncthing_persistent` without the promotion flag once the sync
completed. The screenshots above are from that run.

The included GitHub Actions workflow builds on every push, pull request, and manual run. A
`v*` tag additionally builds, signs, and attaches the release APK to the GitHub Release when
the four signing secrets are configured.

## 🩺 Troubleshooting

| Problem | Suggested action |
| --- | --- |
| Notification shows a progress bar but no chip appears | The `system` scope is missing, or the device has not been rebooted since it was granted. The `system_server` hook is only installed at boot. |
| Nothing changes at all during a sync | Check logcat for `SyncthingLiveUpdate`. `Host notification method not found` means a Syncthing update renamed the method the module observes. |
| Chip shows the icon but no percentage | Expected while Syncthing itself is the visible app, or while the notification is pinned as a heads-up. SystemUI collapses the chip to an icon in both cases; the percentage returns once another app is in front. |
| Chip and the status bar clock overlap or flicker | Another Xposed module is redrawing the status bar and colliding with the chip animation. Disabling it resolves the overlap; the notification itself is unaffected. |
| Progress shows but no folder name | Either only a remote is pulling from this device, in which case no local folder enters a transferring state, or the log reports `Folder names unavailable`, meaning a Syncthing update moved the folder model. |
| Direction badge never appears | Look for `Transfer direction unavailable` in the log. Progress still works without it. |
| Two Syncthing notifications | A build older than `1.0.0` posted its own notification on id `7710`. Force-stop Syncthing once after updating the module. |

## 🙏 Credits

This project depends on and benefits from the following open-source work:

| Project | Contribution |
| --- | --- |
| [Syncthing-Fork](https://github.com/researchxxl/syncthing-android) | The host application whose sync figures and notification this module builds on. Licensed under MPL-2.0. |
| [Vector](https://github.com/JingMatrix/Vector) by JingMatrix | Provides the ART hooking framework used in both the app process and the system server. Licensed under GPL-3.0. |
| [libxposed API](https://github.com/libxposed/api) | The modern Xposed module API this module compiles against. Licensed under Apache-2.0. |

## ⚠️ Disclaimer

This project is not affiliated with, endorsed by, or sponsored by the Syncthing project or
the maintainers of Syncthing-Fork. It is provided for educational and personal use. Host app
updates or Android platform changes may break the hooks without notice.
