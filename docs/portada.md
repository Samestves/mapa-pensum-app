# La portada · «Silueta viva»

Maqueta: `docs/maquetas/portada.html` (PNG al lado). Se recomienda **A · Silueta viva**. B (Cartera) y C (Panel) quedan como alternativas.

## Qué es y por qué

- **Es «Sencilla» crecida.** Una tarjeta con un trabajo: volver a tu carrera. Debajo, las demás carreras.
- **La silueta dice tu estado** con los colores de la app:
  - lo aprobado, en blanco;
  - lo que cursas, en contorno;
  - lo que puedes inscribir, en azul;
  - lo demás, apagado.
  Así, el dibujo propio del producto responde «¿qué toca?» sin otra tarjeta. Los métodos: jerarquía visual y un foco por bloque.
- **Una línea con ahora, próximo y tu fecha.** No hay más datos. El detalle (UC, cadena, plan) sigue en la hoja de progreso, por divulgación progresiva.
- **Un solo botón lleno por pantalla:** «Seguir donde ibas». A su lado, el atajo al horario (la web ya lo tenía).
- **Las otras carreras van en fichas de dos en dos, sin flecha.** Su bloque baja de unos 736 a 476 dp de alto y ya no compite con la tuya (Gestalt: proximidad).

## Anatomía de pie (360 a 412 dp; maqueta a 375)

Lo que no se nombra queda como está hoy: el margen de pantalla (16), la cabecera (rosa, «MAPA DE PENSUM» y el engranaje, 48 dp a `barra + 4`), el contenido desde `barra + 76`, el vidrio de la cabecera y el fundido de abajo.

### La tarjeta «Tu carrera»

Mide todo el ancho × 320 dp. Lleva radio 24 y margen interior 20 (16 en 320 dp). El fondo es el `cristalTarjeta` de hoy: tinte, luz desde arriba, borde a 148° y el filo del acento arriba, sin desenfoque.

| Bloque | Medidas | Contenido |
|---|---|---|
| Cabeza | 52 de alto | Izquierda: `nombreCorto` en Título (Jost 300, 28/32, −0,5 %, `autoSize` de 20 a 28, una línea) y, a 4 dp, el tipo en Rótulo (Inter 500, 11/16, +8 %, mayúsculas, `tenue`). Derecha: `CifraConUnidad` 40 sp (Inter 200, opsz 32, −2 %) con el «%» a 16 sp, peso 300, en `tenue` y a 2 dp, sobre la línea base. La cifra va alineada por línea base con el nombre |
| Silueta | a 20 dp, 80 de alto | Diez columnas a todo el ancho interior. Paso horizontal = (ancho − 8) / (columnas − 1): 32,8 en 375. Punto de 8 y paso vertical de 12. Con más de 7 filas (Agronómica tiene 9), el paso vertical es 72 / (filas − 1) y la tarjeta no crece |
| Dato | a 16 dp, 44 de alto | Izquierda: dos renglones de Nota (Inter 13/16, `tnum`), 4 dp entre ellos. Cada renglón lleva un glifo de 8 dp, el mismo punto de la silueta, a 8 del texto: «◯ **2** cursando» y «● **6** por inscribir». La cifra va en 500 y `tinta`; el resto, en `tenue`. Si una cifra es 0, su renglón no va. Derecha: «Dic 2029» en Fecha (Inter 300, 24/28, opsz 32, −1,5 %) y debajo «Te gradúas» en Nota, todo alineado a la derecha |
| Acción | a 16 dp, 52 de alto | `BotonTinta` (52, radio 26): «Seguir donde ibas» en Inter 500 15/20 y flecha Lucide de 18 a 8 dp. Ocupa lo que quede. A 8 dp, un círculo de 52 de superficie (tinta al 8 %, filo al 6 %) con `calendar-days` de 20. Su `contentDescription` es «Tu horario» y abre la carrera en Horario |

La suma: 20 + 52 + 20 + 80 + 16 + 44 + 16 + 52 + 20 = **320 dp**.

### Las otras carreras

- **Posición:** empiezan 24 dp debajo de la tarjeta.
- **Rótulo de grupo:** «INGENIERÍAS», «LICENCIATURAS» o «GERENCIA», con 4 dp de sangría. Su rejilla va 8 dp debajo, y entre grupos hay 24.
- **Rejilla:** dos columnas con hueco de 8. Si una ficha queda suelta, deja su sitio vacío (como ya pasa acostado).
- **Ficha** (la `FilaCompacta` de hoy, ahora también de pie):
  - 68 dp de alto, radio 16;
  - fondo de tinta al 3,5 % y filo al 6,5 %;
  - silueta de 36 × 28 (punto de 2, color de la carrera al 72 %);
  - a 12 dp, el nombre en Encabezado (Jost 300 17/22; 400 en claro), con `autoSize` de 13 a 17 y hasta 2 líneas;
  - 14 dp a la izquierda y 10 a la derecha;
  - sin flecha: toda la ficha se toca.

