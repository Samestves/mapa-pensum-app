# Rendimiento: 60 fps estables en gama baja

Auditoría de solo lectura del 7-nov, sobre `dev` (`dfe0a40`). Rutas relativas a `composeApp/src/commonMain/kotlin/mpensum/` salvo que digan otra cosa. No se ejecutó gradle ni adb: lo que dice **Verificado** está leído en el código o en el APK de `mapa-pensum.apk` (7-nov 11:36); lo que dice **Sospecha** hay que medirlo en el teléfono con la sección final antes de dar por bueno el arreglo.

Contexto que manda todo lo demás:

- **Strong skipping está activo** (Kotlin 2.3, sin bloque `composeCompiler` en `composeApp/build.gradle.kts`): un parámetro inestable no impide saltarse un composable, pero se compara por identidad (`===`). El problema no es «List es inestable», sino crear instancias nuevas en cada recomposición.
- **El modo ligero ya existe** y cubre bien el cristal, el grano, los bucles y la opacidad de las tarjetas. El riesgo real es que **no se active** en un teléfono flojo (H10) y que varias cosas caras no miren `Tema.ligero`.
- **El teléfono de prueba (Helio G85, 120 Hz)** da 8,3 ms por cuadro. A 60 Hz son 16,7 ms. La política propuesta: en modo ligero, pedir 60 Hz (H10).

## 1. Estado de la auditoría anterior (`qa-interfaz.md`)

| Punto | Estado | Dónde se ve |
|---|---|---|
| C1 girar rehace la carrera | **Cerrado** | `EstadoCarrera.kt:56-80` (`compacto` mutable, layout por `derivedStateOf`), `Interior.kt:92,120` |
| C2 JSON y layout en Main | **Cerrado** | `EstadoCarrera.kt:221-235` y `datos/Pensum.kt:89-90` (`Dispatchers.Default`). Queda la composición del mapa a mitad de la transición: H2 |
| A1 `FontFamily` por llamada | **Cerrado** | `tema/Tema.kt:293-322` (`LocalFuentes`). Quedan 103 `TextStyle(` en línea en 25 archivos: coste bajo, deuda de mantenimiento (A-4) |
| A2 dos desenfoques y la luz invalidando | **Parcial** | La luz ya se congela con la ficha (`MapaCarrera.kt:197`). Siguen abiertas las hojas del horario con dos desenfoques y sombra (H6) y la luz en reposo, que obliga a redibujar el cristal de la barra en cada cuadro (H5) |
| A3 reloj de la luz siempre despierto | **Cerrado** | `MapaCarrera.kt:534-548` (`withFrameNanos` dentro de `snapshotFlow`, sin salto) |
| M1 4 `LaunchedEffect` por tarjeta | **Cerrado** | `TarjetaMapa.kt:147-166` (efectos condicionales). Quedan 4 `Animatable` por tarjeta (≈240 objetos): coste bajo |
| M2 corrutina por evento | **Cerrado** | `Camara.kt:180-195` (un solo vigilante). Queda basura menor: `acotar` crea 2 `Pair` y una `Vista` por evento (`Camara.kt:167-177`) y `pellizco()` hace `filter` por evento (`Gestos.kt:45`) |
| M3 anillo entero por cuadro | **Abierto** | `AnilloGraduacion.kt:199-260`. Ya va en su capa (no repinta la hoja), pero el `Canvas` entero corre en cada cuadro: H16 |
| M4 aurora con basura por cuadro | **Abierto** | `componentes/Comunes.kt:201-205` (array + pares + `Brush` por luz y cuadro); `bienvenida/Bienvenida.kt:102` sigue con `pausada = { false }` |
| M5 halo con `RenderEffect` | **Parcial** | En ligero ya no hay halo (`BarraCarrera.kt:172`), pero `BarraCarrera.kt:183` lee `luz.value` en composición: la pestaña se recompone en cada cuadro durante 350 ms (H14) |
| M6 la ficha con física propia | **Cerrado** | `carrera/FichaMateria.kt:61-87` usa `HojaInferior(modal = false)` |
| B2 rejilla | Abierto, ruido | `MapaCarrera.kt:324-348`: no lee estado por cuadro, no tocar |
| B3 cabeceras filtran por columna | **Abierto** | `MapaCarrera.kt:212-216` (H21) |
| B4 `Path` por cuadro en la carga | Abierto, bajo | `TarjetaMapa.kt:246-253` |

## 2. Hallazgos priorizados

Coste en cuadros: **alto** = puede tumbar varios cuadros seguidos o sostener el redibujo en reposo; **medio** = pierde cuadros sueltos en un momento concreto; **bajo** = ruido o solo en el arranque.

