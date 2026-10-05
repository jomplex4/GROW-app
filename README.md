# GROW

App Android nativa en Kotlin (sin librerias externas, sin anuncios). Rutina diaria de postura, descompresion espinal, impacto y flexibilidad, mas un modulo aparte de mandibula y cuello.

## Flujo de trabajo
1. Descomprime este zip ENCIMA de tu carpeta local, reemplazando todo.
2. Sube los cambios con Git (`git add .`, `git commit`, `git push`).
3. GitHub Actions compila la APK. Para sacar una version descargable en el celular:
   `git tag v1.1` y luego `git push origin v1.1`.
4. En el celular: GitHub, repositorio, Releases, version v1.1, Assets, descarga `GROW-v1.1.apk` e instala.
   (Tambien queda como artefacto en Actions en cada push a main.)

La firma esta en `app/grow.jks`: no la cambies, asi cada APK se instala encima de la anterior sin perder datos.

## Estructura
- `app/src/main/assets/exercises.json`: 43 ejercicios (poses clave, descripcion, tips, MET).
- `app/src/main/assets/plan.txt`: una linea por dia (1292 dias, del 5 de octubre de 2026 al 18 de abril de 2030). `R` = sabado de descanso.
- `app/src/main/assets/jaw.txt`: igual, para mandibula y cuello.
- `app/src/main/assets/fonts/spacegrotesk.ttf`: tipografia.
- `FigureRenderer.kt`: dibujo y animacion de la figura.
- `author/`: scripts de Python que generan ejercicios y plan (`python3 export.py`) y `jvm/Preview.kt` para renderizar las figuras sin Android.

## Ajustes rapidos
- Fechas: `Data.kt` (`START`, `END`) y `author/export.py`.
- Colores: `Theme.kt` (objeto `C`) y `Pal` en `FigureRenderer.kt`.
- Icono: `res/drawable-nodpi/ic_launcher_fg.png` sobre fondo `#BEBEBE`.
