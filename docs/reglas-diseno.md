# Reglas de diseño

Una sola forma de escribir y componer en toda la app: portada, bienvenida, lista, horario y hoja de progreso.
Completa a `sistema-diseno.md` (tokens y responsive) y a `refinado-progreso.md` (la hoja); si chocan, manda esta.
La maqueta de las dos pantallas nuevas es `docs/maquetas/proximo-y-cadena.html` (PNG al lado).

## Las cinco que mandan

1. **Un título por pantalla, sin subtítulo.** Solo lleva subtítulo si dice un dato (cifra, fecha, nombre) que no está ni en el título ni en el primer bloque de debajo.
2. **Siete roles de texto y cuatro de cifra.** Jost es la voz de la marca: titulares, títulos y encabezados. Inter es todo lo que se lee: filas, notas, rótulos y cifras. Plex Mono, solo para los códigos. Un único rótulo en mayúsculas.
3. **Cada palabra trae un dato.** Una nota tiene 40 caracteres como mucho y nunca repite lo que ya dicen el icono, el color o la cifra. Ejemplo: «Puedes inscribirla» junto al candado azul sobra.
4. **El color es estado, nada más.**
   - Azul: lo inscribible.
   - Verde: aprobada, solo en iconos.
   - Rojo: bloqueada o una consecuencia («Jun 2030»).
   - «Cursando» se marca con la forma (contorno) y no tiene color. Ámbar, nunca.
5. **Respuesta arriba, mapa al final.** Cada pantalla interna abre con su respuesta en una tarjeta (cifra, fecha y nada más) y cierra con el camino al mapa («Verla en el mapa», con su miniatura).

## 1. Tipografía: roles

Hoy hay unos 25 estilos sueltos: `estiloRotulo`, `versal`, `etiqueta`, `letrasHorario()` (con 12 propios), los 9,5 / 12,5 / 15,5 sp de ficha y horario… Pasan a ser estos roles, en un solo objeto (`componentes/Texto.kt`, hoy de 26 líneas).

| Rol | Fuente | Peso | Tamaño / interlínea (sp) | Tracking | Dónde |
|---|---|---|---|---|---|
| **Titular** | Jost | 300 | 40 / 44, `autoSize` hasta 28 | −2 % | bienvenida, el titular del horario, «Arma tu semana» |
| **Título** | Jost | 300 | 28 / 32 (acostado 24 / 28) | −0,5 % | título de cada hoja y pantalla interna, nombre de la carrera en «Tu carrera» |
| **Encabezado** | Jost | 300 oscuro / 400 claro | 17 / 22 | −1 % | grupos que se tocan: «Semestre 4», mosaicos de carreras |
| **Fila** | Inter | 500 | 15 / 20 | −0,9 % | nombre de materia en cualquier fila, botones y opciones |
| **Cuerpo** | Inter | 400 | 15 / 20 | −0,9 % | la única frase de una tarjeta (color `suave`) |
| **Nota** | Inter `tnum` | 400 (500 para resaltar) | 13 / 16 | −0,3 % | segunda línea de una fila (color `tenue`) |
| **Rótulo** | Inter, mayúsculas, `tnum` | 500 | 11 / 16 | +8 % | encabezado de sección, columnas (color `tenue`) |
| Código | IBM Plex Mono | 300 | 11 / 16 | 0 | códigos de materia; nada más |
| **Cifra héroe** | Inter `opsz` 32 | 200 | 64 / 64 | −2 % | el % de la hoja |
| **Cifra** | Inter `opsz` 32 | 200 | 40 / 44 | −2 % | la respuesta de una pantalla interna (6 materias, 41 %) |
| **Fecha** | Inter `opsz` 32 | 300 | 24 / 28 | −1,5 % | fechas («Dic 2029») y comparaciones |
| **Cifra de fila** | Inter `opsz` 20 | 300 | 20 / 24 | −1 % | el número a la derecha de una fila («3 UC», «6») |

