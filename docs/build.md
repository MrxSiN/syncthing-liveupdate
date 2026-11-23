# Build

The module is XAX. There is no Java, Kotlin, Brainfuck, Gradle or `d8` in the build.

## Sources

| Path | What it is |
| --- | --- |
| `source/xax/syncthingliveupdate.xax` | The canonical store. Everything else in `source/xax/` is how it was made. |
| `source/xax/module.json` | `xax-construct-v1` carrier: types, logging, platform contract, the `system_server` promotion patch, and the package/module record (scopes, static fields, pinned inputs). |
| `source/xax/host.json` | Syncthing-Fork side: DexKit and name resolvers, reflection diagnostics, progress/direction/folder tracking. |
| `source/xax/promoter.json` | The `startForeground` rewrite, channel, palette, icons and `ProgressStyle` appearance. |
| `source/xax/systemui.json` | SystemUI polish: spring animation, wavy bar hook, chip colours. |
| `source/xax/entry.json` | Static initialisation and the three lifecycle entries. |
| `source/xax/transactions/0N-*.batch` | Native `xaxc edit` batches that define the float code (icon outlines, badge arrows, spring, wave drawing). The construct format cannot express floats. |
| `source/branding/` | Static resources: manifest, label, description, launcher and endpoint icons. |
| `tools/xcheck/xcheck.xax` | `xcheck`, the project's checker: an XAX Linux x86-64 program. `request.json` beside it is the `xax-construct-v1` request it was constructed from. |
| `checks/*.checks` | `xcheck`'s input: the project's structural checks and the Syncthing-Fork host contract. |
| `source/inputs/` | Pinned build inputs, named by BLAKE3 in the module record: the branding archive and DexKit 2.3.0 (DEX and arm64 library). Origins are in `toolchains.json`. |

## Building the APK

See the README's Build section: `xaxc verify`, `resolve` and `build` of the store with
`source/inputs/*`, then `apksigner`. `toolchains.json` names the XAX commit and both seeds' SHA-256;
the Linux and Windows seeds produce the same APK bytes.

## Checks

`xcheck CHECKLIST` runs a checklist: one assertion per line, `f PATH` (the file exists),
`h PATH TEXT` (it contains TEXT) or `l PATH TEXT` (it does not); `#` lines are comments. It
prints `FAIL <line>` for each failure and exits 1 if any failed. `xcheck CHANGELOG VERSION`
prints the changelog section under `## VERSION ` (the release notes) and exits 1 when there is
none. Files are read whole, up to 124 MiB; consecutive lines on the same file read it once.

```sh
"$xaxc" verify tools/xcheck/xcheck.xax build/xcheck.rec
"$xaxc" build  tools/xcheck/xcheck.xax build/xcheck build/xcheck.prov
git ls-files > build/files.txt
build/xcheck checks/project.checks
```

It is a Linux executable; on Windows run it in WSL. The host-contract recipe is in
[compatibility.md](compatibility.md#checking-a-syncthing-fork-release).

## How the store is made

1. `xax_construct.construct` (XAX's reference constructor, run from the XAX checkout) builds a build
   request from the five carriers, merged in this order: `module`, `host`, `promoter`, `systemui`,
   `entry`. Float leaves are stubs with the right signatures.
2. The module carrier is replaced by an `android-libxposed-jvm-module-v3` carrier whose `entries`
   name `on_module_loaded` and `on_system_server_starting` (ADR-411), the same way XAX's
   `tests/test_libxposed_lifecycle.py` does. Neither step can be done natively yet
   ([XAX#135](https://github.com/MrxSiN/XAX/issues/135)).
3. The six batches are applied in order with `xaxc edit`, each followed by `xaxc verify`. Each batch
   names the root it applies to and replaces one stub (one `replace` per batch).

To change the module, edit the committed store with `xaxc edit` batches and `verify` after each;
regenerate from the carriers only when the carriers change, and then re-apply the batches.

Regenerating reproduces the committed store byte for byte when the carriers are unchanged
(checked 2026-11-23 at XAX 707eb6f). The swap in step 2 rebuilds the module over the v3 carrier,
then the package and the release request over that module, as `construct` does; the request is
the store's root, keeping only the objects it reaches. The v3 record also carries
`"auto_hot_reload": true`. When carriers change, the functions' store order changes, so each
batch's `R` line and `F<n>` references are mapped onto the new chain by CID: `F<n>` of the
original chain's store before that batch names a CID, and the same CID's index in the new chain
is used.

## Authoring notes

- Foreign members are typed exactly: a `String` passed where the member takes `Object` needs a
  `jvm-checkcast` first.
- A member that may throw uses a `-catch` ABI and returns the throwable as an extra result; an
  exception from any other call ends the hook, so risky calls are always the catching kind.
- Hookers and listeners are `function.address` values of a `jvm.callback` type. They capture
  nothing; per-instance state lives in the `Runtime` holder's static fields or in weak maps.
- Functions on the JVM target return one value. A two-result function makes `build` decline with
  no record ([XAX#137](https://github.com/MrxSiN/XAX/issues/137)).
- Write batch hex constants in lowercase ([XAX#136](https://github.com/MrxSiN/XAX/issues/136)).

## Upstream gaps

| Issue | Effect here |
| --- | --- |
| [XAX#135](https://github.com/MrxSiN/XAX/issues/135) | A new store (this one, `xcheck`) and carrier changes need XAX's Python reference tooling. Native carrier replacement lands in XAX release 0042. |
| [XAX#160](https://github.com/MrxSiN/XAX/issues/160) | A non-ASCII string constant makes `build` decline silently, so `glyph` makes `↓ ↑ · ≈` at run time. |
| `xcheck` reach | XAX's construct carrier offers Java member calls only to `android-libxposed`, and its Linux platform only `openat`/`read`/`write`/`close`/`mmap`/`exit_group`. So `xcheck` cannot list a directory, fetch a URL or inflate a zip: CI passes it `git ls-files` output and an `unzip`ped dex. |

Closed since the 0038 pin: [XAX#134](https://github.com/MrxSiN/XAX/issues/134) (the branded
manifest already carries `versionName`), [XAX#136](https://github.com/MrxSiN/XAX/issues/136) and
[XAX#137](https://github.com/MrxSiN/XAX/issues/137).
