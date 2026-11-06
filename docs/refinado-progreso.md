# Progreso refinado: especificación para implementar

La maqueta es `docs/maquetas/progreso-refinado.html`. Esta especificación traduce esa maqueta a tokens de la app (dp/sp).
Tiene la misma estructura que `progreso-inteligente.html`; lo que cambia es cómo se ve.

## Tipografía por rol

| Rol | Fuente | Peso | Tamaño / interlínea | Tracking | Uso |
|---|---|---|---|---|---|
| Cifra héroe | Inter, `opsz` 32, `tnum` | 200 | 64 / 64 | −2 % | el % de la tarjeta |
| Cifra | Inter, `opsz` 32, `tnum` | 200 | 40 / 44 | −2 % | cifras de las pantallas internas (12 %, 3 materias, 0) |
| Cifra media / fecha | Inter, `opsz` 32, `tnum` | 300 | 24 / 28 | −1,5 % | Dic 2039, comparaciones |
| Cifra de fila | Inter, `tnum` | 300 | 20 / 24 | −1 % | «0 /15 UC», «4 UC», el número de cada fila |
| Título | Jost | 300 | 28 / 32 | −0,5 % | nombre de la carrera y títulos de las pantallas internas |
| Fila | Inter | 500 | 15 / 20 | −0,9 % | título de cada fila y botones |
| Cuerpo | Inter | 400 | 15 / 20 | −0,9 % | frase de la tarjeta y subtítulo de las pantallas internas (color suave) |
| Nota | Inter, `tnum` | 400 | 13 / 16 | −0,3 % | subtítulos de fila (color tenue) |
| Rótulo | Inter, mayúsculas | 500 | 11 / 16 | +8 % | TE CONVIENE, ELECTIVAS… (color tenue) |
| Código | IBM Plex Mono | 300 | 11 / 16 | — | códigos de materia |

Las cifras de 24 sp en adelante usan `opsz` 32 («Inter Display»). El `inter.ttf` de la app trae los ejes `opsz` 14–32 y `wght` 100–900.
En Compose: `Font(Res.font.inter, FontWeight(200), variationSettings = FontVariation.Settings(FontVariation.weight(200), FontVariation.Setting("opsz", 32f)))`.

## Regla del % y de las unidades, para toda la app

- El % y las unidades (UC) se apoyan en la **línea base** de la cifra. Nunca van arriba, como exponente.
- **Tamaño:** el 40 % de la cifra, redondeado y nunca menos de 11 sp. Para 64 → 26, para 40 → 16, para 24 o menos → 11.
- **Peso:** uno más que la cifra (200 → 300). **Color:** tenue. **Separación:** 2 dp, sin espacio («8%»).
- **En texto corrido** (≤ 17 sp), el % va igual que el número: «12%».
- **Un solo componente** (`CifraConUnidad`) sustituye a los cinco sitios que hoy lo hacen a mano: `TuCarrera.kt:309`, `AnilloGraduacion.kt:342`, `AvanceCarrera.kt:533`, `PasoAvance.kt:200` y `BarraCarrera.kt:207`. Este último es el anillo de la barra; ahí basta con revisar que el % siga en la línea base.

## Color por rol (tokens y opacidad de la tinta)

| Rol | Exterior / claros | Oscuro |
|---|---|---|
| Superficie de tarjeta | tinta 4,5 % | tinta 5 % |
| Hilo separador | tinta 12 % | tinta 9 % |
| Fondo de pastilla | tinta 7 % | tinta 8 % |
| Pendiente (cápsulas, ranuras) | tinta 13 % | tinta 15 % |
| Pastilla azul | azul 10 % | azul 16 % |
| Chevron e icono apagado | apagada | apagada |

- **Cursando:** tinta, con peso 500 en el texto; en las piezas, contorno.
- **Inscribible:** `azul`.
- **Aprobada:** `tinta` en la huella y `verde` solo en los iconos de estado.

## Espacios y radios

- Ritmo vertical de 4 dp.
- **Márgenes:** de la hoja, 20 (Espacio.l); interior de la tarjeta, 16 (m).
- **Separaciones verticales:**
  - entre la cabecera y la tarjeta, 20;
  - entre la tarjeta y la lista, 16;
  - entre secciones, 24 (xl), con 8 (xs) del rótulo a su lista.
- **Radios concéntricos:**
  - tarjeta 24 (Radio.Grande) = pastilla 8 + margen 16;
  - chips del ritmo 8, de 36 dp de alto;
  - botón 52, con radio 26 (Radio.Barra);
  - hoja 28 (Radio.Hoja), arriba;
  - círculos de atrás y cerrar, 36.