- **Mínimo, 11 sp.** Desaparecen los 8,5 (meridiano del horario), 9,5 («CAMBIAR ESTA ELECTIVA», ficha) y 12,5 sp (dato del horario). Pasan a Nota o a Rótulo. Excepción: los dibujos con letra dentro, como el mapa, las escenas de la bienvenida y el anillo.
- **Las unidades** siguen la regla de `CifraConUnidad`: van en la línea base, al 40 % de la cifra (11 como mínimo), un peso más y en tenue.
- **Excepciones de marca:**
  - la placa «MAPA DE PENSUM» (Jost espaciado);
  - la cabecera de semestre del mapa («SEMESTRE 05», Jost), porque forma parte del dibujo del mapa.

  Fuera del mapa, los semestres van sin cero delante: «Semestre 5».

## 2. Títulos y subtítulos

- **Título:** uno por pantalla y como pregunta o lugar («¿Cuándo me gradúo?», «Ene – Jun 2027»). Nunca una instrucción larga.
- **Rótulo sobre el título:** solo si el título, solo, no dice dónde estás. «TU AVANCE» sobre «Ingeniería de Sistemas» sí lo hacía. «MAPA DE PENSUM» sobre «Ajustes y ayuda», no.
- **Subtítulo:** solo con un dato nuevo. Si el bloque de debajo ya lo dice (la cifra, la fecha, el dominó), se quita. Las cinco pantallas internas de la hoja lo pierden (ver §9).
- **Rótulo de sección:** de una a tres palabras, en mayúsculas, y si hace falta una cifra a la derecha («TE ABREN CAMINO · · · 11 UC»). Nunca una frase.
- **Titular de dos líneas** (bienvenida): 22 caracteres por línea como mucho. La segunda va en azul y dice el beneficio.

## 3. Economía de texto

| Pieza | Largo máximo | Regla |
|---|---|---|
| Título | 24 caracteres | si no cabe, abrevia el nombre, no la letra |
| Nota de fila | 40 caracteres | solo con dato: cifra, fecha, nombre de otra materia |
| Frase de tarjeta (Cuerpo) | 2 líneas | una por tarjeta; las cifras en 500 tinta |
| Texto de ayuda o párrafo | 1 línea | si necesita más, falta un dibujo |
| Botón | 3 palabras | verbo + objeto: «Verla en el mapa» |

- **No se repite lo que ya está a la vista:**
  - el estado que dice el icono («Cursando» junto al libro);
  - el motivo que dice el rótulo («En tu cadena» en cada fila de una sección que ya se llama así);
  - la fecha que dice la tarjeta de arriba.
- **Frases cortas:** sin «tienes», «puedes» ni «que» cuando una sola palabra basta. «Pide Estadística I» y no «Se abre al aprobar Estadística I».
- **Vocabulario fijo** (el mismo en el mapa, la lista, la ficha y la hoja):

  | Concepto | Pastilla o rótulo | En una frase |
  |---|---|---|
  | Estados | APROBADA · CURSANDO · DISPONIBLE · BLOQUEADA | «Puedes inscribirla» solo en la ficha |
  | Prelaciones | — | «Pide X» (lo que necesita), «Abre X» o «→ X» (lo que desbloquea) |
  | Cadena | — | «Tu cadena», «No puede esperar», «Puede esperar 2» (semestres) |
  | Fechas | — | «Ene – Jun 2027» (un período), «Dic 2029» (cuando terminas). Sin números romanos |

## 4. Filas

- **Medidas:**
  - de alto, 64 dp con nota, 56 cuando la nota es corta y 48 sin nota;
  - 16 dp a los lados y 12 entre el icono y el texto;
  - el separador empieza en 60 dp.
- **Orden:** a la izquierda, el icono de estado o la casilla; luego Fila y Nota; a la derecha, la Cifra de fila; al final, el chevrón solo si navega.
- **Icono por fila:** solo si cambia de una fila a otra. Si todas llevan el mismo, el icono va en el rótulo de la sección o se quita.
- **Nombres largos:** dos líneas antes que «…». Para recortar, se abrevia como la UDO («Admón.», «Stmas.»), nunca a mitad de palabra.

## 5. Cifras

- **Una respuesta por tarjeta**, en el mismo orden en toda la app:
  - a la izquierda, la cifra con su unidad («6 materias», «41%»);
  - a la derecha, la fecha;
  - debajo de cada una, su Nota («16 UC», «Te gradúas»).
