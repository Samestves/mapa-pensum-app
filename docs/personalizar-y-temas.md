# Personalizar, iconos y temas

Fase 2 de la app nativa. Maqueta: `docs/maquetas/personalizar.html` (PNG al lado). Lo pidió Sam: «El botón Personalizar necesita un icono acorde, al igual que el icono de tema de tu carrera. Replantear esos temas personalizados que tenemos, mejorar y meter más […] y quizá más opciones de personalización».

Todas las tablas de este documento salen del mismo script que dibuja la maqueta: los colores y los contrastes son los mismos en los dos sitios. El contraste es WCAG 2.x (luminancia relativa), con L = sobre el lienzo y P = sobre el panel.

## Lo recomendado

| Qué | Recomendado | Por qué, en una línea |
|---|---|---|
| Icono de Personalizar | **Muestrario** (tres fichas de color en abanico) | Dice «temas y colores» y no «ajustes»; es la única fila de la hoja que trata de colores. |
| Icono de Tu carrera | **Recorrido** (dos paradas y su camino) | Carrera es recorrido, del primer semestre al grado, y no repite ningún icono de la tabla fija (birrete es grado; mapa, ir al mapa; bandera, reportar). |
| Temas | **11**: Oscuro, Negro, Grafito, Medianoche, Ciruela, Morichal · Claro, Papel, Niebla, Aluminio, Cuarzo | Cada uno distinto en una mirada: matiz del lienzo, luz, fondo del mapa y color de los cables. Sin ámbar ni marrón. |
| Exterior | Pasa a ser el interruptor **Más contraste** | El contraste es otro eje, no otro color: así sirve en los once temas (Negro con contraste es el OLED de alto contraste). |
| Opciones nuevas | **Fondo del mapa**, **Cables**, **Más contraste** y, con Sistema, **De día / De noche** | No cuestan cuadros: son tokens, un trazado que se calcula una vez o menos cables que pintar. |
| Pantalla | **Vista previa fija** arriba (portada y mapa) y bloques TEMA · ACENTO · MAPA · LECTURA · MOVIMIENTO | Cada cambio se ve en el acto sin subir; lo que cambia la cara va primero (jerarquía), lo de accesibilidad y rendimiento al final. |

## 1. Iconos

Rejilla de 24, trazo 1,5, puntas y uniones redondas, área viva de 20 (como Lucide). En Ajustes van en la tecla de siempre: 40 × 40, radio `Radio.Chip`, fondo tinta al 6 %, icono de 20 dp con `IconoTrazo(…, grosor = 1.75f)`, como «Ver la guía» o «Reportar un error».

### Personalizar · Muestrario (recomendado)

Es `swatch-book` de Lucide: una ficha vertical con su agujero, una girada y una tumbada.

```svg
<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
  <path d="M11 17a4 4 0 0 1-8 0V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2Z"/>
  <path d="M16.7 13H19a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H7"/>
  <path d="M7 17h.01"/>
  <path d="m11 8 2.3-2.3a2.4 2.4 0 0 1 3.404.004L18.6 7.6a2.4 2.4 0 0 1 .026 3.434L9.9 19.8"/>
</svg>
```

```kotlin
    /** Personalizar: tres fichas de color en abanico (swatch-book de Lucide) */
    val muestrario = listOf(trazo("M11 17a4 4 0 0 1-8 0V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2Z"), trazo("M16.7 13H19a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H7"), trazo("M7 17h.01"), trazo("m11 8 2.3-2.3a2.4 2.4 0 0 1 3.404.004L18.6 7.6a2.4 2.4 0 0 1 .026 3.434L9.9 19.8"))
```

Alternativas dibujadas en la maqueta: **Paleta** (`palette` de Lucide; universal, pero es la de cualquier app y sus puntos compiten con la silueta de puntos) y **Contraste** (`contrast` de Lucide; solo habla de claro u oscuro).

### Tu carrera · Recorrido (recomendado)

Es `route` de Lucide: la parada de abajo a la izquierda es el inicio, la de arriba a la derecha el grado.

```svg
<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
  <path d="M3 19a3 3 0 1 0 6 0a3 3 0 1 0-6 0z"/>
  <path d="M9 19h8.5a3.5 3.5 0 0 0 0-7h-11a3.5 3.5 0 0 1 0-7H15"/>
  <path d="M15 5a3 3 0 1 0 6 0a3 3 0 1 0-6 0z"/>
</svg>
```

```kotlin
    /** Tu carrera: dos paradas y su camino (route de Lucide). Carrera es recorrido */
    val recorrido = listOf(trazo("M3 19a3 3 0 1 0 6 0a3 3 0 1 0-6 0z"), trazo("M9 19h8.5a3.5 3.5 0 0 0 0-7h-11a3.5 3.5 0 0 1 0-7H15"), trazo("M15 5a3 3 0 1 0 6 0a3 3 0 1 0-6 0z"))
```

Alternativas: **Constelación** (la silueta de hoy en trazo: 13 puntos que a 20 dp parecen un teclado) y **Banderín** («tus colores»; queda a dos filas de la bandera de «Reportar un error»).

### Dónde va cada uno

- **`Ajustes.kt`, `FilaPersonalizar`:** la tecla con `Lucide.muestrario` en `Paleta.suave` sustituye a `MiniVistaTema`. Así el texto empieza en 72 dp, como en todas las filas.
- **`Ajustes.kt`, `FilaCarrera`:** la tecla con `Lucide.recorrido`, teñida con el color de la carrera. El fondo es `colorDe(propia.color)` al 16 %, y el icono va en ese color. Sustituye a la silueta de 56 dp. Sin carrera elegida, la tecla neutra con el mismo icono y «Elige la tuya». El birrete queda para «grado».
- **`Personalizar.kt`, `CirculoAcento(null)`:** un disco de 24 en `colorCarrera` con `IconoTrazo(Lucide.recorrido, Paleta.panel, 14.dp, 2.4f)` centrado. Es decir, el recorrido calado. Así «Tu carrera» se lee como un color más de la fila y no como un hueco oscuro con puntitos.
- **La silueta de puntos no desaparece:** sigue en la portada y en «Elige tu carrera», donde distingue una carrera de otra.

## 2. Temas

### Qué los hace distintos (sin coste)

