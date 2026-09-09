#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
APP_GRADLE="$ROOT/app/build.gradle.kts"
WORKFLOW="$ROOT/.github/workflows/android.yml"
MANIFEST="$ROOT/app/src/main/AndroidManifest.xml"
SOURCE="$ROOT/app/src/main/java/my/MrxSiN/syncthingliveupdate"
XPOSED_META="$ROOT/app/src/main/resources/META-INF/xposed"

for file in "$APP_GRADLE" "$WORKFLOW" "$MANIFEST" "$ROOT/CHANGELOG.md" \
  "$SOURCE/ModuleMain.java" \
  "$SOURCE/ModuleRuntime.java" \
  "$SOURCE/Reflect.java" \
  "$SOURCE/HostApp.java" \
  "$SOURCE/SyncSnapshot.java" \
  "$SOURCE/SyncProgressSource.java" \
  "$SOURCE/SyncProgressTracker.java" \
  "$SOURCE/PromotionPolicyPatch.java" \
  "$SOURCE/LiveUpdateChannel.java" \
  "$SOURCE/LiveUpdatePromoter.java" \
  "$SOURCE/SyncDirection.java" \
  "$SOURCE/DirectionBadgeIcon.java" \
  "$SOURCE/SyncingFolders.java" \
  "$XPOSED_META/java_init.list" \
  "$XPOSED_META/module.prop" \
  "$XPOSED_META/scope.list"; do
  test -f "$file"
done

# Build configuration.
grep -q 'val appVersion = "1.0.0"' "$APP_GRADLE"
grep -q 'versionCode = 2' "$APP_GRADLE"
grep -q 'compileOnly("io.github.libxposed:api:102.0.0")' "$APP_GRADLE"
grep -q 'merges += "META-INF/xposed/\*"' "$APP_GRADLE"
# Live Updates are an Android 16 (API 36) feature, so the module cannot run lower.
grep -q 'minSdk = 36' "$APP_GRADLE"
grep -q 'r0adkll/sign-android-release@v1' "$WORKFLOW"
# The release description is the changelog section for the tagged version.
grep -q 'body_path: release-notes.md' "$WORKFLOW"
grep -q "^## $(sed -n 's/^val appVersion = "\([^"]*\)"/\1/p' "$APP_GRADLE") " "$ROOT/CHANGELOG.md"
! grep -q 'signingConfigs' "$APP_GRADLE"

# Modern Xposed API module declaration; no legacy entry point.
grep -q '^my.MrxSiN.syncthingliveupdate.ModuleMain$' "$XPOSED_META/java_init.list"
grep -q '^com.github.catfriend1.syncthingfork$' "$XPOSED_META/scope.list"
grep -q '^system$' "$XPOSED_META/scope.list"
grep -q '^targetApiVersion=102$' "$XPOSED_META/module.prop"
! test -f "$ROOT/app/src/main/assets/xposed_init"
! grep -q 'xposedmodule\|xposedminversion\|xposedscope' "$MANIFEST"
! grep -rq 'de.robv.android.xposed' "$SOURCE"

# Host contract the hooks depend on.
grep -q 'com.github.catfriend1.syncthingfork' "$SOURCE/HostApp.java"
grep -q 'com.nutomic.syncthingandroid.service.NotificationHandler' "$SOURCE/HostApp.java"
grep -q 'updatePersistentNotification' "$SOURCE/HostApp.java"
grep -q '01_syncthing_persistent' "$SOURCE/HostApp.java"

# Promotion patch stays limited to the module's own channel in the host package.
grep -q 'com.android.server.notification.NotificationManagerService' "$SOURCE/PromotionPolicyPatch.java"
grep -q 'fixNotificationWithChannel' "$SOURCE/PromotionPolicyPatch.java"
grep -q 'FLAG_PROMOTED_ONGOING = 0x00040000' "$SOURCE/PromotionPolicyPatch.java"
grep -q 'LiveUpdateChannel.ID.equals(channel.getId())' "$SOURCE/PromotionPolicyPatch.java"

# The host's own notification is rewritten; no second notification is posted.
grep -q 'startForeground' "$SOURCE/LiveUpdatePromoter.java"
grep -q 'recoverBuilder' "$SOURCE/LiveUpdatePromoter.java"
grep -q 'setChannelId(LiveUpdateChannel.ID)' "$SOURCE/LiveUpdatePromoter.java"
grep -q 'HostApp.PERSISTENT_CHANNEL.equals(original.getChannelId())' "$SOURCE/LiveUpdatePromoter.java"
! grep -rq 'NotificationManager.notify\|notificationManager.notify' "$SOURCE"

# Promotable characteristics required by the platform.
# Android 17 refuses to promote a colorized notification and asks for the promotion
# explicitly instead, so the request replaces the colorized flag Android 16 used.
! grep -q 'setColorized' "$SOURCE/LiveUpdatePromoter.java"
grep -q 'setRequestPromotedOngoing' "$SOURCE/LiveUpdatePromoter.java"
grep -q 'DirectionBadgeIcon.badged' "$SOURCE/LiveUpdatePromoter.java"
grep -q 'setContentText' "$SOURCE/LiveUpdatePromoter.java"
grep -q 'sync-preparing' "$SOURCE/SyncingFolders.java"
grep -q 'updateFromConfig' "$SOURCE/HostApp.java"
grep -q 'getTotalFolderCompletion' "$SOURCE/HostApp.java"
grep -q 'getTotalDeviceCompletion' "$SOURCE/HostApp.java"
grep -q 'setOngoing(true)' "$SOURCE/LiveUpdatePromoter.java"
grep -q 'setShortCriticalText' "$SOURCE/LiveUpdatePromoter.java"
grep -q 'Notification.ProgressStyle' "$SOURCE/LiveUpdatePromoter.java"
grep -q 'IMPORTANCE_LOW' "$SOURCE/LiveUpdateChannel.java"
