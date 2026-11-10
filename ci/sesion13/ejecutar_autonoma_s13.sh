#!/usr/bin/env bash
# Actividad autónoma 13: recorrido de diez pasos y reinstalación. No imprime claves ni tokens.
set -u
OUT=evidencias-ci/autonoma; mkdir -p "$OUT"
PKG=pe.edu.upeu.pharmamobil
RUNNER=$PKG.test/androidx.test.runner.AndroidJUnitRunner
CLAVE=$(curl -s http://127.0.0.1:8099/credenciales | python3 -c 'import json,sys;print(json.load(sys.stdin)["password"])')
curl -s http://127.0.0.1:8099/token-vencido > /tmp/vencido.json
VENCIDO=$(python3 -c 'import json;print(json.load(open("/tmp/vencido.json"))["token"])')
python3 -c 'import json;d=json.load(open("/tmp/vencido.json"));print("GET /auth/me con el token vencido: HTTP", d["auth_me"])' > "$OUT/token-vencido.txt"
ocultar() { sed -e "s/$CLAVE/[REDACTADO]/g" -e "s/$VENCIDO/[TOKEN VENCIDO]/g"; }
adb shell getprop ro.build.version.release > "$OUT/android-version.txt"; adb shell getprop ro.build.version.sdk >> "$OUT/android-version.txt"
adb shell settings put global hide_error_dialogs 1 || true
sleep 20
instalar() {
  adb uninstall $PKG >/dev/null 2>&1; adb uninstall $PKG.test >/dev/null 2>&1
  adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
  adb install -r androidApp/build/outputs/apk/androidTest/debug/androidApp-debug-androidTest.apk
  adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
  adb shell input keyevent KEYCODE_WAKEUP; adb shell wm dismiss-keyguard; adb shell input keyevent KEYCODE_HOME
}
adb shell 'rm -f /sdcard/Download/S13A-*'
instalar
adb shell am instrument -w -r -e class $PKG.AutonomaSesion13Test -e notClass $PKG.AutonomaSesion13Test#p11_reinstalacionSinToken \
  -e adminPassword "$CLAVE" -e tokenVencido "$VENCIDO" $RUNNER 2>&1 | ocultar > "$OUT/recorrido.txt"
adb shell cmd connectivity airplane-mode disable || true
# Reinstalación: el estado anterior se lee del archivo de Download antes de desinstalar.
adb pull /sdcard/Download/S13A-estado.txt "$OUT/estado-recorrido.txt" || true
instalar
adb shell am instrument -w -r -e class $PKG.AutonomaSesion13Test#p11_reinstalacionSinToken $RUNNER 2>&1 | ocultar > "$OUT/reinstalacion.txt"
for f in $(adb shell 'ls /sdcard/Download/' | tr -d '\r' | grep '^S13A-.*png'); do adb pull "/sdcard/Download/$f" "$OUT/${f#S13A-}"; done
adb pull /sdcard/Download/S13A-estado.txt "$OUT/estado-reinstalacion.txt" || true
curl -s http://127.0.0.1:8099/productos > "$OUT/productos-servidor.json" || true
tail -5 "$OUT/recorrido.txt" "$OUT/reinstalacion.txt"
grep -q 'OK (10 tests)' "$OUT/recorrido.txt" && grep -q 'OK (1 test)' "$OUT/reinstalacion.txt"