Cada tema tiene cuatro rasgos además de sus colores:

1. **El matiz del lienzo y del panel.**
2. **Las dos luces de la aurora** (`aurora` y `aurora2`) y su fuerza (`Juego.aurora`).
3. **El fondo del mapa por defecto:** rejilla, puntos o liso.
4. **El color de los cables** (`Juego.cables`): 1 es el color de su área y 0 es la tinta.

Todo es tabla y dos números: nada se anima de más ni se desenfoca.

| Tema | Carácter | Fondo | Cables | Luz | Pareja (Sistema) |
|---|---|---|---|---|---|
| **Oscuro** | Acero, el de siempre: grafito frío, luz azul y turquesa. | rejilla | 1,0 | 1,0 | Claro |
| **Negro** | OLED: píxeles apagados, sin luz ni rejilla; los cables brillan solos. | liso | 1,0 | no | Claro |
| **Grafito** | Mina de lápiz: gris mate y tenue para la noche, cables a medio color. | puntos | 0,5 | 0,5 | Aluminio |
| **Medianoche** | Azul de consola: luz azul e índigo desde arriba y rejilla de plano. | rejilla | 1,0 | 1,1 | Niebla |
| **Ciruela** | Berenjena apagada: violeta y rosa muy bajos, nada de neón. | puntos | 0,9 | 0,9 | Cuarzo |
| **Morichal** | Agua oscura de morichal: verde petróleo con luz turquesa y cian. | rejilla | 0,9 | 0,9 | Niebla |
| **Claro** | Acero claro: blanco limpio, cristal lechoso y un tinte azul arriba. | rejilla | 1,0 | 0,3 | Oscuro |
| **Papel** | Cuaderno de puntos: mate, sin luz, tinta azul-negra y cables a tinta. | puntos | 0,1 | no | Grafito |
| **Niebla** | Bruma de Caripe: gris azulado frío; el claro de Medianoche. | rejilla | 0,9 | 0,5 | Medianoche |
| **Aluminio** | Plata cepillada: gris medio sin deslumbrar y un brillo blanco arriba. | rejilla | 0,7 | 0,7 | Grafito |
| **Cuarzo** | Cuarzo rosa: claro cálido sin crema, tinta ciruela y luz rosa y lila. | puntos | 0,8 | 0,3 | Ciruela |

- **Oscuro y Claro no cambian:** la Paleta Acero ya está decidida. A Claro solo se le oscurece el `azul`, de `3E6AE1` a `3A66DC`, para pasar 4,5:1 sobre el lienzo (antes, 4,4).
- **Papel pierde la crema y el marrón.** Lienzo `F2EEE6`, tinta `2A251E` y aurora `8A7458` chocaban con la regla del ámbar. Pasa a gris mate con tinta azul-negra (`141A2A`), puntos y cables a tinta.
- **Negro es el OLED.** Lienzo `000000`, sin luz y fondo liso, para que los píxeles del fondo estén apagados.
- **Exterior desaparece como tema:** ahora es «Más contraste» (§3).
- **Ningún tema toca el significado de los estados:** el azul es lo inscribible, el verde lo aprobado y el rojo lo bloqueado. Cada juego ajusta solo su luminosidad para el contraste.
- **`aspecto()` de `Tarjeta.kt` no cambia:** ya sale todo de la paleta.

### Tokens · oscuros

| Token | Oscuro | Negro | Grafito | Medianoche | Ciruela | Morichal |
|---|---|---|---|---|---|---|
| lienzo | `0B0C0E` | `000000` | `27292C` | `0A1020` | `100D15` | `06110F` |
| panel | `121316` | `0A0A0B` | `303236` | `10192D` | `17131E` | `0C1917` |
| panelSuave | `16181B` | `0F1012` | `36393D` | `151F37` | `1C1825` | `10201E` |
| borde | `262A2F` | `202328` | `494C52` | `24304B` | `2D2738` | `1E3330` |
| tinta | `ECEEF1` | `F2F3F5` | `F1F2F4` | `E9EEF8` | `EFEBF4` | `E5F0EE` |
| suave | `B6BBC2` | `BEC2C8` | `CDD0D5` | `B4BFD3` | `BEB6CA` | `ADC2BE` |
| tenue | `858B94` | `8A9099` | `A9AEB5` | `8794AE` | `9088A1` | `7D9691` |
| apagada | `5D636C` | `5F656E` | `8B9098` | `5F6C88` | `6B6380` | `556B67` |
| azul | `5B8AF5` | `5B8AF5` | `86A9FA` | `7DA4FF` | `6F96FF` | `6C95FF` |
| azulClaro | `9DB8FF` | `9DB8FF` | `B8CCFF` | `AEC6FF` | `A6BEFF` | `A4BEFF` |
| verde | `2EC27E` | `2EC27E` | `52D197` | `3FCF8E` | `37C886` | `35CC85` |
| verdeClaro | `96E0BE` | `96E0BE` | `A8E7CB` | `A0E4C6` | `9DE2C2` | `9BE3C1` |
| rojo | `E5403A` | `EA4A43` | `F47E76` | `F26A63` | `EE5F58` | `EE5E57` |
| aurora | `6094FF` | `6094FF` | `C9D2DE` | `3A78FF` | `A57BFF` | `2BD0BC` |
| aurora2 | `2FE8C6` | `2FE8C6` | `7FA5FF` | `8C6BFF` | `F27BB5` | `3FA8F2` |
| rejilla | `15171A` | `0F1012` | `3A3D42` | `141E36` | `2A2433` | `102320` |
| vidrio | `15171A` | `0C0D0F` | `34373B` | `121B30` | `1A1622` | `0F1C1A` |
| sombra | `000000` | `000000` | `121315` | `02050C` | `040206` | `010605` |

### Tokens · claros