- **Fila:**
  - alto mínimo 64 (48 en la versión corta), con 16 a los lados;
  - entre la pastilla y el texto, 12;
  - el separador empieza en 60 (alineado con el texto).

## Iconos

- Set lineal Lucide de 18 dp dentro de una pastilla de 32 × 32 con radio 8.
- Trazo visual de 1,5 dp, con puntas y uniones redondas.
- Chevrones de 16 dp, en `apagada`.
- **Estado:** va en el color del icono, nunca en el fondo. La excepción es la pastilla azul de «Tu próximo semestre».
- **Asignación:**
  - Este semestre → libro abierto;
  - Próximo → calendario;
  - No puedes atrasar → eslabón;
  - ¿Cuándo me gradúo? → birrete;
  - Técnicas → llave inglesa;
  - Sociohumanísticas → personas;
  - casilla vacía → «+» en una pastilla con contorno.

## «Cursando» sin ámbar (toda la app)

- El ámbar se elimina de la app. Su lugar lo toma un token semántico, `Paleta.cursando`, que vale igual que la `tinta`.
- «Cursando» se distingue por la **forma**, no por el color:
  - en barras y piezas, **contorno** de 1,5 dp en tinta, sin relleno;
  - en el texto «Cursando», tinta con peso 500;
  - el icono del libro, en tinta;
  - la tarjeta del mapa, con borde de 1,5 dp en tinta.
- Los colores quedan solo para los estados que piden actuar o avisar: verde aprobada, azul inscribible, rojo bloqueada.
- La maqueta es `docs/maquetas/cursando-y-barra.html`.

## Barra de la carrera: cápsulas

- **Una cápsula por materia**, en orden de semestre. Dentro de cada semestre: aprobadas, cursando y pendientes, cada grupo en el orden del pensum. Las casillas de electiva sin elegir también cuentan.
- **Medidas:**
  - ancho proporcional a sus UC;
  - todas del mismo alto, 14 dp, con radio 2,5;
  - huecos de 2 dp entre materias y 7 dp entre semestres.
- **Color por estado:**
  - aprobada, rellena de tinta;
  - cursando, contorno de 1,5 dp en tinta;
  - el resto, tinta al 13 % en claro y al 15 % en oscuro.
- No hay azul ni alturas distintas.
- **Regla de semestres**, 6 dp por debajo de la barra:
  - un hilo de 1 dp a todo el ancho;
  - una marca de 4 dp en el centro de cada semestre;
  - el número 15 dp más abajo, en Jost 11 sp: el actual va dentro de un círculo de 17 dp en tinta, con el número en color panel y peso 500; los completos, en tinta y peso 500; los que vienen, en apagada y peso 400.
- **El semestre actual** es el primero con materias en curso o pendientes.
- **Alto total:** 14 + 32 dp.

## Tarjeta de arriba: dónde va cada cosa

- **A la izquierda,** la cifra héroe con su % abajo y, debajo, «13 / 153 UC».
- **A la derecha, en la esquina de arriba,** «Dic 2039» (Cifra media), con su tope alineado con el tope de la cifra. Debajo, «Te gradúas» (Nota, tenue).
- **Debajo de las dos,** la barra de cápsulas y, al final, la frase.

## Cambios de texto (antes → después)

**Filas de la hoja**
- «Lo que cursas y qué te abre» → se mantiene.
- «Ene – Jun 2027 · qué inscribir» → «Qué inscribir · Ene – Jun 2027».
- «La cadena que fija tu fecha» → «Las que fijan tu fecha».
- «A 2 por semestre · a 6, Dic 2031» → «Lo más rápido: Dic 2031».

**Electivas**
- «Ya puedes cursar 4» → «Ya puedes cursar 4» (sin cambios).
- Las electivas técnicas pasan a llevar el icono de llave inglesa.

**Tarjeta**
- «TE GRADÚAS» arriba de la fecha → «Te gradúas», en Nota, debajo de la fecha.

**Pantallas internas, subtítulos**
- Este semestre → «Qué te abre aprobarlas».
- Próximo semestre → «Para graduarte en Dic 2039».
- No puedes atrasar → «7 materias encadenadas».
- ¿Cuándo me gradúo? → «Depende de tu ritmo».
- Electivas → «Tú eliges cuáles».

**Este semestre:** el párrafo de reprobar pasa a ser una fila: «Tu fecha no cambia · Ninguna está en tu cadena».

**No puedes atrasar:** la consecuencia pasa a una comparación: «A tiempo: Dic 2039 | Si atrasas una: Jun 2040 · 6 meses después».