- **Una cifra no lleva rótulo encima si su unidad ya la explica** (%, UC, materias, una fecha). Si no, la explica la Nota de debajo.
- **Comparar** («a tiempo / si atrasas una») se hace con dos fechas lado a lado y un hilo entre ellas. La consecuencia mala va en rojo, en la Nota.

## 6. Iconos

- Lucide lineal: 18 dp dentro de una pastilla de 32 × 32 con radio 8, trazo visual de 1,5 dp. Los chevrones, de 16 dp en `apagada`.
- **Asignación fija:**

  | Icono | Para |
  |---|---|
  | libro abierto | este semestre |
  | calendario | próximo |
  | eslabón | cadena |
  | birrete | grado |
  | llave inglesa | técnicas |
  | personas | sociohumanísticas |
  | candado abierto | inscribible |
  | calendario con check | inscribe |
  | flecha que vuelve | repetir |
  | reloj de arena | margen |
  | mapa | ir al mapa |

- **Nunca dentro de las tarjetas del mapa.** Tampoco hacen falta en filas cuyo rótulo ya da el motivo (§4).

## 7. Color por rol

| Rol | Token |
|---|---|
| Inscribible: candado, pastilla y cifra de «Tu próximo semestre», eslabón 1 de la cadena | `azul` (pastilla al 16 % en oscuro) |
| Aprobada | `verde`, solo el icono de estado |
| Bloqueada; consecuencia de atrasar | `rojo` |
| Cursando | `tinta` + contorno de 1,5 dp, peso 500 en texto |
| Texto: principal, frase, nota, apagado | `tinta`, `suave`, `tenue`, `apagada` |
| Superficie de tarjeta, hilo, pastilla, pendiente | tinta al 5 / 9 / 8 / 16 % (oscuro) |

- En el horario, el color de cada materia es el de su área (es su identidad, no un estado). Es la única excepción.
- Los colores de área se quedan en el mapa, la lista y el horario. Las hojas no los usan.

## 8. Espacio y radios

- Ritmo de 4 dp. El margen de pantalla y de hoja lo da `margenPantalla()` (16 / 20 / 24).
- **Separaciones verticales:**
  - entre la cabecera y la primera tarjeta, 20;
  - entre secciones, 24;
  - del rótulo a su tarjeta, 8;
  - entre tarjetas sueltas, 12.
- **Radios concéntricos:**
  - hoja, 28 arriba;
  - tarjeta, 24 (= pastilla 8 + margen 16);
  - botón de 52 con radio 26;
  - círculos de atrás y cerrar, 36;
  - cápsulas y fichas pequeñas, de 2,5 a 6.
- **Acostado:** índice de 330 dp, hilo y detalle. El detalle va en **una sola columna** con desvanecido abajo: dos columnas de filas no caben en 536 dp sin cortar los nombres.

## 9. Pantalla por pantalla

### Portada y ajustes (`portada/`)

| Dónde | Hoy | Propuesta |
|---|---|---|
| `TuCarrera.kt:166`, `:215` | «APROBADO» (rótulo bajo el 41 %) | «62 / 153 UC» (Nota): explica el % con un dato |
| `TuCarrera.kt:166`, `:215` | «SIN MARCAS» | quitar: la línea de `:261` ya dice «Marca lo que ya aprobaste» |
| `TuCarrera.kt:181`, `:234` | «TOCA UNA DE LA DERECHA» / «TOCA UNA DE ABAJO» | quitar: el título «Elige tu carrera» ya lo pide |
| `Portada.kt:167` + `Filas.kt:82`, `:127` | rótulo «OTRAS CARRERAS» y «INGENIERÍA» repetido en cada mosaico | dos rótulos, «INGENIERÍAS» y «LICENCIATURAS» (ya vienen ordenadas por `TIPOS`), y los mosaicos sin la línea del tipo: una sola línea, más aire |
| `Filas.kt:78` / `:122` | nombre en Jost 17 Light / Jost 15 | Encabezado (17/22) en los dos |
| `TuCarrera.kt:176` | nombre de la carrera en Jost 32/38 | Título 28/32 |
| `Ajustes.kt:128-133` | «MAPA DE PENSUM» + «Ajustes y ayuda» (Jost 17 Medium) | solo «Ajustes», en Título |
| `Ajustes.kt:222` | «Ver la guía otra vez» / «Cómo funciona, en cuatro pasos» | «Ver la guía», sin nota |
| `Ajustes.kt:223` | «Reportar un error» / «Una materia o prelación mal puesta» | sin nota |
| `Ajustes.kt:189`, `:232` | «Código abierto» / «Revísalo en GitHub» | nota «GitHub» |
| `Ajustes.kt:349` | «La tuya sale primero en la portada, con tu avance. Las demás las abres cuando quieras.» | «Sale primera en la portada.» |
| `Personalizar.kt:165-169` | notas «Una luz suave desde arriba en la portada», «Menos animación, más fluida», «El código de cada materia» | quitar las tres: el interruptor y su título bastan |
| `Personalizar.kt:224-226` | «Activo en este teléfono: sin desenfoque ni luces en bucle» (y dos más igual de largas) | «Activo: sin desenfoque» · «Apagado» · «Todo el cristal y las luces» |

