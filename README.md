# 📺 Homelab TV Launcher (`app.homelab.tvlauncher`)

Launcher nativo minimalista y sin publicidad para **Android TV / Google TV**, desarrollado en **Kotlin + Jetpack Compose for TV** con interfaz inmersiva **estilo Netflix** e integración con servidores multimedia autohospedados (**Jellyfin** y **Torrent Stream Web**).

---

## ✨ Características Principales

1. **Interfaz Inmersiva Estilo Netflix a 60 fps (~2 MB APK / ~20 MB RAM):**
   - **Fondo dinámico a pantalla completa (`Crossfade` + doble viñeteado oscuro):** Cambia suavemente según la aplicación o contenido seleccionado, manteniendo baja opacidad (~38%) para dar atmósfera sin restar legibilidad al selector.
   - **Fondos curados + Paleta dinámica (`androidx.palette`):** Arte integrado para las principales aplicaciones de streaming y reproducción, combinado con extracción automática del color dominante y desenfoque gaussiano sobre el banner de cualquier otra aplicación instalada.
   - **Cabecera "Hero" superior izquierda:** Muestra en grande el título, etiqueta de estado/categoría, descripción y barra de progreso en tiempo real.
2. **Selector de 4 Filas Horizontales con Navegación D-Pad Suave:**
   - **Fila 1 — Favoritas:** Aplicaciones fijadas para acceso inmediato.
   - **Fila 2 — Seguir viendo:** Combina en tiempo real sesiones activas de streaming HTTP bajo demanda (**Torrent Stream Web**, con barra de progreso del buffer inicial, caché acumulada y selector de episodio) junto con el historial de reproducción de **Jellyfin**.
   - **Fila 3 — Aplicaciones:** Todas las aplicaciones instaladas en el sistema. Permite fijar en Favoritas, reordenar hacia izquierda/derecha, ocultar paquetes preinstalados (anti-bloatware) o abrir los ajustes de sistema de la aplicación.
   - **Fila 4 — Recientes en Jellyfin:** Últimas películas y capítulos incorporados a la biblioteca multimedia.
3. **Reproducción Flexible:**
   - Lanza `Intent` directo hacia **VLC**, **Kodi**, cliente oficial de **Jellyfin TV** o reproduce directamente el flujo HTTP mediante el **Reproductor Integrado (Media3 ExoPlayer)** incluido en el propio launcher.

---

## 🏗️ Stack Tecnológico

- **Lenguaje y UI:** Kotlin · Jetpack Compose for TV (`androidx.tv.material3` y `androidx.tv.foundation`)
- **Procesamiento Gráfico:** `Coil` (caché asíncrona de pósters y backdrops) · `androidx.palette` (extracción cromática de banners)
- **Motor de Vídeo Integrado:** AndroidX Media3 (`ExoPlayer`) con soporte de `HTTP Range Requests`
- **Red y Serialización:** `kotlinx.serialization` · Corrutinas de Kotlin (`repeatOnLifecycle`)
