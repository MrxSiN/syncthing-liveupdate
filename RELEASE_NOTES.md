# Syncthing Live Update v1.1.1

Compatibility and correctness release. Tested on a Pixel 8 Pro on **Android 17**, build
`CP3A.260905.009`, with Vector 2.2 and Syncthing-Fork 2.1.5.0.

### What's in it

- **Works with the latest Syncthing-Fork.** Every class, method and field the module hooks is
  verified present in **2.1.5.0** and **2.1.6.0-rc.1**, and 2.1.5.0 is confirmed on a device.
- **Survives upstream renames.** Hooked host methods are found by their shape — class,
  signature and the string constants they use — before falling back to a name lookup.
- **No reboot to update.** The module declares `autoHotReload`, so a new build is loaded into
  running processes.
- **Android 16 is promoted again**, through the `setColorized` request that release expects.
- **An unknown Android release degrades predictably**: the base Live Update stays, the
  SystemUI polish switches itself off.
- **The wavy bar costs less**, asking for a frame at most every 16ms, and stops trying after
  three failed draws.

### Install

1. Install `SyncthingLiveUpdate-v1.1.1.apk`
2. Enable Syncthing Live Update in Vector
3. Grant all three scopes: the system server, Syncthing-Fork and SystemUI
4. Reboot once

### Notes

- Upgrading from an earlier version needs one framework restart — `su -c 'stop; start'` —
  because the version being replaced could not hot reload. Updates after this one do not.
- A flat progress bar and a grey chip mean the SystemUI scope is missing.
- Android 16 is supported but has not been tested on a device.
- Needs Android 16 or newer, root with a Zygisk-based Xposed framework exposing libxposed
  API 102, and Syncthing-Fork (`com.github.catfriend1.syncthingfork`).