| Token | Claro | Papel | Niebla | Aluminio | Cuarzo |
|---|---|---|---|---|---|
| lienzo | `F3F4F6` | `EEEFEF` | `E6EBF1` | `D8DBDF` | `F4EDF0` |
| panel | `FFFFFF` | `F8F9F9` | `F5F7FA` | `E8EAED` | `FDF9FA` |
| panelSuave | `F7F8F9` | `F2F3F3` | `EDF1F5` | `E1E4E7` | `F8F2F4` |
| borde | `DADDE1` | `D3D6D9` | `CED6E0` | `BFC4CA` | `E2D5DA` |
| tinta | `171A20` | `141A2A` | `0E1626` | `0D1015` | `1E1419` |
| suave | `393C42` | `343B4D` | `323F58` | `2A2F36` | `45343F` |
| tenue | `5B5F66` | `515A6B` | `4C5973` | `434952` | `65525E` |
| apagada | `72767D` | `6A7282` | `66728B` | `5B616A` | `7E6A76` |
| azul | `3A66DC` | `2F5DC4` | `2E5ED0` | `2A55BF` | `3A62D6` |
| azulClaro | `2F57C9` | `284FA8` | `254FB4` | `22479F` | `2D51B8` |
| verde | `0F8A5F` | `1C7A4F` | `0E7F57` | `0A6B45` | `0D7F55` |
| verdeClaro | `0B7350` | `166541` | `0A6B49` | `07593A` | `0A6A47` |
| rojo | `CC1F24` | `B3302A` | `C0242A` | `A81E22` | `C21F2F` |
| aurora | `2751CC` | `2F5DC4` | `3B6EE0` | `FFFFFF` | `D9568F` |
| aurora2 | `0A9781` | `6A7282` | `7FA6FF` | `B4C2D6` | `8E6FE8` |
| rejilla | `E3E5E8` | `C3C8CF` | `D3DBE5` | `C8CCD1` | `DCCDD4` |
| vidrio | `FFFFFF` | `F8F9F9` | `F5F7FA` | `EDEFF1` | `FDF9FA` |
| sombra | `171A20` | `141A2A` | `0E1626` | `0D1015` | `1E1419` |

`ambar` sale de la tabla: `Paleta` no lo expone y nadie lo lee («Cursando» es la tinta con contorno). `turquesa` pasa a llamarse `aurora2`, porque en Ciruela es rosa y en Aluminio, plata.

### Contraste comprobado

Texto (tinta, suave, tenue, azul, rojo): ≥ 4,5:1 sobre lienzo **y** panel. `apagada` y `verde` solo van en iconos: ≥ 3:1 (WCAG 1.4.11).

| Tema | tinta L / P | suave L / P | tenue L / P | apagada L / P (icono, ≥ 3) | azul L / P | rojo L / P | verde P (icono) |
|---|---|---|---|---|---|---|---|
| Oscuro | 16,8 / 16,0 | 10,1 / 9,6 | 5,7 / 5,4 | 3,2 / 3,1 | 6,0 / 5,7 | 4,8 / 4,5 | 8,1 |
| Negro | 18,9 / 17,8 | 11,7 / 11,1 | 6,5 / 6,2 | 3,6 / 3,4 | 6,4 / 6,0 | 5,6 / 5,2 | 8,6 |
| Grafito | 13,0 / 11,5 | 9,4 / 8,3 | 6,5 / 5,8 | 4,5 / 4,0 | 6,3 / 5,5 | 5,6 / 4,9 | 6,7 |
| Medianoche | 16,3 / 15,1 | 10,2 / 9,5 | 6,2 / 5,7 | 3,6 / 3,3 | 7,8 / 7,2 | 6,3 / 5,9 | 8,8 |
| Ciruela | 16,4 / 15,6 | 9,8 / 9,3 | 5,7 / 5,4 | 3,4 / 3,2 | 6,9 / 6,5 | 5,9 / 5,6 | 8,5 |
| Morichal | 16,5 / 15,4 | 10,3 / 9,6 | 6,1 / 5,7 | 3,4 / 3,2 | 6,7 / 6,3 | 5,8 / 5,5 | 8,7 |
| Claro | 15,8 / 17,4 | 10,0 / 11,1 | 5,8 / 6,4 | 4,1 / 4,6 | 4,6 / 5,1 | 5,0 / 5,5 | 4,4 |
| Papel | 15,1 / 16,4 | 9,7 / 10,6 | 6,0 / 6,6 | 4,2 / 4,6 | 5,2 / 5,7 | 5,4 / 5,9 | 5,0 |
| Niebla | 15,1 / 16,8 | 8,8 / 9,8 | 5,9 / 6,6 | 4,0 / 4,5 | 4,8 / 5,4 | 5,0 / 5,5 | 4,7 |
| Aluminio | 13,7 / 15,8 | 9,7 / 11,2 | 6,5 / 7,5 | 4,5 / 5,2 | 4,8 / 5,5 | 5,3 / 6,1 | 5,4 |
| Cuarzo | 15,6 / 17,2 | 10,0 / 11,1 | 6,2 / 6,9 | 4,3 / 4,8 | 4,7 / 5,2 | 5,2 / 5,7 | 4,8 |

Peor caso en los once temas: tinta 11,5 · suave 8,3 · tenue 5,4 · azul 4,6 · rojo 4,5 · apagada 3,1 · verde 4,4. **Todo pasa.**

### Más contraste (el antiguo Exterior, para los once)

Se calcula una vez por juego y queda en una segunda tabla (`TABLA_CONTRASTE`, más abajo). Cambia esto:

- la tinta va un 60 % hacia el negro o el blanco;
- suave, tenue y apagada se acercan a la tinta (50, 40 y 30 %);
- el borde (30 %) y la rejilla (6 %), también.

Además, hace lo que hoy hace `Tema.fuerte`: bordes de 1,5, cables ×1,5 y lo inscribible con borde azul.

