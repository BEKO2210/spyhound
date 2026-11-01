#!/usr/bin/env bash
# Spyhound matrix emulator runs (one AVD at a time). Sourced paths from repo root.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
EMU="$SDK/emulator/emulator"
AVM="$SDK/cmdline-tools/latest/bin/avdmanager"
ADB="$SDK/platform-tools/adb"
SPY_APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
TEST_APK="$ROOT/testapp/build/outputs/apk/debug/testapp-debug.apk"
TEST_TSV="$ROOT/testapp/debug-test.tsv"
SHOT_DIR="$ROOT/docs/shots/matrix"
PKG=de.belkis.spyhound
unset ANDROID_SERIAL

adb_emu() {
  local serial
  serial=$($ADB devices | awk '/^emulator-/{print $1; exit}')
  if [[ -z "$serial" ]]; then
    echo "no emulator in adb devices" >&2
    return 1
  fi
  export ANDROID_SERIAL="$serial"
}

kill_emu() {
  unset ANDROID_SERIAL
  serial=$($ADB devices | awk '/^emulator-/{print $1; exit}')
  if [[ -n "$serial" ]]; then
    ANDROID_SERIAL="$serial" $ADB emu kill 2>/dev/null || true
  fi
  sleep 3
  if pgrep -f qemu-system-x86_64-headless >/dev/null; then
    pkill -9 -f qemu-system-x86_64-headless || true
    sleep 2
  fi
  find "$HOME/.android/avd" -name '*.lock' -delete 2>/dev/null || true
}