### Bienvenida (`bienvenida/`)

Es la parte más limpia de la app: un titular de dos líneas, un botón y «Saltar». Solo cambia esto:

| Dónde | Hoy | Propuesta |
|---|---|---|
| `Bienvenida.kt:193` y `PasoCarrera.kt:162` | el mismo estilo de titular escrito dos veces | un solo rol, Titular, en `Texto.kt` |
| `PasoCarrera.kt:355` | botón apagado «Elige una carrera», que repite el titular «Elige tu carrera, será tu inicio.» | botón apagado «Empezar»: el estado dice que falta elegir |
| `PasoAvance.kt:214` | «TE GRADÚAS EN» | «TE GRADÚAS» (la fecha va debajo, sin preposición colgando) |
| `PasoHorario.kt:288` | «Período 2026-I» | se queda: imita la captura real de Intradace. Es la única fecha con número romano permitida, porque es una cita |

### Lista (`carrera/Lista*.kt`, `logica/Lista.kt`, ficha)

| Dónde | Hoy | Propuesta |
|---|---|---|
| `logica/Lista.kt:97` | nota «Aprobada» | sin texto de estado: queda «2 UC» (el check verde lo dice) |
| `logica/Lista.kt:98` | «Cursando» | igual: «2 UC» (lo dice el libro) |
| `logica/Lista.kt:99` | «Puedes inscribirla», repetido en cada fila azul | «2 UC · Abre 1» o solo «2 UC» si no abre ninguna |
| `logica/Lista.kt:100` | «Se abre al aprobar Estadística I» / «Se abre el próximo semestre» | «Pide Estadística I» / «El próximo semestre» |
| `logica/Lista.kt:101` | «Requiere X» / «Bloqueada» | «Pide X» / sin nota (lo dice el candado rojo) |
| `ListaFilas.kt:481` | «2 UC» en Plex Mono | Nota (Inter `tnum`): Plex solo para códigos |
| `ListaFilas.kt:470` | nombre de materia en Jost 17 | Fila (Inter 500 15/20, dos líneas). **Cambia la cara de la lista: maquetar las dos antes de tocarla** |
| `ListaFilas.kt:298` | «Semestre 4» en Jost 17 | Encabezado (el mismo tamaño, ya es el rol) |
| `PanelResumenLista.kt:95` | «Toca una materia para ver su ficha» | quitar |
| `FichaMateria.kt:330` | «Puedes inscribirla: tienes aprobadas todas sus prelaciones.», junto a la pastilla DISPONIBLE y el candado azul | quitar la frase |
| `FichaMateria.kt:304` / `:315` | «Nada: puedes verla desde el inicio.» / «Nada: es final de rama.» | «Ninguna» / «Ninguna» |
| `FichaMateria.kt:266` | «SEMESTRE 05» en Jost | Rótulo «SEMESTRE 5» |
| `FichaMateria.kt:407` | «CAMBIAR ESTA ELECTIVA», Jost 9,5 sp | Rótulo 11 sp |

### Horario (`carrera/Horario*.kt`, `HojasHorario.kt`, `logica/Horario.kt`)

