# QA de transiciones: por qué no se siente fluido

Diagnóstico, sin arreglos todavía. Todo sale de leer el código y de la prueba
`composeApp/src/androidUnitTest/kotlin/mpensum/carrera/CapturaTransicionesTest.kt`,
que avanza el reloj de 16 en 16 ms, fotografía cada cuadro y deja en
`composeApp/build/capturas/transiciones/` una tira por recorrido (el cuadro que
salta va con borde rojo) y un `.txt` con el borde de arriba de la hoja en cada
cuadro.

```
./gradlew --no-daemon :composeApp:testDebugUnitTest --tests "mpensum.carrera.CapturaTransicionesTest"
```

## Lo medido

| Recorrido (de pie, 360×800) | Salto mayor de la hoja en un cuadro | Qué se ve |
|---|---|---|
| Índice → Tu próximo semestre | **115 dp** (y otros 115 de vuelta al cuadro siguiente) | la hoja baja y vuelve a subir |
| Índice → No puedes atrasar | **115 dp**, ida y vuelta | igual |
| Índice → ¿Cuándo me gradúo? | 46 dp de golpe y después 60 dp más en 600 ms con resorte | baja de golpe y sigue bajando sola |
| ¿Cuándo me gradúo? → índice | 60 dp | sube de golpe |
| Índice desplazado → Técnicas | **169 dp** | la hoja baja y el índice salta arriba del todo |
| Técnicas → índice desplazado | **183 dp** | la hoja sube vacía; el índice vuelve arriba, no donde estabas |
| Ajustes → Personalizar | 21 dp y la vista previa de 176 dp aparece en el primer cuadro | la cabecera cambia antes que el cuerpo |
| Ficha: otra materia (Pide · Abre) | **128 dp a los 208 ms** | se funde y, ya fundida, la hoja se encoge de golpe |
| Ficha: encuadre de la cámara | pide con la hoja de 462 dp; la hoja queda en 354 dp | la tarjeta queda 108 dp más arriba del centro |

En este equipo (Ryzen, JVM), en frío: `planesPorCarga` tarda 61 ms,
`proximoSemestre` 47 ms y `cadenaExplicada` 55 ms; repetidos, 7, 2 y 1 ms. En un
Android modesto, en frío, cuenta con 3 a 6 veces más (estimado, no medido en el
teléfono): el hueco de 480 dp dura de 150 a 350 ms, no un cuadro como aquí.

## Los fallos, por impacto

### 1. La hoja del progreso cambia de alto tres veces en cada ida (el de más impacto)

- **Dónde:** `carrera/AvanceCarrera.kt:220-239` (cuerpo), `:205` (cabecera) y
  `carrera/Calculado.kt:30-38`; el alto de la hoja es el de su contenido
  (`carrera/HojaInferior.kt:378-418`).
- **Causa:**
  1. La hoja mide lo que mide su contenido y está pegada abajo: cualquier
     cambio de alto mueve su borde de arriba.
  2. El cuerpo usa `SizeTransform(clip = true) { snap() }`: en el primer cuadro
     el alto pasa al del detalle.
  3. Ese detalle todavía es el `Spacer` de 480 dp de `Calculado` más la
     cabecera interna, unos 115 dp menos que el índice.
  4. Cuando llega el cálculo, el alto salta otra vez al real.
  5. La cabecera es otro `AnimatedContent` con el `SizeTransform` por defecto
     (un resorte `StiffnessMediumLow`, unos 500 ms): su alto pasa del título
     de 28 sp a la fila de 44 dp deslizándose, mientras el cuerpo ya saltó.
     Es lo que se ve en Graduación: 46 dp de golpe y 60 dp más resbalando.
- **Qué se ve:** el borde de la hoja baja, vuelve a subir y además resbala.
  Tres movimientos distintos sobre el mismo borde, y ninguno lo pidió el dedo.
