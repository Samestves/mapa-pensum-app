# QA de interfaz y rendimiento móvil

Auditoría de solo lectura. No repite `qa-logica.md`. Rutas relativas a `composeApp/src/commonMain/kotlin/mpensum/`. Las capturas de `composeApp/build/capturas/` son del 7-nov (01:01, vigentes), salvo `resp-*-412-letra13` y `resp-*-320` (6-nov 10:34 a 19:59, con «Cursando» aún en ámbar: viejas). No se ejecutó la app: cada hallazgo dice si está **Verificado** (leído en el código o medido en captura) o es **Sospecha** (falta perfilar o probar en dispositivo).

## Crítico

**C1. Girar el teléfono rehace la carrera entera y pierde estado** (Verificado en código; el efecto en pantalla es sospecha)
- `carrera/Interior.kt:92,94,103`. `estado = remember(pensum, horizontal)` recrea `EstadoCarrera` (recalcula `calcularLayout` en el hilo principal) y, por colgar de él, `EstadoHorario` (`EstadoHorario.kt:191`, `remember(estado)`) y `Camara`.
- Qué pasa: al girar con la lectura de una captura en curso («Leyendo tu captura»), se pierde la lectura, y con ella uno de los 20 usos diarios de Gemini. También se pierden borradores de clase, el filtro y el scroll de la lista (`Lista.kt:204`, `remember(estado)`), y el mapa salta a la vista inicial.
- Además `encuadrada` (`Interior.kt:113`) no se reinicia con `horizontal`: la materia elegida queda sin reencuadrar bajo el panel nuevo.
- Arreglo: `EstadoCarrera` debe depender solo de `pensum`. El `compacto` entra como parámetro de `layout` (un `derivedStateOf` o `remember(estado, horizontal)` solo para el layout). `EstadoHorario`, la lista y la cámara se quedan. Reiniciar `encuadrada` con `horizontal`.
- Esfuerzo: M.

**C2. Abrir una carrera calcula todo en el hilo principal, dentro de la animación de entrada** (Verificado en código; coste sospecha)
- `datos/Pensum.kt:78-82` decodifica el JSON en el contexto de `produceState` (Main). `EstadoCarrera.layout` (`EstadoCarrera.kt:46`) corre en composición. Ambos arrancan justo cuando `App.kt:135-143` anima 420 ms con escala y fundido.
- Qué pasa: los primeros cuadros del zoom de entrada se pierden en gama baja.
- Arreglo: `produceState` con `withContext(Dispatchers.Default)`, devolviendo `Pensum` y `LayoutMapa` ya listos. El `EstadoCarrera` solo los envuelve.
- Esfuerzo: M.

## Alto

**A1. `jost()`, `inter()`, `plex()` y `interCifra()` crean una `FontFamily` nueva en cada llamada** (Verificado)
- `tema/Tema.kt:201-231`. Cada llamada ejecuta 4 a 7 `Font(Res.font…)` composables (con `remember` y carga de recurso) y se llama más de 70 veces desde 27 archivos. Las funciones `estiloFila()`, `estiloNota()`, `estiloRotulo()`, `estiloTitulo()` y `versal()` (`carrera/EstiloAvance.kt:108-128`, `AvanceCarrera.kt:118`) las repiten y además construyen un `TextStyle`.
- Dónde pesa: cada fila de la lista llama a `jost()` más `estiloNota()` dos veces (`ListaFilas.kt:469,477,482`, `519,523`), unas 20 `Font()` por fila que entra en el `LazyColumn`. Riesgo añadido: el primer cuadro de cada sitio de llamada puede pintar con la fuente de reserva y relayoutar al llegar la fuente. Medir con una traza.
- Arreglo: una sola `Fuentes(jost, inter, interCifra, plex)` hecha con `remember` en `App`/`TemaVivo` y servida por un `staticCompositionLocalOf`. `jost()` pasa a leer ese local. Los `estiloX()` hacen `remember(color)` o pasan a `val` por tema.
- Esfuerzo: M.