| Dónde | Hoy | Propuesta |
|---|---|---|
| `Horario.kt:258` + `logica/Horario.kt:306` | rótulo «HOY» y título «Hoy no tienes clases» | rótulo con el día («VIERNES»), título «Sin clases hoy» |
| `Horario.kt:556`, `:562`, `:580` | «VIERNES · HOY», «Sin clases» y además «Este día no tienes clases.» | quitar la frase de `:580`: lo dice dos veces ya |
| `Horario.kt:137`, `:279` | titular 34/37, y 27/30 si el nombre pasa de 26 letras | Titular 40/44 con `autoSize` hasta 28 (como la bienvenida) |
| `Horario.kt:136` | rótulo Jost 11, +20 % | Rótulo (Inter 11/16, +8 %) |
| `Horario.kt:143`, `:146`, `:147`, `:149` | 15,5 / 14 / 13 / 12,5 sp | Fila / Cuerpo / Nota / Nota |
| `Horario.kt:151` | meridiano «AM» a 8,5 sp | unidad de la hora: 11 sp, tenue, en la línea base |
| `HorarioInicio.kt:121` | rótulo «MI HORARIO» sobre «Arma tu semana» | quitar: la pestaña ya es el horario |
| `HorarioInicio.kt:124` | «Sube la captura de tu horario de Intradace y la pasamos a tu semana, clase por clase.» | «Sube una captura y la pasamos a tu semana.» (Intradace ya va en el sello de `:143`) |
| `HorarioInicio.kt:132` | «Crearlo manualmente» | «Crear a mano» |
| `HojasHorario.kt:113` | «LEÍDO DE TU CAPTURA» / «Antes de guardar» | solo el título «Revisa y guarda» |
| `HojasHorario.kt:147` | «EN CURSO · QUEDAN 40 MIN» (rótulo largo) | rótulo «EN CURSO» y nota «Quedan 40 min» |
| `HojasHorario.kt:401` | «Se quitan las 4 clases de tu semana y vuelves a empezar. No se puede deshacer.» | «Se borran tus 4 clases. No se puede deshacer.» |

### Hoja de progreso (`carrera/AvanceCarrera.kt`, `AvanceIndice.kt`, `Detalle*.kt`)

| Dónde | Hoy | Propuesta |
|---|---|---|
| `AvanceCarrera.kt:257` | rótulo «TU AVANCE» + título «Ingeniería de Sistemas» | título «Tu avance», sin rótulo, igual que acostado (`AvanceIndice.kt:133`). Dentro de la carrera, su nombre sobra |
| `AvanceCarrera.kt:162` | subtítulo «Qué te abre aprobarlas» | quitar |
| `AvanceCarrera.kt:166` | «Para graduarte en Dic 2029» | quitar: la fecha pasa a la tarjeta de arriba |
| `AvanceCarrera.kt:171` | «6 materias encadenadas» | quitar: lo dice el dominó |
| `AvanceCarrera.kt:174` | «Depende de tu ritmo» | quitar |
| `AvanceCarrera.kt:177` | «Tú eliges cuáles» | quitar. `DatosCabecera` se queda sin subtítulo |
| `AvanceCarrera.kt:431` | «Lo que cursas y qué abre» | «Te abren 1» (`siApruebas.seAbren.size`) |
| `AvanceCarrera.kt:432` | «Qué inscribir · Ene – Jun 2027» | «Ene – Jun 2027» |
| `AvanceCarrera.kt:433` | «Las que fijan tu fecha» | «Empieza en Sistemas de Costo» |
| `AvanceCarrera.kt:436` y `DetalleGraduacion.kt:181` | «Ya vas a lo más rápido» / «Ya vas lo más rápido» | una sola: «Ya vas lo más rápido» |
| `AvanceCarrera.kt:117` | `versal()`: Inter 600, +5 %, 11/14 | Rótulo; se borra `versal` |
| `DetalleElectivas.kt:123` | párrafo de 190 caracteres | «Cuentan las que marques como aprobadas, hasta cubrir las UC del título.» |
| `DetalleProximo.kt` | «TE CONVIENE · 6» con «En tu cadena» y «Abre 1» fila a fila | opción A de la maqueta (§10) |
| `DetalleCadena.kt` | comparación + vía vertical | opción A de la maqueta (§10) |

## 10. «Tu próximo semestre» y «No puedes atrasar»