- **Arreglo:** de pie, la hoja del progreso no cambia de alto. Va siempre a su
  alto grande (el máximo: pantalla menos `libreArriba`) y el contenido navega
  dentro (patrón A, más abajo).
  - El índice ya llena ese alto en todos los casos medidos.
  - En `HojaInferior`, un parámetro `altoFijo: Boolean`: la `Column` lleva
    `fillMaxHeight()` dentro de su caja en vez de medir el contenido.
  - Con eso, el `Spacer` de 480 dp deja de mover nada.

### 2. El índice y el detalle comparten un solo scroll

- **Dónde:** `carrera/HojaInferior.kt:413`: un solo
  `verticalScroll(rememberScrollState())` envuelve el `AnimatedContent` de
  `AvanceCarrera.kt:220`.
- **Causa:** el desplazamiento es de la hoja, no de cada página.
  - Si bajaste hasta Electivas y entras a Técnicas, el detalle hereda ese
    desplazamiento.
  - El detalle mide menos, así que el scroll se recorta y el índice que sale
    salta arriba del todo.
  - Al volver pasa al revés: la hoja crece vacía (183 dp en un cuadro) y el
    índice reaparece arriba, no donde lo dejaste.
- **Qué se ve:** `progreso-desplazado-ida.png`, cuadro de 32 ms:
  - el índice, ya sin desplazar, cae 169 dp;
  - el detalle se funde encima.
- **Arreglo:** la hoja del progreso no desplaza (`desplazable = false`).
  - Cada página lleva su propio `ScrollState`, guardado por página:
    `rememberSaveableStateHolder()` y `SaveableStateProvider(clave)` con la
    clave del detalle.
  - Al volver, el índice está donde lo dejaste.
  - El `fundidoAbajo` va en cada página, como ya hace el acostado
    (`CuerpoDetalle`).

### 3. Los dos contenidos se cruzan en doble exposición

- **Dónde:** `AvanceCarrera.kt:225-226`, `AvanceIndice.kt:145`,
  `PiezasFicha.kt:177` (`FundidoDeMateria`) y `Ajustes.kt:154-155`.
- **Causa:** el que sale y el que entra se ven a la vez. En el progreso:
  - el que sale se funde en 160 ms;
  - el que entra aparece en 260 ms;
  - los dos se deslizan 340 ms en la misma caja;
  - entre los 60 y los 160 ms están los dos a medias.
- **Qué se ve:** en `progreso-proximo-vuelta.png` (96–144 ms) se leen a la vez
  «Ene — Jun 2027» y «Ingeniería de Sistemas». En `ficha-papel-cambia.png`
  (96–128 ms) se ven «Circuitos y Sistemas» y «Extraacadémica» encimados. Es
  el «se siente raro».
- **Arreglo:** un fundido en serie, no cruzado (patrón A): primero sale y
  después entra, sin un cuadro con los dos a más del 10 %.

### 4. La cabecera y el cuerpo llevan relojes distintos

- **Dónde:**
  - `AvanceCarrera.kt:205`: la cabecera tiene su propio `AnimatedContent`,
    entra con 220 ms (60 de retraso), sale con 120 y su alto va con resorte;
  - `AvanceCarrera.kt:220`: el cuerpo, 340 ms de deslizamiento y alto de golpe;
  - `Ajustes.kt:120-146`: la cabecera no se anima. El título, la flecha de
    volver y la `VistaPrevia` de 176 dp cambian en el primer cuadro mientras
    el cuerpo todavía enseña el menú (`ajustes-personalizar-ida.png`, 16–64 ms).
- **Arreglo:** un solo `updateTransition(detalle)`. La cabecera y el cuerpo son
  `transition.AnimatedContent(...)` del mismo `Transition`, con las mismas
  curvas: un reloj.

### 5. Al volver, el 41 % vuelve a contar desde 0

- **Dónde:** `AvanceCarrera.kt:423-425`. `CifraHeroe` crea
  `Animatable(0f)` y cuenta 700 ms cada vez que el índice entra en la
  composición, y al volver de un detalle entra de nuevo.