## Anatomía acostada (915 × 412)

- **La tarjeta** ocupa todo el alto. Su ancho es el de hoy: el 42 % del ancho útil, entre 300 y 380 dp (354 en 915). Lleva radio 28 y margen 24.
- **Dentro de la tarjeta:**
  - la cabeza, igual que de pie;
  - debajo, la silueta (punto 7, paso 19 × 11, 73 de alto) a la izquierda y la fecha abajo a la derecha, en la misma franja;
  - la leyenda en una sola línea, con 16 dp entre los dos datos;
  - el botón y el círculo.
  Los bloques se reparten el alto con `SpaceBetween`.
- **A la derecha,** `OtrasCompactas` como hoy.

## Color

Todo sale de `Paleta`; el claro solo cambia el juego de tokens.

| Pieza | Token |
|---|---|
| Punto aprobado | `tinta` |
| Punto cursando | contorno de 1,5 dp en `tinta`, sin relleno |
| Punto inscribible | `azul` |
| Punto pendiente | `tinta` al 16 % |
| Filo de la tarjeta | `acentoDe(colorDe(carrera.color))`, como hoy. Es el único sitio del color de tu carrera |
| Botón | fondo `tinta`, texto `panel` (como `BotonTinta`) |
| Círculo del horario | `tinta` al 8 %, filo al 6 %, icono en `tinta` |
| Nombre, cifra y fecha | `tinta` (la unidad «%», en `tenue`) |
| Notas y rótulos | `tenue`; las cifras de la nota en 500 y `tinta` |

- **Claro:**
  - la tarjeta es blanco al 78 % con la sombra corrida de 2 dp (ya está así);
  - el botón queda en tinta oscura;
  - el azul de lo inscribible pasa a `#3E6AE1`;
  - el filo de Sistemas, a `#2751CC`.
- **Ámbar:** no aparece en ningún sitio.
- **Choque que se evita:** el color de Sistemas (`#6094FF`) es casi el azul de lo inscribible. Por eso el acento ya no pinta los puntos ni el chip; queda solo en el filo.

## Estados

| Estado | Qué cambia |
|---|---|
| Con avance | Lo descrito arriba |
| Sin marcas | Se ven el nombre y el tipo. No hay cifra ni fecha (no hay nada que contar ni estimar). La silueta enseña en azul las que no tienen prelaciones (11 en Sistemas). En el Dato va el Cuerpo «Marca lo que ya aprobaste», abajo. El botón dice «Empezar» |
| Sin carrera elegida | El título es «Elige tu carrera», sin tipo, cifra ni filo. Una silueta fantasma de contorno punteado (el «por elegir» de la lista; tinta al 50 %, trazo 1,2). El Cuerpo dice «Aquí verás tu avance». El botón «Elegir carrera» abre `PaginaAjustes.CARRERA`. Sin círculo de horario. Debajo, las nueve |
| Cargando | La silueta va toda pendiente. La cifra, el dato y la fecha guardan su alto vacío: sin esqueletos ni brillos que finjan un dato. El botón ya funciona |
| Sin fecha | Si `periodoFinal` es null, no va el bloque de la fecha y el dato se queda a la izquierda |

## Movimiento

Todo usa la línea de tiempo única de hoy (`FIN_ENTRADA` = 1800 ms) y se lee al dibujar. Solo corre la primera vez por sesión (`EntradaPortada`).

- **Tarjeta:** `sube(80, 14.dp, 560)`, con la curva SALIDA (0,16, 1, 0,3, 1), como hoy.
- **Puntos aprobados:** se encienden por columnas, desde 380 + col × 70 + fila × 12 ms, en 320 ms con la curva ENFATICA (0,2, 0, 0, 1), como hoy.
- **Puntos de lo que cursas:** su contorno se dibuja con un barrido de 0 a 360° en 320 ms cuando llega su columna.
- **Puntos inscribibles:** aparecen después de todos los aprobados, desde 1080 ms. Cada uno es un fundido de alfa de 280 ms, escalonado 40 ms. Sin escala ni halo. Terminan a los 1600 ms, dentro de la línea.
- **Cifra:** cuenta hasta 41 desde los 380 ms, en 900 ms con ENFATICA, como hoy.
- **Dato y fecha:** un fundido de 240 ms al terminar la cifra, a los 1280 ms.
- **Toque:**
  - tarjeta, `pulsable` 0,985;
  - botón, 0,97;
  - círculo, 0,9;
  - fichas, 0,98.
  Al abrir, el mapa nace desde el origen medido, como hoy.