### Lo elegido (opción A en las dos)

**Tu próximo semestre · «Por qué cada una»**

- **La tarjeta de arriba responde la pregunta:**
  - «6 materias» a la izquierda y «Dic 2029 / Te gradúas» a la derecha;
  - debajo, «16 UC» y una barra de cápsulas, una por materia y ancha según sus UC.
- **La barra está agrupada por motivo:** azul lleno para la cadena, azul al 45 % para las que abren y contorno azul para las que completan la carga.
- **Debajo, las materias agrupadas por motivo,** y el motivo se dice una vez, en el rótulo. Cada rótulo lleva delante su muestra de cápsula, que lo une con la barra:
  - **Fija tu fecha:** Sistemas de Costo, con la nota «No puede esperar».
  - **Te abren camino:** Métodos Numéricos → Optimización de Operaciones; Sistemas de Operación → Admón. de Base de Datos; Circuitos y Sistemas → Electrónica; Teoría de Sistemas → Enfoque Sistémico.
  - **Completan tu carga:** Análisis de Decisiones, con la nota «Puede esperar 3 semestres».
- **Sección «¿Solo puedes con 5?»:**
  - «Deja Análisis de Decisiones: sigues en Dic 2029»;
  - «Con 5 cada semestre: Jun 2030».
- **«También puedes»:** Leyes y Deontología.
- **Al final, la miniatura del mapa** con las 6 encendidas, que lleva a «Verlas en el mapa».

**No puedes atrasar · «El dominó»**

- **Una tarjeta con una frase y dos carriles de fichas numeradas del 1 al 6:**
  - la frase: «**6 materias en fila:** cada una abre la siguiente y no queda semestre libre.»;
  - el carril «A tiempo», con las 6 antes de la línea del grado (Dic 2029);
  - el carril «Si atrasas una», con un hueco y las 6 corridas un semestre. La última cae pasada la línea, en rojo (Jun 2030).
- **«La cadena»:** una fila por eslabón, con el número de su ficha y el período en la nota. El 1 va en azul.
- **«Qué hacer»:**
  - «Inscribe Sistemas de Costo · Ene – Jun 2027»;
  - «Si repruebas una, repítela ya · Cada semestre de espera, 6 meses más»;
  - «Vigila Métodos Numéricos · Su rama tiene 1 semestre de margen».
- **Al final, la miniatura del mapa** con el cable de la 1 a la 6, que lleva a «Verla en el mapa».

**Acostado:**
- Próximo, en una columna: la tarjeta en una fila (cifra, barra y fecha) y los grupos debajo.
- Cadena, en dos columnas: el dominó a la izquierda (262 dp) y «Qué hacer» con «Verla en el mapa» a la derecha (210 dp).

### El algoritmo

Todo sale de `estimarGraduacion`: no se inventa otro plan, se explica el mismo que da la fecha. Lo nuevo es el **margen**: cuántos semestres puede esperar una materia sin mover tu fecha (la holgura de un camino crítico).

**1. `estimarGraduacion` acepta un veto y expone la cadena de cada materia**

```kotlin
fun estimarGraduacion(
    pensum: Pensum, marcas: Map<String, Marca>, casillas: Map<String, String>,
    carga: Int? = null, hoy: Periodo = Periodo.actual(),
    noAntesDe: Map<String, Int> = emptyMap(),   // nuevo: código → primer período del plan en que puede entrar (0 = el que viene)
): EstimacionGraduacion
```

- **El veto:** en el bucle, `listas` filtra además `(noAntesDe[p.codigo] ?: 0) <= plan.size`.
- **Un campo nuevo, `cadenaDe: Map<String, Int>`,** con el valor `cadena` que ya se calcula: la altura con el freno del nivel. Es el `prioridad` de hoy.

**2. Margen: rápido y luego confirmado**

```kotlin
/** Semestres que `codigo` puede esperar sin mover periodoFinal. Exacto: vuelve a simular. */
fun margenDe(pensum: Pensum, marcas: Map<String, Marca>, casillas: Map<String, String>,
             base: EstimacionGraduacion, hoy: Periodo, codigo: String): Int
/** Lo mismo, sin simular: plan.size − índice en el plan − cadenaDe[codigo]. */
fun margenAproximado(base: EstimacionGraduacion, codigo: String): Int
```