- **Qué se ve:** en las tiras de vuelta, la cifra pasa por 3 %, 5 %, 9 % y
  12 % mientras se funde.
- **Arreglo:** contar solo en la primera apertura de la hoja.
  - Opción: guardar «ya contó» por `apertura` en `HojaAvance`.
  - Opción: con la página guardada del fallo 2, el índice ni se descompone.

### 6. Lo pesado se calcula al empezar la animación

- **Dónde:**
  - `EstadoCarrera.kt:118`: `planes` se calcula la primera vez que se lee,
    que es en la composición del primer cuadro de la hoja (`ContenidoHoja` lee
    `estado.graduacion`, `AvanceCarrera.kt:319`). Son 61 ms en el hilo
    principal en este equipo, justo cuando arranca el resorte de entrada.
  - `Calculado.kt:31`: cada vez que se abre Próximo o Cadena se vuelve a
    lanzar el cálculo. No hay caché, aunque `Simulador` ya guarda los márgenes.
- **Arreglo:** patrón C.
  - Al entrar en la carrera, en segundo plano: `planes`, `proximo` y `cadena`.
  - El `Simulador` guarda `proximo` y `cadena` con `by lazy`.
  - La hoja abre sin cálculo en el hilo principal y el detalle, con los datos
    ya puestos.

### 7. La franja gris encima de la ficha en tema claro (la captura de Sam)

- **Dónde:** `carrera/HojaInferior.kt:480-487`, `Modifier.sombraSuave()`. La
  aplica la hoja sin velo (`modal = false`, la ficha) en `:398`.
- **Causa:**
  - El degradado va de transparente a `Paleta.sombra.copy(alpha = 0.14f)`.
  - En los claros, `sombra` es la tinta (`#171A20` en Claro, `#141A2A` en
    Papel).
  - Sobre el lienzo claro, eso es un gris sucio de 40 dp que oscurece las
    tarjetas hasta el borde blanco de la hoja.
  - Medido, en `ficha-papel-franja.txt`: la luz baja de 238 (el lienzo) a 221
    junto al borde. En Claro, de 244 a 226.
- **Segundo defecto, de composición:** el degradado es un rectángulo que acaba
  recto en `y = 0`, pero la hoja tiene las esquinas redondas. En las esquinas
  asoma el mapa sin teñir bajo un corte recto de gris (ampliado en
  `ficha-papel-franja.png`).
- **Arreglo:**
  - En claro, el fundido va hacia el lienzo: las tarjetas se disuelven en el
    papel y la separación la da el filo de 1 px que ya tiene `panelPegado`.
  - El rectángulo baja `Radio.Hoja` por detrás de la hoja con el tono final,
    para que las esquinas queden del mismo tono.

```kotlin
/** La separación de la hoja sin velo: en oscuro una sombra, en claro el mapa que se funde en el papel */
private fun Modifier.sombraSuave(): Modifier = drawWithCache {
    val alto = 32.dp.toPx()
    val esquina = Radio.Hoja.toPx()
    val tono = if (Tema.claro) Paleta.lienzo.copy(alpha = 0.88f) else Paleta.sombra.copy(alpha = 0.45f)
    // Pasado endY el degradado se queda en `tono`: así también rellena las esquinas redondas
    val brocha = Brush.verticalGradient(0f to tono.copy(alpha = 0f), 1f to tono, startY = -alto, endY = 0f)
    onDrawBehind { drawRect(brocha, Offset(0f, -alto), Size(size.width, alto + esquina)) }
}
```

- **Por qué 32 dp y 0,88:** a 32 dp basta para que la tarjeta cortada no se
  lea como cortada. Al 88 %, el último texto que asoma sigue legible en la
  hoja blanca sin parecer niebla.

### 8. La ficha se funde y después se encoge, y la cámara encuadra con el alto viejo

- **Dónde:**
  - `PiezasFicha.kt:172-180`: `FundidoDeMateria` con `.using(null)`. Mientras
    dura el fundido, la caja mide lo de la más alta de las dos, y al acabar
    salta al de la nueva.
  - `FichaMateria.kt:54-58`: `alEncuadrar` espera un solo cuadro y lee
    `alto`.
