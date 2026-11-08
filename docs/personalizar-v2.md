# Personalizar v2: orden, temas y el icono de tu carrera

Maqueta: `docs/maquetas/personalizar-v2.html` (PNG al lado). Manda sobre `personalizar-y-temas.md` donde choquen; lo que no se nombra aquí no cambia (tokens, contrastes, acentos, coste de Fondo y Cables).

Lo que pidió Sam: «muchas opciones, más organizadas y más lógicas»; «mostrar mejor los temas, quitar Morichal, una previsualización más premium»; «el logo de mi carrera no es el adecuado, quítale ese tono azul».

## Lo recomendado

| Bloque | Recomendado | Por qué, en una línea |
|---|---|---|
| Organización | **C · Primero la cara**: tema, automático y acento en la página; Mapa, Inicio y horario y Accesibilidad abren su página | Lo que cambia la cara queda a cero toques y lo demás a uno, con su resumen a la vista (divulgación progresiva, un foco por pantalla). |
| Temas | **T3 · Pintura**: dos pantallas reales (portada y mapa) y debajo la paleta de 2 × 6 | Se ve el tema de verdad, a tamaño legible, y los 12 caben sin deslizar (Fitts: discos de 40 en toques de 52). |
| Sistema | **Interruptor «Automático»**: las dos pantallas pasan a ser las ranuras «De día» y «De noche» | Sustituye a las dos filas de píldoras: una sola paleta sirve a las dos ranuras (patrón de Apariencia de iOS). |
| Icono de Tu carrera | **A · Uno por carrera**, en tinta neutra | Responde a «mi carrera» (Sistemas, un chip; Petróleo, su torre) sin tocar el birrete, el libro ni el sello. |

Descartados: A · Índice (el tema queda a un toque más y la página dice poco), B · Una página («Más opciones» es un cajón sin pista), T1 · Carrusel (hay que deslizar para comparar), T2 · Familias (miniaturas de 56 dp que no dejan ver el tema), iconos B · Diploma (igual para todos y cerca del birrete) y C · Carnet (a 20 dp se lee «contacto»).

## 1. Dónde va cada opción

Las 17 de hoy pasan a 16 controles en cuatro sitios:

| Sitio | Opciones | Control |
|---|---|---|
| **Personalizar** | Tema · Automático · Acento | paleta, interruptor y círculos |
| **Mapa** | Fondo · Cables · Grosor · Atenuar lo aprobado · Códigos de materia | baldosas, baldosas, segmentado y dos interruptores |
| **Inicio y horario** | Al abrir · Vista inicial · Hora de 24 h | segmentado, segmentado e interruptor |
| **Accesibilidad** | Texto · Más contraste · Luz de fondo · Reducir movimiento · Vibración · Modo ligero | segmentado «Aa», cuatro interruptores y segmentado |

Qué cambia frente a hoy:

- **Se fusionan:** Sistema, «De día» y «De noche» en el interruptor «Automático». `Apariencia.SISTEMA` sigue igual por debajo.
- **Pasan a interruptor:** «Lo aprobado» (Normal / Atenuado) pasa a «Atenuar lo aprobado», y «Formato de hora» (12 / 24 h) pasa a «Hora de 24 h». Si solo hay dos estados, basta un interruptor.
- **Grosor:** deja las tres baldosas y pasa a un segmentado con su línea delante. Ya lo dibuja la vista previa del mapa.
- **Se muda:** «Luz de fondo» va a Accesibilidad, junto a «Reducir movimiento»: las dos calman la pantalla.
- **Las etiquetas llevan el dato:** «Al abrir» dice «Portada | Sistemas» (el `nombreCorto` de tu carrera; sin carrera, el segmento no aparece y la fila tampoco).
- **Se borran:** `BloqueDiaNoche`, `FilaJuegos` y el carrusel `CarruselTemas`. `MiniTema` se queda para el selector de la hoja de avance si alguien lo usa; si no, también se borra.

## 2. Personalizar (de pie, 375 × 812)

Orden de arriba abajo, con el margen de `margenPantalla()` (20 a 375):

