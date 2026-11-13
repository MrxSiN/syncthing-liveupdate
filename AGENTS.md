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

- BBC: Brainfuck compiler, emits Windows x86-64 and Android ARM64 executables.
- XAX and XAX-MCP (the `xax` MCP server). Call
  `xax_capabilities` first and use the latest revision of each.

Limit, checked 2026-11-11: module code runs as classes inside other apps' JVM processes and
calls libxposed and DexKit. XAX-MCP reports its JVM/Android targets as UNIMPLEMENTED, and BBC
only emits standalone executables, so neither can produce hook code yet. Module code therefore
stays Java. Re-check before each change and switch when a target lands; standalone tooling
(scripts, analyzers) is the first candidate. Report an unsupported case instead of silently
using another language.

## Tooling

- ADB: in Git Bash set `MSYS_NO_PATHCONV=1` or device
  paths like `/system/...` get rewritten to Windows paths.
- Gradle: set `JAVA_HOME` to Android Studio's bundled JBR, then
  `./gradlew testDebugUnitTest assembleRelease`.
- Checks: `sh scripts/check-project.sh`; `scripts/check-host-contract.sh <host version>`.
- Verify hook targets on a device: pull the APK/jar, unzip `classes*.dex`, run
  `dexdump` from `build-tools`.

<!-- ecosystem-policy v1 (2026-11-13): identical in every MrxSiN first-party repository. Change it everywhere in one coordinated set of PRs, never in one repository alone. -->
## Ecosystem rules (v1)

Shared by MrxSiN's first-party repositories. Any other rule in this file that is stricter or more specific to this repository wins.

- **Who owns what.** XAX owns the semantic graph, verifier, compiler, ABIs, backends, targets and artifacts. BBC owns the Brainfuck frontend, BFX and Brainfuck-specific optimization. XAX-MCP owns MCP transport, tool schemas, sandbox and permissions. Dependencies point one way: consumers -> XAX-MCP / BBC -> XAX.
- **New production logic** is XAX, or Brainfuck + XAX, wherever the toolchains reach the target; this repository's own language rules say where that is. Existing code in other languages is tracked migration debt, not permission to add more. Declarative config, assets, fixtures and licenses are not code to convert.
- **Upstream first.** When a toolchain lacks something this repository needs: reproduce it on the toolchain's latest `main`, search its issues and PRs, then fix a small bounded gap with a PR in the owning repository, or open (or link) an issue for a large one and mark only the affected work blocked-upstream. No consumer-side shims, private toolchain forks or local toolchain patches.
- **Pins.** Build against exact toolchain commits, recorded in one machine-readable place. Check the toolchains' latest commits before nontrivial work; move a pin by reviewed PR once checks pass, and write down why when an older pin stays.
- **No hardcoding** of secrets, personal or machine paths, toolchain locations, obfuscated host names or version-specific offsets. Machine-local paths belong in your own `~/.claude/CLAUDE.md` or `CLAUDE.local.md`, never in the repository. Specification constants (opcodes, ABI layouts, protocol markers, security limits) are fine when named and documented.
- **Design.** SOLID, DRY, KISS, YAGNI, Law of Demeter, Boy Scout Rule and Separation of Concerns, applied with judgment. They never override correctness, safety, verified state or evidence quality.
- **Evidence.** Report only what actually ran: EXECUTED, MEASURED, NOT RUN or BLOCKED, with the reason. Emulator, Wine or qemu runs are not device runs. Never rewrite old evidence; add new.
- **Git.** Work on a branch from the default branch and land it by pull request; the owner reviews and merges. No force-push to the default branch, no history rewrites. No AI attribution in commits, PRs or comments; commits use the repository's git identity. PR titles say what changed in plain words.
- **Security.** Report vulnerabilities privately, never in a public issue. Never print or commit tokens.
<!-- /ecosystem-policy v1 -->