- **Qué se ve:**
  - `ficha-papel-cambia.png`: fundido cruzado (fallo 3) y, a los 208 ms, la
    hoja baja 128 dp de golpe.
  - La cámara ya encuadró con la hoja de 462 dp y queda en 354 dp: la tarjeta
    queda 108 dp por encima del centro del hueco.
- **Arreglo:** patrón B.
  - El alto se anima una sola vez hacia el de la nueva materia, ya medido.
  - `alEncuadrar` recibe ese alto de destino, el del contenido nuevo medido
    dentro del `AnimatedContent` con `onSizeChanged` en el hijo y no en la
    caja. Así la cámara y la hoja se mueven a la vez hacia el mismo sitio.

### 9. Respuesta de la ficha: una tarjeta vacía y un segundo fundido

- **Dónde:** `PiezasFicha.kt:343-366`.
- **Causa:**
  - `null` significa a la vez «calculando» y «esta materia no tiene
    respuesta» (fuera del plan): en el segundo caso queda para siempre una
    tarjeta vacía de 64 dp.
  - Al cambiar de materia, `mostrada` nace vacía en el hijo nuevo del
    `AnimatedContent`: la tarjeta entra vacía y su texto se funde otra vez
    150 ms después.
- **Arreglo:**
  - Un estado de tres casos: `Calculando`, `SinRespuesta` y `Lista(r)`. Solo
    `Calculando` reserva los 64 dp.
  - Con el cálculo repetido en 2 ms, se calcula antes de cambiar de materia
    (patrón C) y no hay segundo fundido.

### 10. El selector de electiva tapa la hoja del progreso con el mapa

- **Dónde:** `AvanceCarrera.kt:242` monta `SelectorElectiva` encima de la
  hoja. Su `FondoHoja` (`HojaInferior.kt:457-466`) desenfoca
  `LocalFondoHoja`, que es el mapa y no la hoja del progreso.
- **Qué se ve:** `avance-detalle-8-selector.png`. Detrás del selector aparece
  el mapa y la hoja del progreso desaparece: se pierde de dónde vienes.
- **Arreglo:**
  - Una hoja sobre otra hoja no desenfoca: `fondo = null`, y el velo que ya
    existe (`VELO`) a la mitad de su fuerza.
  - La hoja de abajo se ve atenuada, como las hojas apiladas de iOS.
  - Si se quiere más: la de abajo baja a `scale 0,96` y sube 8 dp, en
    `graphicsLayer`, con el mismo resorte `ENTRA`.

### 11. Mapa ↔ lista ↔ horario: el mapa repite su llegada cada vez

- **Dónde:** `Interior.kt:298-302` (`fadeIn(260, 80)` / `fadeOut(160)`) y
  `mapa/MapaCarrera.kt:149-152, 187, 190`. `llegada` es un `Animatable(0f)`
  nuevo en cada composición del mapa, y la cámara ya está colocada.
- **Qué se ve:** al volver al mapa hay dos fundidos multiplicados (el del
  `AnimatedContent` y la llegada de 620 ms con 12 dp de deriva). El mapa llega
  tarde y flotando, cuando la lista se fue en 160 ms.
- **Arreglo:**
  - La llegada solo la primera vez por carrera: guardar «ya llegó» en
    `Camara`, que vive en `Interior`.
  - El cambio de vista, con el token `CAMBIO_VISTA` (abajo).
  - `Interior.kt:128` espera 380 ms escritos a mano: que salga del token de
    salida de la hoja.

### 12. Portada → carrera: el zoom entra a una pantalla vacía

- **Dónde:** `App.kt:174-176` y `EstadoCarrera.kt:220-236`. El pensum y su
  geometría se calculan en otro hilo después de empezar el zoom.