| Id | Hallazgo | Archivo:línea | Coste | Arreglo concreto | Esfuerzo |
|---|---|---|---|---|---|
| H1 | **Una capa fuera de pantalla por tarjeta apagada** (fuera del modo ligero). Con el foco puesto, unas 50 tarjetas bajan a 0,14; con «Lo aprobado: Atenuado», todas las aprobadas se quedan siempre en 0,45. En la estrategia `Auto`, `alpha < 1` = `saveLayer` por tarjeta, en cada cuadro de la cámara. En una GPU por teselas (Mali-G52) cada capa es un cambio de destino de render. Verificado en código; coste, sospecha (de los mayores) | `mapa/TarjetaMapa.kt:171-175`, `mapa/MapaCarrera.kt:155-160,260-264` | **alto** | `compositingStrategy = CompositingStrategy.ModulateAlpha` siempre, no solo en ligero. La tarjeta tiene fondo opaco: a 0,14 no se nota la diferencia; a 0,45, revisar con una captura `atenuar-*` | S |
| H2 | **Entrar a una carrera apila 3 capas a pantalla completa más la primera composición del mapa.** `AnimatedContent` funde las dos pantallas con `alpha` (2 capas fuera de pantalla), y el plano del mapa entra con `alpha = llegada` durante 620 ms (otra capa fuera del modo ligero). El pensum llega del hilo de fondo a mitad de los 420 ms, y en ese cuadro se componen 60 tarjetas con 3 `BasicText` cada una (≈180 maquetaciones de texto), semántica y `pointerInput`, y se arman dos veces los `Path` de los cables (H13). Sospecha fuerte | `App.kt:168-183`, `mapa/MapaCarrera.kt:184-193`, `carrera/Interior.kt:298-306` | **alto** | (a) `ModulateAlpha` también en el plano del mapa (`MapaCarrera.kt:192`, quitar el `if`). (b) No componer `CapaVista` hasta que la transición de `App` acabe: pasar `entrando = transition.isRunning` desde el `AnimatedContent` de `App` (es su `AnimatedContentScope`) a `Interior` y, mientras sea true, pintar solo el lienzo. La «llegada» de 620 ms ya hace de entrada del mapa | M |
| H3 | **La lista virtualiza por semestre, no por fila.** Cada `item` del `LazyColumn` es una sección entera, con 6 a 10 filas en una `Column` normal. Cada fila monta 3 `Animatable`, 2 `animateColorAsState`, 2 `LaunchedEffect`, un `AnimatedVisibility` siempre compuesto (aunque `conDeshacer` sea false) y 3 textos. Al entrar una sección por scroll, o al desplegarla, todo eso se compone en un solo cuadro | `carrera/Lista.kt:259-271`, `carrera/ListaFilas.kt:224-240,412-443,490` | **alto** | Corto plazo (S): `if (conDeshacer) AnimatedVisibility(...)` → componer el botón solo cuando hace falta, con `AnimatedVisibility` de estado propio si se quiere la salida; `Animatable` de `pop`, `brillo` y `sacudida` creados al primer cambio. De fondo (L): aplanar en items `cabecera`, `fila` y `casilla` con `contentType` y `key`, y el fondo de grupo dibujado por fila (esquinas según primera/última) | S + L |
| H4 | **Dos `hazeSource` anidados en la lista.** La lista graba su propio origen (`local`) y además vive dentro del origen de `Interior`. En cada cuadro de scroll se redibuja el desenfoque de la cabecera de filtros y el de la barra | `carrera/Lista.kt:254,496-500`, `carrera/Interior.kt:300`, `componentes/Cristal.kt:90` | **alto** (fuera de ligero) | Esmerilar solo en reposo: con `lista.isScrollInProgress`, la cabecera usa `tinteLiso`, y el `hazeEffect` entra al parar (el `alpha` ya existe). Igual para la barra: `blurEnabled = !desplazando`. Medir antes y después en el escenario «scroll lista» | M |
| H5 | **La luz de los cables, en reposo, redibuja el cristal de la barra en cada cuadro, y a 120 Hz.** `withFrameNanos` va al ritmo de la pantalla: en el G85, 120 cuadros por segundo con la app quieta, y cada uno invalida el `hazeSource` y obliga a rehacer el desenfoque de la barra | `mapa/MapaCarrera.kt:197,534-548`, `carrera/BarraCarrera.kt:128` | **alto** en reposo (batería y calor que luego bajan la frecuencia) | (a) Topar la luz a 30 cuadros como la aurora (`CUADRO_NS`, `Comunes.kt:108,174`): acumular tiempo y solo escribir `reloj` cada ≥33 ms. (b) Apagar la luz tras 8 s sin tocar el mapa, que vuelve con el siguiente gesto (añadir `quieto > 8 s` a `congelada`) | S |
| H6 | **Hojas del horario: dos desenfoques más una sombra de 24 dp que se mueven.** `FondoHoja` desenfoca la pantalla entera y la hoja flotante desenfoca el mismo origen bajo ella (`vidrio`). Al subir, la hoja se mueve: su desenfoque se rehace en cada cuadro de la entrada. El del fondo solo cambia de `alpha` (HWUI lo guarda en caché) | `carrera/HojaInferior.kt:398,403,458-466`, `carrera/HojasHorario.kt:94-97` | **medio** | Mientras `hoja.desp != 0f`, la hoja usa `tinteLiso` y el desenfoque entra al asentarse (un `derivedStateOf { hoja.desp == 0f }`). La sombra de 24 dp, en ligero, pasa a `sombraSuave()` | M |
| H7 | **`fundidoAbajo` siempre fuera de pantalla.** `CompositingStrategy.Offscreen` fijo: la columna entera se pinta en una capa aparte en cada cuadro de scroll, aunque `canScrollForward` sea false. Además, `Portada.kt` tiene su propia copia | `componentes/Fundido.kt:19-31`; usos en `carrera/AvanceCarrera.kt:296`, `carrera/AvanceIndice.kt:124`, `carrera/DetalleGraduacion.kt:125,129`; copia en `portada/Portada.kt:418-434` | **medio** | En el bloque: `compositingStrategy = if (desplazo.canScrollForward) Offscreen else Auto`. Mejor aún, sobre panel opaco (las hojas pegadas): sin capa, un degradado de `Paleta.panel` encima. Borrar la copia de `Portada.kt` y usar `fundidoAbajo(desplazo, 40.dp)` | S |
| H8 | **Fuentes comprimidas en el APK.** `inter.ttf` (876 KB) va `Defl:N` (se queda en 459 KB). `Typeface.Builder(AssetManager, ruta)` infla el archivo entero en memoria por cada `Font` que se resuelve: hasta 6 copias de Inter (4 pesos y 2 cifras, ≈5 MB) y 7 de Jost, cada una en el hilo principal la primera vez que se usa ese peso. Verificado en el APK | `composeApp/build.gradle.kts` (falta `noCompress`), `tema/Tema.kt:324-351` | **medio** (arranque y primer uso de cada peso) | `android { androidResources { noCompress += "ttf" } }`: el asset queda en el APK sin comprimir y se mapea sin copiar. Y recortar Inter a latino con los ejes `wght` y `opsz` (`pyftsubset --unicodes=U+0000-00FF,U+2010-2027,U+20AC`): de 876 KB a unos 200 KB | S |
| H9 | **Los 8 planes de graduación se calculan en el hilo principal** la primera vez que se abre «Tu avance»: `IndiceYDetalle` lee `estado.graduacion` en composición, y eso dispara `planesPorCarga` (8 × `estimarGraduacion`) en el primer cuadro de la subida de la hoja | `carrera/AvanceIndice.kt:109-111`, `carrera/EstadoCarrera.kt:118-132`, `carrera/AvanceCarrera.kt:493-494` | **medio** | Precalentar fuera del hilo: en `Interior`, `LaunchedEffect(estado) { snapshotFlow { Triple(estado.marcas, estado.casillas, estado.hoy) }.collectLatest { withContext(Dispatchers.Default) { estado.planes } } }`. Al leerse en otro hilo, el `derivedStateOf` deja el resultado en caché. Mejor: que `planes` sea un estado que se llena desde fondo y que la hoja muestre `RESERVA_DETALLE` mientras llega (como `Calculado`) | M |
| H10 | **La detección de gama baja es frágil.** El G85 entra en ligero solo porque su núcleo rápido marca exactamente 2,0 GHz (`khz in 1..2_000_000`). Un Snapdragon 680 (Adreno 610) con 4 GB (8 núcleos, 2,4 GHz, ~3,7 GB de `totalMem`) **no** entra, y lleva H1, H4, H5 y H6 enteros. Tampoco se usa el refresco: a 120 Hz un teléfono flojo tiene la mitad de presupuesto | `androidMain/.../plataforma/Plataforma.android.kt:219-240`, `androidMain/.../MainActivity.kt` | **alto** (decide todo lo demás) | Ver el plan, tanda 3, paso 1: `Build.SOC_MODEL` con una lista de SoC modestos, `Build.VERSION.MEDIA_PERFORMANCE_CLASS` como señal de gama alta y núcleo rápido < 2,2 GHz con RAM < 6 GB. En ligero, pedir 60 Hz a la ventana | S |
| H11 | **Pellizcar con el mapa alejado vuelve a rasterizar ≈180 textos por cuadro.** Al escalar una capa con texto, Skia rehace los glifos al tamaño nuevo en cada cuadro. Con el mapa alejado se ven las 60 tarjetas con letra ilegible. Sospecha, hay que medirlo (en la traza, el `RenderThread` se alarga durante el pellizco) | `mapa/TarjetaMapa.kt:227-236`, `mapa/MapaCarrera.kt:184-193` | **medio** | Nivel de detalle: en `MapaCarrera`, `val lejos by remember { derivedStateOf { camara.escala < 0.32f } }` (solo recompone al cruzar el umbral); con `lejos`, la tarjeta no compone sus `BasicText`: forma, borde, icono y punto | M |
| H12 | **La luz crea `Stroke`, `FloatArray` y un `DashPathEffect` nativo por cable y por cuadro** (×120 por segundo en el G85) | `mapa/MapaCarrera.kt:579-585` | **medio-bajo** | Sin `PathEffect`: el guion es `medida.getSegment(inicio, fin, parte)` sobre un `Path` reutilizado (como `CableDibujo.hasta`, `:376-380`), con un `Stroke` creado una vez en el `drawWithCache` | S |
| H13 | **Los `Path` de los cables se arman dos veces** (cables y luz) en el hilo principal en el primer dibujo, y la luz los rehace enteros en cada marca porque su caché lee `estado.situaciones` | `mapa/MapaCarrera.kt:457,552-557` | **medio-bajo** (primer cuadro del mapa, cada marca) | `val cables = remember(estado.layout, estado.colores) { cablesDe(estado) }` en `MapaCarrera`, pasado a `Cables` y a `LuzDeFrontera`; en la luz, la caché solo filtra las fronteras | S |
| H14 | La pestaña lee `luz.value` en composición: recompone en cada cuadro durante 350 ms al cambiar de vista | `carrera/BarraCarrera.kt:183` | bajo | Pasar el color como lambda: `IconoRelleno` con `color: () -> Color`, o un `drawWithContent` con `ColorFilter.tint` leído al dibujar | S |
| H15 | **Las secciones de la lista se recomponen todas en cada marca.** `SeccionLista`, `FilaMateria` y `ModeloLista` son `data class` con `List`, así que son inestables: se comparan por identidad, y `modeloDeLista` las crea todas de nuevo en cada marca | `logica/Lista.kt:45-93`, `carrera/Lista.kt:201-205,262` | **medio** | Archivo de estabilidad (plan, tanda 3, paso 4), que declara estables `kotlin.collections.*`, `mpensum.datos.*` y `mpensum.logica.*`: el compilador compara con `equals` y las secciones iguales se saltan | S |
| H16 | M3 sigue: con latido, el anillo rehace 2 `filter`, `map`, ~25 `measure` (en caché) y listas en cada cuadro | `carrera/AnilloGraduacion.kt:199-260` | medio (solo con esa pantalla) | Dos capas: estática en `drawWithCache` (anillo, notas, años) y una de 24 dp solo con el halo de HOY. O en ligero, sin latido (ya pasa con `sinBucles`) | M |
| H17 | `PuntoQueLate` corre a 120 Hz mientras haya una clase en curso, dentro del origen del cristal: redibuja la barra en cada cuadro (fuera de ligero) | `carrera/Horario.kt:259,365-378` | bajo-medio | Topar a 30 cuadros como la aurora, o 3 latidos y quieto | S |
| H18 | El índice de carreras se decodifica en Main al arrancar (sin `withContext`), y la primera pantalla espera a él (`if (carreras.isEmpty()) return@Box`) | `datos/Carreras.kt:43-47`, `App.kt:167` | bajo (arranque) | `withContext(Dispatchers.Default)` como `leerPensumDelPaquete` | S |
| H19 | Desenfoque `RenderEffect` en la entrada del titular de la guía, aunque sea ligero | `bienvenida/Bienvenida.kt:203`, `bienvenida/PasoCarrera.kt:175` | bajo (una vez) | `renderEffect = if (Tema.sinDesenfoque) null else desenfoque(...)` | S |
| H20 | Aurora: M4 sigue (basura por luz y cuadro; nunca se pausa) | `componentes/Comunes.kt:201-205`, `bienvenida/Bienvenida.kt:102` | bajo | Paradas en un `Array` reutilizado (`remember`), y el `Brush` se crea solo si cambió el color | S |
| H21 | B3: `CabecerasSemestres` filtra nodos por columna (n×n) en cada recomposición | `mapa/MapaCarrera.kt:212-216` | bajo | `remember(layout) { layout.nodos.groupBy { it.semestre } }` | S |
| H22 | `ZonaSegura` es una clase normal y se crea en cada recomposición de `Interior`: `BurbujaFicha` nunca se salta | `carrera/Interior.kt:224-227`, `carrera/BurbujaFicha.kt:70` | bajo | `data class` | S |
| H23 | El manifiesto no es `profileable`: Perfetto (lo que pinta la app) y Macrobenchmark no pueden mirar la versión de verdad | `androidMain/AndroidManifest.xml` | — (bloquea medir) | `<profileable android:shell="true" />` dentro de `<application>` | S |
| H24 | **Perfil de arranque escrito con comodines.** Tras expandirlo y pasarlo por R8, marca ~9 900 métodos calientes de 17 163 (57 % del dex) y 2 638 clases. Todo es «S», así que no hay perfil de arranque para ordenar el dex. Y en APK instalado a mano, `profileinstaller` solo deja el perfil escrito: ART compila en el mantenimiento en reposo (cargando, de noche), no al instalar. Verificado en `build/intermediates/r8_art_profile` y en el `.prof` del APK | `composeApp/src/androidMain/baselineProfiles/baseline-prof.txt` | medio (arranque los primeros días) | Módulo `:baselineprofile` con Macrobenchmark (plan, tanda 3, pasos 6 y 7). Hasta entonces, instalar con el `.dm` (sección 5) para que la compilación sea inmediata | L |

