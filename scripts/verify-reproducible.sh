#!/usr/bin/env bash
# Rebuilds a release from its git tag and compares it with the published APK.
#
#   scripts/verify-reproducible.sh v0.3.5 spyhound-0.3.5.apk
#
# Signatures cannot match without the release key, so the comparison is made on the unpacked contents: every dex
# file, resource, native library and asset must be byte-identical. Needs JDK 21, the Android SDK (ANDROID_HOME or
# sdk.dir in local.properties of this checkout) and unzip.
set -euo pipefail

tag=${1:?usage: $0 <tag> <published.apk>}
published=$(realpath "${2:?usage: $0 <tag> <published.apk>}")
repo=$(cd "$(dirname "$0")/.." && pwd)
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

git clone -q -c advice.detachedHead=false --branch "$tag" "$repo" "$work/src"
[ -f "$repo/local.properties" ] && cp "$repo/local.properties" "$work/src/"
(cd "$work/src" && ./gradlew -q --max-workers=2 :app:assembleRelease < /dev/null)
built=$(ls "$work"/src/app/build/outputs/apk/release/*.apk | head -n1)

mkdir "$work/published" "$work/built"
unzip -q "$published" -d "$work/published"
unzip -q "$built" -d "$work/built"
# v1 (JAR) signature files are the only entries allowed to differ; v2+ signatures live outside the zip entries.
strip_sig() { find "$1/META-INF" -maxdepth 1 \( -name '*.SF' -o -name '*.RSA' -o -name '*.EC' -o -name 'MANIFEST.MF' \) -delete 2>/dev/null || true; }
strip_sig "$work/published"
strip_sig "$work/built"

if diff -r "$work/published" "$work/built" > "$work/diff.txt"; then
  echo "REPRODUCIBLE: $(find "$work/built" -type f | wc -l) files identical ($tag)"
else
  echo "DIFFERENT ($tag):"
  cat "$work/diff.txt"
  exit 1
fi