- **Qué se ve:**
  - el interior crece desde la tarjeta durante 420 ms con solo el lienzo y la
    barra;
  - el mapa llega después con su propio fundido de 620 ms;
  - la cifra de la barra cuenta desde 0 (`BarraCarrera.kt:190`).
- **Arreglo:** precargar.
  - Al tocar la tarjeta, o al pintar la portada para «Tu carrera», un caché
    de proceso por `slug` con el `CarreraLista`.
  - Con el pensum ya listo cuando arranca el zoom, el mapa nace dentro del
    zoom.
  - La llegada del mapa se apaga cuando entra con el zoom: el zoom ya es la
    llegada.

### 13. Hojas que cambian de contenido sin transición

- **Dónde:** `HojasHorario.kt:126-135`. De Detalle a Editar, o del menú a
  Borrar, se cambia en un cuadro y el alto salta con él.
- **Arreglo:** patrón B.

### 14. Sin sistema: catorce curvas y duraciones sueltas

- `ENFATICA = CubicBezierEasing(0.2f, 0f, 0f, 1f)` está copiada seis veces:
  `App.kt:80`, `PasoMapa.kt:68`, `PasoHorario.kt:65`, `PasoAvance.kt:100`,
  `Portada.kt:107` y `ListaFilas.kt:99`.
- `(0.32, 0.72, 0, 1)` va con cinco nombres: `CURVA_HORARIO`, `CURVA`,
  `SUAVE`, `CRECER` y `CURVA_FOCO`.
- `(0.16, 1, 0.3, 1)` se llama `SALIDA` y es una entrada.
- Muchos `tween(n)` sin curva (la de serie, `FastOutSlowIn`):
  - `Ajustes.kt:154`;
  - `Calculado.kt:37`;
  - `PiezasFicha.kt:177`;
  - `BurbujaFicha.kt:143-156`;
  - `Interior.kt:301, 377`.
- Para la misma idea, «empujar dentro de una hoja», hay tres versiones:
  - progreso: 340 ms, un tercio de ancho;
  - ajustes: 320/280 ms, un cuarto;
  - horario: 320/260 ms, un quinto.
- **Reducir movimiento** solo lo respetan `App.kt:173`, `Horario.kt:543`,
  `Personalizar.kt:526` y `EstiloAvance.kt:348`. Lo ignoran:
  - la hoja (`HojaInferior`);
  - el progreso;
  - ajustes;
  - la ficha;
  - la burbuja;
  - el cambio de vista;
  - la cifra que cuenta.

### 15. Menores (rendimiento y pulido)

- `Bienvenida.kt:203`: el título entra con un `BlurEffect` de radio distinto
  en cada cuadro durante 1 s. Es el desenfoque por cuadro que la regla
  prohíbe y en gama baja cuesta. Que entre con alfa y 12 dp de subida, sin
  desenfoque.
- `Bienvenida.kt:109`: `fadeIn(500, 120)` con `fadeOut(180)` deja 120 ms con
  el lienzo vacío entre pasos. Debe ser serie con el patrón A (100 + 250).
- `AvanceCarrera.kt:134-138` (`numeroAnimado`) y `BarraCarrera.kt:190` leen
  el `Animatable` al componer: recomponen el Núcleo en cada cuadro durante
  600 ms. Que la cifra se dibuje como `CifraHeroe`, leyendo al pintar.
- `Calculado.kt:38` y el `fadeIn` del `AnimatedContent` son dos
  `graphicsLayer(alpha)` anidados sobre una columna de 900 dp. Son dos capas
  fuera de pantalla por cuadro. Con el patrón C desaparece la de `Calculado`.
- `BurbujaFicha.kt:143-156`: 180/120/150 ms sin curva. Debe ir con los tokens
  `CORTA` y `ENTRA`/`SALE`.
- La barra vuelve en 200 ms mientras la ficha todavía tarda 260 en salir
  (`Interior.kt:377`). Que vuelva con 120 ms de retraso.
- Nota de la prueba: `captureToImage()` espera un redibujado con el reloj
  parado y no llega nunca. La prueba fotografía con Roborazzi.