Bien hecho, no tocar: la cámara solo por `graphicsLayer` con lambdas (`MapaCarrera.kt:184-193`); la burbuja, colocada en `graphicsLayer` (`BurbujaFicha.kt:180-198`); los cables en su capa con `drawWithCache` (`:456`); la opacidad por lambda sin recomponer (`:155-160`); el vigilante único de gesto (`Camara.kt:180`); el tema que cambia con una foto en vez de animar colores (`tema/Fundido.kt`); la barra que se oculta a `alpha 0` (HWUI no la dibuja); `Calculado` para lo pesado de las pantallas internas; R8 activo (`isMinifyEnabled`, `isShrinkResources`; AGP 8.13 ya trae el modo completo de serie).

## 3. Efectos activos por pantalla (fuera del modo ligero)

| Pantalla | Desenfoques | Capas fuera de pantalla | Sombras | Bucles por cuadro |
|---|---|---|---|---|
| Mapa en reposo | 1 (barra, rehecho en cada cuadro por la luz: H5) + halo de 6 dp en la pestaña activa (en caché) | 0; ~30 con «Atenuado» (H1) | barra 22 dp | luz (120 Hz) |
| Mapa con ficha | 0 (la barra va a alpha 0) | ~50 tarjetas mientras dura el foco (H1) | `sombraSuave` (degradado, barato) | ninguno (luz congelada) |
| Entrar a una carrera | 0-1 | 2 a pantalla completa (fundido de `App`) + plano del mapa 620 ms (H2) | — | llegada |
| Lista con scroll | 2, rehechos en cada cuadro (H4) | 0 | barra 22 dp | ninguno |
| Horario con hoja | 2 + el de la barra en caché (H6) | 0 | 24 dp en movimiento | `PuntoQueLate` si hay clase (H17) |
| Hoja «Tu avance» | 1 a pantalla completa (en caché si el mapa está quieto) | `fundidoAbajo` en cada scroll (H7) | 24 dp | latido del anillo (H16) |
| Portada | 1 (cabecera, solo con scroll) | `OtrasCompactas` acostada (H7) | — | ninguno |
| Guía | titular al entrar (H19) | `TomaEnBucle` y pasos, en cada cuadro | — | aurora a 30 fps, escenas |