- **Con «Reducir movimiento» o en modo ligero:** nada escalonado ni cuenta; todo entra con un fundido de 200 ms.
- **Sin bucles.** Lo que cursas no late.

## Rendimiento (gama baja)

- **Tarjeta:** un `drawBehind` y nada de desenfoque. Detrás no hay nada nítido que borrar, solo la luz.
- **Silueta:** un solo `Canvas` de unos 50 círculos que lee el tiempo en la fase de dibujo. No recompone por cuadro.
- **Fichas:** un `roundRect` y un borde cada una.
- **Página más corta:** con fichas de dos en dos, las otras carreras miden 260 dp menos. Aunque la tarjeta crece de 152 a 320, la página de pie queda unos 100 dp más corta, así que hay menos que componer y desplazar.
- **Cálculo:** la fecha cuesta una simulación (`estimarGraduacion`; la hoja hace 8). Corre en `Dispatchers.Default` dentro de `rememberResumenes`, solo para las carreras con marcas, y se guarda en `UltimosResumenes`: al volver de una carrera no parpadea.
- **Vidrio de la cabecera:** igual que hoy. Usa Haze solo con la página desplazada, y en modo ligero pasa a tinte liso.

## Qué cambia en el código

1. **`logica/Avance.kt`**
   - `ResumenPortada` gana tres campos:
     - `cursandoPorSemestre: List<Int>`;
     - `disponiblesPorSemestre: List<Int>`;
     - `graduacion: Periodo?`.
   - Las dos listas cuentan sobre `pensum.obligatorias` y los `estados` que ya calcula `resumenParaPortada`. Usan el mismo reparto por semestre que `aprobadasPorSemestre`, así que encajan con las columnas de la silueta.
   - La cifra de «por inscribir» sigue siendo `disponibles`, la misma que dentro. Si hay electivas inscribibles fuera de la silueta, puede tener más que puntos azules.
   - `graduacion` es `estimarGraduacion(pensum, marcas, casillas, carga).periodoFinal`, con la carga guardada (`Guardado.carga(slug)`). Sin carga, toma la habitual. Es la misma cuenta que `EstadoCarrera.graduacion`, así que la portada y la hoja dicen la misma fecha.
2. **`datos/Carreras.kt`**
   - `rememberResumenes` pasa la carga a `resumenParaPortada`.
3. **`componentes/Silueta.kt`**
   - Nuevo parámetro opcional `porEstado: Pair<List<Int>, List<Int>>?` (cursando y disponibles por columna).
   - Con él, cada columna se dibuja de arriba abajo en este orden: aprobadas (color), cursando (contorno), disponibles (`Paleta.azul`) y el resto (apagado).
   - `encendido` recibe también el estado, para la coreografía de arriba.
   - Sin el parámetro dibuja como hoy, así que los demás usos no cambian.
4. **`portada/TuCarrera.kt`**
   - Las dos ramas, de pie y `lleno`, pasan a la anatomía de arriba.
   - `Chip` se va. En su lugar entran `BotonTinta` (de `carrera/EstiloAvance.kt`, con la flecha) y el círculo del horario.
   - El dato de dos renglones sustituye a `Dato`.
   - Se quitan `ANCHO_CIFRA` y la cifra de 56 sp.
   - El color de la silueta pasa de `acento` a `Paleta.tinta`.
5. **`portada/Filas.kt` y `portada/Portada.kt`**
   - De pie, los grupos usan una rejilla de dos columnas con `FilaCompacta`. Es la misma lógica de `OtrasCompactas`, sin su propio desplazamiento, dentro de la `Column` de la página.
   - `Fila` (72 dp con flecha) se puede borrar.
6. **Navegación**
   - `alAbrir` acepta una vista inicial: `(Carrera, TransformOrigin, Vista?)`. El círculo abre en `Vista.HORARIO`.
   - Opcional, para que «donde ibas» sea literal: guardar la última vista por carrera (`Guardado.vista(slug)`). Hoy `Interior` empieza siempre en el mapa.

## Choques con lo decidido (se dicen)

- **«62 / 153 UC» sale de la tarjeta.** El §9 de `reglas-diseno.md` lo puso bajo el 41 %. Aquí lo explica la silueta y el detalle vive en la hoja. Si se quiere, cabe bajo la cifra con 12 dp más de alto.
- **El botón de la portada pasa a lleno.** El comentario de `Chip` reservaba el lleno a la bienvenida. `equipo.md` ya admite «un solo botón lleno en tinta por pantalla», y este es el de la portada.
- **El acento de la carrera deja la silueta y el chip.** Es por la regla del color como estado: Sistemas (`#6094FF`) choca con el azul de lo inscribible y Agronómica (`#3FCB74`) con el verde de aprobada. El acento se queda en el filo de tu tarjeta y en las siluetas de las demás, que no llevan estados.