**A2. Dos desenfoques a pantalla completa mientras sube la hoja, y la luz de cables los invalida cada cuadro** (Sospecha, de las más caras)
- `carrera/HojaInferior.kt:445-448,386` y `componentes/Cristal.kt:90`. En no ligero, la hoja flotante hace `hazeEffect` de fondo (todo el alto, con `alpha = progreso()`) y `vidrio()` sobre el mismo `hazeSource` (`Interior.kt:278`, toda la vista). Cada cuadro de la subida recoge y desenfoca ambos, más `shadow(24.dp)` en capa móvil.
- `LuzDeFrontera` (`MapaCarrera.kt:473-521`) repinta el mapa cada cuadro. Esa luz va dentro del `hazeSource` y obliga a rehacer también el cristal de la barra (`BarraCarrera.kt:98`) en reposo. Con la hoja abierta la luz se congela (`congelado`), pero no con la ficha (`seleccionado`) abierta.
- Arreglo: congelar la luz también con `seleccionado != null`. En la hoja, dejar el velo liso (como en `Tema.ligero`) en teléfonos no «altos», o fijar el fondo desenfocado en cuanto la hoja termina de entrar (sin `alpha` por cuadro). Perfilar con la traza de Chrome (Layerize/Paint) antes de decidir.
- Esfuerzo: M.

**A3. El reloj de la luz sigue latiendo con el mapa congelado** (Verificado)
- `mapa/MapaCarrera.kt:474-476`. `rememberInfiniteTransition` con `tween(1_000_000)` pide un cuadro por vsync siempre que el mapa esté compuesto, aunque `congelada()` sea true (hoja abierta, gesto): coste de batería y de despertares.
- Al soltar un gesto, `reloj` salta de golpe (`:493`) y todas las luces cambian de sitio.
- Arreglo: bucle propio `withFrameNanos` dentro de un `LaunchedEffect(congelada)` que solo corre si no está congelada y acumula el tiempo (sin salto). Es el patrón de `rememberRelojAurora`.
- Esfuerzo: S.

**A4. El mapa es invisible para TalkBack y la casilla de semestre no se acierta** (Verificado)
- `mapa/TarjetaMapa.kt:164-228`: cero `semantics` en todo `mapa/`. Cada tarjeta es `pointerInput` más textos sueltos: no hay acción de clic, el estado (aprobada, disponible…) solo está dibujado y se lee «Cálculo», «2 UC» en fragmentos.
- `CabeceraSemestre.kt:142,192`: la casilla es `Role.Checkbox` sin descripción ni `toggleableState`, de 32×150 dp **del plano**, es decir, de ~10 dp con el mapa alejado.
- Arreglo: por tarjeta, `semantics(mergeDescendants = true) { contentDescription = <la misma `fila.descripcion` de la lista>; role = Button; onClick }`. Por casilla, `contentDescription = "Aprobar el semestre N"` y `toggleableState`. Un nodo por tarjeta, no por trazo.
- Esfuerzo: M.

**A5. Botones de icono sin nombre y menores de 44 dp** (Verificado)

| Botón | Archivo:línea | Tamaño | Nombre |
|---|---|---|---|
| Cerrar de Ajustes y del selector de electivas (`BotonCerrar`) | `SelectorElectiva.kt:107-112` | 32 dp | ninguno |
| Cerrar de la ficha | `FichaMateria.kt:282-289` | 40 dp | ninguno |
| Volver de Ajustes | `Ajustes.kt:121-125` | 36 dp | ninguno |
| «Quitar» | `SelectorElectiva.kt:165` | alto 32 dp | texto |
| Pestañas del selector | `SelectorElectiva.kt:185` | alto 32 dp | sin `selected` |
| Buscador del selector | `SelectorElectiva.kt:171` | ≥40 dp | sin etiqueta (el placeholder es un texto aparte) |