| Tema | tinta L / P | suave L / P | tenue L / P | apagada L / P (icono, ≥ 3) | azul L / P | rojo L / P | verde P (icono) |
|---|---|---|---|---|---|---|---|
| Oscuro | 18,4 / 17,5 | 13,9 / 13,2 | 9,7 / 9,2 | 6,1 / 5,8 | 6,0 / 5,7 | 4,8 / 4,5 | 8,1 |
| Negro | 20,1 / 19,0 | 15,6 / 14,7 | 10,8 / 10,2 | 6,7 / 6,3 | 6,4 / 6,0 | 5,6 / 5,2 | 8,6 |
| Grafito | 14,0 / 12,3 | 11,6 / 10,2 | 9,1 / 8,0 | 6,7 / 5,9 | 6,3 / 5,5 | 5,6 / 4,9 | 6,7 |
| Medianoche | 17,8 / 16,5 | 13,7 / 12,7 | 9,9 / 9,2 | 6,4 / 5,9 | 7,8 / 7,2 | 6,3 / 5,9 | 8,8 |
| Ciruela | 18,1 / 17,2 | 13,5 / 12,9 | 9,6 / 9,1 | 6,2 / 5,9 | 6,9 / 6,5 | 5,9 / 5,6 | 8,5 |
| Morichal | 18,1 / 17,0 | 13,9 / 13,0 | 9,9 / 9,3 | 6,2 / 5,8 | 6,7 / 6,3 | 5,8 / 5,5 | 8,7 |
| Claro | 18,0 / 19,8 | 14,3 / 15,7 | 9,9 / 10,9 | 6,7 / 7,4 | 4,6 / 5,1 | 5,0 / 5,5 | 4,4 |
| Papel | 17,2 / 18,8 | 13,8 / 15,0 | 9,9 / 10,8 | 6,7 / 7,3 | 5,2 / 5,7 | 5,4 / 5,9 | 5,0 |
| Niebla | 16,6 / 18,6 | 13,0 / 14,5 | 9,7 / 10,8 | 6,5 / 7,3 | 4,8 / 5,4 | 5,0 / 5,5 | 4,7 |
| Aluminio | 14,6 / 16,8 | 12,5 / 14,4 | 9,8 / 11,3 | 6,8 / 7,9 | 4,8 / 5,5 | 5,3 / 6,1 | 5,4 |
| Cuarzo | 17,3 / 19,1 | 14,0 / 15,5 | 10,2 / 11,3 | 6,9 / 7,6 | 4,7 / 5,2 | 5,2 / 5,7 | 4,8 |

Peor caso con contraste: suave 10,2 · tenue 8,0 · apagada 5,8. Los textos pasan 7:1 (AAA) en los once.

## 3. Acentos

| Hoy | Veredicto |
|---|---|
| Arcilla `E08A6A` / `A8492C` | **Fuera.** En claro es teja, un marrón rojizo, y queda a un paso del rojo de bloqueada. |
| Rosa `E596AE` / `A8466A` | **Se queda**, más vivo en oscuro (`EE93B0`) y más hondo en claro (`9C3860`): con `A8466A` daba 4,0:1 sobre Aluminio. |
| Laguna `45C4C8` / `0B767C` | **Se queda**, con el claro en `0A6A70` (daba 3,9:1 sobre Aluminio). |
| Arena `CDBBA4` / `75644F` | **Fuera.** Es el marrón-amarillo prohibido. |

La propuesta es Neutro, Laguna, Lila, Orquídea y Rosa, más «Tu carrera»:

| Acento | Oscuro | Claro | Chip sobre el panel oscuro más claro (Grafito) | Chip sobre el claro más oscuro (Aluminio, lienzo y panel) |
|---|---|---|---|---|
| Laguna | `45C4C8` | `0A6A70` | 6,1 | 4,6 |
| Lila | `A99CF6` | `5644C2` | 5,4 | 5,0 |
| Orquídea | `D98BE3` | `8E3A9C` | 5,4 | 4,7 |
| Rosa | `EE93B0` | `9C3860` | 5,8 | 4,8 |

- **Ninguno es azul:** el único azul sigue siendo lo inscribible.
- **«arcilla» y «arena» guardados:** `Acento.de()` devuelve null y la app vuelve a «Tu carrera», sin migración.

**Chocan con la regla, fuera de esta fase.** Son datos del índice y del mapa; se anotan para la fase del mapa:

- **Color de Petróleo** en `indice.json`: `E8A33D` / `9A6410`, ámbar. Con «Tu carrera», el acento sale ámbar.
  - Candidato: un violeta orquídea, `C77DF0` / `7E3AB0`, en el hueco de color libre entre Administración y Recursos Humanos.
- **`colorArea`** (el color de los cables):
  - `electronica` `F2A65A` / `B8610F` es naranja;
  - `tesis` `E8CF4F` / `957200` es mostaza.
  - Candidatos: lima `B5D96A` / `5E7F12` para electrónica y plata `C3CAD3` / `5D6570` para tesis.
- **A vigilar:** el claro de Producción Animal (`5F7A16`) es oliva.

## 4. Opciones nuevas

| Opción | Valores | Por defecto | Coste en gama baja |
|---|---|---|---|
| **Fondo del mapa** | Rejilla · Puntos · Liso | el del tema; elegir otro tema vuelve al suyo | El de hoy. Se pinta en la capa del mapa, como la rejilla. Los puntos son una llamada `drawPoints(PointMode.Points, cap = Round)` con la lista guardada por tamaño; liso no pinta nada. |
| **Cables** | Curvos · Rectos · Al tocar | Curvos | **Rectos**: el trazado cambia una vez en `Geometria.trazar` (mismo número de `Path`). **Al tocar**: solo se pintan los cables de la tarjeta señalada, así que es **menos** trabajo por cuadro. |
| **Más contraste** | interruptor | apagado; encendido si venía de Exterior | Ninguno: es otra tabla de tokens, calculada una vez. |
| **Sistema: de día y de noche** | dos temas | Claro y Oscuro | Ninguno. Aparece solo con Sistema elegido (divulgación progresiva). Elegir «De noche» propone su pareja para «De día» si no se ha tocado. |

**Descartadas:**

| Opción | Por qué no |
|---|---|
| Densidad del mapa | el zoom ya la da; cambiar el tamaño de tarjeta rehace el layout y el corte de nombres (`partirEnLineas`) |
| Estilo de las tarjetas | la tarjeta es el lenguaje de estado (`aspecto()`): 5 estados × 11 temas × N estilos que verificar |
| Forma de la barra | la barra Núcleo es pieza de marca y memoria del pulgar; serían dos layouts por orientación |
| Fuente de los títulos | Jost es la voz de la marca (regla 2) |
| Radio de las esquinas | rompe los radios concéntricos |
| Intensidad de la luz | el interruptor y el tema ya la fijan; en modo ligero no existe |
| Grano o textura | cuesta en gama baja; el modo ligero ya lo quita |
| Icono de la app | pide `activity-alias`, cierra la app al cambiar y algunos lanzadores borran el acceso directo |
| Más tamaños de texto | Android ya escala la letra; Normal y Grande bastan |

