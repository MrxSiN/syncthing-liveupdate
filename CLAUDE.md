# Syncthing Live Update — working rules

Xposed module (libxposed API 102) that turns Syncthing-Fork's persistent notification into an
Android 16+ Live Update. Hooks run in three processes: `system_server`, Syncthing-Fork and
SystemUI. See `docs/architecture.md`.

## Principles

- **SOLID.** One reason to change per class (SRP); extend through new implementations, not
  edits (OCP, e.g. a new `PlatformContract` per Android release); implementations must stand
  in for their interface (LSP); small interfaces (ISP); depend on `MemberResolver`,
  `PlatformContract`, not concrete classes (DIP).
- **DRY, KISS, YAGNI, Law of Demeter, Separation of Concerns, Boy Scout Rule.**
- **No hardcoding.** Hooked members are declared once as a `MemberQuery` (`HostApp`,
  `SystemUi`) and found dynamically: DexKit by shape first, name lookup second. Identify a
  member by what it does (signature, string constants, fields it reads), not only by name;
  R8-shrunk host builds rename everything.
- **DexKit reads APK files, never process memory.** `DexKitBridge.create(ClassLoader, true)`
  faults natively on Android 17 QPR2 and crash-looped SystemUI. Never index SystemUI: its
  hooked widgets live in `framework.jar`.

## Language rule

New or replaced code is written in **Brainfuck** (compiled with BBC) and **XAX** wherever the
target supports it:

- BBC: `D:\GitHub\BBC` — Brainfuck compiler, emits Windows x86-64 and Android ARM64 executables.
- XAX: `D:\GitHub\XAX`; XAX-MCP: `D:\GitHub\XAX-MCP` (the `xax` MCP server). Call
  `xax_capabilities` first and use the latest revision of each.

Limit, checked 2026-11-11: module code runs as classes inside other apps' JVM processes and
calls libxposed and DexKit. XAX-MCP reports its JVM/Android targets as UNIMPLEMENTED, and BBC
only emits standalone executables, so neither can produce hook code yet. Module code therefore
stays Java. Re-check before each change and switch when a target lands; standalone tooling
(scripts, analyzers) is the first candidate. Report an unsupported case instead of silently
using another language.

## Tooling

- ADB: `D:\Android\platform-tools\adb.exe`. In Git Bash set `MSYS_NO_PATHCONV=1` or device
  paths like `/system/...` get rewritten to Windows paths.
- Gradle: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`, then
  `./gradlew testDebugUnitTest assembleRelease`.
- Checks: `sh scripts/check-project.sh`; `scripts/check-host-contract.sh <host version>`.
- Verify hook targets on a device: pull the APK/jar, unzip `classes*.dex`, run
  `dexdump` from `build-tools`.
