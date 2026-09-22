#!/usr/bin/env sh
#
# Structural checks over the project's own files.
#
# Every assertion goes through one of the helpers below. A negative assertion
# must never be written as `! grep ...`: a command whose status is inverted with
# `!` is exempt from `set -e`, so such a line reports nothing when it fails and
# the script carries on to exit 0. That is not a style preference; it silently
# hid a contradiction between this script and app/build.gradle.kts for several
# releases.
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
APP_GRADLE="$ROOT/app/build.gradle.kts"
WORKFLOW="$ROOT/.github/workflows/android.yml"
MANIFEST="$ROOT/app/src/main/AndroidManifest.xml"
SOURCE="$ROOT/app/src/main/java/my/MrxSiN/syncthingliveupdate"
TESTS="$ROOT/app/src/test/java/my/MrxSiN/syncthingliveupdate"
XPOSED_META="$ROOT/app/src/main/resources/META-INF/xposed"

failures=0

fail() {
  echo "check-project: $1" >&2
  failures=$((failures + 1))
}

# A file that has to exist.
assert_file() {
  if [ ! -f "$1" ]; then
    fail "missing file $1"
  fi
}

# A pattern that has to appear in a file.
assert_present() {
  if ! grep -q "$1" "$2"; then
    fail "expected pattern '$1' in $2"
  fi
}

# A pattern that must not appear in a file.
assert_absent() {
  if grep -q "$1" "$2"; then
    fail "unexpected pattern '$1' in $2"
  fi
}

# A pattern that must not appear anywhere under a directory.
assert_absent_in_tree() {
  if grep -rq "$1" "$2"; then
    fail "unexpected pattern '$1' under $2"
  fi
}

for file in "$APP_GRADLE" "$WORKFLOW" "$MANIFEST" "$ROOT/CHANGELOG.md" \
  "$ROOT/scripts/check-host-contract.sh" \
  "$SOURCE/ModuleMain.java" \
  "$SOURCE/ModuleRuntime.java" \
  "$SOURCE/Reflect.java" \
  "$SOURCE/ReflectionDiagnostics.java" \
  "$SOURCE/HostApp.java" \
  "$SOURCE/SyncSnapshot.java" \
  "$SOURCE/SyncProgressSource.java" \
  "$SOURCE/SyncProgressTracker.java" \
  "$SOURCE/PlatformContract.java" \
  "$SOURCE/PlatformContracts.java" \
  "$SOURCE/Android36Contract.java" \
  "$SOURCE/Android37Contract.java" \
  "$SOURCE/UnknownContract.java" \
  "$SOURCE/MemberQuery.java" \
  "$SOURCE/MemberResolver.java" \
  "$SOURCE/ReflectionResolver.java" \
  "$SOURCE/DexKitResolver.java" \
  "$SOURCE/DexKitLibrary.java" \
  "$SOURCE/PromotionStatus.java" \
  "$SOURCE/Resolvers.java" \
  "$SOURCE/PromotionPolicyPatch.java" \
  "$SOURCE/LiveUpdateChannel.java" \
  "$SOURCE/LiveUpdatePromoter.java" \
  "$SOURCE/SyncDirection.java" \
  "$SOURCE/DirectionBadgeIcon.java" \
  "$SOURCE/DirectionGlyph.java" \
  "$SOURCE/LiveUpdateAppearance.java" \
  "$SOURCE/ExpressiveAppearance.java" \
  "$SOURCE/FolderLine.java" \
  "$SOURCE/ProgressIcons.java" \
  "$SOURCE/ShapedIcon.java" \
  "$SOURCE/IconShape.java" \
  "$SOURCE/SyncthingPalette.java" \
  "$SOURCE/TransferEndpoint.java" \
  "$SOURCE/TransferRoute.java" \
  "$SOURCE/ModuleDrawables.java" \
  "$SOURCE/SystemUi.java" \
  "$SOURCE/ProgressBarMotion.java" \
  "$SOURCE/SpringInterpolator.java" \
  "$SOURCE/StatusBarChipColors.java" \
  "$SOURCE/WavyProgressTrack.java" \
  "$SOURCE/CookieShape.java" \
  "$SOURCE/SyncingFolders.java" \
  "$SOURCE/FolderStates.java" \
  "$TESTS/SyncSnapshotTest.java" \
  "$TESTS/SyncDirectionTest.java" \
  "$TESTS/SpringInterpolatorTest.java" \
  "$TESTS/FolderStatesTest.java" \
  "$TESTS/FolderLineTest.java" \
  "$TESTS/PlatformContractsTest.java" \
  "$TESTS/HostContractTest.java" \
  "$TESTS/ReflectionResolverTest.java" \
  "$TESTS/PromotionStatusTest.java" \
  "$XPOSED_META/java_init.list" \
  "$XPOSED_META/module.prop" \
  "$XPOSED_META/scope.list"; do
  assert_file "$file"