| Pieza | Medida |
|---|---|
| Cabecera | la de hoy: atrás de 36, rótulo AJUSTES, «Personalizar» en Título 28/32 y cerrar de 36 |
| **Vista previa** | dos pantallas de 160 × 208 con 15 entre ellas, centradas. Radio 20 y filo de tinta al 10 % de 1 dp. Sombra solo en oscuro: `shadow(18.dp)` con negro al 55 %, una vez y sin animar. Ya no va en un marco |
| Nombre del tema | Encabezado (Jost 300, 17/22), centrado, a 12 de las pantallas; 8 debajo |
| **Paleta** | dos filas: los cinco oscuros, un hilo y Exterior oscuro; debajo, los cinco claros, un hilo y Exterior. Disco de 40 en un toque de 52, con 4 entre filas. El hilo es de 1 × 36, en tinta al 16 %, con 8 a cada lado. El elegido lleva un aro de 2 dp en tinta, a 52 |
| Automático | tarjeta de una fila de 64, con radio 24 y a 16 de la paleta. Apagado, sin nota; encendido, la nota «Claro de día · Oscuro de noche» |
| Acento | Rótulo ACENTO, a 24, con el valor a la derecha. Seis toques de 44 repartidos; el elegido lleva un aro de 36 |
| Grupos | a 24, una tarjeta de 3 filas de 64: pastilla de 32 con radio 8 e icono de 18, Fila y Nota, chevrón de 16 en `apagada` y separador desde 60 |

Notas de los grupos (`resumen…()`, una por grupo):

- **Mapa:** «Rejilla · cables curvos», con el fondo y la forma de los cables.
- **Inicio y horario:** «Portada · 12 h», o «Sistemas · 24 h».
- **Accesibilidad:** «Texto normal · ligero automático». Si hay más contraste, se añade «· más contraste».

Iconos de los grupos:

- Mapa: `Lucide.mapa`, el de siempre.
- Inicio y horario: `Lucide.casa` (`house` de Lucide), que es nuevo.
- Accesibilidad: `Lucide.persona` (`person-standing` de Lucide), que también es nuevo. No es el icono de «personas»: ese es de sociohumanísticas.

La vista previa ya no va en la `cabecera` de la hoja, sino arriba del contenido, y se desplaza. A cambio, sigue a la vista mientras eliges, porque la paleta va justo debajo. Así se ganan 176 dp fijos en las subpáginas, que no la usan.

### La paleta: cómo se pinta cada disco

Es un `Canvas` de 40 dp con los tokens de ESE juego (`tokensDe(j)`), sin animación:

- **Fondo:** `lienzo`.
- **Luz de arriba:** `radialGradient` de `aurora` con alfa `min(aurora × 0,24, 0,3) × 1,7`, centrado en (26 %, 20 %) y con radio del 66 %.
- **Luz de abajo:** `radialGradient` de `aurora2` con alfa `… × 1,1`, centrado en (84 %, 92 %) y con radio del 58 %.
- **Filo:** círculo de 1 dp en la `tinta` del juego, al 18 %.
- **Exterior y Exterior oscuro:** sin luz. Llevan un filo de 1,5 dp al 70 % y, dentro, el símbolo de contraste: un círculo de 20 dp con su mitad izquierda en tinta.

Sin carrera, la paleta es la misma.

### Automático (la ranura de día y la de noche)

- **Encendido** guarda `Apariencia.SISTEMA`. **Apagado** guarda el juego que se está viendo, `Tema.juego`, para que la pantalla no salte.
- **Las dos pantallas** pasan a ser la portada en `temaDia` y en `temaNoche`. Debajo de cada una va su etiqueta: icono de 16 (`sol` o `luna`) y «De día · **Claro**». La etiqueta es Nota y el nombre va en 500.
- **La ranura activa** lleva un aro de 2 dp, a 4 de la pantalla y con radio 24 (20 + 4), y su etiqueta en tinta. Empieza activa la que coincide con la noche del teléfono. Tocar una pantalla la activa.
- **La paleta edita la ranura activa.** El aro marca su tema. El tema de la otra ranura lleva una insignia de 20 en el ángulo de abajo a la derecha: un disco de `panel` con su sol o su luna de 12 y un filo de tinta al 20 %.
- **Se mantiene** la regla de hoy: elegir «De noche» propone su `pareja` para «De día» si ese no se ha tocado.

## 3. Las tres páginas

