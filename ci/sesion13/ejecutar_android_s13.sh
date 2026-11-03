#!/usr/bin/env bash
# Instala la app limpia, revoca POST_NOTIFICATIONS y ejecuta IntegradorSesion13Test. No imprime la clave.
set -u
OUT=evidencias-ci/android; mkdir -p "$OUT"
PKG=pe.edu.upeu.pharmamobil
CLAVE=$(curl -s http://127.0.0.1:8099/credenciales | python3 -c 'import json,sys;print(json.load(sys.stdin)["password"])')
adb shell getprop ro.build.version.release > "$OUT/android-version.txt"
adb shell getprop ro.build.version.sdk >> "$OUT/android-version.txt"
adb uninstall $PKG >/dev/null 2>&1; adb uninstall $PKG.test >/dev/null 2>&1
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb install -r androidApp/build/outputs/apk/androidTest/debug/androidApp-debug-androidTest.apk
adb shell pm revoke $PKG android.permission.POST_NOTIFICATIONS || true
adb shell input keyevent KEYCODE_WAKEUP; adb shell wm dismiss-keyguard
adb shell 'rm -f /sdcard/Download/S13-*.png'
adb shell am instrument -w -r -e class $PKG.IntegradorSesion13Test -e adminPassword "$CLAVE" \
  $PKG.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | sed "s/$CLAVE/[REDACTADO]/g" > "$OUT/instrumentacion.txt"
adb shell cmd connectivity airplane-mode disable || true
for f in $(adb shell 'ls /sdcard/Download/' | tr -d '\r' | grep '^S13-'); do adb pull "/sdcard/Download/$f" "$OUT/${f#S13-}"; done
adb logcat -d -t 3000 > "$OUT/logcat.txt" 2>&1 || true
curl -s http://127.0.0.1:8099/productos > evidencias-ci/productos-servidor.json || true
tail -20 "$OUT/instrumentacion.txt"
grep -q 'OK (2 tests)' "$OUT/instrumentacion.txt"