- `BotonRedondo` (`EstiloAvance.kt:199`) sí lleva `descripcion`, pero mide 36 dp.
- Arreglo: un solo `BotonCerrar(descripcion = "Cerrar")` de 44 dp de zona táctil (36 dibujados con `Modifier.minimumInteractiveComponentSize()` o `size(44)` y círculo interior). Reutilizar `BotonRedondo` ampliado. Poner `selected` en las pestañas.
- Esfuerzo: S.

**A6. La hoja y la ficha convierten todo su contenido en un nodo clicable** (Sospecha, a probar con TalkBack)
- `HojaInferior.kt:392` y `FichaMateria.kt:203`: `.clickable(indication = null) {}` solo para tragarse toques. `clickable` fusiona descendientes y añade «doble toque para activar» a toda la hoja.
- Arreglo: `pointerInput(Unit) { detectTapGestures { } }` (ya usado en `PantallaCompleta`, `HojaInferior.kt:424`).
- Esfuerzo: S.

## Medio

**M1. Cada tarjeta del mapa monta 4 `Animatable` y 4 `LaunchedEffect`** (Verificado)
- `TarjetaMapa.kt:134-162`: unas 60 tarjetas × 4 = ~240 corrutinas al abrir, aunque casi nunca se anima nada.
- Arreglo: crear el `Animatable` al primer evento (`animaciones.toque != null`) con `remember(animaciones.toque)` condicional, o un único `Animatable` compartido por las animaciones de toque de la tarjeta activa.
- Esfuerzo: S a M.

**M2. `marcarGesto` lanza y cancela una corrutina en cada evento o cuadro** (Verificado)
- `Camara.kt:177-185`, llamado desde `Gestos.kt:89,107` y desde `animarHacia` (`:223`) y `lanzar` (`:294`).
- Arreglo: guardar `ultimoMovimiento` (ms) y mantener un solo `Job` que duerme hasta `ultimo + FIN_GESTO_MS`.
- De paso, `acotar` crea 2 `Pair` y un `Vista` por llamada, y `pellizco()` (`Gestos.kt:44`) hace `filter` en cada evento de un dedo. Es basura por cuadro en pleno arrastre.
- Esfuerzo: S.

**M3. El anillo de graduación recalcula todo cada cuadro con el latido** (Verificado)
- `AnilloGraduacion.kt:200-354`. Con `latidoActivo`, el `Canvas` entero se dibuja en cada cuadro: `a.fichas.filter` ×2, `a.anios.map`, `elegirAnguloNota`, ~25 `measure` (cacheados con `cacheSize = 96`), listas nuevas.
- Arreglo: dividir en dos capas. Estática (anillo, notas, años) con `drawWithCache`, y una capa pequeña solo con el halo de HOY.
- Esfuerzo: M.

**M4. La aurora crea `Array`, `Pair` y `Brush` por luz y por cuadro** (Verificado, coste bajo)
- `componentes/Comunes.kt:201-205`. Tres luces × 60 cuadros × (array de 6 pares + brush). Solo en la bienvenida.
- `Bienvenida.kt:102` usa `pausada = { false }`: nunca se pausa, ni con otro paso con su propio bucle encima.
- Esfuerzo: S.

**M5. Halo de la pestaña activa con `RenderEffect` aunque sea modo ligero** (Verificado)
- `BarraCarrera.kt:176-182`. `desenfoque(6.dp)` no mira `Tema.ligero`. La pestaña lee `luz` en composición (`animateFloatAsState`, `:157`) y recompone 350 ms cada cuadro.
- Arreglo: sin halo en ligero. Leer `luz` en el bloque de dibujo.
- Esfuerzo: S.

**M6. La ficha de la materia es una segunda hoja con su propia física** (Verificado)
- `FichaMateria.kt:151-244` reimplementa entrada, salida, arrastre de cierre, sombra (28 dp frente a 24 dp de `HojaInferior`), radio (`Radio.Campo`) y borde.
- Arrastre: lanza una corrutina por evento (`:218`, `alcance.launch { bajada.snapTo(dy) }`), sin holgura táctil, y no cede al scroll interno.
- Atrás se resuelve aparte en `Interior.kt:116-117`.
- Arreglo: a medio plazo, que la ficha use la `Hoja` de `HojaInferior` (variante «sin velo», `fondo = null`, y sin cristal). Hasta entonces, mover `bajada` a un `mutableFloatState` escrito directamente y compartir sombra y radio.
- Esfuerzo: L.