Las tres usan la misma cabecera, con el rótulo PERSONALIZAR, el título y el botón de atrás, que vuelve a Personalizar. Llevan Rótulo a 20–24 y 8 hasta su control.

**Mapa**

- **Vista previa:** 335 × 168, con radio 24. Es un `Canvas` con tres semestres reales de tu carrera, el de trabajo y sus vecinos: tarjetas con su código, sus estados, sus cables y el fondo elegido. Lee `Tema.fondo`, `cables`, `grosor`, `atenuarAprobadas` y `codigos`. Sin barra Núcleo, porque esas opciones no la tocan.
- **FONDO:** tres baldosas, las de hoy.
- **CABLES:** tres baldosas, las de hoy.
- **GROSOR:** segmentado de 48, con una línea de 18 × (1 / 2 / 3) dp delante de «Fino», «Normal» y «Grueso».
- **Interruptores:** una tarjeta con «Atenuar lo aprobado» y «Códigos de materia».

**Inicio y horario**

- **AL ABRIR:** segmentado «Portada | Sistemas».
- **VISTA INICIAL:** segmentado con iconos de 18: el mapa plegado (`mapa3`) para «Mapa» y `lista` para «Lista».
- **HORARIO:** «Hora de 24 h», con una nota que enseña el formato elegido: «1:00 p. m.» o «13:00».

**Accesibilidad**

- **LECTURA:** el segmentado «Aa Normal | Aa Grande» y, debajo, la tarjeta «Más contraste» (con un Exterior: encendida, desactivada y con la nota «Exterior ya lo trae»).
- **MOVIMIENTO Y TACTO:** «Luz de fondo», «Reducir movimiento» y «Vibración».
- **MODO LIGERO:** «Automático | Sí | No» y su nota, la de hoy.

**Segmentado:** la opción elegida pasa a tinta al 11 % con un filo de tinta al 12 % y texto 500. Hoy es `panel` con sombra, que en oscuro queda más oscura que la pista.

## 4. Acostado (915 × 412)

- **A la izquierda, un índice de 330 que no se desplaza:**
  - Rótulo TEMA con el nombre a la derecha;
  - dos pantallas de 154 × 168 con 14 entre ellas;
  - la paleta con toques de 50.
- **Al centro, un hilo de 1 dp.**
- **A la derecha, una columna que se desplaza** con desvanecido de 40 abajo. Lleva «Automático», ACENTO y los tres grupos.
- **Al tocar un grupo, su página sustituye a esa columna,** con atrás en la cabecera. El índice no se mueve: es maestro y detalle, como Ajustes en una tableta.

## 5. Icono de Tu carrera · uno por carrera

Están en la rejilla de 24, con trazo 1,5 y puntas redondas, como Lucide. En `Lucide` (Iconos.kt):