**Política para gama baja (lo que va liso siempre con `Tema.ligero`).** Ya está: sin `hazeSource` ni `hazeEffect`, sin grano, sin bucles, `ModulateAlpha` en tarjetas y plano. Falta añadir:

1. Sin `RenderEffect` en ningún sitio: H19.
2. Sin sombras de elevación de más de 8 dp: la hoja modal y la barra pasan a `sombraSuave()` o a un borde. `shadow()` en una capa que se mueve se vuelve a teselar en cada cuadro.
3. `fundidoAbajo` sin capa: H7.
4. 60 Hz pedidos a la ventana: H10.
5. Texto del mapa fuera por debajo de la escala 0,32: H11.

Y en cualquier modo, sin desenfoque mientras algo se mueve encima o debajo (hoja entrando, lista desplazándose): el cristal se esmerila al quedarse quieto (H4, H6).

## 4. Plan de arreglo en 3 tandas independientes

Cada tanda toca archivos distintos de las otras dos y puede ir en su propia rama. Al acabar cada una: `./gradlew :composeApp:assembleRelease` (compila y pasa lint y pruebas, según el proyecto) y las capturas de la tanda. Nada de capturas píxel a píxel de toda la app.

### Tanda 1: mapa (solo `mapa/`)

1. **H1.** `TarjetaMapa.kt:171-175`: borrar el `if (Tema.ligero)` y dejar `compositingStrategy = CompositingStrategy.ModulateAlpha` siempre. Comentario: «la tarjeta es opaca: modular el alfa evita una capa por tarjeta».
2. **H2 (a).** `MapaCarrera.kt:192`: lo mismo con el plano (`ModulateAlpha` siempre).
3. **H13.** En `MapaCarrera`, `val cables = remember(estado.layout, estado.colores, Tema.juego, Tema.contraste) { cablesDe(estado) }`. `Cables(...)` y `LuzDeFrontera(...)` reciben `cables: List<CableDibujo>` en vez de llamar a `cablesDe` dentro de su `drawWithCache`.
4. **H12.** En `LuzDeFrontera`, quitar `PathEffect.dashPathEffect`. Añadir a `CableDibujo` un `fun trozo(desde: Float, hasta: Float): Path` que reutiliza `parte` con `medida.getSegment(desde, hasta, parte, true)`. Dibujar `drawPath(c.trozo(ini, fin), vivos[k], alpha, style = trazoLuz)`, con `trazoLuz = Stroke(2.25f, cap = StrokeCap.Round)` creado en el `drawWithCache`. El guion va de `largo·p − guion` a `largo·p`, acotado a `[0, largo]`.
5. **H5.** En el bucle de `LuzDeFrontera`: solo `reloj += ...` si han pasado ≥ 33 ms desde la última escritura (acumular `n - antes` y escribir al pasar el umbral, igual que `Comunes.kt:171-181`). Añadir un `var ultimoToque by remember { mutableLongStateOf(ahora) }` que actualiza `camara.enGesto` (vía `snapshotFlow`), y en `congelada` sumar `|| quietoMasDe8s`. La forma más simple: un `LaunchedEffect` con `delay(8_000)` que se reinicia con cada `enGesto == true` y pone `dormida = true`.
6. **H21.** `CabecerasSemestres`: `val porColumna = remember(layout) { layout.nodos.groupBy { it.semestre } }`, y `marcasDeSemestres(...)` dentro de `remember(layout, estado.estados, estado.casillas)`.
7. **H11** (último, opcional si la traza no lo pide). En `MapaCarrera`, `val lejos by remember { derivedStateOf { camara.escala < 0.32f } }`; pasar `conTexto = !lejos` a `TarjetaMapa` y, si es false, no componer los tres `BasicText`. Captura del mapa alejado (`ZOOM_MIN`) para ver que no queda raro.

