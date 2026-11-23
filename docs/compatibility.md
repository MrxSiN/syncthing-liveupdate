# Compatibility

The module reaches into private code in three places, so what it supports is exactly what
someone has read the code of. This page separates what is claimed from what has been
observed, because those are not the same thing.

## Android releases

Every assumption about a release lives in the platform-contract functions of the XAX source
(`sdk`, `contract_describe`, `polish_supported`, `request_promotion`) rather than spread
through the module, so adding a release is an edit in one place.

| Release | Promotion requested through | SystemUI polish |
| --- | --- | --- |
| Android 16 (API 36) | `setColorized(true)` | attempted |
| Android 17 (API 37) | `setRequestPromotedOngoing(true)` | attempted |
| Anything else | as API 37 | **off** |

The rule for what may be promoted changed between the two releases and the two forms are
mutually exclusive: Android 17 refuses to promote a colorized notification, and Android 16
has no explicit request to make. A colorized notification is also why a Live Update can come
out looking like a filled, light-coloured card rather than an ordinary one.

An unverified release keeps the base Live Update — that side is public notification API plus
one private method name — and deliberately leaves the SystemUI polish off rather than
discovering private class and field names opportunistically. The result is predictable and
easy to describe in a bug report: base Live Update, no wavy bar, no chip colour. The load
message names the contract in use:

```text
Syncthing Live Update v1.2.0 loaded in …, contract=Android 17 (API 37), …
```

## What has actually been tested

| Component | Evidence |
| --- | --- |
| Android 17 (SDK 37) | Pixel 8 Pro, builds `CP2A.260805.005` and `CP3A.260905.009`, KernelSU, Vector 2.2. Full path confirmed on device: `channel=05_syncthing_live_update`, `flags=…|PROMOTED_ONGOING`, `template=Notification$ProgressStyle`, tracker colour `0xff8ccdff`, wavy bar, spring animation, chip colour. |
| XAX pre-release (built as 2.0.0, released as 1.2.0) | Pixel 8 Pro, `CP4A.261105.040.A1`, Vector 2.2, Syncthing-Fork 2.1.7.0, 2026-11-19. The module loads in `system_server`, Syncthing-Fork and SystemUI; DexKit resolved `sn1.g`, `SyncthingService.v` and `ft2.f`; all three SystemUI hooks installed. A real 1 GB upload was promoted: `channel=05_syncthing_live_update`, `PROMOTED_ONGOING`, `ProgressStyle`, colour `0xff8ccdff`, short text `82%`, with the wavy bar, cookie tracker, endpoint icons, upload badge and blue chip on screen and no crash. Not observed: folder names (download only) and the spring's motion. |
| 1.2.0 (XAX) | Pixel 8 Pro, `CP4A.261105.040.A1`, Vector 2.2, Syncthing-Fork 2.1.7.0, 2026-11-23. Uploads of 0.5–1 GB showed `↑ 20 MB/s · ≈ 12 sec` … `≈ 7 sec` and finished about 4 s after the last estimate; 1–2 GB downloads showed `Device - Zephyrus G15 · ↓ 25 MB/s · ≈ 32 sec` … `≈ 7 sec`. The public (lock-screen) version was attached for the whole transfer. On cached starts the hooks installed in 4–5 ms with no DexKit index; a download then showed the rate and estimate as before. No crash in Syncthing or SystemUI. Not observed: the public version on a lock screen that hides sensitive content (this device shows sensitive content, and hides silent and seen notifications on the lock screen). |
| Android 16 (SDK 36) | **Not tested on a device.** The contract is written from the documented promotion rule; the runtime behaviour is not tested. |
| Syncthing-Fork 2.1.3.0 | Validated on-device, and every hooked member confirmed present by `scripts/check-host-contract.sh`. |
| Syncthing-Fork 2.1.5.0 | Every hooked member confirmed present by `scripts/check-host-contract.sh`, and exercised on a device: DexKit resolved `updatePersistentNotification` and a real upload was promoted. |
| Android 17 QPR2 | Pixel 8 Pro, build `CP4A.261105.040.A1`. v1.1.1 crash-looped SystemUI: DexKit's in-memory index faulted (SIGSEGV in `libdexkit.so`). 1.1.2 reads the APK file and leaves SystemUI to name lookups. Confirmed on device: SystemUI restarts cleanly with the wavy bar, animation and chip hooks installed, and a real download was promoted (`PROMOTED_ONGOING`, `ProgressStyle`, tracker colour `0xff8ccdff`). |
| Syncthing-Fork 2.1.7.0 (F-Droid, R8-shrunk) | `NotificationHandler` is `sn1`, `updatePersistentNotification` is `g`, and `State` is an unboxed int. Confirmed in the installed APK's dex that the signature plus `always_run_in_background` and the state-name lookup each match exactly one method. Confirmed on device: DexKit resolved `sn1.g`, `SyncthingService.v` and `ft2.f` in 30ms, and the running state was read correctly. |
| DexKit resolution | Host only, from its APK file, at hook-installation time, and only for a query the resolution cache has no answer for (from 1.2.0). |
| Promotion through the contracts | Confirmed after a framework restart: `PromotionPolicyPatch` driven by `Android37Contract` set `PROMOTED_ONGOING` on a real upload. |

