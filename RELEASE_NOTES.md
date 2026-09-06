# Syncthing Live Update 1.0.0

First release.

- Shows Syncthing's overall sync progress as an Android Live Update: a promoted ongoing
  notification with a `ProgressStyle` bar and a percentage chip in the status bar.
- Rewrites Syncthing's own persistent notification instead of adding a second one. Its
  title, icon, actions and tap target are kept, and it stays the foreground-service
  notification; when the sync finishes it is passed through untouched and behaves exactly
  as Syncthing intended.
- Reads the figures straight from
  `NotificationHandler.updatePersistentNotification(...)` in the Syncthing process, so
  there is no REST polling and no extra load on the host.
- Names the folders currently transferring on a second line, using their configured
  labels; past two, the remainder is shown as a count.
- Badges the status icon with an arrow for the transfer direction — down while this device
  pulls from a remote, up while a remote pulls from this device, both at once when each is
  happening — so the status bar chip shows which way the data is moving.
- Restores `FLAG_PROMOTED_ONGOING` in `system_server` for the module's own channel only,
  because Syncthing-Fork cannot declare `POST_PROMOTED_NOTIFICATIONS` itself.

Requires Android 16 or newer, root with a Zygisk-based Xposed framework exposing libxposed
API 102 (developed against Vector 2.2), and Syncthing-Fork
(`com.github.catfriend1.syncthingfork`).
