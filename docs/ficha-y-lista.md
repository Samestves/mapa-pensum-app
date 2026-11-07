# Ficha del mapa y lista: especificación

Maquetas: `docs/maquetas/ficha-mapa.html` y `docs/maquetas/lista.html`, cada una con su PNG al lado.
Manda `reglas-diseno.md`; esto solo dice cómo se construye lo recomendado.

## Lo recomendado

| Pantalla | Opción | Por qué |
|---|---|---|
| Ficha de pie | **A · Siguiente paso** | Un verbo lleno dice qué hacer y el botón dice qué pasa («Se te abre 1»), sin repetir el estado. |
| Ficha acostada | **A · Franja** | La misma hoja, baja y ancha: el mapa sigue arriba con la materia, lo que pide y lo que abre. |
| Lista de pie | **A · Se abre en su sitio**, nombres en Inter 15/500 | Se toca la fila como se toca un semestre; marca ahí mismo y nunca abre la hoja del mapa. |
| Lista acostada | **A · Tabla** | Lo que de pie hay que abrir aquí se ve: Materia · UC · Pide · Abre · Marca, y se marca en un toque. |

---

## 1. Ficha de pie · «Siguiente paso»

### Anatomía (dp)

Una hoja pegada abajo y a todo el ancho, color `Paleta.panel`, radio 28 arriba y el filo de 1 dp de `panelPegado`. Laterales `margenPantalla()` (20 en 375), abajo 16 más la barra de gestos.

| # | Pieza | Medidas | Rol |
|---|---|---|---|
| 1 | Asa | 32 × 4, radio 2, tinta 28 %; 8 arriba y 8 abajo | — |
| 2 | Cabecera | fila de 36: «SEMESTRE 5» + código, y la X | Rótulo · Código (Plex 11/16) · `BotonRedondo` 36 |
| 3 | Nombre | 2 arriba; dos líneas como mucho (`autoSize` hasta 24) | Título, Jost 300 28/32 |
| 4 | Datos y estado | fila de 24, 10 arriba: «2 UC · Gestión» y la píldora a la derecha | Nota · píldora (abajo) |
| 5 | Respuesta | `TarjetaLista` (radio 24), 16 arriba, 0 o 1 `FilaLista` de 64 con `Pastilla` 32 | Fila + Nota |
| 6 | Pide · Abre | `TarjetaLista`, 12 arriba; una fila de 48 por materia: rótulo (36), icono de estado 16, nombre a dos líneas, chevrón 16 | Rótulo · Fila · `Chevron` |
| 7 | Botones | 16 arriba; dos de 52 con radio 26, 8 entre ellos; uno solo a todo el ancho en aprobada | Fila 15/20 (+ Nota 13/16 debajo) |

- **Alto:** el del contenido, con tope en el **60 %** de la pantalla (lo que sobra se desplaza dentro). Medido en 812: disponible 420 (52 %), cursando 452 (56 %), aprobada 396 (49 %), bloqueada 476 (59 %). La casilla vacía llega al tope.
- **Pide · Abre:** el rótulo va solo en la primera fila de cada grupo y hay un hilo entre los dos grupos. «Ninguna» (tenue) si no hay. Tocar una fila lleva a esa materia: cambia la hoja y viaja la cámara (`alIrA`, ya existe).
- **Con 320 dp o letra grande** (`ventana().compacta || letraGrande`), los dos botones se apilan.

**Píldora de estado** (24 de alto, radio 6, icono 14 + Rótulo; la palabra solo se dice aquí, en las filas basta el icono):

| Estado | Fondo | Texto | Icono |
|---|---|---|---|
| DISPONIBLE | azul 16 % | azul | candado abierto azul |
| CURSANDO | sin fondo, contorno 1,5 de tinta | tinta | libro, tinta |
| APROBADA | tinta 8 % | tinta | sello verde |
| BLOQUEADA | tinta 8 % | suave | candado rojo |
| PRÓXIMA | tinta 8 % | suave | reloj de arena, suave |
| POR ELEGIR (casilla) | sin fondo, contorno punteado tinta 30 % | suave | aro punteado |