## El sistema de movimiento único

Un archivo, `tema/Movimiento.kt`, y nadie escribe un `tween` con números sueltos.

```kotlin
object Movimiento {
    // Duraciones, por tamaño y distancia
    const val CORTA = 150      // píldora, botones, estado de una fila: lo pequeño
    const val MEDIA = 250      // un contenido que se funde dentro del mismo sitio
    const val EMPUJE = 350     // una página dentro de una hoja (100 sale + 250 entra)
    const val CAMBIO_VISTA = 300 // mapa ↔ lista ↔ horario (100 sale + 200 entra)
    const val PANTALLA = 420   // portada ↔ carrera
    const val ESCALONADO = 50  // entre elementos de una misma entrada

    // Curvas (Material 3)
    val ENTRA = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)   // emphasized decelerate: lo que llega se posa
    val SALE = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)    // emphasized accelerate: lo que se va no frena
    val ESTANDAR = CubicBezierEasing(0.2f, 0f, 0f, 1f)     // lo que cambia sin salir de su sitio (la ENFATICA de hoy)

    // Resortes, sin rebote
    val HOJA = spring<Float>(dampingRatio = 1f, stiffness = 380f)          // ya en HojaInferior: se queda
    val ALTO = spring<IntSize>(dampingRatio = 1f, stiffness = 500f)        // cambio de alto de una hoja, ~300 ms

    /** Con «Reducir movimiento» todo pasa a un fundido de CORTA sin desplazamiento */
    val reducido: Boolean get() = Tema.movimiento
}
```

- **Por qué 350 ms en dos tiempos:** Material «shared axis X» con fundido en
  serie. Lo que sale se va rápido (100 ms, `SALE`) y lo que entra tiene todo
  el protagonismo (250 ms, `ENTRA`). Nunca hay dos textos encimados.
- **Por qué 24 dp y no un tercio del ancho:** dentro de una hoja de 360 dp,
  120 dp de deslizamiento con fundido se leen como un borrón. Con 24 dp basta
  para decir «adelante» o «atrás».
- **Por qué resorte de 500 para el alto:** con amortiguación crítica asienta en
  unos 300 ms y, si llega otro cambio a medio camino, sigue desde donde iba
  sin frenazo.

### Patrón A: hoja con páginas (progreso de pie, Ajustes, horario)

1. **Alto fijo.** La hoja va a su alto grande desde que abre y no cambia al
   navegar. Es lo que hace iOS con la navegación dentro de una hoja.
2. **Un reloj.** `val t = updateTransition(pagina)`. La cabecera y el cuerpo
   son `t.AnimatedContent`, con `contentAlignment = TopStart` y
   `SizeTransform(clip = true) { _, _ -> snap() }`. Con el alto fijo, ese
   `snap` ya no mueve nada.
3. **Ida:**
   - sale: `fadeOut(tween(100, easing = SALE)) + slideOutHorizontally(tween(100, easing = SALE)) { -24.dp }`;
   - entra: `fadeIn(tween(250, 100, ENTRA)) + slideInHorizontally(tween(250, 100, ENTRA)) { +24.dp }`.
4. **Vuelta:** igual, con los signos al revés.
5. **Una página, un scroll:** `SaveableStateProvider(clave)` por página, con su
   `ScrollState` y su `fundidoAbajo`.
6. **Reducir movimiento:** `fadeIn(tween(150)) togetherWith fadeOut(tween(150))`,
   sin desplazamiento.

### Patrón B: hoja de una pieza que cambia de contenido (ficha, selector, una hoja del horario)

1. Lo nuevo se compone y se mide antes de enseñarse. En el `AnimatedContent`:
   `SizeTransform(clip = false) { _, _ -> Movimiento.ALTO }`, más el fundido
   en serie (100 + 150, sin desplazamiento: es el mismo sitio).
2. El alto se anima en la fase de layout (`SizeTransform` ya lo hace así): no
   recompone. El recorte lo pone la forma de la hoja, que ya existe.
