# Videos de Mapa de Pensum para la pantalla LED

Motion graphics para Resolume, una fase por carpeta. Todas salen a 1920 × 1280
(3:2, la pantalla LED) y 60 fps, duran 20 s y **empiezan y acaban en el mismo
cuadro**: el logo sobre su placa de vidrio. Por eso se pueden encadenar en
cualquier orden, o dejar una sola en bucle, sin que se note el corte.

| Fase | Carpeta | Qué enseña | Estado |
| --- | --- | --- | --- |
| 00 · Inicio | `fase-00-inicio` | El logo en 3D sobre vidrio y la cuadrícula de fondo. Para dejar fijo o descansar la vista | Hecha |
| 01 · El mapa | `fase-01-mapa` | La placa se abre en un panel y aparece el mapa de la carrera: se arma, se toca una materia y se enciende su cadena, se aprueba un semestre | Hecha |
| 02 · La ficha | | La hoja de una materia: qué pide, qué abre y cuándo te gradúas si la atrasas | Pendiente |
| 03 · El avance | | El porcentaje, las UC por área y el anillo de graduación con su fecha | Pendiente |
| 04 · El horario | | Se importa la hoja de inscripción y la semana se arma sola | Pendiente |
| 05 · Las carreras y los temas | | Las nueve carreras con su color y los juegos de tema de Personalizar | Pendiente |
| 06 · Cierre | | El logo, el nombre y la llamada a descargar (QR) | Pendiente |

## Uso

    npm install
    node render.mjs fase-01-mapa --servir          # la escena en el navegador
    node render.mjs fase-01-mapa --cuadros 0,480   # cuadros sueltos a out/
    node render.mjs fase-01-mapa                   # out/mapa-pensum-fase-01-mapa-3x2.mp4

El render necesita `ffmpeg` y Playwright con Chromium. El video entero tarda
unos 40 minutos; si se corta, al relanzarlo sigue por donde iba.

## Cómo está hecho

- `comun/tiempo.js`: el reloj del bucle. Todo lo que se mueve en el fondo es periódico en 20 s.
- `comun/fondo.js`: la cuadrícula de vidrio, las luces y la placa (que puede abrirse en panel).
- `comun/logo.js`: el mapa plegado en three.js, con la ruta pintada en una textura.
- `fase-01-mapa/plano.js`: la geometría del mapa, la misma de la app (`mapa/Geometria.kt`).
- `fase-01-mapa/guion.js`: tiempos, cámara, rótulos y la historia de las materias.
- `fase-01-mapa/dibujo.js`: tarjetas, cables y efectos, con el tema Medianoche de la app.

Los datos y las letras no se copian: el render sirve el pensum
(`composeResources/files/carreras`) y las fuentes (`composeResources/font`) de la app.
