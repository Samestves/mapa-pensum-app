# Inicio · prototipo minimalista

Prototipo: `docs/prototipo/inicio.html` (se abre en Chrome del móvil). Sustituye a la portada inmersiva: menos piezas, más calma.

## Lo que queda

- **Cabecera:** la fecha pequeña y, debajo, el saludo según la hora en título grande («Buenas tardes», Geist 650, 32 sp), con el botón de ajustes. Sin logo.
- **Tu carrera:** una sola tarjeta con:
  - el icono, el nombre y el tipo;
  - un **anillo de avance** como los de Actividad (196 dp, trazo de 14 dp con punta redonda). Se dibuja al entrar mientras la cifra sube de 0 a 41 en el centro (Geist 250, 60 sp);
  - dos datos: «5 / 10 · Semestre» y «Dic 2029 · Te gradúas»;
  - un botón («Seguir donde ibas»).
- **Otras carreras:** un título de sección y una lista con el icono, el nombre, las materias y la flecha.
- **Barra inferior:** Inicio, Mapa, Por hacer y Horario.
- **Ficha de carrera:** el icono, el nombre, tres datos (materias, UC y semestres), «Ver el mapa» y «Hacerla mi carrera».

## Reglas

- **Fondo plano** (negro o el gris claro de iOS) y una sola superficie para tarjetas y listas.
- **El color de la carrera** va solo en su icono y en su anillo.
- **Una sola familia tipográfica:** Geist (OFL, se puede empaquetar en la app), con cifras tabulares. Los títulos usan 650 y tracking negativo, el texto 400–500 y las cifras grandes 250.
- **Una acción principal** por pantalla.
- **Movimiento mínimo:** el anillo se dibuja al entrar, las pulsaciones encogen al 97 % y las pantallas cambian con un fundido.
- **Sin** aurora, puntos, grano, desenfoques de fondo, cifras animadas ni buscador.
