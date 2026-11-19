#!/usr/bin/env sh
#
# Structural checks over the project's own files.
#
# Every assertion goes through one of the helpers below. A negative assertion
# must never be written as `! grep ...`: a command whose status is inverted with
# `!` is exempt from `set -e`, so such a line reports nothing when it fails and
# the script carries on to exit 0.
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
XAX="$ROOT/source/xax"
MODULE="$XAX/module.json"
HOST="$XAX/host.json"
PROMOTER="$XAX/promoter.json"
SYSTEMUI="$XAX/systemui.json"
ENTRY="$XAX/entry.json"
BATCHES="$XAX/transactions"
STORE="$XAX/syncthingliveupdate.xax"
BRANDING="$ROOT/source/branding/AndroidManifest.xml"
PINS="$ROOT/toolchains.json"
WORKFLOW="$ROOT/.github/workflows/android.yml"

failures=0

fail() {
  echo "check-project: $1" >&2
  failures=$((failures + 1))
}

assert_file() {
  if [ ! -f "$1" ]; then
    fail "missing file $1"
  fi
}

assert_present() {
  if ! grep -q -- "$1" "$2"; then
    fail "expected pattern '$1' in $2"
  fi
}

assert_absent() {
  if grep -q -- "$1" "$2"; then
    fail "unexpected pattern '$1' in $2"
  fi
}

assert_absent_in_tree() {
  if grep -rq -- "$1" "$2"; then
    fail "unexpected pattern '$1' under $2"
  fi
}

for file in "$MODULE" "$HOST" "$PROMOTER" "$SYSTEMUI" "$ENTRY" "$STORE" "$BRANDING" "$PINS" \
  "$WORKFLOW" "$ROOT/CHANGELOG.md" "$ROOT/scripts/check-host-contract.sh" \
  "$BATCHES/01-circle.batch" "$BATCHES/02-cookie.batch" "$BATCHES/03-badge.batch" \
  "$BATCHES/04-interp.batch" "$BATCHES/05-duration.batch" "$BATCHES/06-wave.batch" \
  "$ROOT/source/inputs/branding.apk" "$ROOT/source/inputs/dexkit.dex" "$ROOT/source/inputs/libdexkit.so" \
  "$ROOT/source/branding/res/drawable/ic_endpoint_this_device.xml" \
  "$ROOT/source/branding/res/drawable/ic_endpoint_remote_device.xml"; do
  assert_file "$file"
done

# Application code is XAX only.
if find "$ROOT" -path "$ROOT/.git" -prune -o \( -name '*.java' -o -name '*.kt' -o -name '*.bf' \) -print | grep -q .; then
  fail "Java, Kotlin or Brainfuck source found; application code is XAX"
fi

# The committed store is the one the pins name.
expected=$(grep -A2 '"store"' "$PINS" | sed -n 's/.*"sha256": "\([0-9a-f]*\)".*/\1/p')
actual=$(sha256sum "$STORE" | cut -d' ' -f1)
if [ "$expected" != "$actual" ]; then
  fail "store sha256 $actual does not match toolchains.json ($expected)"
fi

# Module identity: same package and entry class as the Java releases, so an update installs in place.
assert_present '"package": "io.github.mrxsin.syncthingliveupdate"' "$MODULE"
assert_present '"module_class": "my.MrxSiN.syncthingliveupdate.ModuleMain"' "$MODULE"
assert_present '"com.github.catfriend1.syncthingfork"' "$MODULE"
assert_present '"com.android.systemui"' "$MODULE"
assert_present '"system"' "$MODULE"
assert_present '"min_sdk": 36' "$MODULE"
assert_present 'package="io.github.mrxsin.syncthingliveupdate"' "$BRANDING"
# DexKit's library is loaded by absolute path, which needs it unpacked at install.
assert_present 'android:extractNativeLibs="true"' "$BRANDING"
assert_absent 'xposedmodule\|xposedminversion\|xposedscope' "$BRANDING"

# Promotion patch: one private method, the module's own channel, the host package.
assert_present 'com.android.server.notification.NotificationManagerService' "$MODULE"
assert_present 'fixNotificationWithChannel' "$MODULE"
assert_present '05_syncthing_live_update' "$MODULE"
assert_present '262144' "$MODULE"
# Android 16 asks through the colorized characteristic; Android 17 asks explicitly.
assert_present 'setColorized' "$PROMOTER"
assert_present 'setRequestPromotedOngoing' "$PROMOTER"
assert_present 'SystemUI polish left off on ' "$ENTRY"

# Hook installation is the single fail-open boundary.
assert_present 'jvm-invokeinterface-catch' "$MODULE"
assert_present 'hook(Ljava/lang/reflect/Executable;)' "$MODULE"
assert_present 'Hook installation failed: ' "$MODULE"

# DexKit reads the host APK from disk; its in-memory mode crash-looped SystemUI on Android 17 QPR2.
assert_present 'create(Ljava/lang/String;)Lorg/luckypray/dexkit/DexKitBridge;' "$HOST"
assert_absent_in_tree 'create(Ljava/lang/ClassLoader;Z)' "$XAX"

# The host's own notification is rewritten; no second notification is posted.
assert_present 'startForeground' "$PROMOTER"
assert_present 'recoverBuilder' "$PROMOTER"
assert_present '01_syncthing_persistent' "$PROMOTER"
assert_present 'setShortCriticalText' "$PROMOTER"
assert_present 'setProgressTrackerIcon' "$PROMOTER"
assert_absent_in_tree 'NotificationManager", "notify(' "$XAX"
assert_absent_in_tree 'setCustomContentView\|setCustomBigContentView\|DecoratedCustomViewStyle' "$XAX"
assert_present 'sync-preparing' "$HOST"
assert_present 'always_run_in_background' "$HOST"
assert_present 'sys.system_server.start_elapsed' "$PROMOTER"

# SystemUI polish stays limited to the host's own notification and has a ceiling.
# Batch member names are hex inside their object lines: areAnimatorsEnabled, postInvalidateDelayed.
assert_present 'com.github.catfriend1.syncthingfork' "$SYSTEMUI"
assert_present '|com.github.catfriend1.syncthingfork|' "$SYSTEMUI"
assert_present '617265416e696d61746f7273456e61626c6564' "$BATCHES/06-wave.batch"
assert_present '706f7374496e76616c696461746544656c61796564' "$BATCHES/06-wave.batch"
assert_present 'Wavy progress disabled after ' "$SYSTEMUI"

# Releases: notes come from the changelog section for the branded versionName.
version=$(sed -n 's/.*android:versionName="\([^"]*\)".*/\1/p' "$BRANDING")
assert_present "^## $version " "$ROOT/CHANGELOG.md"
assert_present 'body_path: release-notes.md' "$WORKFLOW"
assert_present 'contents: read' "$WORKFLOW"
assert_present 'contents: write' "$WORKFLOW"
assert_absent 'r0adkll/sign-android-release' "$WORKFLOW"

if [ "$failures" -ne 0 ]; then
  echo "check-project: $failures check(s) failed" >&2
  exit 1
fi

echo "check-project: all checks passed"