Hecho cuando: compila, las capturas del mapa (`CapturaMapa*`, foco, «Atenuado») se ven igual, y en la traza (sección 5, escenario B) el mapa con foco no tiene `saveLayer` por tarjeta.

### Tanda 2: cristal, hojas y lista (`componentes/Fundido.kt`, `portada/Portada.kt`, `carrera/HojaInferior.kt`, `carrera/Lista.kt`, `carrera/ListaFilas.kt`, `carrera/BarraCarrera.kt`, `carrera/Horario.kt`, `carrera/Interior.kt`)

1. **H7.** `Fundido.kt`: `graphicsLayer { compositingStrategy = if (desplazo.canScrollForward) CompositingStrategy.Offscreen else CompositingStrategy.Auto }`. Crear el `Brush` en un `drawWithCache` (según `size`). `Portada.kt:418-434`: sustituir el bloque por `.fundidoAbajo(desplazo, 40.dp)`.
2. **H3 (corto).** `ListaFilas.kt:490`: `if (conDeshacer || deshacerVisible)` alrededor del `AnimatedVisibility`, con `deshacerVisible` un `MutableTransitionState` recordado para que la salida se vea. Mejor aún, `AnimatedVisibility(visibleState = estado)` y componerlo solo mientras `estado.currentState || estado.targetState`. `pop`, `brillo` y `sacudida`: `remember { Animatable(…) }` pasa a crearse dentro del `LaunchedEffect` la primera vez (un `var` de tipo `Animatable?` en `remember { arrayOfNulls(3) }`), y el `graphicsLayer` lee `?.value ?: 1f`.
3. **H4.** `Lista.kt:496-500`: dentro del bloque de `hazeEffect`, `blurEnabled = vidrio.value > 0f && !lista.isScrollInProgress`. Con el desenfoque apagado, Haze pinta el tinte; comprobar que el tinte de `ESTILO_VELO` tiene `fallbackTint`. Para la barra: `Interior` le pasa `desplazando: () -> Boolean` (de la lista; del mapa, `camara.enGesto`), y `vidrio()` (`Cristal.kt:90`) recibe ese lambda y lo usa en `blurEnabled`. Si cambiar `Cristal.kt` choca con otra tanda, va aquí: ninguna otra lo toca.
4. **H6.** `HojaInferior.kt`: `val asentada by remember { derivedStateOf { hoja.desp == 0f } }`. Con `cristal != null && !asentada`, la hoja usa `Modifier.tinteLiso(ESTILO_CRISTAL)` en vez de `vidrio(...)`. En ligero, la sombra de `:398` pasa a `sombraSuave()`.
5. **H14.** `BarraCarrera.kt:183`: `IconoRelleno` gana un parámetro `color: () -> Color` (o una variante), y se dibuja leyendo `lerp(REPOSO, ACTIVO, luz.value)` en la fase de dibujo.
6. **H17.** `Horario.kt:365-378`: el latido escribe a 30 cuadros como mucho (bucle `withFrameMillis` con umbral de 33 ms en vez de `rememberInfiniteTransition`).
7. **H22.** `BurbujaFicha.kt:70`: `data class ZonaSegura`.

Hecho cuando: compila, las capturas de la lista, el horario con hoja y el avance se ven igual en reposo, y en el escenario C (sección 5) el scroll de la lista baja de percentil 90.

### Tanda 3: arranque, detección y medición (`composeApp/build.gradle.kts`, `AndroidManifest.xml`, `androidMain/.../Plataforma.android.kt`, `MainActivity.kt`, `datos/Carreras.kt`, `carrera/EstadoCarrera.kt`, `carrera/AvanceIndice.kt`, `bienvenida/*`, `componentes/Comunes.kt`, archivo de estabilidad nuevo, módulo nuevo)

1. **H10, la detección.** En `medirModesto`, antes de las señales de memoria:
   ```kotlin
   if (Build.VERSION.MEDIA_PERFORMANCE_CLASS >= Build.VERSION_CODES.S) return@runCatching false  // certificado: no es gama baja
   val soc = Build.SOC_MODEL.uppercase()   // "MT6769", "SM6225"…; "UNKNOWN" en algunos
   if (SOC_MODESTOS.any { soc.startsWith(it) }) return@runCatching true
   ```
   con `SOC_MODESTOS = listOf("MT676", "MT6765", "MT6762", "MT6761", "MT6768", "MT6769", "MT6781", "SM4", "SM6115", "SM6125", "SM6225", "SDM4", "SDM6", "SDM450", "UMS", "T6", "T7", "S5E3830")` (Helio G y P, Snapdragon 4xx y 6xx bajos, Unisoc, Exynos 850; comentar que es una lista a mano). Cambiar la señal de CPU por «núcleo más rápido < 2 200 000 kHz y `totalMem` < 6 GB»: hoy el G85 entra por un empate exacto.
