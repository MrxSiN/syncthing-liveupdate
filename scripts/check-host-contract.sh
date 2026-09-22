#!/usr/bin/env sh
#
# Checks that the Syncthing-Fork release the module claims to support still
# contains the classes, methods and fields the module hooks.
#
# This is the test that actually protects users: the module is a set of hooks
# into someone else's app, so the update most likely to break it is an upstream
# release that renames or removes one of those members. Compiling the module
# cannot notice that, and neither can a unit test, because the host is not on the
# module's classpath at all.
#
# What is checked: every name below is looked up in the host's dex string pools,
# class names as their dex type descriptors. A name that is gone is a definite
# break. A name that is present is strong but not conclusive evidence that the
# member is unchanged, because the string pool does not say which class a method
# name belongs to; pairing that check with the module's own DexKit resolution on
# a device is what closes the gap.
#
# Usage: scripts/check-host-contract.sh [host version] [release tag]
#
# The tag is only needed when it differs from "v<version>", which is the case for
# a release candidate: v2.1.6.0-rc.1 carries an APK named for 2.1.6.0.
set -eu

HOST_VERSION="${1:-2.1.5.0}"
HOST_TAG="${2:-v${HOST_VERSION}}"
HOST_REPO="researchxxl/syncthing-android"
APK="com.github.catfriend1.syncthingfork_release_v${HOST_VERSION}.apk"
URL="https://github.com/${HOST_REPO}/releases/download/${HOST_TAG}/${APK}"

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

failures=0

fail() {
  echo "check-host-contract: $1" >&2
  failures=$((failures + 1))
}

# A class the module reaches into, given as a Java name.
assert_class() {
  descriptor="L$(echo "$1" | tr '.' '/');"
  if ! grep -qa -- "$descriptor" "$WORK"/classes*.dex; then
    fail "class $1 is not in Syncthing-Fork $HOST_VERSION"
  fi
}

# A method or field name the module looks up by name.
assert_member() {
  if ! grep -qa -- "$1" "$WORK"/classes*.dex; then
    fail "member $1 is not in Syncthing-Fork $HOST_VERSION"
  fi
}

echo "check-host-contract: fetching Syncthing-Fork $HOST_VERSION ($HOST_TAG)"
if ! curl -fsSL -o "$WORK/host.apk" "$URL"; then
  echo "check-host-contract: could not download $URL" >&2
  exit 1
fi
unzip -qo "$WORK/host.apk" 'classes*.dex' -d "$WORK"

# The one method the module cannot work without: the host publishes every sync
# figure through it, and SyncProgressTracker hooks nothing else.
assert_class com.nutomic.syncthingandroid.service.NotificationHandler
assert_class com.nutomic.syncthingandroid.service.SyncthingService
assert_member updatePersistentNotification

# The channel the host's persistent notification is posted on, which is both what
# LiveUpdatePromoter recognises and the string DexKit falls back to.
assert_member 01_syncthing_persistent

# The completion models the transfer direction is read from.
assert_class com.nutomic.syncthingandroid.model.LocalCompletion
assert_class com.nutomic.syncthingandroid.model.RemoteCompletion
assert_member getTotalFolderCompletion
assert_member getTotalDeviceCompletion

# The folder status updates the transferring set is kept from.
assert_class com.nutomic.syncthingandroid.model.FolderStatus
assert_member updateFromConfig
assert_member setFolderStatus

# The folder states that mean data is moving.
assert_member sync-preparing

# The service state that means the host is running.
assert_member getCurrentState

if [ "$failures" -ne 0 ]; then
  echo "check-host-contract: $failures member(s) missing from Syncthing-Fork $HOST_VERSION" >&2
  exit 1
fi

echo "check-host-contract: Syncthing-Fork $HOST_VERSION still carries every hooked member"