**M7. «Atrás» depende del orden de registro** (Sospecha)
- `Lista.kt:238-240` ya lo documenta: «se monta después de las hojas, y sin la guarda su Atrás pasaría por delante». Funciona porque `rememberPensum` es asíncrono y `HojasCarrera` se compone después de los `ManejarAtras` de `Interior.kt:116-117`.
- Si `pensum` llegara en caché (síncrono), las hojas se registrarían antes que los de `Interior` y Atrás soltaría la ficha antes que la hoja de avance.
- Arreglo: un único `ManejarAtras` en `Interior` con prioridad explícita (hoja, ficha, ruta, salir), o poner `ManejarAtras(!hojaAbierta && seleccionado != null)`.
- Esfuerzo: S.

**M8. Rotando con la lista abierta se pierde el filtro y el scroll** (Verificado; ver C1)
- `Lista.kt:204,214-216`.

**M9. Contraste de `apagada`** (Verificado, calculado)
- Sobre el lienzo: 3,2 en Oscuro, 3,5 en Negro, 3,6 en Medianoche, 4,3 en Claro. Sobre el panel: 3,1 a 3,4 en los oscuros, 3,8 en Grafito.
- Usos con texto: «VERSIÓN 0.1» (`Ajustes.kt:241`), AM/PM del horario a **8 sp** (`HorarioSemana.kt:373`), años de la cadena (`DetalleCadena.kt:164`), rótulos del vuelo a 9 sp (`VueloHorario.kt:208,364`).
- `tenue` sí cumple en todos los juegos (5,2 a 11,8).
- Arreglo: usar `tenue` para texto y dejar `apagada` para iconos y estados deshabilitados. Subir `apagada` a ≥ 4,5 en los juegos oscuros si se quiere usar en texto.
- Esfuerzo: S.

**M10. La letra del mapa no escala con el sistema** (Verificado, decisión de diseño)
- `TarjetaMapa.kt:65` (`tam(v) = dp.toSp()`). Códigos y UC van a 10 dp. Aceptable por ser dibujo, pero la lista es el único camino para letra grande. Dejarlo dicho en la ayuda o poner un tope en el zoom mínimo.
- Esfuerzo: S (documentar).

## Bajo

- **B1.** Tokens muertos: `Paleta.ambar`, `azulClaro`, `verdeClaro` (`tema/Tema.kt:150-166`, 0 usos). `turquesa` y `aurora`, 1 uso cada uno.
- **B2.** `rejilla()` (`MapaCarrera.kt:293-306`) dibuja unas 30 líneas por cuadro si el `Canvas` se invalida. Está en su capa y no lee estado, así que es barato. Pasar a `drawWithCache` con un solo `Path` no costaría nada.
- **B3.** `CabecerasSemestres` (`MapaCarrera.kt:202-226`) filtra `layout.nodos` por columna en cada recomposición (n columnas × n nodos). Hacerlo con `remember(layout)`.
- **B4.** `contornoDeCarga` crea `Path` y `PathMeasure` por cuadro durante la pulsación larga (`TarjetaMapa.kt:236-244`). Solo una tarjeta a la vez: coste bajo.
- **B5.** `LeerHorario.kt:300` usa 14,5 sp. `VueloHorario` y `HorarioSemana` usan 8 y 9 sp (las reglas fijan 11 sp como mínimo, excepto dibujos).
- **B6.** `Ventana` (`componentes/Ventana.kt`) va en un `staticCompositionLocalOf`: cualquier cambio de tamaño o de letra recompone todo el árbol. Es lo esperado, pero conviene saberlo al valorar el cambio de «Texto grande» en caliente.

