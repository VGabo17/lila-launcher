# Lilac Launcher

Launcher minimalista para Android: base OLED negra, acentos lila neón, panel de utilidades
estilo Lunar Client y cajón de apps con búsqueda.

## Gestos
- **Deslizar hacia arriba** en el Home: cajón de apps (búsqueda + orden alfabético).
- **Deslizar hacia la derecha** en el Home: panel **Mods** (RAM, batería, temperatura y accesos rápidos).
- **Botón Home**: vuelve al centro desde cualquier panel.

## Obtener el APK

### Opción A: GitHub Actions (sin instalar nada)
1. Crea un repositorio en GitHub y sube todo este proyecto.
2. Pestaña **Actions** → **Build APK** → **Run workflow**.
3. Cuando termine (~5 min), descarga `lilac-release-apk` (optimizado) o `lilac-debug-apk` desde *Artifacts*.

### Opción B: Android Studio
1. *File → Open* y elige esta carpeta. Espera a que termine el Gradle Sync.
2. Conecta el teléfono con depuración USB y pulsa **Run**, o usa *Build → Build APK(s)*.

## Instalar y usar como launcher
1. Instala el APK (permite "instalar apps de orígenes desconocidos" si te lo pide).
2. Pulsa el botón Home: Android preguntará qué launcher usar → elige **Lilac** → *Siempre*.
3. Para volver al anterior: Ajustes → Apps → Apps predeterminadas → App de inicio.

## Notas de diseño
- Reloj actualizado por el broadcast `TIME_TICK` (1 vez/min), sin timers propios.
- RAM/batería/temperatura se muestrean cada 2 s **solo** con el panel Mods abierto y la app en primer plano.
- Iconos cargados bajo demanda con un `LruCache` acotado (~12 MB máx.).
- Animaciones leídas solo en `graphicsLayer`: arrastrar no provoca recomposición.
- Wi-Fi y Bluetooth abren el panel del sistema (Android 10+ no permite que una app normal los conmute).
  "No molestar" y la linterna sí se controlan directamente.
- La temperatura mostrada es la de la batería, la única que Android expone a las apps.
