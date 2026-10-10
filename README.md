# GROWTH

App Android nativa en Kotlin (sin librerias externas, sin anuncios). Rutina diaria de postura, descompresion espinal, impacto y flexibilidad, mas un modulo aparte de mandibula y cuello. Version 2.4.

## Flujo de trabajo
1. Descomprime este zip ENCIMA de tu carpeta local, reemplazando todo.
2. Sube los cambios con Git (`git add .`, `git commit -m "GROWTH v2.4"`, `git push`).
3. Para sacar la APK descargable: `git tag v2.4` y luego `git push origin v2.4`. GitHub Actions compila y publica `GROWTH-v2.4.apk` en Releases.
4. En el celular: GitHub, repositorio, Releases, v2.4, Assets, descarga e instala.

La firma esta en `app/grow.jks`: no la cambies, asi cada APK se instala encima de la anterior sin perder datos.

## Estructura
- `app/src/main/assets/anim/<ejercicio>/`: cuadros de la animacion (WebP con fondo transparente). `anim/meta.json` describe cada ejercicio. Las miniaturas de la lista usan estos mismos cuadros, a media resolucion y solo mientras la fila esta en pantalla.
- `app/src/main/assets/exercises.json`: 43 ejercicios (textos en ingles y espanol, tiempos). Tambien guarda la animacion dibujada en codigo, que se usa solo si falta el arte de un ejercicio.
- `app/src/main/assets/plan.txt` y `jaw.txt`: una linea por dia (1292 dias, del 5 de octubre de 2026 al 18 de abril de 2030). `R` = sabado de descanso.
- `app/src/main/assets/fonts/spacegrotesk.ttf`: tipografia.
- `Sprites.kt` y `FigureView.kt`: reproductor de las animaciones. `FigureRenderer.kt`: figura dibujada en codigo (respaldo).
- `pipeline/`: scripts de Python que recortan las celdas de las imagenes de ChatGPT, quitan el fondo, alinean y generan los cuadros (`build3.py`: detecta el esqueleto con MediaPipe, alinea las poses por los huesos que no se mueven, deforma la figura entre poses, gira la cabeza en los ejercicios de cuello y define el ritmo de cada ejercicio en la tabla `T`).
- `author/qa/`: pruebas de calendario, rachas, plan y assets (`audit_assets.py`, `QA.kt`) y generador de GIFs de vista previa.

## Ajustes rapidos
- Fechas: `Data.kt` (`START`, `END`) y `author/export.py`.
- Colores: `Theme.kt` (objeto `C`) y `Pal` en `FigureRenderer.kt` (fondo del escenario).
- Ícono: `res/drawable-nodpi/ic_launcher_fg.png` sobre fondo `#BEBEBE`. Logo de la pantalla de inicio: `res/drawable-nodpi/grow_logo.png`.

## Ilustraciones de los ejercicios (v2.4)
Las imagenes de los 36 ejercicios principales vienen de **RepDB** (https://repdb.co, plan gratuito: uso personal o comercial dentro de apps, con enlace de atribucion visible; ya esta en la pantalla de informacion de la app). Se recolorean a la paleta de la app (polo rojo, short negro, fondo plomo) y se animan entre la pose inicial y la final. Los 7 ejercicios de mandibula y cuello conservan sus imagenes propias.
- `pipeline/repdb_map.py`: que ejercicio de RepDB corresponde a cada ejercicio de la app.
- `pipeline/repdb_fetch.py`: descarga las ilustraciones originales a `pipeline/repdb/` (no se suben al repositorio por la licencia).
- `pipeline/repdb_build.py`: recolorea, interpola y escribe los cuadros en `assets/anim/` y `meta.json`.
- `author/patch_text.py`: textos en espanol de los ejercicios cuya imagen cambio. `author/export.py` los aplica; `author/plan_gen.py` genera `plan.txt`.
