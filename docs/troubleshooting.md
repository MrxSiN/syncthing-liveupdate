# Troubleshooting

Everything the module does is logged under one tag:

```text
SyncthingLiveUpdate
```

A healthy start logs the framework it attached to, the platform contract it selected, the
host method it is observing, and the channel it will promote on.

| Problem | Suggested action |
| --- | --- |
| Notification shows a progress bar but no chip appears | The `system` scope is missing, or the device has not been rebooted since it was granted. The `system_server` hook is only installed at boot. |
| Nothing changes at all during a sync | Check logcat for `SyncthingLiveUpdate`. `Host notification method not found` means a Syncthing update renamed the method the module observes, and that neither DexKit nor a name lookup could find it. Run `scripts/check-host-contract.sh <version>` to confirm. |
| Chip shows the icon but no percentage | Expected while Syncthing itself is the visible app, or while the notification is pinned as a heads-up. SystemUI collapses the chip to an icon in both cases; the percentage returns once another app is in front. |
| Chip and the status bar clock overlap or flicker | Another Xposed module is redrawing the status bar and colliding with the chip animation. Disabling it resolves the overlap; the notification itself is unaffected. |
| Progress shows but no folder name | Either only a remote is pulling from this device, in which case no local folder enters a transferring state, or the log reports `Folder names unavailable`, meaning a Syncthing update moved the folder model. |
| Bar is flat, jumps between values, or the chip is grey | Three possibilities, and the log says which. `SystemUI polish left off on unverified API …` means the device runs a release the module has not been read against; see [compatibility.md](compatibility.md). `Wavy progress unavailable`, `Progress bar animation unavailable` or `Chip colours unavailable` mean a system update moved the SystemUI internals. Neither message means the SystemUI scope is missing — that one produces no SystemUI log lines at all. |
| Bar starts wavy and goes flat mid-sync | `Wavy progress disabled after 3 failed draws` in the log. The custom drawing hit a structural mismatch with this SystemUI build and switched itself off for the rest of the process; the platform's own bar is used from then on. The accompanying stack trace is what to report. |
| Direction badge never appears | Look for `Transfer direction unavailable` in the log. Progress still works without it. |
| Promotion never happens on Android 16 | Look for `Promotion could not be requested on …`. Android 16 asks through `setColorized`; a build that rejects it will say so once. |
| A module change does not seem to take effect at all | The framework caches the module's dex from when it loaded it, so reinstalling the APK does not replace the code running in host processes. `DexKit library is not at …` naming a directory that no longer exists is the giveaway. Restart the framework. |
| Styled bar but never promoted, right after installing or updating | The log says *"This module was installed or updated after the device booted"*. A module cannot be injected into a running `system_server`, so the promotion half is whatever was loaded at boot. Restart the framework with `su -c 'stop; start'` — about as long as a boot animation — or reboot. |
| Two Syncthing notifications | A build older than `1.0.0` posted its own notification on id `7710`. Force-stop Syncthing once after updating the module. |

## Reading a reflection failure

A member the module cannot reach is reported once, with enough context to act on without a
second run:

```text
Reflection ABSENT: com.nutomic.syncthingandroid.model.LocalCompletion#setFolderStatus
  expected=(java.lang.String, java.lang.Boolean, …FolderStatus)
  sdk=37, fingerprint=…, cause=java.lang.NoSuchMethodException: …
```

- `ABSENT` — the member is not declared where the module expects it. An upstream rename.
- `DENIED` — the member exists but the runtime refused it. A tightened hidden-API policy.
- `THREW` — the member exists and was called, and the call itself threw. It is still there
  but no longer behaves as the module assumed.