**Respuesta**: una sola fila, la que importa para tu fecha. Todo sale de lo que ya existe en `EstadoCarrera`:

| Estado | Pastilla | Fila | Nota | De dónde sale |
|---|---|---|---|---|
| Disponible, margen 0 | eslabón (azul si es el 1.º de la cadena) | No puede esperar | Si la atrasas, te gradúas en **Jun 2030** (rojo) | `simulador.margen(c)`; `simulador.conVeto(c, indiceEnPlan(c) + 1).periodoFinal.mesYAnio()` |
| Disponible, margen ≥ 1 | reloj de arena | Puede esperar 2 semestres | Sin mover tu fecha | `simulador.margen(c)` |
| Cursando | flecha que vuelve | Si la repruebas | Sigues en **Dic 2029** · o en rojo si cambia | `fechaSiRepruebas(…, c)` |
| Bloqueada o próxima | eslabón si va en `graduacion.cuelloDeBotella`, si no calendario | En tu plan, Jul – Dic 2027 | No puede esperar · Puede esperar N | `graduacion.plan`: índice `i` → `hoy.mas(i + 1).rango()` |
| Aprobada | — (sin fila) | | | |

**Botones** (el lleno es siempre el siguiente paso; «Se te abre N» = `seAbrenAlAprobar(pensum, marcas, listOf(c)).size`, solo si N > 0):

| Estado | Lleno (`BotonTinta`) | De superficie (tinta 8 %) |
|---|---|---|
| Disponible | La estoy cursando | Ya la aprobé · *Se te abre 1* (azul) |
| Cursando | Ya la aprobé · *Se te abre 1* (panel al 60 %) | Desmarcar |
| Bloqueada o próxima | — | La estoy cursando · Ya la aprobé (*Se te abren 2*) |
| Aprobada | — | Desmarcar, a todo el ancho |

### Casilla de electiva

- **Vacía:** la hoja **es el selector**, con la misma cabecera: «SEMESTRE 4 · ELECTIVA», «Electiva Sociohumanística», «Elige 1 de 14 · llevas 0 de 6 UC» y la píldora POR ELEGIR. Debajo, el buscador (más de 10 opciones) y la lista «PUEDES CURSARLAS YA · 13»: radio de 22, nombre en Fila y «2 UC» como `CifraFila`. Tocar una la coloca (`estado.colocar`) y la hoja baja. Va al tope (60 %). Reutiliza `Contenido` de `SelectorElectiva.kt` cambiando las fichas en mosaico por filas.
- **Con electiva puesta:** la ficha normal de esa materia; la cabecera dice «SEMESTRE 7 · ELECTIVA TÉCNICA» y a la derecha del rótulo un texto-botón «Cambiar» (Nota 500, tinta), que abre el selector.

### Cámara

- Al abrir, la tarjeta queda **centrada en el hueco libre**: 16 bajo la barra de estado y 16 sobre la hoja; en horizontal, en el centro. Es `camara.mostrar(caja, Margenes(arriba = barraEstado + 16, abajo = altoHoja + 16, izq = 20, der = 20))`: la llamada de `alTapar` que ya existe, pero con el alto medido de la hoja y sin la reserva de la barra.
- **No acerca nunca** (`mostrar` ya usa `minOf(v.escala, …)`): solo aleja si la tarjeta no cabe.
- **Se encuadra una vez por materia:** la guarda `encuadrada` ya existe. Cambiar de alto al marcar no mueve el mapa.

### Movimiento