3. El alto de destino se avisa una vez, medido en el hijo que entra
   (`onSizeChanged` dentro del contenido): es el que usan la cámara y
   `alMedir`.
4. Si lo nuevo depende de un cálculo, se espera a tenerlo (patrón C) antes de
   empezar. El alto se anima una sola vez, ya con el contenido final.

### Patrón C: contenido asíncrono sin saltos

1. **Precalcular antes del gesto.** Al entrar en la carrera, en segundo plano:
   `planes`, `simulador.proximo` y `simulador.cadena`, guardados en el
   `Simulador` con `by lazy`. Se invalidan solos al cambiar las marcas, porque
   el `Simulador` es un `derivedStateOf`.
2. **Retener el toque, no la pantalla.** Si al tocar aún no está:
   - la fila se queda en su estado pulsado hasta 150 ms y la página entra
     cuando llega;
   - por debajo de 150 ms no se percibe espera;
   - por encima sí: entonces se entra igual.
3. **Si entra sin datos:** con el patrón A el alto no depende de nada. Se
   enseña la cabecera, que es síncrona, y el cuerpo se funde con `MEDIA` y
   `ESTANDAR` al llegar. Es un único fundido, nunca dentro de otro.
4. **Tres estados, no dos:** `Calculando`, `Vacio` y `Listo(dato)`. Solo
   `Calculando` reserva sitio, y lo reserva con el alto del caso típico de esa
   pieza (64 dp en la respuesta de la ficha), no con un número genérico.

## Plan en tandas

Cada tanda se compila y se comprueba con
`--tests "mpensum.carrera.CapturaTransicionesTest"` (y la `Captura*Test` de la
pantalla tocada). Después se miran las tiras.

1. **Tokens.**
   - Crear `tema/Movimiento.kt`.
   - Borrar las seis `ENFATICA` y las cinco copias de `(0.32, 0.72, 0, 1)`.
   - Que cada `tween` sin curva tome la suya.
   - Sin cambio visible.
2. **Progreso de pie (fallos 1 a 6).**
   - `HojaInferior(altoFijo)`, `desplazable = false` en el progreso y un
     scroll por página.
   - Un `updateTransition` para la cabecera y el cuerpo, con el patrón A.
   - `CifraHeroe` cuenta una vez por apertura.
   - Precálculo de `planes`, `proximo` y `cadena` al entrar en la carrera, y
     `Calculado` lee el caché.
   - Al acabar, `EXIGIR = true` en `CapturaTransicionesTest`: la hoja asentada
     no puede moverse 12 dp en un cuadro.
3. **Velos (fallos 7 y 10).**
   - El `sombraSuave` de arriba.
   - El selector sobre la hoja: sin desenfoque y con velo a media fuerza.
   - Comprobar con `fichaClaroPapel` y `fichaClaroClaro`: la luz junto al
     borde debe quedar a 5 o menos del lienzo, no a 17.
4. **Ficha (fallos 8, 9 y 13).**
   - Patrón B en `FundidoDeMateria` y en `HojasHorario`.
   - `alEncuadrar` con el alto de destino.
   - Respuesta en tres estados.
   - Comprobar con `ficha-papel-cambia.txt`: sin saltos, y el encuadre igual al
     alto asentado.
5. **Ajustes y Personalizar (fallo 4).** La cabecera entra en el mismo
   `Transition` que el cuerpo. La `VistaPrevia` entra con la página, no antes.
6. **Vistas e interior (fallos 11, 12 y 15).**
   - La llegada del mapa, una vez.
   - `CAMBIO_VISTA` en serie.
   - Precarga del pensum desde la portada.
   - Sin `BlurEffect` animado en la guía.
   - La cifra del Núcleo, leída al dibujar.
7. **Reducir movimiento en todo.** Cada transición nueva pasa por
   `Movimiento.reducido`. Una prueba con `Tema.movimiento = true` graba la ida
   del progreso y no debe haber desplazamiento, solo alfa.
