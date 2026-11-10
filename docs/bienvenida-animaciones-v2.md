# Bienvenida v2: las animaciones de las pantallas 2 y 3

Sam pidió una animación más bonita para la segunda y la tercera pantalla de la
bienvenida. Tal como las ve el usuario, son estas:

| Pantalla | Código | Título |
|---|---|---|
| 1 | `PasoMapa` (paso 0) | Marca lo aprobado… |
| **2** | **`PasoHorario` (paso 1)** | Una foto al horario, / *tu semana lista.* |
| **3** | **`PasoAvance` (paso 2)** | Cuánto llevas, / *cuándo te gradúas.* |
| 4 | `PasoCarrera` (paso 3) | Elegir tu carrera |

El texto, el botón, la barra de pasos y «Saltar» no cambian. Lo que cambia es la
escena: ahora enseña las piezas de la app de hoy en vez de versiones viejas. La
pantalla 2 tenía la tarjeta «En curso» de cristal, que ya no existe. La 3 tenía
la silueta de puntos verdes con un riel y un birrete, y hoy la app usa la barra
de cápsulas y la fecha.

## Reglas comunes

- **Un solo reloj por escena.** Ciclo de 10 s. Se lee en el dibujo, así que
  no se recompone nada por cuadro. Al final del ciclo, todo se apaga a la vez
  en 700 ms (`Movimiento.ESTANDAR`) y la siguiente vuelta empieza de cero.
- **Toma continua** (`TomaEnBucle`, Hermite monótona):
  - inclinación de 1° a 12° y giro de menos de 1,5°;
  - zoom entre 0,98 y 1,12;
  - perspectiva de 1100;
  - la vuelta no tiene costura.
- **Curvas.**
  - Entradas: `ASIENTA` (sale rápido y se asienta despacio), 500 a 600 ms, con
    8 dp de subida.
  - Cambios en su sitio: estándar `(0.4, 0, 0.2, 1)`.
  - Sin rebotes.
- **Sin ninguna capa Offscreen ni desenfoque, y nada se crea por cuadro.**
  - Los textos se miden una vez.
  - Los pinceles se crean una vez por tema (la estela de la lectura se mueve
    con `translate`, no se rehace).
- **Modo ligero** (`Tema.ligero`):
  - la escena se ve de frente, sin toma ni fundido de bordes (`EscenaEnBucle(fija = …)`);
  - así no se abre la capa del fundido, que es lo único caro;
  - la historia se cuenta entera;
  - sin latidos ni respiración (`Tema.sinBucles`).
- **Reducir movimiento** (`Movimiento.reducido`) o las animaciones del sistema
  apagadas: la escena se queda quieta en su mejor cuadro (`QUIETO`).
- **Al girar:**
  - el reloj y los textos viven por encima de la bifurcación de pie o
    acostado, así que la escena sigue donde iba;
  - el título no repite su entrada (`rememberEntrada`, sin tocar).
- **Acostado:** la misma escena, a la izquierda (peso 1,5). El plano de
  390 × 468 se encoge a la par si no cabe.

---

## Pantalla 2: «Una foto al horario, tu semana lista»

### Elegida: «De la captura a tu semana»

Es el flujo real de «Leer horario» en 10 s: la captura se lee, cada clase se
vuelve un punto, vuela a su día de la semana en miniatura y florece en su
bloque. Arriba se escribe la cabecera del horario en curso, tal como está en
`Horario.kt`.

