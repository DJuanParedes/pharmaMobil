#!/usr/bin/env bash
# Sesión 14, paso 4 (alternativa sin Secrets en GitHub). Firma los artefactos SIN firmar que publica el workflow
# (rama evidencias-s14-ci, carpeta entrega/sin-firmar) con el almacén del equipo y verifica la firma.
# Uso: scripts/firmar-entrega.sh <ruta/equipo2.jks> <carpeta-con-los-sin-firmar> <apksigner.jar>
# Pide las contraseñas por teclado; no se guardan en ningún archivo del repositorio.
set -euo pipefail
KS="$1"; DIR="$2"; APKSIGNER="$3"
read -rsp "Contraseña del almacén: " SP; echo; read -rsp "Contraseña de la clave: " KP; echo
T=$(mktemp -d); trap 'rm -rf "$T"' EXIT; printf '%s' "$SP" > "$T/sp"; printf '%s' "$KP" > "$T/kp"
java -jar "$APKSIGNER" sign --ks "$KS" --ks-key-alias equipo2 --ks-pass "file:$T/sp" --key-pass "file:$T/kp" \
  --out "$DIR/Equipo2_ProyectoFinal_Android.apk" "$DIR/Equipo2_ProyectoFinal_Android-sin-firmar.apk"
cp "$DIR/Equipo2_ProyectoFinal_Android-sin-firmar.aab" "$DIR/Equipo2_ProyectoFinal_Android.aab"
jarsigner -keystore "$KS" -storepass:file "$T/sp" -keypass:file "$T/kp" -sigalg SHA256withRSA -digestalg SHA-256 \
  "$DIR/Equipo2_ProyectoFinal_Android.aab" equipo2
java -jar "$APKSIGNER" verify --verbose --print-certs "$DIR/Equipo2_ProyectoFinal_Android.apk"
jarsigner -verify -certs "$DIR/Equipo2_ProyectoFinal_Android.aab" | tail -3
keytool -printcert -jarfile "$DIR/Equipo2_ProyectoFinal_Android.aab" | grep -E 'Owner|Propietario|SHA256'
