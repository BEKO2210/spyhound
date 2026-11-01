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
ARTIFACT_DIR="/tmp/spyhound-matrix-artifacts"
ROOT_DIR="/tmp/spyhound-root"
SHIZUKU_APK="$ROOT_DIR/shizuku.apk"
MAGISK_APK="$ROOT_DIR/Magisk-v31.1.apk"
ROOTAVD="$ROOT_DIR/rootAVD/rootAVD.sh"
PKG=de.belkis.spyhound
SCAN_WAIT_ITER=150 # 150 * 2s = 5 min max
unset ANDROID_SERIAL

adb_emu() {
  unset ANDROID_SERIAL
  local serial
  serial=$($ADB devices | awk '/^emulator-/{print $1; exit}')
  if [[ -z "$serial" ]]; then
    echo "no emulator in adb devices" >&2
    return 1
  fi
  export ANDROID_SERIAL="$serial"
}

require_emu() {
  adb_emu || return 1
}

kill_emu() {
  unset ANDROID_SERIAL
  local serial
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
  for _ in $(seq 1 45); do
    serial=$($ADB devices | awk '/^emulator-/{print $1; exit}')
    [[ -z "$serial" ]] && break
    sleep 1
  done
  unset ANDROID_SERIAL
}

wait_boot() {
  unset ANDROID_SERIAL
  local serial=""
  for _ in $(seq 1 120); do
    serial=$($ADB devices | awk '/^emulator-/{print $1; exit}')
    [[ -n "$serial" ]] && break
    sleep 2
  done
  if [[ -z "$serial" ]]; then
    echo "wait_boot: emulator never appeared" >&2
    return 1
  fi
  export ANDROID_SERIAL="$serial"
  $ADB wait-for-device
  $ADB shell true >/dev/null 2>&1 || true
  for _ in $(seq 1 180); do
    local boot anim
    boot=$($ADB shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
    anim=$($ADB shell getprop init.svc.bootanim 2>/dev/null | tr -d '\r')
    if [[ "$boot" == "1" && "$anim" == "stopped" ]]; then
      break
    fi
    sleep 2
  done
  $ADB shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
  sleep 3
  for _ in $(seq 1 30); do
    $ADB shell pm path android >/dev/null 2>&1 && break
    sleep 2
  done
  sleep 10
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
  printf '%s' "$xml" | python3 -c '
import re, sys
label = sys.argv[1]
xml = sys.stdin.read().replace("\n", " ")
patterns = [
    rf"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"[^>]*(?:text|content-desc)=\"{re.escape(label)}\"",
    rf"(?:text|content-desc)=\"{re.escape(label)}\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"",
]
for pat in patterns:
    m = re.search(pat, xml)
    if m:
        print(m.group(1), m.group(2), m.group(3), m.group(4))
        sys.exit(0)
for m in re.finditer(rf"(?:text|content-desc)=\"{re.escape(label)}\"", xml):
    chunk = xml[m.start() : m.start() + 1200]
    b = re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", chunk)
    if b:
        print(b.group(1), b.group(2), b.group(3), b.group(4))
        sys.exit(0)
' "$label"
}

click_desc() {
  local desc=$1
  local bounds
  bounds=$(find_tap_bounds "$desc") || true
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

click_any() {
  local label
  for label in "$@"; do
    click_desc "$label" 2>/dev/null && return 0
  done
  return 1
}

screenshot() {
  local out=$1
  $ADB exec-out screencap -p >"$out"
}

save_failure() {
  local tag=$1
  mkdir -p "$ARTIFACT_DIR"
  screenshot "$ARTIFACT_DIR/${tag}-fail.png" || true
  $ADB logcat -d >"$ARTIFACT_DIR/${tag}-logcat.txt" 2>/dev/null || true
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

dismiss_intro_ui() {
  local i
  for i in $(seq 1 8); do
    click_any "Skip" "Überspringen" 2>/dev/null && sleep 1
    local xml
    xml=$(ui_dump)
    if ! echo "$xml" | grep -qE 'intro_skip|Überspringen|>Skip<'; then
      return 0
    fi
    sleep 1
  done
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

start_emulator() {
  local avd=$1
  shift
  sleep 2
  $EMU -avd "$avd" -no-window -no-audio -no-snapshot -gpu swiftshader_indirect \
    -partition-size 8192 -wipe-data "$@" >/tmp/emu-"${avd}".log 2>&1 &
}

install_apps() {
  require_emu || return 1
  $ADB install -r -g "$SPY_APK" >/dev/null || return 1
  prepare_spyhound_data
  push_debug_tsv || return 1
  skip_intro || return 1
  $ADB install -r -g "$TEST_APK" >/dev/null || return 1
}

launch_spyhound() {
  $ADB shell am start -n "$PKG/.MainActivity" >/dev/null
  sleep 4
  dismiss_runtime_permissions
  dismiss_intro_ui
  dismiss_runtime_permissions
}

wait_scan_done() {
  local tag=${1:-scan}
  for _ in $(seq 1 "$SCAN_WAIT_ITER"); do
    local xml
    xml=$(ui_dump)
    if echo "$xml" | grep -qi 'testspy'; then
      return 0
    fi
    if echo "$xml" | grep -qE 'text="Scan again"|text="Neu prüfen"|content-desc="Scan again"|content-desc="Neu prüfen"'; then
      sleep 2
      return 0
    fi
    sleep 2
  done
  echo "scan timeout ($tag)" >&2
  save_failure "$tag"
  return 1
}

check_finding() {
  local xml
  xml=$(ui_dump)
  echo "$xml" | grep -qi 'testspy'
}

check_logcat_fatal() {
  local fatals
  fatals=$($ADB logcat -d -s AndroidRuntime:E 2>/dev/null | tail -30)
  if echo "$fatals" | grep -q FATAL; then
    echo "$fatals" >&2
    return 1
  fi
  return 0
}

check_no_shell_dead_gap() {
  local xml
  xml=$(ui_dump)
  if echo "$xml" | grep -qiE 'antwortet nicht|does not respond'; then
    echo "Shizuku/root dead gap visible" >&2
    return 1
  fi
  return 0
}

tap_scan() {
  local i
  for i in $(seq 1 40); do
    dismiss_runtime_permissions
    dismiss_intro_ui
    click_any "Scan now" "Jetzt prüfen" 2>/dev/null && return 0
    sleep 2
  done
  echo "tap_scan: Scan button not found" >&2
  save_failure tap-scan
  return 1
}

grant_dialogs() {
  local i
  for i in $(seq 1 12); do
    click_any "Allow" "Erlauben" "Zulassen" "OK" "Grant" "Gewähren" \
      "Allow all the time" "Immer zulassen" "Always allow" 2>/dev/null || true
    sleep 1
  done
}

dismiss_runtime_permissions() {
  local i
  for i in $(seq 1 20); do
    local xml
    xml=$(ui_dump)
    if ! echo "$xml" | grep -q 'com.android.permissioncontroller'; then
      return 0
    fi
    # Prefer Allow; fall back to deny so scans are not blocked forever
    click_any "Allow" "Erlauben" 2>/dev/null || \
      click_any "Don't allow" "Nicht zulassen" 2>/dev/null || true
    sleep 1
  done
}

check_tablet_width() {
  local density width_px max_px xml w
  density=$($ADB shell wm density 2>/dev/null | tr -dc '0-9')
  width_px=$($ADB shell wm size 2>/dev/null | tail -1 | grep -oE '[0-9]+x[0-9]+' | cut -dx -f1)
  max_px=$(( 640 * density / 160 ))
  local pad_px=$(( 40 * density / 160 ))
  local limit_px=$(( max_px + pad_px + 32 ))
  xml=$(ui_dump)
  w=$(printf '%s' "$xml" | python3 -c "
import re, sys
xml = sys.stdin.read()
screen_w = int(sys.argv[1])
limit = int(sys.argv[2])
widths = []
for m in re.finditer(r'bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    w = int(m.group(3)) - int(m.group(1))
    if w <= 200:
        continue
    if w >= screen_w * 9 // 10:
        continue
    if w <= limit:
        widths.append(w)
if not widths:
    sys.exit(1)
print(max(widths))
" "$width_px" "$limit_px")
  if [[ -z "$w" ]]; then
    echo "tablet layout: could not measure content width" >&2
    return 1
  fi
  if [[ "$w" -gt "$limit_px" ]]; then
    echo "tablet layout: content ${w}px > limit ${limit_px}px (640dp column @ density $density)" >&2
    return 1
  fi
  echo "tablet layout OK: content ${w}px <= ${limit_px}px (640dp column)"
}

start_shizuku_adb() {
  local apk_path dir
  apk_path=$($ADB shell pm path moe.shizuku.privileged.api 2>/dev/null | head -1 | tr -d '\r' | cut -d: -f2)
  [[ -z "$apk_path" ]] && { echo "Shizuku not installed" >&2; return 1; }
  dir=$(dirname "$apk_path")
  $ADB shell "cd '$dir' && ./lib/x86_64/libshizuku.so" >/dev/null 2>&1 &
  sleep 4
  $ADB shell pm list packages | grep -q shizuku
}

root_avd_magisk() {
  local ramdisk="system-images/android-34/google_apis/x86_64/ramdisk.img"
  [[ -f "$MAGISK_APK" ]] || { echo "missing Magisk apk" >&2; return 1; }
  [[ -x "$ROOTAVD" ]] || { echo "missing rootAVD" >&2; return 1; }
  mkdir -p "$ROOT_DIR/rootAVD/Apps"
  cp -f "$MAGISK_APK" "$ROOT_DIR/rootAVD/Apps/Magisk.apk"
  cp -f "$MAGISK_APK" "$ROOT_DIR/rootAVD/Magisk.zip"
  local sha
  sha=$(sha256sum "$MAGISK_APK" | awk '{print $1}')
  if [[ "$sha" != "89cefac0a318afa39fb3880c45c87b036c05bc1ebf02a7a715b7f8d7c26ac6e7" ]]; then
    echo "Magisk SHA mismatch" >&2
    return 1
  fi
  adb_emu
  $ADB shell svc data disable 2>/dev/null || true
  local log=/tmp/rootAVD-api34.log
  (
    cd "$ROOT_DIR/rootAVD"
    export PATH="$SDK/platform-tools:$PATH"
    export ANDROID_HOME="$SDK"
    timeout 600 ./rootAVD.sh "$ramdisk" >"$log" 2>&1
  )
  local rc=$?
  if [[ $rc -ne 0 ]] || grep -q 'aborting the script' "$log"; then
    echo "rootAVD failed — see $log" >&2
    return 1
  fi
  $ADB shell svc data enable 2>/dev/null || true
}

run_phone_test() {
  local api=$1 nn=$2 sysimg=$3 avd=$4
  echo "=== API $api (api${nn}) ===" | tee -a /tmp/matrix-emulator-test.log
  kill_emu
  create_avd "$avd" "$sysimg" pixel_6
  start_emulator "$avd" \
    -prop persist.sys.language=de -prop persist.sys.country=DE
  wait_boot || { kill_emu; return 1; }
  install_apps || { kill_emu; return 1; }
  launch_spyhound
  sleep 4
  screenshot "$SHOT_DIR/api${nn}-start.png"
  tap_scan || { kill_emu; return 1; }
  sleep 2
  screenshot "$SHOT_DIR/api${nn}-scan.png"
  wait_scan_done "api${nn}" || { kill_emu; return 1; }
  screenshot "$SHOT_DIR/api${nn}-result.png"
  check_finding || { echo "FAIL: no Testspy finding on API $api" >&2; save_failure "api${nn}-finding"; kill_emu; return 1; }
  check_logcat_fatal || { save_failure "api${nn}-fatal"; kill_emu; return 1; }
  kill_emu
  echo "OK API $api"
}

run_tablet_test() {
  local avd=spyhound-matrix-tablet-36
  echo "=== Tablet API 36 ==="
  kill_emu
  create_avd "$avd" "system-images;android-36;google_apis;x86_64" pixel_tablet
  start_emulator "$avd" -prop persist.sys.language=de -prop persist.sys.country=DE
  wait_boot
  install_apps
  launch_spyhound
  screenshot "$SHOT_DIR/api36-tablet-start.png"
  check_tablet_width || { save_failure tablet-layout; kill_emu; return 1; }
  tap_scan || { kill_emu; return 1; }
  wait_scan_done tablet || { kill_emu; return 1; }
  screenshot "$SHOT_DIR/api36-tablet-result.png"
  check_logcat_fatal || { kill_emu; return 1; }
  kill_emu
  echo "OK Tablet"
}

run_shizuku_test() {
  local avd=spyhound-matrix-shizuku-34
  echo "=== Shizuku API 34 ==="
  kill_emu
  create_avd "$avd" "system-images;android-34;google_apis;x86_64" pixel_6
  start_emulator "$avd" -prop persist.sys.language=de -prop persist.sys.country=DE
  wait_boot
  $ADB install -r -g "$SHIZUKU_APK" >/dev/null
  start_shizuku_adb
  install_apps
  launch_spyhound
  screenshot "$SHOT_DIR/api34-shizuku-start.png"
  tap_scan
  grant_dialogs
  sleep 2
  grant_dialogs
  wait_scan_done shizuku || { kill_emu; return 1; }
  screenshot "$SHOT_DIR/api34-shizuku-result.png"
  check_no_shell_dead_gap || { save_failure shizuku-gap; kill_emu; return 1; }
  local xml
  xml=$(ui_dump)
  if echo "$xml" | grep -qiE 'Prozesse und Systemprotokolle ohne Shizuku|processes and system logs were not checked without Shizuku'; then
    echo "FAIL: demo Shizuku gap still shown" >&2
    save_failure shizuku-demo-gap
    kill_emu
    return 1
  fi
  check_logcat_fatal || { kill_emu; return 1; }
  kill_emu
  echo "OK Shizuku"
}

run_magisk_root_test() {
  local avd=spyhound-matrix-root-34
  local rd="$SDK/system-images/android-34/google_apis/x86_64/ramdisk.img"
  local bak="${rd}.backup"
  echo "=== Magisk root API 34 ==="
  kill_emu
  create_avd "$avd" "system-images;android-34;google_apis;x86_64" pixel_6
  if [[ -f "$bak" && "$rd" -nt "$bak" ]]; then
    echo "Patched ramdisk already in SDK image — skipping rootAVD"
    start_emulator "$avd" -prop persist.sys.language=de -prop persist.sys.country=DE
  else
    start_emulator "$avd" -prop persist.sys.language=de -prop persist.sys.country=DE
    wait_boot
    root_avd_magisk || { kill_emu; return 1; }
    unset ANDROID_SERIAL
    for _ in $(seq 1 90); do
      serial=$($ADB devices | awk '/^emulator-/{print $1; exit}')
      [[ -z "$serial" ]] && break
      sleep 2
    done
    find "$HOME/.android/avd" -name '*.lock' -delete 2>/dev/null || true
    sleep 3
    echo "Cold boot after Magisk ramdisk patch"
    start_emulator "$avd" -prop persist.sys.language=de -prop persist.sys.country=DE
  fi
  wait_boot || { kill_emu; return 1; }
  install_apps
  launch_spyhound
  screenshot "$SHOT_DIR/api34-root-start.png"
  click_any "Root anfragen" "Request root" 2>/dev/null || true
  sleep 2
  grant_dialogs
  click_any "Grant" "Gewähren" "Zulassen" "Allow" 2>/dev/null || true
  sleep 2
  grant_dialogs
  screenshot "$SHOT_DIR/api34-root-granted.png"
  tap_scan
  grant_dialogs
  wait_scan_done root || { kill_emu; return 1; }
  screenshot "$SHOT_DIR/api34-root-result.png"
  check_finding || { echo "FAIL: no Testspy on root scan" >&2; save_failure root-finding; kill_emu; return 1; }
  check_no_shell_dead_gap || { kill_emu; return 1; }
  check_logcat_fatal || { kill_emu; return 1; }
  kill_emu
  echo "OK Magisk root"
}

run_root_probe() {
  local avd=spyhound-matrix-rootprobe-34
  echo "=== adb root probe (API 34, no Magisk) ==="
  kill_emu
  create_avd "$avd" "system-images;android-34;google_apis;x86_64" pixel_6
  start_emulator "$avd"
  wait_boot
  $ADB root >/dev/null 2>&1 || true
  sleep 2
  $ADB install -r "$SPY_APK" >/dev/null
  skip_intro
  launch_spyhound
  click_any "Root anfragen" "Request root" 2>/dev/null || true
  sleep 4
  local xml
  xml=$(ui_dump)
  echo "$xml" | grep -qiE 'nicht verfügbar|unavailable' || {
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
  local avd=spyhound-matrix-release-34
  echo "=== Release APK API 34 ==="
  kill_emu
  create_avd "$avd" "system-images;android-34;google_apis;x86_64" pixel_6
  start_emulator "$avd" -prop persist.sys.language=de -prop persist.sys.country=DE
  wait_boot
  $ADB uninstall "$PKG" 2>/dev/null || true
  $ADB install -r "$rel" >/dev/null
  $ADB install -r "$TEST_APK" >/dev/null
  launch_spyhound
  dismiss_intro_ui
  tap_scan
  wait_scan_done release || { kill_emu; return 1; }
  if check_finding; then
    echo "FAIL: release showed Testspy finding" >&2
    kill_emu
    return 1
  fi
  check_logcat_fatal
  screenshot "$SHOT_DIR/api34-release-result.png"
  kill_emu
  echo "OK release"
}

main() {
  mkdir -p "$SHOT_DIR" "$ARTIFACT_DIR"
  [[ -f "$SPY_APK" && -f "$TEST_APK" ]] || { echo "Build APKs first" >&2; exit 1; }
  local only=${1:-all}
  if [[ "$only" == "29" || "$only" == "all" ]]; then
    run_phone_test 29 29 "system-images;android-29;google_apis;x86_64" spyhound-matrix-29 || echo "FAIL API 29" >&2
  fi
  if [[ "$only" == "31" || "$only" == "all" ]]; then
    run_phone_test 31 31 "system-images;android-31;google_apis;x86_64" spyhound-matrix-31 || echo "FAIL API 31" >&2
  fi
  if [[ "$only" == "34" || "$only" == "all" ]]; then
    run_phone_test 34 34 "system-images;android-34;google_apis;x86_64" spyhound-matrix-34 || echo "FAIL API 34" >&2
  fi
  if [[ "$only" == "37" || "$only" == "all" ]]; then
    run_phone_test 37 37 "system-images;android-37.0;google_apis;x86_64" spyhound-matrix-37 || echo "FAIL API 37" >&2
  fi
  if [[ "$only" == "tablet" || "$only" == "all" ]]; then
    run_tablet_test || true
  fi
  if [[ "$only" == "shizuku" || "$only" == "all" ]]; then
    run_shizuku_test || true
  fi
  if [[ "$only" == "root" || "$only" == "all" ]]; then
    run_magisk_root_test || true
  fi
  if [[ "$only" == "rootprobe" ]]; then
    run_root_probe || true
  fi
  if [[ "$only" == "release" || "$only" == "all" ]]; then
    run_release_test || true
  fi
  if [[ "$only" == "all" || "$only" == "stop" ]]; then
    cd "$ROOT" && ./gradlew --stop
    free -m
  fi
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  main "$@"
fi