done

# Build configuration.
assert_present 'val appVersion = "1.1.1"' "$APP_GRADLE"
assert_present 'versionCode = 4' "$APP_GRADLE"
assert_present 'compileOnly("io.github.libxposed:api:102.0.0")' "$APP_GRADLE"
assert_present 'org.luckypray:dexkit' "$APP_GRADLE"
# DexKit's library is loaded by absolute path, which needs it unpacked at install.
assert_present 'useLegacyPackaging = true' "$APP_GRADLE"
assert_present 'System.load(' "$SOURCE/DexKitLibrary.java"
assert_present 'merges += "META-INF/xposed/\*"' "$APP_GRADLE"
# Live Updates are an Android 16 (API 36) feature, so the module cannot run lower.
assert_present 'minSdk = 36' "$APP_GRADLE"
# The release description is the changelog section for the tagged version.
assert_present 'body_path: release-notes.md' "$WORKFLOW"
assert_present "^## $(sed -n 's/^val appVersion = "\([^"]*\)"/\1/p' "$APP_GRADLE") " "$ROOT/CHANGELOG.md"

# Tests run in CI, which is the point of having them.
assert_present 'testImplementation' "$APP_GRADLE"
assert_present 'gradlew test' "$WORKFLOW"

# One release signing path: Gradle signs from the environment, and no third-party
# Action is handed the keystore. Credentials never reach version control.
assert_present 'signingConfigs.create("release")' "$APP_GRADLE"
assert_present 'ANDROID_KEYSTORE_PATH' "$APP_GRADLE"
assert_absent 'r0adkll/sign-android-release' "$WORKFLOW"

# Only the release job may write to the repository.
assert_present 'contents: read' "$WORKFLOW"
assert_present 'contents: write' "$WORKFLOW"

# Modern Xposed API module declaration; no legacy entry point.
assert_present '^my.MrxSiN.syncthingliveupdate.ModuleMain$' "$XPOSED_META/java_init.list"
assert_present '^com.github.catfriend1.syncthingfork$' "$XPOSED_META/scope.list"
assert_present '^system$' "$XPOSED_META/scope.list"
assert_present '^com.android.systemui$' "$XPOSED_META/scope.list"
assert_present '^targetApiVersion=102$' "$XPOSED_META/module.prop"
# Vector reloads an updated module into a running system_server when asked to.
assert_present '^autoHotReload=true$' "$XPOSED_META/module.prop"
if [ -f "$ROOT/app/src/main/assets/xposed_init" ]; then
  fail "unexpected legacy entry point app/src/main/assets/xposed_init"
fi
assert_absent 'xposedmodule\|xposedminversion\|xposedscope' "$MANIFEST"
assert_absent_in_tree 'de.robv.android.xposed' "$SOURCE"

# Host contract the hooks depend on.
assert_present 'com.github.catfriend1.syncthingfork' "$SOURCE/HostApp.java"
assert_present 'com.nutomic.syncthingandroid.service.NotificationHandler' "$SOURCE/HostApp.java"
assert_present 'updatePersistentNotification' "$SOURCE/HostApp.java"
assert_present '01_syncthing_persistent' "$SOURCE/HostApp.java"
assert_present 'PERSISTENT_NOTIFICATION_QUERY' "$SOURCE/HostApp.java"
assert_present 'FOLDER_STATUS_QUERY' "$SOURCE/HostApp.java"

# Version assumptions live in the contracts, not spread through the module.
assert_present 'fixNotificationWithChannel' "$SOURCE/Android36Contract.java"
assert_present 'fixNotificationWithChannel' "$SOURCE/Android37Contract.java"
assert_present '0x00040000' "$SOURCE/Android36Contract.java"
assert_present '0x00040000' "$SOURCE/Android37Contract.java"
# Android 16 asks through the colorized characteristic; Android 17 asks explicitly.
assert_present 'setColorized' "$SOURCE/Android36Contract.java"
assert_present 'setRequestPromotedOngoing' "$SOURCE/Android37Contract.java"
assert_absent 'setColorized' "$SOURCE/LiveUpdatePromoter.java"
assert_absent 'setRequestPromotedOngoing' "$SOURCE/LiveUpdatePromoter.java"
assert_present 'return false' "$SOURCE/UnknownContract.java"
assert_present 'contract.systemUiPolishSupported()' "$SOURCE/ModuleMain.java"

# A module cannot be injected into a running system_server, so the host says when
# what is running there predates this APK.
assert_present 'status.report()' "$SOURCE/LiveUpdatePromoter.java"
assert_present 'sys.system_server.start_elapsed' "$SOURCE/PromotionStatus.java"
assert_present 'elapsedRealtime' "$SOURCE/PromotionStatus.java"

# Promotion patch stays limited to the module's own channel in the host package.
assert_present 'com.android.server.notification.NotificationManagerService' \
  "$SOURCE/PromotionPolicyPatch.java"
assert_present 'contract.promotionMethod()' "$SOURCE/PromotionPolicyPatch.java"
assert_present 'LiveUpdateChannel.ID.equals(channel.getId())' "$SOURCE/PromotionPolicyPatch.java"

# Hook installation is the module's single fail-open boundary.
assert_present 'catch (RuntimeException | LinkageError' "$SOURCE/ModuleRuntime.java"
assert_present 'Hook installation failed' "$SOURCE/ModuleRuntime.java"

# The host's own notification is rewritten; no second notification is posted.
assert_present 'startForeground' "$SOURCE/LiveUpdatePromoter.java"
assert_present 'recoverBuilder' "$SOURCE/LiveUpdatePromoter.java"
assert_present 'setChannelId(LiveUpdateChannel.ID)' "$SOURCE/LiveUpdatePromoter.java"
assert_present 'HostApp.PERSISTENT_CHANNEL.equals(original.getChannelId())' \
  "$SOURCE/LiveUpdatePromoter.java"
assert_absent_in_tree 'NotificationManager.notify\|notificationManager.notify' "$SOURCE"

# Promotable characteristics required by the platform.
assert_present 'sync-preparing' "$SOURCE/FolderStates.java"
assert_present 'updateFromConfig' "$SOURCE/HostApp.java"
assert_present 'getTotalFolderCompletion' "$SOURCE/HostApp.java"
assert_present 'getTotalDeviceCompletion' "$SOURCE/HostApp.java"
assert_present 'setOngoing(true)' "$SOURCE/LiveUpdatePromoter.java"
assert_present 'setShortCriticalText' "$SOURCE/LiveUpdatePromoter.java"
assert_present 'appearance.apply' "$SOURCE/LiveUpdatePromoter.java"
assert_present 'IMPORTANCE_LOW' "$SOURCE/LiveUpdateChannel.java"

# Material 3 Expressive appearance, expressed through ProgressStyle only: a promoted
# notification may not carry custom views.
assert_present 'new LiveUpdatePromoter(tracker, new ExpressiveAppearance(), contract)' \
  "$SOURCE/ModuleMain.java"
assert_present 'Notification.ProgressStyle' "$SOURCE/ExpressiveAppearance.java"
assert_present 'setProgressTrackerIcon' "$SOURCE/ExpressiveAppearance.java"
assert_present 'setProgressStartIcon' "$SOURCE/ExpressiveAppearance.java"
assert_present 'setProgressEndIcon' "$SOURCE/ExpressiveAppearance.java"
assert_present 'getModuleApplicationInfo' "$SOURCE/ModuleRuntime.java"
assert_file "$ROOT/app/src/main/res/drawable/ic_endpoint_this_device.xml"
assert_file "$ROOT/app/src/main/res/drawable/ic_endpoint_remote_device.xml"
assert_present 'DirectionBadgeIcon.badged' "$SOURCE/ExpressiveAppearance.java"
assert_present 'setContentText' "$SOURCE/ExpressiveAppearance.java"
assert_absent_in_tree \
  'setCustomContentView\|setCustomBigContentView\|DecoratedCustomViewStyle' "$SOURCE"

# SystemUI polish stays limited to the host's own notification.
assert_present 'NotificationProgressDrawable' "$SOURCE/SystemUi.java"
assert_present 'setProgressModel' "$SOURCE/SystemUi.java"
assert_present 'OngoingActivityChipModel$Active' "$SOURCE/SystemUi.java"
assert_present 'HostApp.PACKAGE.equals(bar.getContext().getPackageName())' \
  "$SOURCE/WavyProgressTrack.java"
assert_present 'HostApp.PACKAGE.equals(bar.getContext().getPackageName())' \
  "$SOURCE/ProgressBarMotion.java"
assert_present 'HOST_KEY_PART' "$SOURCE/StatusBarChipColors.java"
assert_present 'areAnimatorsEnabled' "$SOURCE/WavyProgressTrack.java"

# The SystemUI hot path has a ceiling and a way to give up.
assert_present 'FRAME_INTERVAL_MS' "$SOURCE/WavyProgressTrack.java"
assert_present 'FAILURE_BUDGET' "$SOURCE/WavyProgressTrack.java"
assert_present 'scheduleSelf' "$SOURCE/WavyProgressTrack.java"

# Each hooked chip constructor carries its own argument index.
assert_present 'intercept(chain, index)' "$SOURCE/StatusBarChipColors.java"
assert_absent 'colorsArgument' "$SOURCE/StatusBarChipColors.java"

if [ "$failures" -ne 0 ]; then
  echo "check-project: $failures check(s) failed" >&2
  exit 1
fi

echo "check-project: all checks passed"