| ms | Qué pasa | Duración y curva |
|---|---|---|
| 150 | Entra la captura: la tarjeta de «Leer horario» (panel, radio 14, filo de tinta al 7 %). Es una tabla de la intranet: título, columnas y 7 filas. Entra con 2,5° de giro, hundida 24 dp y al 96 %, y se endereza. | 600 ASIENTA; se endereza en 1000 |
| 1000–2500 | La línea de lectura de la app (un hilo de tinta con estela) baja a paso constante. Cada fila que pasa gana la marca de su materia (2,5 × 12, en su color) y se aclara del 60 al 100 %. | 1500 lineal; 240 por fila |
| 2650 | Las letras se van. Cada día de cada fila («LU», «MI») se condensa en un cuadrito de 9 dp del color de su materia. | 300 / 260 ASIENTA |
| 2750–3350 | La captura se retira: sube 12 dp, se encoge un 6 % y se apaga. | 600 |
| 2900 | Aparece la semana en miniatura (Semanita): LUN…VIE y sus cajas, con la tinta al 4 %. | 600 |
| 2950–3975 | Los 10 puntos vuelan, cada uno a su día y su hora, en arco y escalonados 45 ms. Los arcos van alternos a cada lado (0, ±12…±30 dp) y ninguna ruta pisa a otra. | 620 c/u, `(0.65, 0, 0.35, 1)` |
| 3850–4510 | Florecen, día a día (70 ms entre días): cada punto crece hasta su bloque de MiniDia, con su color al 58 % y radio 3. | 380 estándar |
| 4500 | Ahora:<br>• la lente cae sobre LUN y LUN pasa a tinta;<br>• aparece el punto rojo de hoy;<br>• entran «SEMANA» y «10 clases · 20 h · 7 AM – 3 PM»;<br>• la línea roja de ahora (10:40) se traza sobre el lunes;<br>• Estadística II (ya pasó) baja al 30 %. | 420 / 560 / 500 |
| 5000–5260 | Se escribe la cabecera, línea a línea y escalonada 70 ms:<br>• **EN CURSO** con su punto rosa;<br>• **Sistemas de Costo** (Jost Light 40);<br>• «Quedan **1 h 20 min** · Aula 5»;<br>• el trayecto de 10:00 AM a 12:00 PM.<br>El bloque del lunes se enciende un momento: de ahí sale lo que se escribe. | 560 ASIENTA c/u |
| 5650 | El trayecto avanza hasta las 10:40 y aparece su cabeza. | 900 estándar / 300 ASIENTA |
| 5600–9300 | **Queda vivo:** el punto de EN CURSO late como PuntoQueLate (2600 ms: el aro crece 2,4× y se apaga en el 70 % del ciclo). Lo demás queda quieto. | — |
| 9300 | Todo se apaga y vuelve a empezar. | 700 |

**Toma:**

1. Cerca de la captura para leerla (zoom de 1,07 a 1,10).
2. Baja con los puntos hasta la semana (y = 232).
3. Retrocede a 0,98 mientras florecen.
4. Sube a la cabecera, en 1,00.

**Sin animación:** a los 8200 ms se ve la cabecera escrita y la semana hecha.

**Por qué:**

- Muestra el producto en acción, no una ilustración: la tarjeta de captura, la
  línea de lectura, el viaje en puntos (VueloHorario), Semanita y la cabecera
  son las piezas reales.
- Tiene un solo héroe: el vuelo de los diez puntos.
- Termina en lo que el estudiante ve cada día al abrir el horario.

### Alternativa: «Tu día, de cerca»

La toma empieza pegada a la línea de tiempo del día (7:00 Estadística II,
«Libre · 1 h», 10:00 Sistemas de Costo con su riel llenándose) y se aleja hasta
la semana entera, mientras la captura se desvanece al fondo. Es más tranquila,
pero vende menos el «una foto basta».

---

## Pantalla 3: «Cuánto llevas, cuándo te gradúas»

### Elegida: «La tarjeta que se cuenta sola»

Es la tarjeta de arriba de la hoja «Tu avance», tal cual (superficie de tinta
al 5 %, radio 24, relleno 16). Muestra a un estudiante de Sistemas en quinto
semestre: los cuatro primeros aprobados y dos materias en curso. Todo sale de
la lógica de la app: `capsulasDe` da la barra, `resumenParaPortada` da el % y
las UC, y `estimarGraduacion` da la fecha, que se calcula con el período de hoy.