2. **H10, 60 Hz.** En `MainActivity.onCreate`, tras `aplicarAlTema`: si `Tema.ligero`, elegir de `display.supportedModes` el modo de la misma resolución con `refreshRate` más cercano a 60 y fijar `window.attributes = window.attributes.also { it.preferredDisplayModeId = modo.modeId }`. Si el modo ligero cambia a mano en Personalizar, reaplicarlo (un `SideEffect` en `BarrasDelSistema` sirve).
3. **H8.** En `android { }`: `androidResources { noCompress += "ttf" }`. Y en `herramientas/`, un script de subconjunto de Inter (`pyftsubset inter.ttf --unicodes="U+0000-00FF,U+2010-2027,U+20AC,U+2192" --layout-features="*" --output-file=inter.ttf`), con los ejes intactos.
4. **H15, estabilidad e informes.** Crear `composeApp/estabilidad-compose.conf`:
   ```
   // Inmutables de hecho: se comparan por equals y no por identidad
   kotlin.collections.List
   kotlin.collections.Map
   kotlin.collections.Set
   mpensum.datos.*
   mpensum.logica.*
   ```
   y en `composeApp/build.gradle.kts`:
   ```kotlin
   composeCompiler {
       stabilityConfigurationFiles.add(layout.projectDirectory.file("estabilidad-compose.conf"))
       reportsDestination.set(layout.buildDirectory.dir("informes-compose"))
       metricsDestination.set(layout.buildDirectory.dir("informes-compose"))
   }
   ```
   Revisar `build/informes-compose/*-composables.txt` y buscar `restartable` sin `skippable` en `TarjetaMapa`, `SeccionDeLista`, `FilaDeMateria`, `BarraCarrera`, `HojaInferior`, `CabeceraSemestre`.
5. **H23, H18, H9, H19, H20.** Añadir `<profileable android:shell="true" />` al manifiesto. `rememberCarreras` con `withContext(Dispatchers.Default)`. Para H9, en `Interior`: `LaunchedEffect(estado) { if (estado != null) snapshotFlow { Triple(estado.marcas, estado.casillas, estado.hoy) }.collectLatest { delay(600); withContext(Dispatchers.Default) { estado.graduacion } } }` (deja `planes` y `graduacion` en caché fuera del hilo antes de que nadie abra la hoja). `Bienvenida.kt:203` y `PasoCarrera.kt:175`: `if (Tema.sinDesenfoque) null else desenfoque(...)`. Aurora: un `Array` de paradas reutilizado.
6. **H24, el perfil generado (necesita el teléfono).** Módulo `:baselineprofile` (`com.android.test`, `androidx.baselineprofile`, `targetProjectPath = ":composeApp"`, dependencia `androidx.benchmark:benchmark-macro-junit4` y `androidx.test.uiautomator:uiautomator`, ambas en su última versión estable). En `composeApp`: plugin `androidx.baselineprofile`, `dependencies { "baselineProfile"(project(":baselineprofile")) }`, `baselineProfile { saveInSrc = true; automaticGenerationDuringBuild = false }`. En la raíz de `App()`: `Modifier.semantics { testTagsAsResourceId = true }`, para que UiAutomator encuentre los `testTag`. Generador:
   ```kotlin
   @get:Rule val regla = BaselineProfileRule()
   @Test fun generar() = regla.collect("com.mapapensum.app", includeInStartupProfile = true) {
       pressHome(); startActivityAndWait()
       // portada → tu carrera → mapa: arrastrar → tocar tarjeta → lista: desplazar → avance
   }
   ```
   `./gradlew :composeApp:generateReleaseBaselineProfile` escribe `baseline-prof.txt` y `startup-prof.txt`. Con KMP, comprobar en qué carpeta los deja y apuntar `sourceSets["main"].baselineProfiles` a esa carpeta; el perfil escrito a mano se borra cuando el generado esté dentro.
7. **Benchmarks** en el mismo módulo: `StartupTimingMetric` con `CompilationMode.None()` frente a `CompilationMode.Partial(BaselineProfileMode.Require)`, y `FrameTimingMetric` en tres escenarios (arrastrar el mapa, desplazar la lista, abrir y cerrar «Tu avance»), 10 iteraciones cada uno.

Hecho cuando: el TECNO KL5 entra en ligero por `SOC_MODEL` (no por los 2,0 GHz), corre a 60 Hz en ligero, y los benchmarks dan cifras (sección 5).

### Tanda 4: los picos del hilo de UI al abrir y navegar (8-nov)

Medido en el Redmi Note 15 Pro: abrir «Tu avance» con un cuadro de 133 ms, ir a «Tu próximo semestre» y volver con p99 de 101 ms, y entrar a la carrera con uno de 150 ms. GPU p50 7 ms: era CPU. `carrera/CosteCuadrosTest` (Robolectric) recorre los tres desde la portada con un `CompositionTracer` y deja en `build/capturas/coste-cuadros.txt` cuánto cuesta cada cuadro y qué se compuso. Cada pico era **un solo cuadro que creaba todos los nodos y medía todos los textos de una pantalla**; la lógica ya venía precalculada, salvo la del índice y la de «Este semestre».

| Recorrido (JVM, calentado) | Antes: cuadro mayor | Después |
|---|---|---|
| Entrar a la carrera | 104 ms, 1 144 funciones (60 tarjetas y 11 cabeceras) | 19 ms; el mapa entra en 16 cuadros de 4 tarjetas |
| Abrir «Tu avance» | 43 ms, 607 funciones | 14 ms; el índice en 4 cuadros |
| Ir a «Tu próximo semestre» | 47 ms, 679 funciones | 16 ms; la pantalla en 5 cuadros |
| Volver al índice | 33 ms, 557 funciones (el índice entero otra vez) | 12 ms; el índice no se recompone |

- `componentes/PorPartes.kt`: `rememberPartesListas(n)` deja componer una parte más en cada cuadro. Solo se usa donde lo que falta no se ve: la página que entra 100 ms después de salir la otra, la hoja que sube desde fuera y las columnas que la cámara aún no mira.
- El índice del progreso queda montado (`IndiceRetenido`, `PaginasHoja.kt`): al entrar a una pantalla se aparta con el mismo reloj y deja de colocarse; volver no compone nada.
- `EstadoCarrera.resumen` (barra, preguntas, electivas y lo que te da aprobar lo que cursas) y `Simulador.retrasos` («Este semestre») se calculan en `precalentar`, fuera del hilo principal.
- `BarraCapsulas` recuerda los textos medidos; el dominó usa una caché de 48 en vez de 8.

Falta medirlo en el teléfono: con esto `gfxinfo` debe dar p50 ≤ 8 ms y ningún cuadro de más de 33 ms en los tres recorridos.

## 5. Cómo medirlo de verdad (TECNO KL5, Helio G85 MT6769, Android 14, 720×1600 a 320 dpi, 120 Hz)

Datos del build: paquete **`com.mapapensum.app`**, actividad **`mpensum.MainActivity`**, versión 0.1, minSdk 26. A 320 dpi la densidad es 2,0: **1 dp = 2 px**, y la pantalla mide 360×800 dp (el ancho «compacto» exacto de la cámara, `Camara.kt:76`).