## 5. La pantalla Personalizar

### De pie (375 × 812)

| Pieza | Medida |
|---|---|
| Cabecera | la de hoy: atrás de 36, rótulo AJUSTES (11/16) y «Personalizar» (Jost 300, 28/32), cerrar de 36 |
| **Vista previa** | 343 × 176, radio 24, fondo de tinta al 5 %, relleno 8. Dentro, dos ventanas de 159 × 160 con radio 16 (24 − 8): la portada (luz, cristal de «Tu carrera» con su silueta, el %, «Continuar →» en el acento) y el mapa (fondo, ocho tarjetas en sus cinco estados, cables y la barra con el anillo del % en el acento) |
| Rótulos | Rótulo (Inter 500, 11/16, +8 %), a 20 dp del borde; el valor a la derecha en Nota `suave` («Medianoche», «Tu carrera»). 20 encima, 8 debajo |
| Tema | carrusel: Sistema, los seis oscuros, un hilo de 1 × 64 y los cinco claros. Celdas de 72 × 96 con la mini de 60 × 84 (radio 10); la elegida con aro de 2 dp y radio 16; nombre en 12/16 |
| Acento | seis círculos de 24 en toques de 44, repartidos; el elegido con aro de 36 |
| Mapa | «Fondo» y «Cables» como Nota (13/16, `tenue`) sobre tres baldosas de 109 × 76, radio 16, fondo de tinta al 5 %, con un dibujo de 64 × 30 y su nombre en Nota (elegida: 500 `tinta` y aro de 2 dp). Debajo, «Códigos en el mapa» |
| Lectura | segmentado «Aa Normal / Aa Grande» (48, radio 24; opción de 40, radio 20) y «Más contraste» |
| Movimiento | «Luz de fondo» y «Reducir movimiento», y el segmentado del modo ligero con su nota («Activo: sin desenfoque») |
| Filas de interruptor | 64 de alto. El título pasa a **Fila** (Inter 500, 15/20), como manda `reglas-diseno.md` §1 para las opciones (hoy es Jost 17) |

**Lo que no se desplaza:** la vista previa va en la `cabecera` de `HojaInferior`, bajo el título, que no se mueve. Al bajar, el contenido pasa por debajo con un desvanecido de 20 dp.

### Acostado (915 × 412)

- **A la izquierda, el índice de 330:** vista previa de 330 × 166 y el carrusel de temas.
- **Al centro, el hilo.**
- **A la derecha, en una sola columna que se desplaza:** acento, mapa, lectura y movimiento, con desvanecido abajo.
- **La columna de 330 no se desplaza.**

### La vista previa en el código

- **Qué es:** `VistaPrevia(colorCarrera)`, dos `Canvas` que pintan con la `Paleta` del tema que se ve, como `MiniTema`. Sin Haze ni animación. Lee `Tema.juego`, `Tema.contraste`, `Tema.fondo`, `Tema.cables` y el acento, así que solo se redibuja cuando cambia uno de ellos.
- **Textos:** van medidos una vez con `rememberTextMeasurer`.
- **Nada inventado, con carrera elegida:**
  - la portada lleva el nombre, el % y la silueta de `resumenParaPortada`;
  - las ocho tarjetas son las de tu semestre de trabajo y el siguiente (las que encuadra la cámara al abrir).
- **Sin carrera:** los nombres van como barras, como en `MiniTema`.

## 6. Qué cambia en el código

### `tema/Tema.kt`

```kotlin
/** El fondo del mapa */
enum class Fondo { REJILLA, PUNTOS, LISO }

/** Cómo se dibujan los cables de prelación */
enum class FormaCables { CURVOS, RECTOS, AL_TOCAR }

/**
 * Los once juegos de color. `aurora` es cuánta luz trae de serie, `fondo` el
 * fondo del mapa que pone al elegirlo y `cables` cuánto color de su área
 * llevan los cables (1 el del área, 0 la tinta).
 */
enum class Juego(val claro: Boolean, val aurora: Float, val nombre: String, val fondo: Fondo, val cables: Float) {
    OSCURO(false, 1f, "Oscuro", Fondo.REJILLA, 1f),
    NEGRO(false, 0f, "Negro", Fondo.LISO, 1f),
    GRAFITO(false, 0.45f, "Grafito", Fondo.PUNTOS, 0.5f),
    MEDIANOCHE(false, 1.15f, "Medianoche", Fondo.REJILLA, 1f),
    CIRUELA(false, 0.9f, "Ciruela", Fondo.PUNTOS, 0.9f),
    MORICHAL(false, 0.9f, "Morichal", Fondo.REJILLA, 0.9f),
    CLARO(true, 0.35f, "Claro", Fondo.REJILLA, 1f),
    PAPEL(true, 0f, "Papel", Fondo.PUNTOS, 0.15f),
    NIEBLA(true, 0.45f, "Niebla", Fondo.REJILLA, 0.9f),
    ALUMINIO(true, 0.7f, "Aluminio", Fondo.REJILLA, 0.7f),
    CUARZO(true, 0.35f, "Cuarzo", Fondo.PUNTOS, 0.8f),
}

/** La pareja de cada juego, para proponer el otro de Sistema */
val Juego.pareja: Juego get() = when (this) {
    Juego.OSCURO -> Juego.CLARO
    Juego.NEGRO -> Juego.CLARO
    Juego.GRAFITO -> Juego.ALUMINIO
    Juego.MEDIANOCHE -> Juego.NIEBLA
    Juego.CIRUELA -> Juego.CUARZO
    Juego.MORICHAL -> Juego.NIEBLA
    Juego.CLARO -> Juego.OSCURO
    Juego.PAPEL -> Juego.GRAFITO
    Juego.NIEBLA -> Juego.MEDIANOCHE
    Juego.ALUMINIO -> Juego.GRAFITO
    Juego.CUARZO -> Juego.CIRUELA
}
```

**`Tema`:**

- `var contraste by mutableStateOf(false)`, `var fondo by mutableStateOf(Fondo.REJILLA)` y `var cables by mutableStateOf(FormaCables.CURVOS)`.
- `val fuerte get() = contraste`: así `Tarjeta.kt`, `MapaCarrera.opacidad` y `TuCarrera.kt:343` siguen igual.

**`Tokens` (18 valores):**

