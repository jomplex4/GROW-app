# GROW

App Android nativa en Kotlin (sin librerias externas, sin anuncios). Rutina diaria de postura, descompresion espinal, impacto y flexibilidad, mas un modulo aparte de mandibula y cuello. Version 1.3.

## Flujo de trabajo
1. Descomprime este zip ENCIMA de tu carpeta local, reemplazando todo.
2. Sube los cambios con Git (`git add .`, `git commit -m "GROW v1.3"`, `git push`).
3. Para sacar la APK descargable: `git tag v1.3` y luego `git push origin v1.3`. GitHub Actions compila y publica `GROW-v1.3.apk` en Releases.
4. En el celular: GitHub, repositorio, Releases, v1.3, Assets, descarga e instala.

La firma esta en `app/grow.jks`: no la cambies, asi cada APK se instala encima de la anterior sin perder datos.

## Estructura
- `app/src/main/assets/anim/<ejercicio>/`: cuadros de la animacion (WebP con fondo transparente) y `t.webp` (miniatura). `anim/meta.json` describe cada ejercicio.
- `app/src/main/assets/exercises.json`: 43 ejercicios (textos en ingles y espanol, tiempos). Tambien guarda la animacion dibujada en codigo, que se usa solo si falta el arte de un ejercicio.
- `app/src/main/assets/plan.txt` y `jaw.txt`: una linea por dia (1292 dias, del 5 de octubre de 2026 al 18 de abril de 2030). `R` = sabado de descanso.
- `app/src/main/assets/fonts/spacegrotesk.ttf`: tipografia.
- `Sprites.kt` y `FigureView.kt`: reproductor de las animaciones. `FigureRenderer.kt`: figura dibujada en codigo (respaldo).
- `pipeline/`: scripts de Python que recortan las celdas de las imagenes de ChatGPT, quitan el fondo, alinean y generan los cuadros (`run_all.py`).
- `author/qa/`: pruebas de calendario, rachas, plan y assets (`audit_assets.py`, `QA.kt`) y generador de GIFs de vista previa.

## Ajustes rapidos
- Fechas: `Data.kt` (`START`, `END`) y `author/export.py`.
- Colores: `Theme.kt` (objeto `C`) y `Pal` en `FigureRenderer.kt` (fondo del escenario).
- Ícono: `res/drawable-nodpi/ic_launcher_fg.png` sobre fondo `#BEBEBE`. Logo de la pantalla de inicio: `res/drawable-nodpi/grow_logo.png`.
