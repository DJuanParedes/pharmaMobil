#!/usr/bin/env bash
# Sesión 14 en el emulador de CI: 1) el universal.apk sacado del AAB con bundletool instala y abre;
# 2) recorrido completo de la versión de entrega minificada contra PharmaSoft real (pasos 5 y 7).
set -u
OUT=evidencias-ci/emulador; mkdir -p "$OUT"
PKG=pe.upeu.pharmamobil
sleep 20
adb shell settings put global hide_error_dialogs 1 || true
adb shell input keyevent KEYCODE_WAKEUP; adb shell wm dismiss-keyguard
# 1) Paquete de aplicación → instalable universal (bundletool) → instalación
adb uninstall $PKG >/dev/null 2>&1
adb install -r entrega/universal.apk > "$OUT/universal-install.txt" 2>&1
adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 8
adb exec-out screencap -p > "$OUT/00-universal-desde-aab.png"
adb shell dumpsys package $PKG | grep -E "versionName|versionCode" > "$OUT/universal-version.txt"
adb uninstall $PKG >> "$OUT/universal-install.txt" 2>&1
# 2) Recorrido de la versión de entrega (URL del emulador)
python3 ci/sesion14/recorrido_release.py entrega/Equipo2_ProyectoFinal_Android_emulador.apk "$OUT/recorrido"
codigo=$?
adb shell cmd connectivity airplane-mode disable || true
curl -s http://127.0.0.1:8099/productos > "$OUT/productos-servidor.json" || true
echo "codigo=$codigo" > "$OUT/resultado.txt"
exit $codigo
