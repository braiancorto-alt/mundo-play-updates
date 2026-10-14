# Mundo Play Next — base nueva

Proyecto Android independiente para reconstruir Mundo Play con una base editable. La aplicación ofrece inicio de sesión Xtream ingresado por el cliente o carga de una lista M3U autorizada; muestra canales por categoría con búsqueda y logos; reproduce en pantalla completa, gira a horizontal al abrir un canal y oculta los controles después de 3 segundos. Los botones permiten volver y pasar al canal anterior/siguiente. Si una conexión Xtream falla, el reproductor intenta la extensión alternativa.

## Importante
- No se incrustan usuarios ni contraseñas en el código.
- No se precarga la lista pública de iptv-org: incluye señales de terceros y su publicación no acredita derechos de retransmisión. La persona responsable de la lista debe confirmar autorización antes de cargarla.
- El proyecto usa un identificador nuevo (`com.mundoplay.next`), por lo que esta app de prueba se instalaría aparte de la 6.32.
- No tiene todavía backend propio para altas, vencimientos, límite de dispositivos ni panel administrativo. La validación inicial es la que expone el proveedor Xtream.
- Este entorno no dispone de Android SDK/Gradle, así que el proyecto aún no fue compilado ni empaquetado como APK.

## Abrir
Abrir esta carpeta en Android Studio y sincronizar Gradle. El workflow de GitHub Actions compila una APK de depuración cuando el proyecto se sube a un repositorio.