| ms | Qué pasa | Duración y curva |
|---|---|---|
| 50–650 | Aparece la superficie. | 600 ASIENTA |
| 150–800 | Nace la barra de cápsulas, de izquierda a derecha: una por materia, ancha según sus UC, con 2 dp entre materias y 7 entre semestres. Cada una crece del 40 al 100 % de su alto, en la tinta pendiente (15 %). | 360 por cápsula |
| 350–1050 | El hilo de la regla se traza. Las marcas y los números de cada semestre aparecen con 50 ms de escalón. | 700 / 400 |
| 900 | Entran la cifra y lo aprobado: **0 %** (Inter Display 200, de 64, con su % de 26 en la línea base) y «0 / 153 UC». | 500 ASIENTA |
| 1100–3680 | Lo aprobado se llena de tinta, una ola de cápsula en cápsula. La cifra y las UC cuentan las UC reales llenadas, hasta 41 % y 62. Cada semestre que termina pasa a tinta en la regla, y el círculo del semestre actual avanza 1 → 2 → 3 → 4 → 5; el número que cubre se ve en el color del panel. | 280 por cápsula; 420 por paso del círculo |
| 3650 | Las dos materias que cursas pasan de gris a contorno de tinta (1,5 dp), con 90 ms de escalón. | 420 estándar |
| 4050 | Arriba a la derecha aparecen «Te gradúas» y la fecha de hoy (p. ej. «Dic 2026»). | 550 ASIENTA |
| 4300–7000 | Una luz recorre lo que falta a paso constante: cada cápsula se aclara (+42 %) cuando pasa y vuelve a su gris. A la vez, la fecha pasa período a período: como un contador, cada fecha pasa por su propia ventana: la que se va sube 24 dp y sale, y la que llega entra desde abajo. Termina en el período del grado. | 2700 lineal; cada cambio 300 |
| 7000 | La fecha final se asienta en tinta (las intermedias van en suave). | 400 estándar |
| 4070–9300 | **Queda vivo:** el contorno de lo que cursas respira (70–100 %, cada 2600 ms, el ritmo del punto «En curso»). Arranca en su máximo, así que no salta. | — |
| 9300 | Todo se apaga y vuelve a empezar. | 700 |

**Toma:**

1. Empieza un poco más cerca (1,12, inclinada 10°), con la tarjeta entera dentro, mientras la barra nace y se llena.
2. Se aleja a 1,04 para ver la cifra entera.
3. Se va a la derecha con la luz hasta la fecha (1,08).
4. Se asienta.

**Sin animación:** a los 8400 ms se ve la tarjeta entera: 41 %, 62 / 153 UC,
la fecha asentada, la barra y el 5 marcado.

**Por qué:**

- Es la misma tarjeta que el estudiante abre en la hoja de progreso, y la
  misma cifra con su barra fina que ve en el home: no hay un idioma nuevo.
- La ola de cápsulas explica de dónde sale el 41 % (las UC), y la fecha que
  corre explica que «cuándo te gradúas» es una cuenta, no un adorno.
- No repite la idea de la pantalla 1: allí se marca; aquí se lee el resultado.

### Alternativa: «El anillo del grado»

El anillo de «¿Cuándo me gradúo?» se traza marca a marca en sentido horario:
lo aprobado en verde hasta HOY, lo que cursas en tinta y el plan apagándose
hasta la META. El birrete queda arriba, el % al centro y la fecha cae al
final. Es más espectacular, pero es una pantalla interna que el estudiante ve
menos que la tarjeta.

---

## El fondo: lienzo liso, sin aurora (decisión de Sam)

- La bienvenida ya no lleva la aurora viva (las luces azul, turquesa e
  índigo) ni el grano. El fondo es `Paleta.lienzo`, el del tema, en oscuro y
  en claro.
- Sin la luz cenital del home: así el fundido de los bordes de cada escena
  puede ser un velo del propio lienzo pintado encima (`fundidoBordes`), sin
  `saveLayer` por cuadro. Lo de dentro se recorta a su zona con un `clipRect`
  (no es una capa), para que la toma no pise el título.
- Se quitaron de `componentes/Comunes.kt` `aurora()`, `auroraViva`,
  `rememberRelojAurora`, `RelojAurora` y `grano()`, que solo usaba la
  bienvenida.
- Prueba: `FondoBienvenidaTest` (los cuatro pasos en oscuro, más horario y
  avance en claro).

## Archivos

- `bienvenida/PasoHorario.kt`: la escena nueva de la pantalla 2.
- `bienvenida/PasoAvance.kt`: la escena nueva de la pantalla 3. Ahora usa
  `EscenaEnBucle`, como las demás, y deja de anclarse al título, lo que
  cortaba «TE GRADÚAS» al acostar el teléfono.
- `bienvenida/TomaEnBucle.kt`:
  - `rememberRelojEscena(…, parado)` se queda quieto con «Reducir movimiento»
    o mientras faltan datos;
  - `EscenaEnBucle(…, fija)` es el modo ligero: sin toma ni fundido.

  La pantalla 1 no cambia: los dos parámetros tienen valores por defecto.
- `bienvenida/Linea.kt`: `mezclarColor` (pasa aquí desde PasoAvance, porque
  la usa también PasoCarrera).
- Pruebas: `androidUnitTest/.../bienvenida/TiraBienvenidaTest.kt`, con tres
  clases:
  - las tiras de pie (9 instantes) y acostadas (3);
  - un ciclo a 30 fps para el vídeo, que solo corre con `TIRA_VIDEO=1`.