Android 16 is the boundary worth closing next, because it is the one release where the
promotion path differs and where nothing but a physical device can confirm the result.

## Checking a Syncthing-Fork release

The update most likely to break the module is an upstream release that renames or removes
something it hooks. Compiling the module cannot notice that, because the host is not on the
module's classpath.

```sh
HOST_VERSION=2.1.5.0 HOST_TAG=v2.1.5.0
curl -fsSL -o build/host.apk "https://github.com/researchxxl/syncthing-android/releases/download/$HOST_TAG/com.github.catfriend1.syncthingfork_release_v$HOST_VERSION.apk"
unzip -p build/host.apk 'classes*.dex' > build/host.dex
build/xcheck checks/host-contract.checks
```

`xcheck` ([docs/build.md](build.md#checks)) looks for every hooked class, method and field of
`checks/host-contract.checks` in that release's dex string pools. A name that is gone is a definite break. A name that is present is
strong but not conclusive evidence the member is unchanged, because a string pool does not
say which class a method name belongs to; on a device, DexKit resolution closes that gap by
matching the declaring class and signature as well.

CI runs the check weekly and on demand, not on pull requests, so an upstream release shows
up as a failing scheduled run rather than as a network flake on an unrelated change.

## Updating the module without a reboot

1.2.0 declares `autoHotReload=true` again (the XAX rewrite's pre-release builds could not, until
[XAX#133](https://github.com/MrxSiN/XAX/issues/133)). A newly started Syncthing-Fork or SystemUI
process picks up each new build. `system_server` keeps the build it started with, so after an
update the promotion rules in force are the old ones until the framework restarts. What follows
applies to the stale-`system_server` detection.

The module detects this itself and says so once, in the log, rather than leaving
the Live Update silently unpromoted. Restarting the framework is enough, and is
much shorter than a reboot:

```sh
su -c 'stop; start'
```

The detection compares the module's own APK against
`sys.system_server.start_elapsed`, the moment `system_server` last started, and
not against boot time. That distinction is the whole point: the restart above
does not reboot the kernel, so `elapsedRealtime` keeps running and a boot-time
comparison would go on reporting a problem the restart had already fixed. If the
APK's recorded path no longer exists at all, that counts as stale on its own —
installing an update replaces the whole directory, so a path the framework still
points at belongs to an install that has been superseded.

### Without the flag

Observed on Vector 2.2 before `autoHotReload` was declared: the framework caches
a module's dex in memory and records its install path when it first loads it.
Reinstalling the APK did **not** replace the code running in host processes — the
host kept loading the cached dex, and its `nativeLibraryDir` kept pointing at the
replaced directory, which is why DexKit's library could not be found in that
state. That is the failure mode the flag removes, and it is worth recognising:
`DexKit library is not at …` naming a directory that no longer exists means the
running module is not the installed one.

## Frameworks

A framework implementing the modern Xposed API, version 101 or newer — [Vector][vector] with
Magisk or KernelSU and Zygisk enabled. LSPatch cannot run this module: a rootless patch
injects into the target app only, and the promotion flag has to be restored inside
`system_server`.

[vector]: https://github.com/JingMatrix/Vector