El G85 tiene 2 núcleos A75 a 2,0 GHz, 6 A55 a 1,8 GHz y GPU Mali-G52 MC2. Con la tanda 3 hecha, debería entrar solo en modo ligero. Hay que medir **las dos cosas**: ligero (lo que verán los teléfonos flojos) y con ligero apagado a mano en Personalizar (el peor caso, y lo que verá un SD680 mal detectado).

### 5.0 Preparar el teléfono y el APK

```sh
adb devices                                   # debe salir el KL5
adb shell getprop ro.soc.model                # MT6769: comprueba la detección de H10
adb shell getprop ro.build.version.sdk        # 34

# Versión de release con <profileable> (H23). Instalar CON el perfil para que ART compile ya, no en el mantenimiento nocturno:
adb install-multiple -r composeApp/build/outputs/apk/release/composeApp-release.apk \
    composeApp/build/outputs/apk/release/baselineProfiles/0/composeApp-release.dm   # carpeta 0 = API 31+ (output-metadata.json)
adb shell dumpsys package dexopt | grep -A3 com.mapapensum.app   # debe decir speed-profile

# Pantalla y ruido fuera: brillo fijo, sin ahorro de batería, animaciones a 1x
adb shell settings put global window_animation_scale 1
adb shell settings put global transition_animation_scale 1
adb shell settings put global animator_duration_scale 1
adb shell svc power stayon usb
```

Dejar que se enfríe entre series: HiOS baja la frecuencia por temperatura y eso falsea la comparación. Medir siempre con el cargador desconectado, o siempre conectado, pero igual en todas las series.

### 5.1 Los 120 Hz: dos presupuestos

A 120 Hz cada cuadro tiene **8,3 ms**. `gfxinfo` cuenta como «jank» lo que pase de ahí. Para la meta de Sam (60 fps estables) hay que medir también **fijado a 60 Hz (16,7 ms)**, que es lo que la tanda 3 pide en modo ligero.

```sh
# Ver el modo activo
adb shell dumpsys display | grep -iE "mActiveSfDisplayMode|renderFrameRate|refreshRate" | head
# Fijar 60 Hz para la serie de 60
adb shell settings put system peak_refresh_rate 60.0
adb shell settings put system min_refresh_rate 60.0
# (HiOS puede ignorarlo: entonces Ajustes > Pantalla > Frecuencia de actualización > 60 Hz, y confirmar con el dumpsys de arriba)
# Volver a lo de fábrica
adb shell settings delete system peak_refresh_rate
adb shell settings delete system min_refresh_rate
```

Al leer resultados: a 120 Hz, «Janky frames» y los percentiles se miden contra 8,3 ms. Un 90.º percentil de 12 ms a 120 Hz es **bueno para 60 Hz**. La cifra que manda es la de la serie a 60 Hz. **`framestats` solo guarda los últimos 120 cuadros: a 120 Hz es 1 segundo.** Para gestos de más de un segundo, usar el resumen de `gfxinfo` (cuenta todo desde el `reset`) o Perfetto.

### 5.2 `gfxinfo`: la cifra rápida por escenario

```sh
adb shell am start -n com.mapapensum.app/mpensum.MainActivity
adb shell dumpsys gfxinfo com.mapapensum.app reset
#   … hacer el escenario (abajo) …
adb shell dumpsys gfxinfo com.mapapensum.app > gfx-<escenario>-<hz>-<ligero|pleno>.txt
adb shell dumpsys gfxinfo com.mapapensum.app framestats > fs-<escenario>.txt   # justo al acabar: los últimos 120 cuadros
```

Leer: `Janky frames` (%), `50th/90th/95th/99th percentile`, `Number Missed Vsync`, `Number Slow UI thread` (composición o medida en Main: H2, H3, H9), `Number Slow issue draw commands` y `Number Frame deadline missed` (RenderThread y GPU: H1, H4, H5, H6, H7), y `GPU 90th percentile` si sale. Meta a 60 Hz: **percentil 95 < 16,7 ms y menos del 1 % de cuadros con jank**.

Escenarios, con gestos repetibles (720×1600 px; la barra está abajo, a unos 1 480 px):

| Esc. | Qué | Cómo |
|---|---|---|
| A | Reposo en el mapa, 10 s sin tocar (H5) | Entrar al mapa, `reset`, esperar 10 s. Con la luz, hoy saldrán ~1 200 cuadros a 120 Hz; tras H5, unos 300 o ninguno |
| B | Arrastrar el mapa con foco (H1) | Tocar una tarjeta (ficha abierta) y luego `for i in 1 2 3 4 5; do adb shell input swipe 600 500 120 500 250; adb shell input swipe 120 500 600 500 250; done`. Repetir con «Atenuado» activo y sin ficha |
| C | Desplazar la lista (H3, H4, H15) | Vista Lista: `for i in 1 2 3 4 5 6; do adb shell input swipe 360 1300 360 300 300; done` y vuelta arriba |
| D | Entrar a la carrera (H2) | Desde la portada, `reset`, tocar la tarjeta «Tu carrera», esperar 2 s. Mirar sobre todo `Slow UI thread` |
| E | Hoja «Tu avance» (H9, H16, H7) | Tocar el núcleo de la barra (centro, ~360,1480), esperar 2 s, desplazar el índice y cerrar con atrás (`adb shell input keyevent 4`). La primera apertura tras matar la app es la que cuenta |
| F | Hoja del horario (H6) | Vista Horario, abrir una clase, cerrar; 5 veces |
| G | Pellizco alejado (H11) | A mano (adb no pellizca): alejar del todo y volver, 3 veces. Mejor en Perfetto |

### 5.3 Arranque

```sh
adb shell am force-stop com.mapapensum.app
adb shell am start-activity -W -S -n com.mapapensum.app/mpensum.MainActivity   # leer TotalTime y WaitTime
# 10 veces y quedarse con la mediana. Comparar sin perfil y con perfil:
adb shell cmd package compile -f -m verify com.mapapensum.app          # como recién instalado a mano, antes del mantenimiento
adb shell cmd package compile -f -m speed-profile com.mapapensum.app   # con el perfil aplicado
```

