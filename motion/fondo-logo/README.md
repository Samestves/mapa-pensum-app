# Fondo con logo · bucle para Resolume

Un fondo animado de 20 s que empalma consigo mismo: el logo de Mapa de Pensum en 3D
sobre una placa de vidrio, y detrás una cuadrícula de mosaicos al estilo del historial
de contribuciones de GitHub, en el azul de la marca. Cada 5 s un pulso recorre la ruta
del mapa y, al llegar a la meta, suelta una onda que enciende la cuadrícula.

Sale a 1920 × 1280 (3:2, la pantalla LED) y 60 fps.

    npm install
    npm run ver       # sirve la escena para verla en el navegador
    npm run render    # out/mapa-pensum-fondo-logo-3x2.mp4

El render necesita `ffmpeg` y Playwright con Chromium.

- `src/tiempo.js`: el reloj del bucle (duración, pulsos).
- `src/fondo.js`: la cuadrícula, las luces y la placa de vidrio (lienzo 2D).
- `src/logo.js`: el mapa plegado en three.js, con la ruta pintada en una textura.

Todo lo que se mueve es periódico en el bucle, así que el clip se puede dejar en
bucle en Resolume sin que se note el corte.