## Rendimiento

Ordenado por cuadros que cuesta (más a menos). Todo lo que no es «Verificado» es sospecha y hay que perfilar antes de tocar.

1. **A2**, dos desenfoques a pantalla completa más sombra mientras sube la hoja, con la luz del mapa invalidando el origen del cristal. Sospecha, el mayor candidato.
2. **C2**, JSON y `calcularLayout` en Main durante la animación de entrada: cuadros perdidos al abrir una carrera.
3. **A1**, familias de fuente nuevas por llamada: coste por fila de lista y por cada recomposición (verificado). Posible relayout por fuente de reserva (sospecha).
4. **C1**, rotar rehace layout, horario, cámara y lista.
5. **M3**, anillo entero cada cuadro durante el latido (solo con la hoja de graduación visible).
6. **A3**, reloj de la luz siempre despierto, aunque congelado. Coste continuo (batería) y salto al soltar.
7. **M2**, corrutina y asignaciones por evento en cada arrastre de cámara y por cuadro en la inercia.
8. **M1**, ~240 corrutinas al componer el mapa.
9. **M5** y **M4**, halo `RenderEffect` fuera del modo ligero y aurora con asignaciones por cuadro.
10. Resto (B2 a B4): ruido.

Bien hecho (no tocar): cámara por `graphicsLayer` con lambdas (`MapaCarrera.kt:178`), cables en capa propia con `drawWithCache` (`:408`), opacidad de tarjeta leída al dibujar (`TarjetaMapa.kt:168`), `LazyColumn` con `contentType` y `key` (`Lista.kt:265`), `hazeSource` condicionado a `Tema.ligero` (`Cristal.kt:71`), `grano` e `Interior` sin cristal en ligero, `derivedStateOf` bien usado en `modelo`.

## Coherencia visual

Comparado en capturas `resp-*-360`, `resp-*-acostado-915` y `avance-*`.

1. **Rótulo en mayúsculas: seis implementaciones para un solo rol.** Las reglas piden uno (Inter 500, 11/16, +8 %).
   - `estiloRotulo` (`EstiloAvance.kt:109`)
   - `versal` (`AvanceCarrera.kt:118`: SemiBold, +5 %, interlínea 14)
   - `etiqueta` (`Portada.kt:235`: **Jost**, +14 %)
   - `LetrasHorario.etiqueta` (`Horario.kt:138`: copia de `estiloRotulo`)
   - rótulos del anillo (`AnilloGraduacion.kt:190-198`: +4 % y +6 %)
   - «EN TU MAPA» del selector (`SelectorElectiva.kt:~165`: Jost, +10 %)
   - Se ve en la captura: el «AJUSTES» de Ajustes y el «TU AVANCE» de la hoja no tienen el mismo peso ni espaciado.
2. **Dos escalas de texto en paralelo.** `Tipo.xxs…xxl` (11, 12, 13, 14, 15, 17, 22, 24) más los `estiloX()` (11, 13, 15, 28, 40) y los 12,5, 13,5, 11,5 y 14,5 sueltos (`SelectorElectiva.kt:167,190,275,279,280,291,295`, `FichaMateria.kt:128`, `LeerHorario.kt:300`). La ficha y el selector de electivas usan **Jost para texto de lectura** (nombres de materia a 13,5 sp, notas), contra la regla «Inter es todo lo que se lee».
3. **`LetrasHorario` duplica los roles.** `nombreFila` = `estiloFila`, `cuerpo` = `estiloCuerpo`, `texto` = `estiloNota`, `etiqueta` = `estiloRotulo` (`Horario.kt:138-157`). Además `tituloHoja` es 24/28 y `estiloTitulo` 28/32, y las reglas dicen 28/32 (acostado 24/28). `LeerHorario.kt:172,272` fuerza 36 y 26 sp.
4. **Botones de cerrar y volver: cinco versiones.**

   | Pieza | Tamaño | Detalle |
   |---|---|---|
   | `BotonCerrar` | 32 dp | sin aro |
   | cerrar de la ficha | 40 dp | con aro |
   | `BotonRedondo` | 36 dp | |
   | volver de Ajustes | 36 dp | radio 16, no círculo |
   | `BotonIcono` | 44 dp | |

   El icono de cerrar mide 16, 18 y 20 dp según el botón. Las reglas piden iconos de 18 dp en pastilla de 32 y chevrones de 16.
