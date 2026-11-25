#!/bin/sh
# Lightweight project launcher (not the official Gradle Wrapper JAR).
# Uses an installed Gradle ONLY if it is exactly the pinned version; otherwise
# downloads and checksum-verifies the pinned Gradle distribution.
set -eu

GRADLE_VERSION="8.9"
APP_HOME=$(cd "$(dirname "$0")" && pwd)

if command -v gradle >/dev/null 2>&1; then
  if gradle --version 2>/dev/null | grep -q "^Gradle ${GRADLE_VERSION}\$"; then
    exec gradle -p "$APP_HOME" "$@"
  fi
fi

GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
DIST_ROOT="$GRADLE_USER_HOME/wrapper/dists/gradle-${GRADLE_VERSION}-bin/turkan-bootstrap"
GRADLE_BIN="$DIST_ROOT/gradle-${GRADLE_VERSION}/bin/gradle"
URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

fetch() {
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL --retry 2 "$1" -o "$2"
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "$2" "$1"
  else
    echo "ERROR: curl or wget is required to download Gradle." >&2
    return 1
  fi
}

sha256_of() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | cut -d' ' -f1
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | cut -d' ' -f1
  else
    echo ""
  fi
}

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$DIST_ROOT"
  ZIP="$DIST_ROOT/gradle-${GRADLE_VERSION}-bin.zip"
  echo "Downloading Gradle ${GRADLE_VERSION}..." >&2
  fetch "$URL" "$ZIP"
  fetch "${URL}.sha256" "$ZIP.sha256"
  expected=$(tr -d ' \r\n' < "$ZIP.sha256" | cut -c1-64)
  actual=$(sha256_of "$ZIP")
  if [ -z "$actual" ]; then
    echo "WARNING: no sha256 tool found; skipping checksum verification." >&2
  elif [ "$expected" != "$actual" ]; then
    rm -f "$ZIP" "$ZIP.sha256"
    echo "ERROR: Gradle download checksum mismatch." >&2
    exit 1
  fi
  unzip -q -o "$ZIP" -d "$DIST_ROOT"
  rm -f "$ZIP" "$ZIP.sha256"
fi

exec "$GRADLE_BIN" -p "$APP_HOME" "$@"
