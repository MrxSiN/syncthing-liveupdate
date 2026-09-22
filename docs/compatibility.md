# Compatibility

The module reaches into private code in three places, so what it supports is exactly what
someone has read the code of. This page separates what is claimed from what has been
observed, because those are not the same thing.

## Android releases

Every assumption about a release lives in one `PlatformContract` implementation rather than
spread through the module, so adding a release is a new implementation rather than edits in
several files.

| Release | Contract | Promotion requested through | SystemUI polish |
| --- | --- | --- | --- |
| Android 16 (API 36) | `Android36Contract` | `setColorized(true)` | attempted |
| Android 17 (API 37) | `Android37Contract` | `setRequestPromotedOngoing(true)` | attempted |
| Anything else | `UnknownContract` | as API 37 | **off** |

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
Syncthing Live Update v1.1.0 loaded in …, contract=Android 17 (API 37), …
```

## What has actually been tested

| Component | Evidence |
| --- | --- |
| Android 17 (SDK 37) | Pixel 8 Pro, builds `CP2A.260805.005` and `CP3A.260905.009`, KernelSU, Vector 2.2. Full path confirmed on device: `channel=05_syncthing_live_update`, `flags=…|PROMOTED_ONGOING`, `template=Notification$ProgressStyle`, tracker colour `0xff8ccdff`, wavy bar, spring animation, chip colour. |
| Android 16 (SDK 36) | **Not tested on a device.** The contract is written from the documented promotion rule; the selection logic is covered by unit tests, the runtime behaviour is not. |
| Syncthing-Fork 2.1.3.0 | Validated on-device, and every hooked member confirmed present by `scripts/check-host-contract.sh`. |
| Syncthing-Fork 2.1.5.0 | Every hooked member confirmed present by `scripts/check-host-contract.sh`, and exercised on a device: DexKit resolved `updatePersistentNotification` and a real upload was promoted. |
| DexKit resolution | On device, 5 dex files indexed in ~50ms in the host and 3 in ~78ms in SystemUI, once per process at hook-installation time. |
| Promotion through the contracts | Confirmed after a framework restart: `PromotionPolicyPatch` driven by `Android37Contract` set `PROMOTED_ONGOING` on a real upload. |

Android 16 is the boundary worth closing next, because it is the one release where the
promotion path differs and where nothing but a physical device can confirm the result.

## Checking a Syncthing-Fork release

The update most likely to break the module is an upstream release that renames or removes
something it hooks. Compiling the module cannot notice that, because the host is not on the
module's classpath.

```bash
scripts/check-host-contract.sh 2.1.5.0
```

The script downloads that release's APK and looks for every hooked class, method and field
in its dex string pools. A name that is gone is a definite break. A name that is present is
strong but not conclusive evidence the member is unchanged, because a string pool does not
say which class a method name belongs to; on a device, DexKit resolution closes that gap by
matching the declaring class and signature as well.

CI runs the check weekly and on demand, not on pull requests, so an upstream release shows
up as a failing scheduled run rather than as a network flake on an unrelated change.

## Updating the module without a reboot

The module declares `autoHotReload=true` in `module.prop`, which asks the
framework to load an updated build into already-running processes. In the host
process this was confirmed on device: installing an update and restarting only
Syncthing brought up the new version immediately, with DexKit resolving from the
new install path.

`system_server` is the half that lags. Until the framework has reloaded it, the
promotion rules in force are the ones loaded earlier, so an update can leave the
two halves at different versions. Upgrading to the first build that declares the
flag necessarily needs a framework restart, because the build being replaced did
not declare it.

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
