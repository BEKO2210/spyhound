#!/usr/bin/env bash
# Shows which settings screen each guide button of Spyhound opens on a connected device, without tapping anything:
# every intent is only resolved by the package manager, never started.
#
#   scripts/check-settings-intents.sh [adb-serial]
set -euo pipefail
adb=(adb ${1:+-s "$1"})
pkg=de.belkis.spyhound

echo "# $("${adb[@]}" shell getprop ro.product.model) / Android $("${adb[@]}" shell getprop ro.build.version.release) / $("${adb[@]}" shell getprop ro.build.display.id)"
resolve() {
  local out
  out=$("${adb[@]}" shell cmd package resolve-activity --brief "$@" 2>/dev/null | tail -n1 | tr -d '\r')
  case "$out" in */*) echo "$out" ;; *) echo "NOT RESOLVED" ;; esac
}
for a in DEVICE_INFO_SETTINGS APPLICATION_DEVELOPMENT_SETTINGS SECURITY_SETTINGS ACCESSIBILITY_SETTINGS \
         MANAGE_UNKNOWN_APP_SOURCES SETTINGS; do
  printf '%-36s %s\n' "$a" "$(resolve -a "android.settings.$a")"
done
printf '%-36s %s\n' APPLICATION_DETAILS_SETTINGS "$(resolve -a android.settings.APPLICATION_DETAILS_SETTINGS -d "package:$pkg")"
printf '%-36s %s\n' MANAGE_APP_ALL_FILES_ACCESS_PERMISSION "$(resolve -a android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION -d "package:$pkg")"