5. **Dos botones primarios.** `BotonPrincipal` (56 dp, radio 28, Inter 17 SemiBold) y `BotonTinta` (52 dp, radio 26 `Radio.Barra`, estilo de fila). Ambos «lleno en tinta».
6. **Tres sombras y radios flotantes.** Ficha (28 dp de sombra, `Radio.Campo`), hoja (24 dp, 32 dp de esquina flotante o `Radio.Hoja` pegada) y barra (22 dp, 26 dp en riel). No hay regla de concentricidad: la hoja flotante está a 8 dp del borde y su radio de 32 dp no es concéntrico con el de pantalla.
7. **Mismo concepto, palabras distintas.**
   - Selector de la ficha: «Aprobada / Cursando / **Sin cursar**». Pastilla de estado y filtros: «DISPONIBLE», «Disponibles». Lista: «Abre 1» en las capturas vigentes (360); la captura vieja de 412 aún decía «Puedes inscribirla» y la ficha repetía «Puedes inscribirla: tienes aprobadas todas sus prelaciones» junto al candado azul (regla 3). Revisar que la ficha vigente ya no lo diga.
   - Cabecera de semestre del mapa: «SEMESTRE 05», «0/7 APROBADAS»; lista: «Semestre 5», «0 de 7 aprobadas»; mapa horizontal: «0/7».
   - Cierre de pantallas: «Verla en el mapa» frente a «Ver en el mapa» (`alVerEnMapa`).
8. **Ajustes retrato y acostado no coinciden.** Acostado (`Ajustes.kt:178`) lleva el rótulo «TU MAPA» sobre «Tu carrera» y «Personalizar»; retrato no lo lleva (`:197-203`).
9. **Márgenes laterales.** Lista 16 (`Espacio.m` fijo, `Lista.kt:~299`) frente a `margenPantalla()` 16/20/24 en el resto (Ajustes, Horario). En 360 dp la lista debería ser 20.
10. **Pastillas de estado de la lista** (23, 2, 6, 18 en 4 columnas iguales a 360 dp) llegan casi al borde de la pantalla; las filas de abajo están a 16 dp, la cabecera a ~12 dp (captura `resp-lista-360`).
11. **Cabecera de «¿Cuándo me gradúo?»** (`resp-graduacion-360`): flecha de volver redonda en círculo propio y título al lado; las otras hojas internas llevan X a la derecha y rótulo arriba. Dos patrones de cabecera en la misma hoja.

## Redundancia y código muerto

- Ficha frente a `HojaInferior` (M6).
- `LetrasHorario` frente a los `estiloX()` (coherencia 3).
- `BotonCerrar`, `BotonRedondo`, `BotonIcono` y la X de la ficha (coherencia 4); `BotonPrincipal` y `BotonTinta` (5).
- Tokens muertos (B1).
- `HojaInferior.kt:380` pinta un `drawBehind` de 1 px solo para el modo no flotante con `Paleta.panel`: es un parche del recorte, no un duplicado, pero comparte forma con `panelPegado`.

## Lo que no pude comprobar

- No hay captura de giro en caliente, de teclado abierto en el buscador del selector, de letra 1,3 en 320 dp en el mapa, ni de toques a 44 dp en pantalla: lo anterior sobre ellos sale del código.
- Los costes de GPU de A2 y del cambio de fuentes de A1 requieren la traza de Chrome por CDP (memoria `mapa-pensum-medir-rendimiento`).
- Comportamiento de TalkBack en A4 y A6: leído en la semántica de Compose, no probado en dispositivo.
