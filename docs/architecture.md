# How it works

Two problems have to be solved, and they live in different processes. A third process,
SystemUI, only adds polish.

## Finding what to hook

The module hooks members of other people's code, so every one of them is declared once, as
a `MemberQuery`, in `HostApp` for Syncthing-Fork and `SystemUi` for SystemUI. A query says
where the method is expected to be declared, what it is expected to be called, what it
takes, and — where one exists — a string constant its body references.

`Resolvers.open(...)` answers those queries with DexKit first and a plain name lookup
second. DexKit searches the dex files already loaded in the process by shape, so a method
that upstream renamed or moved can still be found by the constants it uses; the name lookup
answers whenever DexKit is unavailable or finds no unique match, which means the module is
never worse off than it was before DexKit. The index is opened while a process's hooks are
installed and closed immediately afterwards — nothing holds one past startup.

## In the Syncthing process

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
   which is why a plain rescan never puts a folder name on the notification. The bookkeeping
   itself is in `FolderStates`, apart from the hooks, and is covered by unit tests.
5. The rewrite itself happens on `Service.startForeground(...)`, filtered to the host's
   persistent channel. `Notification.Builder.recoverBuilder(...)` reopens the notification
   the host just built, so nothing has to be reconstructed from scratch, and the module only
   adds what promotion requires.
6. The look is kept apart from the promotion. `LiveUpdatePromoter` sets only what the
   platform needs to promote the notification, and hands the builder to a
   `LiveUpdateAppearance`. The shipped `ExpressiveAppearance` works entirely through
   `ProgressStyle`, because a promoted notification may not carry custom views. SystemUI
   draws the tracker, start and end icons untinted, so each shape and its glyph are rendered
   into a bitmap once per theme. The endpoint glyphs live in the module's own resources and
   are opened in the host through `getModuleApplicationInfo()`.

## In `system_server`

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
    notification.flags |= contract.flagPromotedOngoing();
}
```

Nothing else about the platform's promotion policy is touched.

## In SystemUI

SystemUI draws the notification and the chip, so the parts `ProgressStyle` cannot express
are added where they are drawn. Each hook first checks that it is looking at Syncthing-Fork's
notification; everything else is drawn by the platform as before. The whole of this section
is switched off on a release the module has not been read against — see
[compatibility.md](compatibility.md).

- `NotificationProgressDrawable.draw(Canvas)` is replaced for Syncthing's bar. The platform's
  layout of the bar is kept and only its painting changes: the filled part becomes a wave
  (4dp stroke, 3dp amplitude, 40dp wavelength) that travels one wavelength per second and
  flattens near either end, and the rest becomes a flat 4dp track ending in a stop dot. The
  wave holds still when animations are turned off. Frames are scheduled at most every 16ms,
  so a 120Hz panel does not double the work, and three failed draws disable the wave for the
  rest of the process rather than retrying it on every frame.
- `NotificationProgressBar.setProgressModel(Bundle)` is where each update reaches the bar.
  The update is applied at the value on screen, and the bar then travels to the new value on
  a critically damped spring. The notification reports progress in tenths of a percent so the
  travel is smooth rather than stepped.
- Android 17 always paints a notification chip in the system surface colour. The constructor
  of `OngoingActivityChipModel.Active` is intercepted for Syncthing's notification key, and
  its colours are swapped for Syncthing's primary and on-primary pair. Each hooked
  constructor carries its own argument index, so a build that exposes several constructors
  cannot have one hook rewriting another's argument.

## The channel

The host posts its persistent notification on an `IMPORTANCE_MIN` channel, and the platform
refuses to promote anything posted there. During a transfer the notification is moved to
`05_syncthing_live_update` at `IMPORTANCE_LOW`; when the sync completes it goes back to
`01_syncthing_persistent` and behaves exactly as before.

## Failing open

A module that patches private code will eventually meet a build it does not fit, and the
only acceptable outcome is that a feature disappears rather than the host breaking. That is
enforced in two places rather than by habit at each call site:

- `ModuleRuntime.hook(...)` is the single path to the framework, and it catches a failed
  installation and returns `null`. Every feature therefore inherits the same policy.
- `Reflect` returns `null` or `false` for anything it cannot reach, and hands the reason to
  `ReflectionDiagnostics` first. A missing member, a denied access and a call that threw
  read the same to the caller and differently in the log, together with the SDK level and
  the build fingerprint, reported once per distinct failure.