```kotlin
internal class Tokens(hex: String) {
    private val v = hex.split(' ').map { Color(0xFF000000 or it.toLong(16)) }
    val lienzo = v[0]; val panel = v[1]; val panelSuave = v[2]; val borde = v[3]; val tinta = v[4]
    val suave = v[5]; val tenue = v[6]; val apagada = v[7]; val azul = v[8]; val azulClaro = v[9]
    val verde = v[10]; val verdeClaro = v[11]; val rojo = v[12]; val aurora = v[13]; val aurora2 = v[14]
    val rejilla = v[15]; val vidrio = v[16]; val sombra = v[17]
}

/** En el orden del enum [Juego] */
private val TABLA = arrayOf(
    Tokens("0B0C0E 121316 16181B 262A2F ECEEF1 B6BBC2 858B94 5D636C 5B8AF5 9DB8FF 2EC27E 96E0BE E5403A 6094FF 2FE8C6 15171A 15171A 000000"), // Oscuro
    Tokens("000000 0A0A0B 0F1012 202328 F2F3F5 BEC2C8 8A9099 5F656E 5B8AF5 9DB8FF 2EC27E 96E0BE EA4A43 6094FF 2FE8C6 0F1012 0C0D0F 000000"), // Negro
    Tokens("27292C 303236 36393D 494C52 F1F2F4 CDD0D5 A9AEB5 8B9098 86A9FA B8CCFF 52D197 A8E7CB F47E76 C9D2DE 7FA5FF 3A3D42 34373B 121315"), // Grafito
    Tokens("0A1020 10192D 151F37 24304B E9EEF8 B4BFD3 8794AE 5F6C88 7DA4FF AEC6FF 3FCF8E A0E4C6 F26A63 3A78FF 8C6BFF 141E36 121B30 02050C"), // Medianoche
    Tokens("100D15 17131E 1C1825 2D2738 EFEBF4 BEB6CA 9088A1 6B6380 6F96FF A6BEFF 37C886 9DE2C2 EE5F58 A57BFF F27BB5 2A2433 1A1622 040206"), // Ciruela
    Tokens("06110F 0C1917 10201E 1E3330 E5F0EE ADC2BE 7D9691 556B67 6C95FF A4BEFF 35CC85 9BE3C1 EE5E57 2BD0BC 3FA8F2 102320 0F1C1A 010605"), // Morichal
    Tokens("F3F4F6 FFFFFF F7F8F9 DADDE1 171A20 393C42 5B5F66 72767D 3A66DC 2F57C9 0F8A5F 0B7350 CC1F24 2751CC 0A9781 E3E5E8 FFFFFF 171A20"), // Claro
    Tokens("EEEFEF F8F9F9 F2F3F3 D3D6D9 141A2A 343B4D 515A6B 6A7282 2F5DC4 284FA8 1C7A4F 166541 B3302A 2F5DC4 6A7282 C3C8CF F8F9F9 141A2A"), // Papel
    Tokens("E6EBF1 F5F7FA EDF1F5 CED6E0 0E1626 323F58 4C5973 66728B 2E5ED0 254FB4 0E7F57 0A6B49 C0242A 3B6EE0 7FA6FF D3DBE5 F5F7FA 0E1626"), // Niebla
    Tokens("D8DBDF E8EAED E1E4E7 BFC4CA 0D1015 2A2F36 434952 5B616A 2A55BF 22479F 0A6B45 07593A A81E22 FFFFFF B4C2D6 C8CCD1 EDEFF1 0D1015"), // Aluminio
    Tokens("F4EDF0 FDF9FA F8F2F4 E2D5DA 1E1419 45343F 65525E 7E6A76 3A62D6 2D51B8 0D7F55 0A6A47 C21F2F D9568F 8E6FE8 DCCDD4 FDF9FA 1E1419"), // Cuarzo
)

/** Lo mismo con Más contraste: textos, bordes y rejilla hacia la tinta. Calculado aquí, no en cada cuadro */
private val TABLA_CONTRASTE = arrayOf(
    Tokens("0B0C0E 121316 16181B 65686C F7F8F9 D6DADE B3B7BC 8B9096 5B8AF5 9DB8FF 2EC27E 96E0BE E5403A 6094FF 2FE8C6 232427 15171A 000000"), // Oscuro
    Tokens("000000 0A0A0B 0F1012 616467 FAFAFB DCDEE2 B7BAC0 8E9298 5B8AF5 9DB8FF 2EC27E 96E0BE EA4A43 6094FF 2FE8C6 1D1E20 0C0D0F 000000"), // Negro
    Tokens("27292C 303236 36393D 7E8085 F9FAFB E3E5E8 C9CCD1 ACB0B6 86A9FA B8CCFF 52D197 A8E7CB F47E76 C9D2DE 7FA5FF 45484D 34373B 121315"), // Grafito
    Tokens("0A1020 10192D 151F37 636C80 F6F8FC D5DCE8 B3BCCD 8C96AB 7DA4FF AEC6FF 3FCF8E A0E4C6 F26A63 3A78FF 8C6BFF 222B42 121B30 02050C"), // Medianoche
    Tokens("100D15 17131E 1C1825 6A6572 F9F7FB DCD6E2 BAB4C5 968FA5 6F96FF A6BEFF 37C886 9DE2C2 EE5F58 A57BFF F27BB5 36313F 1A1622 040206"), // Ciruela
    Tokens("06110F 0C1917 10201E 5E6E6C F5F9F8 D1DEDB ADBEBA 859692 6C95FF A4BEFF 35CC85 9BE3C1 EE5E57 2BD0BC 3FA8F2 1E302D 0F1C1A 010605"), // Morichal
    Tokens("F3F4F6 FFFFFF F7F8F9 9B9EA1 090A0D 212328 3A3D42 52565B 3A66DC 2F57C9 0F8A5F 0B7350 CC1F24 2751CC 0A9781 D6D8DB FFFFFF 171A20"), // Claro
    Tokens("EEEFEF F8F9F9 F2F3F3 96999D 080A11 1E222F 343A47 4D5360 2F5DC4 284FA8 1C7A4F 166541 B3302A 2F5DC4 6A7282 B8BDC4 F8F9F9 141A2A"), // Papel
    Tokens("E6EBF1 F5F7FA EDF1F5 9298A1 06090F 1C2434 30394B 495266 2E5ED0 254FB4 0E7F57 0A6B49 C0242A 3B6EE0 7FA6FF C7CED8 F5F7FA 0E1626"), // Niebla
    Tokens("D8DBDF E8EAED E1E4E7 878B90 050608 181A1F 2A2E34 41464D 2A55BF 22479F 0A6B45 07593A A81E22 FFFFFF B4C2D6 BCC0C5 EDEFF1 0D1015"), // Aluminio
    Tokens("F4EDF0 FDF9FA F8F2F4 A2989C 0C080A 281E24 41343C 5C4D56 3A62D6 2D51B8 0D7F55 0A6A47 C21F2F D9568F 8E6FE8 D0C1C8 FDF9FA 1E1419"), // Cuarzo
)
```

