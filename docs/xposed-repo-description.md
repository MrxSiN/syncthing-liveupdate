# Xposed-Modules-Repo description

The module's page in the Xposed module repository is a separate repository and cannot be
updated from here. Its current text is out of date: it says the module hooks two processes
and tells users to grant only the system server and Syncthing-Fork, with no mention of the
SystemUI work. A user installing from the repository therefore has no way to know that the
flat bar and grey chip are a missing scope rather than the intended look.

Copy the text below into the module's `README.md` in the `Xposed-Modules-Repo` mirror.

---

## Syncthing Live Update

Turns Syncthing-Fork's sync progress into an Android Live Update: a promoted ongoing
notification visible in the status bar chip, on the lock screen and on Always-on Display.

The module rewrites the notification Syncthing already posts rather than adding a second
one. Its title, icon, Exit action and tap target are kept, and it stays the
foreground-service notification the app depends on. When nothing is transferring, the
notification is passed through untouched.

### Scope — three processes

| Process | Required | Without it |
| --- | --- | --- |
| `system` | yes | The notification is rewritten but never promoted, and no chip appears |
| `com.github.catfriend1.syncthingfork` | yes | Nothing happens at all |
| `com.android.systemui` | optional | The bar is flat and jumps between values, and the chip keeps the system grey |

Grant all three. Do not enable any other application in the scope.

**A flat bar and a grey chip mean the SystemUI scope is missing, not that the module is
working as intended.**

After granting the system server scope, **reboot** — that hook is installed while the system
server starts and cannot take effect until the next boot.

### What it shows

- Overall sync completion as a `Notification.ProgressStyle` bar, and a percentage in the
  status bar chip.
- The filled part of the bar drawn as a Material 3 Expressive wavy progress indicator that
  flows while the transfer runs, led by a cookie-shaped tracker carrying the Syncthing
  glyph, with a tonal circle at each end showing this device and the remote one in the order
  the data moves.
- The bar gliding to each new value on a spring instead of jumping.
- The status bar chip filled with Syncthing's blue rather than the system surface colour.
- The names of the folders currently transferring, using the labels configured in Syncthing.
- A direction badge on the status icon: down while this device pulls, up while a remote
  pulls from it, both when each is happening at once.

### Requirements

- Android 16 (API 36) or newer. Developed and validated on Android 17.
- Root, and a framework implementing the modern Xposed API version 101 or newer, such as
  Vector with Magisk or KernelSU and Zygisk enabled. LSPatch cannot run this module: the
  promotion flag has to be restored inside `system_server`, which a rootless patch cannot
  reach.
- Syncthing-Fork (`com.github.catfriend1.syncthingfork`).

### Notes

- Every figure comes from the host's own callbacks. No REST polling, no API key, no timers,
  no background service.
- `FLAG_PROMOTED_ONGOING` is restored for this module's own notification channel only. Every
  other notification on the device, including Syncthing's own, keeps the platform's verdict.
- A hook that cannot install is logged and skipped, and Syncthing is left exactly as it was.
- Logs are tagged `SyncthingLiveUpdate`.

Source, full documentation and troubleshooting:
https://github.com/MrxSiN/syncthing-liveupdate