wait_boot() {
  for _ in $(seq 1 90); do
    serial=$($ADB devices | awk '/^emulator-/{print $1; exit}')
    [[ -n "$serial" ]] && break
    sleep 2
  done
  adb_emu
  $ADB wait-for-device
  for _ in $(seq 1 120); do
    boot=$($ADB shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
    [[ "$boot" == "1" ]] && break
    sleep 2
  done
  $ADB shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
  sleep 2
}

create_avd() {
  local name=$1 sysimg=$2 device=$3
  if $AVM list avd 2>/dev/null | grep -q "Name: $name"; then
    return 0
  fi
  echo "no" | $AVM create avd -n "$name" -k "$sysimg" -d "$device" --force
}

ui_dump() {
  $ADB shell uiautomator dump /data/local/tmp/uidump.xml >/dev/null 2>&1
  $ADB exec-out cat /data/local/tmp/uidump.xml 2>/dev/null
}

find_tap_bounds() {
  local label=$1
  local xml
  xml=$(ui_dump)
  echo "$xml" | python3 - "$label" <<'PY'
import re, sys
label = sys.argv[1]
xml = sys.stdin.read().replace("\n", " ")
patterns = [
    rf'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*(?:text|content-desc)="{re.escape(label)}"',
    rf'(?:text|content-desc)="{re.escape(label)}"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',
]
for pat in patterns:
    m = re.search(pat, xml)
    if m:
        print(m.group(1), m.group(2), m.group(3), m.group(4))
        sys.exit(0)
for m in re.finditer(rf'(?:text|content-desc)="{re.escape(label)}"', xml):
    chunk = xml[m.start():m.start() + 1200]
    b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', chunk)
    if b:
        print(b.group(1), b.group(2), b.group(3), b.group(4))
        sys.exit(0)
PY
}

click_desc() {
  local desc=$1
  local bounds
  bounds=$(find_tap_bounds "$desc")
  if [[ -z "$bounds" ]]; then
    echo "click_desc: not found: $desc" >&2
    return 1
  fi
  read -r x1 y1 x2 y2 <<<"$bounds"
  local x=$(( (x1 + x2) / 2 )) y=$(( (y1 + y2) / 2 ))
  $ADB shell input tap "$x" "$y"
}

click_text() {
  click_desc "$1"
}

screenshot() {
  local out=$1
  $ADB exec-out screencap -p >"$out"
}

skip_intro() {
  local prefs=/tmp/spyhound_intro.xml
  printf '%s\n' \
    '<?xml version="1.0" encoding="utf-8" standalone="yes" ?>' \
    '<map>' \
    '<boolean name="completed" value="true" />' \
    '</map>' >"$prefs"
  $ADB push "$prefs" /sdcard/spyhound_intro.xml >/dev/null
  $ADB shell "cat /sdcard/spyhound_intro.xml | run-as $PKG sh -c 'mkdir -p shared_prefs && cat > shared_prefs/spyhound_intro.xml'"
}

prepare_spyhound_data() {
  $ADB shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
  sleep 2
  $ADB shell am force-stop "$PKG"
  sleep 1
}

push_debug_tsv() {
  local filtered=/tmp/spyhound-debug-test.tsv
  grep -v '^#' "$TEST_TSV" | grep -v '^$' >"$filtered"
  $ADB push "$filtered" /sdcard/spyhound-debug-test.tsv >/dev/null
  $ADB shell "cat /sdcard/spyhound-debug-test.tsv | run-as $PKG sh -c 'mkdir -p files/iocs && cat > files/iocs/debug-test.tsv'"
}

install_apps() {
  $ADB install -r "$SPY_APK" >/dev/null
  prepare_spyhound_data
  push_debug_tsv
  skip_intro
  $ADB install -r "$TEST_APK" >/dev/null
}

wait_scan_done() {
  for _ in $(seq 1 180); do
    xml=$(ui_dump)
    if echo "$xml" | grep -qi 'testspy'; then
      return 0
    fi
    if echo "$xml" | grep -qE 'text="Scan again"|text="Neu prüfen"|content-desc="Scan again"'; then
      sleep 2
      return 0
    fi
    sleep 2
  done
  echo "scan timeout" >&2
  return 1
}

check_finding() {
  local xml
  xml=$(ui_dump)
  echo "$xml" | grep -qi 'testspy'
}

check_logcat_fatal() {
  local fatals
  fatals=$($ADB logcat -d -s AndroidRuntime:E 2>/dev/null | tail -20)
  if echo "$fatals" | grep -q FATAL; then
    echo "$fatals" >&2
    return 1
  fi
  return 0
}

tap_scan() {
  local i
  for i in $(seq 1 30); do
    set +e
    click_text "Scan now" 2>/dev/null && { set -e; return 0; }
    click_desc "Scan now" 2>/dev/null && { set -e; return 0; }
    click_text "Jetzt prüfen" 2>/dev/null && { set -e; return 0; }
    click_desc "Jetzt prüfen" 2>/dev/null && { set -e; return 0; }
    set -e
    sleep 2
  done
  $ADB shell input tap 540 520
}

run_phone_test() {
  local api=$1 nn=$2 sysimg=$3 avd=$4
  echo "=== API $api (api${nn}) ===" | tee -a /tmp/matrix-emulator-test.log
  kill_emu
  create_avd "$avd" "$sysimg" pixel_6
  $EMU -avd "$avd" -no-window -no-audio -no-snapshot -gpu swiftshader_indirect \
    -prop persist.sys.language=de -prop persist.sys.country=DE >/tmp/emu-${avd}.log 2>&1 &
  wait_boot
  install_apps
  $ADB shell am start -n "$PKG/.MainActivity" >/dev/null
  sleep 8
  screenshot "$SHOT_DIR/api${nn}-start.png"
  tap_scan
  sleep 2
  screenshot "$SHOT_DIR/api${nn}-scan.png"
  wait_scan_done
  screenshot "$SHOT_DIR/api${nn}-result.png"
  check_finding || { echo "FAIL: no Testspy finding on API $api" >&2; return 1; }
  check_logcat_fatal || { echo "FAIL: logcat fatal on API $api" >&2; return 1; }
  kill_emu
  echo "OK API $api"
}

run_tablet_test() {
  local avd=spyhound-matrix-tablet-36
  echo "=== Tablet API 36 ==="
  kill_emu
  create_avd "$avd" "system-images;android-36;google_apis;x86_64" pixel_tablet
  $EMU -avd "$avd" -no-window -no-audio -no-snapshot -gpu swiftshader_indirect \
    -prop persist.sys.language=de -prop persist.sys.country=DE >/tmp/emu-tablet.log 2>&1 &
  wait_boot
  install_apps
  skip_intro
  $ADB shell am start -n "$PKG/.MainActivity" >/dev/null
  sleep 3
  screenshot "$SHOT_DIR/api36-tablet-start.png"
  tap_scan
  wait_scan_done
  screenshot "$SHOT_DIR/api36-tablet-result.png"
  check_logcat_fatal
  kill_emu
  echo "OK Tablet"
}

run_root_probe() {
  local avd=spyhound-matrix-root-34
  echo "=== adb root probe (API 34) ==="
  kill_emu
  create_avd "$avd" "system-images;android-34;google_apis;x86_64" pixel_6
  $EMU -avd "$avd" -no-window -no-audio -no-snapshot -gpu swiftshader_indirect >/tmp/emu-root.log 2>&1 &
  wait_boot
  $ADB root >/dev/null 2>&1 || true
  sleep 2
  $ADB install -r "$SPY_APK" >/dev/null
  skip_intro
  $ADB shell am start -n "$PKG/.MainActivity" >/dev/null
  sleep 3
  click_text "Root anfragen" || click_text "Request root"
  sleep 4
  xml=$(ui_dump)
  echo "$xml" | grep -q 'nicht verfügbar\|unavailable' || {
    echo "FAIL: root denial message missing" >&2
    kill_emu
    return 1
  }
  check_logcat_fatal
  kill_emu
  echo "OK root probe"
}

run_release_test() {
  local rel="$ROOT/app/build/outputs/apk/release/app-release.apk"
  [[ -f "$rel" ]] || { echo "missing release apk" >&2; return 1; }
  local avd=spyhound-matrix-release-37
  echo "=== Release APK API 37 ==="
  kill_emu
  create_avd "$avd" "system-images;android-37.0;google_apis;x86_64" pixel_6
  $EMU -avd "$avd" -no-window -no-audio -no-snapshot -gpu swiftshader_indirect \
    -prop persist.sys.language=de -prop persist.sys.country=DE >/tmp/emu-release.log 2>&1 &
  wait_boot
  $ADB uninstall "$PKG" 2>/dev/null || true
  $ADB install -r "$rel" >/dev/null
  $ADB install -r "$TEST_APK" >/dev/null
  # Release must ignore debug-test.tsv even if present
  push_debug_tsv || true
  skip_intro
  $ADB shell am start -n "$PKG/.MainActivity" >/dev/null
  sleep 3
  tap_scan
  wait_scan_done
  if check_finding; then
    echo "FAIL: release showed Testspy finding" >&2
    kill_emu
    return 1
  fi
  check_logcat_fatal
  screenshot "$SHOT_DIR/api37-release-result.png"
  kill_emu
  echo "OK release"
}

main() {
  mkdir -p "$SHOT_DIR"
  [[ -f "$SPY_APK" && -f "$TEST_APK" ]] || { echo "Build APKs first" >&2; exit 1; }
  local only=${1:-all}
  if [[ "$only" == "29" || "$only" == "all" ]]; then
    run_phone_test 29 29 "system-images;android-29;google_apis;x86_64" spyhound-matrix-29 || true
  fi
  if [[ "$only" == "31" || "$only" == "all" ]]; then
    run_phone_test 31 31 "system-images;android-31;google_apis;x86_64" spyhound-matrix-31 || true
  fi
  if [[ "$only" == "34" || "$only" == "all" ]]; then
    run_phone_test 34 34 "system-images;android-34;google_apis;x86_64" spyhound-matrix-34 || true
  fi
  if [[ "$only" == "37" || "$only" == "all" ]]; then
    run_phone_test 37 37 "system-images;android-37.0;google_apis;x86_64" spyhound-matrix-37 || true
  fi
  if [[ "$only" == "tablet" || "$only" == "all" ]]; then
    run_tablet_test || true
  fi
  if [[ "$only" == "root" || "$only" == "all" ]]; then
    run_root_probe || true
  fi
  if [[ "$only" == "release" || "$only" == "all" ]]; then
    run_release_test || true
  fi
  if [[ "$only" == "all" ]]; then
    cd "$ROOT" && ./gradlew --stop
    free -m
  fi
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  main "$@"
fi