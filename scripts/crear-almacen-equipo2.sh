#!/usr/bin/env bash
# Sesión 14, paso 2. Se ejecuta UNA sola vez y FUERA de la carpeta del repositorio.
# Pide la contraseña del almacén y la de la clave; no las escribe en ninguna parte.
set -euo pipefail
DESTINO="${1:-$HOME/claves}"
case "$(cd "$(dirname "$0")/.." && pwd)" in "$(mkdir -p "$DESTINO" && cd "$DESTINO" && pwd)"*) echo "El destino está dentro del repositorio"; exit 1;; esac
keytool -genkeypair -v \
  -keystore "$DESTINO/equipo2.jks" -storetype JKS \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias equipo2 \
  -dname "CN=Juan Paredes Cardenas, OU=Equipo 2 - Desarrollo de Aplicaciones Moviles, O=Universidad Peruana Union, L=Lima, ST=Lima, C=PE"
keytool -list -v -keystore "$DESTINO/equipo2.jks" -alias equipo2 | grep -E 'Owner|Propietario|SHA256'