**`Paleta`:**

- `private val t get() = (if (Tema.contraste) TABLA_CONTRASTE else TABLA)[Tema.juego.ordinal]`.
- `val turquesa` pasa a `val aurora2`; su único uso es `Comunes.kt:85`.

**`Acento`:**

```kotlin
    NEUTRO("neutro", "Neutro", 0, 0),
    LAGUNA("laguna", "Laguna", 0xFF45C4C8, 0xFF0A6A70),
    LILA("lila", "Lila", 0xFFA99CF6, 0xFF5644C2),
    ORQUIDEA("orquidea", "Orquídea", 0xFFD98BE3, 0xFF8E3A9C),
    ROSA("rosa", "Rosa", 0xFFEE93B0, 0xFF9C3860),
```

### `plataforma/Plataforma.kt`

- **`Apariencia`:** los once juegos más SISTEMA. Al leer, `"exterior"` da CLARO y además guarda `contraste = true` (la migración va una sola vez, en `Preferencias`).
- **`juego(noche)`:** para SISTEMA devuelve `temaNoche` o `temaDia`.
- **Preferencias nuevas:**
  - `fondoMapa: StateFlow<Fondo?>`: null es el del tema, y se vuelve a null al cambiar de tema;
  - `cables: StateFlow<FormaCables>`;
  - `contraste: StateFlow<Boolean>`;
  - `temaDia` y `temaNoche: StateFlow<Juego>`.
- **Cada una con su `guardar…`:** las pone `App` en `Tema` como las demás.

### `componentes/Iconos.kt`

`Lucide.muestrario` y `Lucide.recorrido` (§1).

### `componentes/SelectorTema.kt`

- **`TEMAS`:** Sistema, los seis oscuros, un hilo (`Box(1.dp × 64.dp)` en `Paleta.borde`) y los cinco claros.
- **`MiniTema`:** pinta la aurora de cada juego (dos `radialGradient`) y su fondo con poca fuerza.
- **`VistaPrevia`** vive aquí, junto a `MiniTema`, con las mismas medidas en dp.

### `portada/Personalizar.kt`

- **Bloques:** `BloqueTema` (con la fila «De día / De noche» si es Sistema), `BloqueAcento`, `BloqueMapa` (baldosas de fondo y de cables, y el interruptor de códigos), `BloqueLectura` (texto y más contraste) y `BloqueMovimiento` (luz, reducir movimiento y modo ligero).
- **`Baldosa`:** composable nuevo de 76 de alto, con el dibujo en un `Canvas` de 64 × 30.
- **Acostado:** `Row(330 | hilo | weight(1f) con verticalScroll)`.
- **`resumenPersonalizar()`:** añade «· más contraste» si está encendido.

### `portada/Ajustes.kt`

- **`FilaPersonalizar`:** `Renglon`, con su tecla y el muestrario, en vez de `MiniVistaTema` (que se borra).
- **`FilaCarrera`:** la tecla con el recorrido en el color de la carrera.
- **La vista previa:** la cabecera de la página PERSONALIZAR la pinta bajo el título.

### `mapa/MapaCarrera.kt`

- **`rejilla()` pasa a `fondo()`:**
  - rejilla, como hoy;
  - puntos, con `drawPoints` (la lista se guarda por tamaño y el color es `rejilla` un 10 % hacia la tinta);
  - liso, nada.
- **El color de cada cable:** `lerp(Paleta.tinta, colorArea(area), Tema.juego.cables)`.
- **Con `AL_TOCAR`:** solo se dibujan las aristas de la tarjeta señalada (las de `Relaciones` que ya se encienden al tocar) y la descarga de luz al aprobar.

### `mapa/Geometria.kt`

- **`trazar(…, rectos)`:** cambia cada `Segmento.Curva` por un codo octilineal: horizontal hasta `xm − d`, 45° hasta `y1 ± d`, vertical, 45° y horizontal. `d = min(16, |dy| / 2, (x2 − x1) / 2 − 4)`.
- **El canal vertical:** `xm` se corre ±6 dp por puerto para que los cables de una misma tarjeta no se monten en el mismo canal.
- **El layout:** se calcula con `remember(…, Tema.cables == RECTOS)`.

### Lo que no cambia

- `aspecto()`, `colorEstado`, `iconoEstado` y los estados;
- `HojaInferior` y Haze;
- el modo ligero (el cristal sigue pasando a tinte liso);
- la aurora viva.

## 7. Verificación

- **Contrastes:** calculados con la fórmula WCAG sobre los hex de las tablas, con el mismo script que dibuja la maqueta.
- **La maqueta:** renderizada con Edge sin interfaz (1700 de ancho) y revisada en el PNG.
- **Al implementar:** añadir los cinco juegos nuevos y Más contraste a las pruebas `Captura*Tema*`. Revisar en cada uno el mapa, la lista, el horario, las hojas y la bienvenida antes del APK.

## 8. Lo que Sam pidió al aprobar (7 de noviembre)

Sam aprobó lo recomendado con tres cambios. **Esta sección manda sobre las anteriores** donde choquen.

### 8.1 Tu carrera · Universidad (sustituye a Recorrido)

Pidió «un logo más acorde, más académico». El birrete sigue siendo del grado (anillo, «¿Cuándo me gradúo?», bienvenida) y el libro es «cursando», así que el icono de Tu carrera es la **fachada de columnas** (`landmark` de Lucide): frontón, cuatro columnas y escalón. A 20 dp se lee «universidad» sin texto.