| Carrera (slug) | Icono | SVG |
|---|---|---|
| ingenieria-de-sistemas | chip | `<rect x="5" y="5" width="14" height="14" rx="2"/><rect x="9" y="9" width="6" height="6" rx="1"/><path d="M9 2v3"/><path d="M15 2v3"/><path d="M9 19v3"/><path d="M15 19v3"/><path d="M2 9h3"/><path d="M2 15h3"/><path d="M19 9h3"/><path d="M19 15h3"/>` |
| ingenieria-de-petroleo | torre de perforación | `<path d="M8 21 11 3h2l3 18"/><path d="M9.7 11h4.6"/><path d="M9 15h6"/><path d="m9.7 11 5.3 4"/><path d="m14.3 11-5.3 4"/><path d="M4 21h16"/><path d="m8.6 18.5 6.8-3.5"/>` |
| ingenieria-agronomica | espiga (`wheat` de Lucide) | `<path d="M2 22 16 8"/><path d="M3.47 12.53 5 11l1.53 1.53a3.5 3.5 0 0 1 0 4.94L5 19l-1.53-1.53a3.5 3.5 0 0 1 0-4.94Z"/><path d="M7.47 8.53 9 7l1.53 1.53a3.5 3.5 0 0 1 0 4.94L9 15l-1.53-1.53a3.5 3.5 0 0 1 0-4.94Z"/><path d="M11.47 4.53 13 3l1.53 1.53a3.5 3.5 0 0 1 0 4.94L13 11l-1.53-1.53a3.5 3.5 0 0 1 0-4.94Z"/><path d="M20 2h2v2a4 4 0 0 1-4 4h-2V6a4 4 0 0 1 4-4Z"/><path d="M11.47 17.47 13 19l-1.53 1.53a3.5 3.5 0 0 1-4.94 0L5 19l1.53-1.53a3.5 3.5 0 0 1 4.94 0Z"/><path d="M15.47 13.47 17 15l-1.53 1.53a3.5 3.5 0 0 1-4.94 0L9 15l1.53-1.53a3.5 3.5 0 0 1 4.94 0Z"/><path d="M19.47 9.47 21 11l-1.53 1.53a3.5 3.5 0 0 1-4.94 0L13 11l1.53-1.53a3.5 3.5 0 0 1 4.94 0Z"/>` |
| ingenieria-ambiental | hoja (`leaf`) | `<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10Z"/><path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/>` |
| ingenieria-en-produccion-animal | cabeza de res | `<path d="M8.5 7C6.5 7 5 5.5 5 3.5"/><path d="M15.5 7c2 0 3.5-1.5 3.5-3.5"/><path d="M7 13V8.5A1.5 1.5 0 0 1 8.5 7h7A1.5 1.5 0 0 1 17 8.5V13"/><path d="M7 9.5H4.5c0 1.7 1 2.5 2.5 2.5"/><path d="M17 9.5h2.5c0 1.7-1 2.5-2.5 2.5"/><path d="M10.5 13h3a4 4 0 0 1 0 8h-3a4 4 0 0 1 0-8z"/><path d="M10 17h.01"/><path d="M14 17h.01"/><path d="M9.8 10.2h.01"/><path d="M14.2 10.2h.01"/>` |
| licenciatura-en-administracion | maletín (`briefcase`) | `<path d="M16 20V4a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/><rect width="20" height="14" x="2" y="6" rx="2"/>` |
| licenciatura-en-contaduria-publica | calculadora (`calculator`) | `<rect width="16" height="20" x="4" y="2" rx="2"/><path d="M8 6h8"/><path d="M16 14v4"/><path d="M16 10h.01"/><path d="M12 10h.01"/><path d="M8 10h.01"/><path d="M12 14h.01"/><path d="M8 14h.01"/><path d="M12 18h.01"/><path d="M8 18h.01"/>` |
| licenciatura-en-tecnologia-de-los-alimentos | matraz (`flask-conical`) | `<path d="M14 2v6a2 2 0 0 0 .245.96l5.51 10.08A2 2 0 0 1 18 22H6a2 2 0 0 1-1.755-2.96l5.51-10.08A2 2 0 0 0 10 8V2"/><path d="M6.453 15h11.094"/><path d="M8.5 2h7"/>` |
| gerencia-de-recursos-humanos | apretón de manos (`handshake`) | `<path d="m11 17 2 2a1 1 0 1 0 3-3"/><path d="m14 14 2.5 2.5a1 1 0 1 0 3-3l-3.88-3.88a3 3 0 0 0-4.24 0l-.88.88a1 1 0 1 1-3-3l2.81-2.81a5.79 5.79 0 0 1 7.06-.87l.47.28a2 2 0 0 0 1.42.25L21 4"/><path d="m21 3 1 11h-2"/><path d="M3 3 2 14l6.5 6.5a1 1 0 1 0 3-3"/><path d="M3 4h8"/>` |

- **Cada `<rect>` va como path en `trazo(...)`,** como ya se hace con `universidad`. Las rutas son las mismas que dibuja la maqueta.
- **La función:** `fun iconoCarrera(slug: String?): List<Path>`, con un `when (slug)` y `Lucide.universidad` como respaldo para un slug desconocido.
- **Nada repetido:** Sistemas no lleva `</>`, porque ese es «Código abierto», dos filas más abajo en la misma hoja. RRHH no lleva «personas», porque ese icono es de sociohumanísticas.

Dónde va (todo en tinta, sin el color de la carrera ni el azul):

- **`Ajustes.kt`, `FilaCarrera`:** `TeclaIcono(iconoCarrera(propia?.slug), Paleta.suave)`, con el fondo de tinta al 6 % de siempre. Sin carrera, el mismo `universidad` en neutro y «Elige la tuya».
- **`Personalizar.kt`, `CirculoAcento(null)`:**
  - disco de **28** en tinta al 12 %, con el icono de 14 en `Paleta.tinta` y trazo 1,75; los demás discos siguen en 24;
  - el elegido lleva su aro de 36, como los demás;
  - ya no se pinta el disco en `colorCarrera`.