- **`margenAproximado` alcanza para listar.** En Sistemas da lo mismo que la simulación para las 24 obligatorias que faltan: Costo 0, Métodos 1, Operación 2, Circuitos 2, Teoría 2 y Análisis 3.
- **Una materia que podrías inscribir y el plan pone más tarde** cuenta su margen desde el semestre que viene: su índice en el plan más su margen. Leyes cae en el índice 4 con margen 1, así que tiene 5.
- **`margenDe` confirma lo que se afirma en una frase.** Por ejemplo «Deja Análisis: sigues en Dic 2029».
  - **Cómo:** prueba `noAntesDe = codigo → k₀ + j` para j = 1, 2, … hasta que cambia la fecha.
  - **Costo:** unas 6 simulaciones por materia, unas 40 en total. Va en `Dispatchers.Default` y queda en `remember(marcas, casillas, carga)`.
- **Por qué hace falta confirmarlo:** el tope de carga puede hacer optimista a la aproximación.

**3. Recomendación del próximo semestre**

```kotlin
enum class Motivo { FIJA_FECHA, ABRE_CAMINO, COMPLETA_CARGA, OPCIONAL }
data class Recomendacion(val codigo: String, val uc: Int, val motivo: Motivo, val margen: Int, val abre: List<Asignatura>)
fun recomendarProximo(pensum: Pensum, marcas: Map<String, Marca>, casillas: Map<String, String>,
                      base: EstimacionGraduacion, hoy: Periodo): List<Recomendacion>
```

- **Lo que se recomienda:**
  - las de `base.plan[0]`, que reciben un motivo cada una;
  - las de `inscribiblesDelProximo` que quedan fuera del plan, como OPCIONAL.
- **Cómo se decide el motivo:**

  | Si la materia… | Motivo |
  |---|---|
  | tiene margen 0 | FIJA_FECHA |
  | `abreElProximo` no está vacío | ABRE_CAMINO |
  | ninguna de las dos | COMPLETA_CARGA |

- **Orden dentro de cada grupo:** margen ascendente, luego más que abre y luego más UC.
- **Casillas de electiva:** van por margen. Su nota es «Tú eliges cuál».

**4. Dominó y plan B**

```kotlin
data class Domino(val fecha: Periodo?, val seCorren: List<String>)
/** Atrasa `codigo` un período: a qué fecha llegas y qué materias del plan caen más tarde. */
fun dominoSiAtrasas(pensum: Pensum, marcas: Map<String, Marca>, casillas: Map<String, String>,
                    base: EstimacionGraduacion, hoy: Periodo, codigo: String): Domino
/** La del primer período que puedes dejar sin mover tu fecha: la de más margen (≥ 1), confirmada con margenDe. */
fun cualDejar(recomendaciones: List<Recomendacion>): Recomendacion?
/** La rama que viene detrás de la cadena: la del primer período con margen 1 y más cadenaDe, seguida hasta el final. */
fun siguienteCadena(base: EstimacionGraduacion, margenes: Map<String, Int>): List<String>
```

- **`dominoSiAtrasas`:** para Sistemas de Costo, las 6 de la cadena se corren un período y llegas a Jun 2030.
- **«Con 5 cada semestre»:** no hace falta una función nueva, ya sale de `planesPorCarga(...)[4].periodoFinal`.
- **`siguienteCadena`:** recorrerla pide sacar `siguientes` y `altura` de `estimarGraduacion` a un `GrafoPendiente` interno. Así lo comparten el cuello de botella y esta rama.

**5. Mapa**

- `alVerEnMapa(codigos)` ya resalta las tarjetas. La cadena pide además encender los cables entre eslabones seguidos: `alVerEnMapa(codigos, cable = true)`, en `mapa/Relaciones.kt`.
- La miniatura de las hojas no es otro mapa: es un dibujo de 96 × 64 dp con columnas de cápsulas, para que se reconozca y se toque.

**Lo que no cambia:**
- el plan;
- la fecha;
- `cuelloDeBotella`;
- `fechaSiRepruebas`;
- `periodosDeCadena`.

Todo lo nuevo es explicación encima de la misma cuenta.