H8 se ve aquí y en Perfetto (en el hilo principal, rodajas largas de `Typeface`/`Font` la primera vez que aparece cada peso). Con `noCompress`, `dumpsys meminfo com.mapapensum.app` debería bajar unos MB de `Native Heap` tras abrir portada, mapa y lista.

### 5.4 Perfetto: el porqué de cada cuadro

```sh
# Opcional, más detalle de Skia en el RenderThread (reiniciar la app después):
adb shell setprop debug.hwui.skia_atrace_enabled true
adb shell am force-stop com.mapapensum.app

adb shell perfetto -o /data/misc/perfetto-traces/pensum.pftrace -t 15s -b 64mb \
    sched freq idle gfx view input am wm dalvik binder_driver hal
#   … durante esos 15 s, hacer UN escenario …
adb pull /data/misc/perfetto-traces/pensum.pftrace
# Abrir en https://ui.perfetto.dev
```

Qué mirar en el proceso `com.mapapensum.app`:

- **Hilo principal**: `Choreographer#doFrame` de más de 8,3 ms (o 16,7) → abrir y ver si es `Recomposer:recompose`, `measure`/`layout` (texto: H2, H3) o `draw` (`drawWithCache`, cables: H13).
- **RenderThread**: `DrawFrames` largo con muchas `saveLayer` y `flush commands` → H1, H7. `RenderEffect`/blur → H4, H5, H6.
- **Pista de la GPU y `queueBuffer`**: si el RenderThread acaba a tiempo pero el cuadro llega tarde, es la GPU (fill-rate, desenfoques).
- **Pistas de frecuencia de CPU**: si los A75 bajan de 2,0 GHz a mitad de serie, es temperatura: repetir en frío.

Para ver los nombres de los composables en la traza (solo en una rama de medición, no en release normal): añadir `androidx.compose.runtime:runtime-tracing` a `androidMain.dependencies`. Las rodajas `Compose:recompose` salen con el nombre del composable.

Ayudas visuales rápidas, sin traza (reiniciar la app después de cambiarlas):

```sh
adb shell setprop debug.hwui.profile visual_bars     # barras de tiempo por cuadro en pantalla
adb shell setprop debug.hwui.overdraw show           # sobrepintado: rojo = 4x o más
adb shell setprop debug.hwui.profile false && adb shell setprop debug.hwui.overdraw false   # quitarlas
```

### 5.5 Macrobenchmark (tanda 3, pasos 6 y 7)

```sh
./gradlew :composeApp:generateReleaseBaselineProfile                 # escribe baseline-prof.txt y startup-prof.txt
./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest     # arranque y FrameTimingMetric en el KL5
# Resultados: baselineprofile/build/outputs/connected_android_test_additional_output/…/*benchmarkData.json
```

`FrameTimingMetric` da `frameDurationCpuMs` (lo que tarda la app) y `frameOverrunMs` (cuánto se pasa del plazo; negativo es holgura; necesita API 31, el KL5 la tiene). Correr cada escenario a 120 Hz y fijado a 60 Hz. **Meta:** `frameOverrunMs` P90 ≤ 0 a 60 Hz en ligero, en los escenarios A a F; y el arranque en frío (`timeToInitialDisplayMs`) con perfil, al menos un 20 % por debajo de `CompilationMode.None`.

## 6. Arquitectura: lo que estorba al rendimiento o al mantenimiento

- **A-1. `mapa/MapaCarrera.kt` (614 líneas) mezcla cinco piezas**: el composable, el fondo, las cabeceras, los cables y la luz. Partir en `Cables.kt` (`CableDibujo`, `cablesDe`, `degradado`, `girar`, `Cables`, `descargaPor`) y `LuzFrontera.kt`. Así H12 y H13 no se pisan con cambios del mapa.
- **A-2. `carrera/HojaInferior.kt` (536) es una hoja con cuatro modos** por banderas (`modal`, `cristal`, `flotante`, `completa`) y combinaciones que nadie usa. Los costes (sombra, uno o dos desenfoques, velo) dependen de esa combinación y no se ven desde quien la llama. Exponer tres variantes con nombre (`HojaModal`, `HojaCristal`, `HojaFicha`) sobre la misma `Hoja` física.
- **A-3. Dos fuentes de verdad para las preferencias.** `Preferencias` (≈20 `MutableStateFlow`) → `App` hace 16 `collectAsState` → escribe 13 estados de `Tema` en plena composición (`App.kt:94-125`), y `plataforma/Plataforma.kt:161-163` repite el volcado. Que `Preferencias` guarde `mutableStateOf` y `Tema` los lea directamente: se van 16 corrutinas del arranque y las escrituras hacia atrás en composición.
- **A-4. Tipografía repetida.** 103 `TextStyle(` en línea en 25 archivos, más `estiloX()` y `LetrasHorario` con los mismos roles (ver «Coherencia» de `qa-interfaz.md`). Un `Tipografia` por tema, armado una vez junto a `LocalFuentes`, quita asignaciones de cada fila y deja un solo sitio donde cambiar un rol.
- **A-5. Copias de piezas.** El fundido de abajo está copiado en `portada/Portada.kt:418-434`. `cablesDe` se llama dos veces (H13). Hay dos `vidrio`: `componentes/Cristal.kt:88` y la función privada `vidrio(lienzo)` de `Portada.kt` (mismo nombre, otra cosa).
- **A-6. Ganchos de prueba como estado global.** `internal var latidoActivo = true` (`carrera/EstiloAvance.kt:509`) apaga el latido para las capturas desde un `var` global. Mejor un `CompositionLocal` o `Tema.movimiento` en la prueba.
- **A-7. Lo pesado que cuelga de un getter.** `EstadoCarrera.planes`, `graduacion` y `simulador` son `derivedStateOf` que corren en el hilo que los lea primero: a veces Main (H9), a veces `Default` (`PiezasFicha.kt:305`). Además `Simulador.medidos` es un `mutableStateMapOf` escrito desde fondo (`logica/Recomendacion.kt:61,69`). Que el cálculo tenga un solo dueño, en fondo, y que la UI lea un estado ya listo.
- **A-8. Archivos grandes que conviene partir cuando se toquen**: `carrera/Horario.kt` (710), `carrera/AvanceCarrera.kt` (633), `carrera/HojaInferior.kt` (536), `carrera/PiezasFicha.kt` (552), `carrera/ListaFilas.kt` (544). No afectan a los cuadros; sí a que un agente toque lo justo.
