# Mundo Play TV Beta

Aplicación Android independiente con identificador `com.mundoplay.next`, para probar TV en vivo en celular y Android TV. No comparte la instalación ni el código de la app de Pelis y Series.

## Prueba

1. Instala `app-debug.apk` en Android o Android TV.
2. Abre **Lista M3U** e ingresa la URL de una lista M3U autorizada que controles.
3. Elige una señal para reproducirla. También se puede conectar mediante credenciales Xtream del proveedor.

Esta beta lee listas M3U estándar y prueba fuentes HLS (`.m3u8`) y MPEG-DASH (`.mpd`) cuando el proveedor permite reproducción directa. No incluye una lista de canales ni credenciales, y no configura claves ClearKey/DRM. Las señales protegidas necesitan la integración y autorización oficial del proveedor.

Las conexiones se ingresan en el dispositivo durante la prueba; no están incluidas en este proyecto.
