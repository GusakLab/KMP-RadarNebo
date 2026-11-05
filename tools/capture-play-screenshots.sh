#!/bin/sh
# Captures the Play store screenshots from a real build on a device or emulator, in one screen shape per run, with
# PlayScreenshots (app/android/src/androidTest) waiting for the map to load. Frame phone captures with
# tools/make-play-assets.swift afterwards; the large-screen ones are uploaded as they are.
#
#   tools/capture-play-screenshots.sh <adb serial> <width>x<height> <density> <output dir>
#
# Builds with -PradarNebo.capture, which tests the release build type (the UI Play users get) signed with the debug key.
# Resizes the device's display and restores it at the end.
set -eu

serial=$1 size=$2 density=$3 out=$4
root=$(cd "$(dirname "$0")/.." && pwd)
package=com.gusak.lab.radarnebo.missioncontrol
adb="adb -s $serial"

cd "$root"
./gradlew :app:android:assembleRelease :app:android:assembleReleaseAndroidTest -PradarNebo.capture=true --console=plain -q

navigation=$($adb shell settings get secure navigation_mode | tr -d '\r')
restore() {
    [ "$navigation" = 2 ] || $adb shell cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.threebutton
    $adb shell wm size reset
    $adb shell wm density reset
    $adb shell cmd uimode night no
    $adb shell settings put global sysui_demo_allowed 0
    $adb shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null
}
trap restore EXIT

$adb uninstall "$package" >/dev/null 2>&1 || true
$adb install -r app/android/build/outputs/apk/release/android-release.apk >/dev/null
$adb install -r -t app/android/build/outputs/apk/androidTest/release/android-release-androidTest.apk >/dev/null

$adb shell wm size "$size"
$adb shell wm density "$density"
# Gesture navigation, so a large screen shows a slim handle rather than a taskbar of the device's own apps.
$adb shell cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.gestural

# A tidy status bar: fixed time, full Wi-Fi and battery, no notifications. System UI redraws its bars after the changes
# above; demo commands sent during that can leave a second Wi-Fi icon, so give it time and start demo mode afresh.
sleep 6
demo() { $adb shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null; }
$adb shell settings put global sysui_demo_allowed 1
demo exit
sleep 1
demo enter
demo clock -e hhmm 1200
demo battery -e level 100 -e plugged false
demo network -e wifi hide
demo network -e mobile hide
demo network -e airplane hide -e nosim hide
demo network -e wifi show -e level 4 -e fully true
demo notifications -e visible false

result=$($adb shell am instrument -w -e capture true -e class com.gusak.lab.radarnebo.PlayScreenshots \
    "$package.test/androidx.test.runner.AndroidJUnitRunner")
echo "$result"
case "$result" in
    *"OK (1 test)"*) ;;
    *) echo "Capture failed" >&2; exit 1 ;;
esac

mkdir -p "$out"
$adb pull "/sdcard/Android/data/$package/files/play-captures/." "$out" >/dev/null
$adb logcat -d -s PlayScreenshots:I | grep 'Shot' || true
echo "Captured to $out"