```svg
<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
  <path d="M10 18v-7"/><path d="M14 18v-7"/><path d="M18 18v-7"/><path d="M6 18v-7"/>
  <path d="M11.12 2.198a2 2 0 0 1 1.76.006l7.866 3.847c.476.233.31.949-.22.949H3.474c-.53 0-.695-.716-.22-.949z"/>
  <path d="M3 22h18"/>
</svg>
```

```kotlin
    /** Tu carrera: la fachada de columnas (landmark de Lucide). El birrete es del grado */
    val universidad = listOf(trazo("M10 18v-7"), trazo("M14 18v-7"), trazo("M18 18v-7"), trazo("M6 18v-7"), trazo("M11.12 2.198a2 2 0 0 1 1.76.006l7.866 3.847c.476.233.31.949-.22.949H3.474c-.53 0-.695-.716-.22-.949z"), trazo("M3 22h18"))
```

Va en los mismos tres sitios que §1 daba a `recorrido` (tecla de `FilaCarrera` teñida con el color de la carrera, tecla neutra sin carrera, y calado en el disco de `CirculoAcento(null)`). `recorrido` no se añade.

### 8.2 Exterior vuelve, en claro y en oscuro

Sam quiere «un modo exterior pero en versión oscura, que se vea moderno, bonito, clean». Más contraste (§2) se queda como interruptor para los once temas, y además hay **dos temas para el sol**, en su propio grupo al final del carrusel (tras otro hilo):

| Tema | Carácter | Fondo | Cables | Luz | Pareja |
|---|---|---|---|---|---|
| **Exterior oscuro** | Negro puro y blanco puro, bordes que se ven a pleno sol, colores de estado subidos de luz. Sin aurora, sin cristal. | liso | 1,0 | no | Exterior |
| **Exterior** | El de hoy: blanco puro y tinta casi negra. | rejilla | 1,0 | no | Exterior oscuro |

- **`Juego` gana `val exterior: Boolean`** (true en estos dos). Con `exterior`, `Tema.fuerte` es siempre true (bordes 1,5, cables ×1,5, lo inscribible con borde azul) y las superficies de cristal van en tinte liso, como en modo ligero: a pleno sol el desenfoque solo resta, y en gama baja ahorra.
- **En Personalizar,** con un Exterior elegido, «Más contraste» se ve encendido y desactivado, con la nota «Exterior ya lo trae».
- **`TABLA_CONTRASTE`** repite para ellos la fila de `TABLA` (ya son el máximo).
- **Migración:** quien tenía `"exterior"` guardado queda en **Exterior** (ya no pasa a Claro + contraste, como decía §6).

Tokens (orden de `Tokens`, 18 valores):

```
Exterior oscuro: 000000 0E0F11 16181B 8A8F96 FFFFFF E8EAED C9CDD3 A3A8AF 84ADFF B8CFFF 45E096 A8F0CC FF7A72 000000 000000 26292D 0E0F11 000000
Exterior:        FFFFFF FFFFFF F0F1F3 84898F 050608 1C1F23 34383E 52565C 1A47C2 133A9F 00603F 00573A A80F1A 1A47C2 006B5C E2E4E7 FFFFFF 000000
```

Contraste de Exterior oscuro (lienzo / panel): tinta 21,0 / 19,2 · suave 17,4 / 15,9 · tenue 13,2 / 12,0 · apagada 8,8 / 8,0 · azul 9,4 / 8,6 · rojo 8,3 / 7,6 · verde 11,3 · borde 6,5. Todo AAA, incluso los iconos.

Carrusel final: Sistema · seis oscuros · hilo · cinco claros · hilo · Exterior oscuro, Exterior. **Trece temas.**

### 8.3 Más cosas que personalizar

Sam pidió «seguir agregando más cosas». Entran las que no cuestan cuadros en gama baja ni multiplican los estados de la tarjeta:

| Bloque | Opción | Valores | Por defecto | Coste |
|---|---|---|---|---|
| MAPA | **Grosor de los cables** | Fino · Normal · Grueso (×0,7 · ×1 · ×1,5 del trazo actual) | Normal | Ninguno: un factor en el `Stroke` que ya se crea. Se multiplica con el ×1,5 de `fuerte`. |
| MAPA | **Lo aprobado** | Normal · Atenuado | Normal | Ninguno: con Atenuado, las tarjetas aprobadas y sus cables se dibujan al 45 % de opacidad, en el mismo `draw`. Así el ojo va a lo que falta. |
| INICIO (bloque nuevo) | **Al abrir la app** | Portada · Mi carrera | Portada | Ninguno. Con «Mi carrera» y una carrera elegida, la app arranca dentro de ella; Atrás vuelve a la portada. Sin carrera, siempre portada. |
| INICIO | **Mi carrera se abre en** | Mapa · Lista | Mapa | Ninguno: la vista inicial de la carrera. |
| HORARIO (bloque nuevo) | **Formato de hora** | 12 h · 24 h | 12 h | Ninguno: solo el texto de la hora. En 24 h no hay meridiano y «13:00» cabe en el carril igual que «10:00». |
| MOVIMIENTO Y TACTO | **Vibración** | interruptor | encendida | Ninguno: con ella apagada, los `performHapticFeedback` de lista, barra, mapa y ficha no se llaman. |

Las de MAPA usan las mismas baldosas (§5) que Fondo y Cables. Las de INICIO y HORARIO, el segmentado de «Aa Normal / Aa Grande».

**Orden de los bloques:** TEMA · ACENTO · MAPA · INICIO · HORARIO · LECTURA · MOVIMIENTO Y TACTO.

**Preferencias nuevas** (además de las de §6): `grosorCables: StateFlow<Grosor>` (enum FINO, NORMAL, GRUESO), `atenuarAprobadas: StateFlow<Boolean>`, `abrirEnCarrera: StateFlow<Boolean>`, `vistaInicial: StateFlow<Vista>` (MAPA, LISTA; reutiliza el enum de vista si ya existe), `hora24: StateFlow<Boolean>`, `vibracion: StateFlow<Boolean>`. Cada una con su `guardar…`, y `App` las pone en `Tema` (o en un `CompositionLocal` de preferencias de uso) como las demás.

**Siguen descartadas** las de la tabla de §4, por los mismos motivos.