- **Abrir:** el resorte de `HojaInferior` (rigidez 380, sin rebote, ~350 ms) y, a la vez, la cámara.
- **Cerrar:** la curva `SALE` (260 ms). Se cierra con el asa, la X, Atrás o tocando el mapa vacío.
- **Otra materia** (otra tarjeta o una fila de Pide · Abre): el contenido se funde en 150 ms (`AnimatedContent` con `SizeTransform` nulo) y el alto cambia de golpe; la hoja no baja.
- **Marcar:**
  - los textos de los botones y la píldora se funden en 150 ms, con el háptico `SegmentTick`;
  - «Ya la aprobé» cierra la hoja y encuadra la materia con lo que abre (`cajaDe(abre + codigo)`) y la descarga de luz: es el camino que ya tiene `alMarcar`.
- **La barra Núcleo** baja 72 y se funde en 200 ms mientras la hoja está abierta, y vuelve al cerrar (`NavegacionCarrera(oculta = …)`, solo `graphicsLayer`).

### Rendimiento (gama baja)

- Solo se anima la capa: `translationY` de la hoja y de la barra, `alpha` del contenido. Nada de layout por cuadro.
- **Sin desenfoque y sin sombra grande:**
  - fuera el `shadow(28.dp)` de hoy;
  - la separación es el color del panel, el filo de 1 dp y un degradado de 40 dp por encima, pintado una vez en `drawBehind`;
  - sin `hazeEffect` sobre el mapa: la hoja no es modal, el mapa sigue vivo debajo.
- **La respuesta se calcula fuera del hilo principal:**
  - `produceState` + `Dispatchers.Default`; `Simulador` ya memoriza el margen;
  - la fila reserva sus 64 dp desde el primer cuadro y el texto entra con un fundido de 150 ms, así la hoja no cambia de alto;
  - la primera vez son hasta 8 simulaciones.

### Cambios en el código

- **`HojaInferior`**: tres parámetros, con valores por defecto que dejan igual las demás hojas:
  - `modal = true`: en falso, sin `FondoHoja` (ni velo ni desenfoque), y los toques fuera llegan al mapa;
  - `tope: Float? = null`: 0,6 para la ficha;
  - `completaAcostada = true`: en falso, la franja.
- **`FichaMateria.kt`** se rehace sobre `HojaInferior`:
  - sale el panel flotante, `PanelLateral`, `SelectorMarca`, `AvisoSituacion` y `ListaPrelaciones`;
  - entran `RespuestaFicha`, `PideAbre` y `BotonesMarca`;
  - reutiliza `TarjetaLista`, `FilaLista`, `Pastilla`, `BotonTinta`, `BotonRedondo`, `Chevron`, `IconoLucide`, `iconoEstado`, `estiloRotulo/Titulo/Fila/Nota` y `superficie()`.
- **`Interior.kt`**:
  - `FichaMateria` sin `reservaAbajo`, `lateral` ni `horizontal` flotante;
  - `NavegacionCarrera` gana `oculta`;
  - fuera `panelMapa()`.

### Textos que cambian

| Hoy | Pasa a |
|---|---|
| «REQUIERE» / «DESBLOQUEA» (`FichaMateria.kt:302`, `:313`) | «PIDE» / «ABRE» |
| Aprobada · Cursando · Sin cursar (selector, `:411`) | «La estoy cursando», «Ya la aprobé», «Desmarcar» |
| `AvisoSituacion` entero (`:322-350`) | fuera: lo dicen la píldora, la fila de respuesta y «Pide» |
| «Se abre el próximo semestre si apruebas X» | píldora PRÓXIMA; X sale en «Pide» con su libro |
| «Aún te falta X. Puedes marcarla igual si ya la viste.» | X sale en «Pide»; los botones de superficie ya dicen que se puede |
| «CAMBIAR ESTA ELECTIVA» (`:405`) | «Cambiar», junto al rótulo |
| «Ciencias básicas 3 UC» con punto de color | «3 UC · Ciencias básicas»: sin color de área, que la hoja no lo usa |

---

## 2. Ficha acostada · «Franja»