- **Sin carrera, el acento «Tu carrera» cae a `Paleta.suave`** y no a `Paleta.azul`: hoy `Ajustes.kt:144` y `:165` le pasan `?: Paleta.azul`. Es un azul que no es inscribible.

## 6. Archivos que cambian

| Archivo | Cambio |
|---|---|
| `tema/Tema.kt` | quitar `MORICHAL` de `Juego`, de `pareja`, de `TABLA` y de `TABLA_CONTRASTE` (las tablas van por `ordinal`: borrar la fila en las dos) |
| `plataforma/Plataforma.kt` | quitar `Apariencia.MORICHAL`. Al leer `"morichal"`, migra a **Medianoche** (el oscuro de luz azul más cercano). Si estaba en `temaDia` o `temaNoche`, igual |
| `componentes/Iconos.kt` | los nueve iconos de §5, `casa`, `persona`, `sol`, `luna`, `mapa3` y `iconoCarrera(slug)` |
| `componentes/SelectorTema.kt` | `PaletaTemas` (las dos filas, el disco y la insignia) y `VistaPrevia` nueva: dos pantallas de portada y mapa, o la ranura de día y la de noche. `previaMapaAncha` para la página Mapa. Se borra `CarruselTemas` |
| `portada/Personalizar.kt` | raíz (tema, automático, acento y grupos) y tres páginas: `PersonalizarMapa`, `PersonalizarInicio` y `PersonalizarAccesibilidad`. Interruptores nuevos para «Atenuar lo aprobado» y «Hora de 24 h». Segmentado de grosor. `resumenMapa()`, `resumenInicio()` y `resumenAccesibilidad()` |
| `portada/Ajustes.kt` | `PaginaAjustes` gana `MAPA`, `INICIO` y `ACCESIBILIDAD`, con atrás a `PERSONALIZAR` (rótulo PERSONALIZAR). Fuera, la `VistaPrevia` de la cabecera. `FilaCarrera` lleva el icono neutro. Fuera también el `?: Paleta.azul` |
| pruebas `Captura*` | Personalizar de pie y acostado, con Automático y en las tres páginas. Quitar Morichal de `Captura*Tema*` |

## 7. Estados

- **Con carrera y avance:** la portada de la vista previa es tu carrera. Lleva el nombre, el %, la silueta de `resumenParaPortada`, la fecha y «Seguir donde ibas». El mapa es tu semestre de trabajo y el siguiente.
- **Sin marcas:**
  - la silueta va toda en pendiente, con el 0 %;
  - el mapa pone los dos primeros semestres, con las inscribibles en azul.
- **Sin carrera:**
  - la portada enseña «Elige tu carrera» con una silueta tenue, y el mapa, los nombres como barras (como `MiniTema`);
  - el acento «Tu carrera» lleva `universidad` en neutro;
  - «Al abrir» se queda sin el segmento de la carrera.
- **Cargando el pensum:** la portada ya puede dibujarse con el índice. El mapa espera con sus tarjetas en barras, sin indicador de carga y sin mover el layout: el tamaño es fijo.

## 8. Coste en gama baja

- **Sin desenfoque nuevo y sin animación** en ninguna vista previa ni en la paleta. Son `Canvas` que leen la `Paleta` y el `Tema`, así que solo se redibujan cuando cambia una preferencia.
- **Los textos de las pantallas en miniatura** (nombre, %, fecha, nombres y códigos de las tarjetas) se miden una vez con `rememberTextMeasurer`, en `remember(carrera, juego, textoGrande)`. Se dibujan con `scale()` sobre la medida real de 375, la misma maqueta.
- **Los 12 discos** son 12 `Canvas` de 40 dp con dos `radialGradient` cada uno. Es lo mismo que pintaban hoy los 13 `MiniTema` del carrusel, con menos trazos.
- **La sombra de las dos pantallas** es una sola `shadow()` estática por pantalla. En modo ligero se quita y queda solo el filo.
- **Navegar entre páginas** usa el `AnimatedContent` de hoy, con deslizamiento de 320 ms y fundido.
