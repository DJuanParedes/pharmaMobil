# Actividad autónoma 13: flujo completo y evidencias por plataforma

Juan Paredes Cárdenas. Rama `feature/integrador-13`.

- **PDF de entrega:** `output/pdf/Paredes_Juan_Sesion13_Evidencias.pdf`. Es tamaño carta, tiene 13 páginas y pesa menos de 2 MB. Se genera con `python ci/generar_autonoma_s13.py`.
- **Recorrido (Producto A):** lo ejecuta `androidApp/src/androidTest/.../AutonomaSesion13Test.kt`, con los pasos p01 a p10 y la reinstalación p11. Corre en `.github/workflows/sesion13-autonoma.yml` contra PharmaSoft real con H2, usando la clave efímera y un token vencido firmado por el backend.
- **Evidencias originales:** `evidencias/sesion13-autonoma`. Incluye capturas sin recortar, `estado.txt` escrito por la prueba, la salida de la instrumentación y `token-vencido.txt`.
- **iOS:** `evidencias/sesion13-autonoma/ios`. El framework para dispositivo enlaza. El binario de pruebas del simulador no enlaza con Xcode 16.4 porque falta `UIViewLayoutRegion`, que viene en el SDK de iOS 26. El workflow es `.github/workflows/sesion13-ios.yml`.
- **Bitácora de permisos (Producto C):** usa las capturas de `evidencias/sesion13/android`, de la práctica integradora.
- **Ficha de integración (Producto D):** 13 huecos con gravedad, decisión y fecha. Los cerrados tienen fecha 03/11/2026; los de la sesión 14, 10/11/2026.

Para reproducir en local, con emulador y PharmaSoft activos:

```
./gradlew :androidApp:assembleDebug :androidApp:assembleDebugAndroidTest
adb shell am instrument -w -e class pe.edu.upeu.pharmamobil.AutonomaSesion13Test \
  -e notClass pe.edu.upeu.pharmamobil.AutonomaSesion13Test#p11_reinstalacionSinToken \
  -e adminPassword <clave> -e tokenVencido <jwt> pe.edu.upeu.pharmamobil.test/androidx.test.runner.AndroidJUnitRunner
```