- **Es la misma pieza:** `HojaInferior(modal = false, completaAcostada = false)` a todo el ancho, alto del contenido.
  - En 915 × 412 mide unos **154 dp, el 37 %**.
  - Laterales 28 más los recortes seguros.
- **Rejilla de 3 columnas:** `276 | hilo | 1fr | 232`, con 24 entre columnas.
  1. **Quién es:** rótulo y código; Título en su tamaño acostado (Jost 300 24/28); «2 UC · Gestión» y la píldora.
  2. **La respuesta** (`Pastilla` 32 + Fila + Nota) y debajo «PIDE» / «ABRE», una línea cada uno (icono 16 + Nota; la segunda y siguientes, «y N más»).
  3. **Los dos botones apilados**, de 52.
- **Sin X:** se cierra con el asa, Atrás o tocando el mapa.
- **El riel** se aparta 80 dp a la izquierda y se funde, en 200 ms, mientras la franja está arriba.
- **Cámara:**
  - encuadra la caja de la materia, lo que pide y lo que abre: `cajaDe(listOf(c) + atras + adelante)`;
  - márgenes: arriba barra de estado + 16, abajo alto de la franja + 16, a los lados 24;
  - sin acercar. Al zoom acostado (~0,46) el vecindario de Sistemas de Costo cabe en la banda de 220 dp. Si no cabe, se centra solo la materia.
- **Rendimiento:** igual que de pie. Fuera `panelMapa()` y el reparto lateral.

---

## 3. Lista de pie · «Se abre en su sitio»

### La fila cerrada (cambia solo la letra)

- **Nombre:** `estiloFila()`, Inter 500 15/20, dos líneas. Las aprobadas, en 400 y suave.
- **Nota:** la de `notaDe`, que ya sigue las reglas («2 UC · Abre 1» con «Abre 1» en azul, «Pide X»), en `estiloNota()`.
- **Medidas:** 64 de alto (74 con dos líneas), 16 a los lados, icono de estado 20 en caja de 32, hilo desde 60.
- **La cabecera del semestre se queda en Jost** (Encabezado 17/22).
- **Medido en las 49 materias:** Inter ocupa lo mismo que Jost (179 frente a 180 dp de media) y deja las mismas 9 a dos líneas. El cambio es de jerarquía y de coherencia, no de espacio.

### La fila abierta

- Tocar la fila la abre; una sola a la vez (`control.abierta: String?`); tocarla otra vez o abrir otra la cierra.
- Panel con 8 de margen dentro del grupo y radio 16 (24 − 8): tinta 5,5 % y contorno de 1 dp al 7 %.
  - **Arriba:** icono 20 · nombre (Fila) · «2 UC · Gestión» (Nota) · `BotonRedondo` 36 con el mapa, que lleva al mapa con la materia encuadrada.
  - **Pide / Abre:** dos líneas a 44 del borde: rótulo 36, icono 14 y nombre en Nota 500 a una línea; si hay más de una, «y N más».
  - **`SelectorMarcas`** a todo el ancho: pista de 44 con radio 22, lente de 36 con radio 18. Tres segmentos: icono 16 + Nota 500. El icono activo lleva su color (verde en aprobada); los demás van en `apagada`.
- **Al marcar:**
  - la lente se desliza (`translationX`, 220 ms, `ENFATICA`) y el icono de la fila hace su `pop`;
  - el aro, el resumen del semestre y las pastillas se actualizan solos;
  - la fila sigue abierta: tocar otra marca es deshacer, así que el «Deshacer» por fila desaparece. El del aro del semestre se queda.
- **La lista ya no abre la ficha:**
  - `accionesDeLista.tocar` alterna `abierta`;
  - `VistaLista.alAbrirFicha` pasa a `alVerEnMapa(codigo)`, que en `Interior` hace `vista = MAPA`, `porEncuadrar = listOf(codigo)` y abre la ficha.
