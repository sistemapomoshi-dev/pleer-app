#!/usr/bin/env sh
DIR="$(cd "$(dirname "$0")" && pwd)"
GRADLE_VERSION="8.10.2"
GRADLE_HOME="$DIR/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin/gradle-${GRADLE_VERSION}"
GRADLE_BIN="$GRADLE_HOME/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$DIR/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin"
  ZIP="$DIR/.gradle/gradle-${GRADLE_VERSION}-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -L "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$ZIP"
  else
    wget "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -O "$ZIP"
  fi
  unzip -o "$ZIP" -d "$DIR/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin"
fi

exec "$GRADLE_BIN" "$@"