- **Casilla vacía:** se queda como hoy, con «Elegir», que abre el selector.

### Rendimiento

- Lo abierto **solo se compone cuando está abierto**.
- **El alto se anima como el pliegue del semestre:** un `layout` que recorta, con el contenido medido una vez, en 220 ms. En modo ligero, de golpe. Nada de `animateContentSize` en la lista.
- Las demás filas no se recomponen: `FilaMateria` ya se compara por valor.

### Textos

- Con 320 dp o letra grande, los segmentos se quedan en solo icono.
- «Deshacer» por fila (`ListaFilas.kt`) desaparece; el del aro del semestre se queda.

---

## 4. Lista acostada · «Tabla»

- **Sustituye el reparto lista + panel:** se borran `repartoLista`, `RepartoLista`, `PanelResumenLista.kt` y la `PanelLateral` de la lista.
- **Zona:** desde el riel (`izquierda + 84`) hasta 16 del borde derecho.
  - Arriba, la fila de pastillas de filtro: 36 de alto, radio 18.
  - Debajo, la tabla en una tarjeta de radio 24 (`superficie()`) que llena el resto.
- **Columnas:** `32 | 1fr | 44 | 156 | 156 | 132`, con 12 entre columnas y 16 de relleno.
  - **Encabezados:** Rótulos MATERIA, UC, PIDE, ABRE, MARCA, en una fila de 32 con hilo debajo.
  - **Semestre:** fila de 52 que se queda fija mientras pasan sus filas (`stickyHeader`). Lleva el aro de 24, Encabezado, la Nota del resumen, «17 UC» como `CifraFila` y el chevrón, que pliega el semestre.
  - **Materia:** fila de 52 (72 con dos líneas):
    - icono de estado 20 · nombre en Fila, a dos líneas · `CifraFila` «2 UC»;
    - Pide y Abre: icono 14 + Nota suave, a dos líneas, nunca cortadas a mitad de palabra; «y N más»;
    - `SelectorMarcas(etiquetas = false)`: pista de 36 con radio 18, lente de 28 con radio 14, segmentos de 44 de ancho.
- **Toques:**
  - en Pide o Abre, la tabla baja hasta esa fila y la ilumina (el `brillo` que ya existe);
  - en el nombre, lleva al mapa con la materia encuadrada y su ficha abierta.
- **Rendimiento:**
  - `LazyColumn` con `contentType` por tipo de fila y altos estables;
  - la lente solo se traslada en la capa;
  - la cabecera fija es lisa (tinta 3 %), sin desenfoque.

---

## Riesgos de implementación

1. **`HojaInferior` es de todas las hojas.** El modo no modal, el tope y la franja acostada deben dejar los valores por defecto intactos: avance, electivas y horario no cambian. Además, con la hoja no modal, los gestos del mapa no deben empezar debajo de la hoja.
2. **La hoja cambia de alto:** nombres de dos líneas, varias en Pide o Abre, la respuesta que llega tarde. Si se reencuadra cada vez, el mapa tiembla: reservar la fila de respuesta y encuadrar una vez por materia.
3. **El margen cuesta simulaciones:** hasta 8 la primera vez por materia. En gama baja la fila puede tardar 100-300 ms: va fuera del hilo principal y con su sitio reservado.
4. **Un vecindario alto no cabe acostado:** con Trabajo de Grado (lo que pide está en otras filas), la caja no cabe en la franja a un zoom legible. La alternativa es centrar solo la materia.
5. **La fila abierta vive dentro del ítem del semestre:** reanima ese ítem y desplaza los de debajo. Una sola abierta, recorte medido una vez, y probar con letra 1,3 y 320 dp.
6. **Se borran piezas con pruebas:** `PanelLateral`, `panelMapa`, `repartoLista`, `PanelResumenLista`. Hay que rehacer las capturas `horizontal-mapa-ficha`, `horizontal-lista-*`, `resp-ficha-*` y `resp-lista-*`.
