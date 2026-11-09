# Inicio · «Ordenada» premium

Maqueta: `docs/maquetas/inicio-premium.html` (PNG al lado). Parte de la propuesta C de `inicio-nuevo.md`, que fue la elegida, y la lleva a un acabado premium sin perder fluidez en gama baja.

Referencias: OKX y Binance (filas de mercado y cifras que ruedan), Apple (resortes, muelle flotante, vidrio), Tesla (cifra fina y grande sobre negro OLED), Steam y Epic (el fondo toma el color de lo que eliges).

## Qué cambia frente a la «Ordenada»

1. **Iconos nuevos de Phosphor**: en duotono dentro de una tecla de cristal teñido (el color de la carrera al 10–26 % y un filo del mismo color), no en un bloque de color sólido. La tuya va rellena y con brillo.
2. **Filas de mercado**: nombre, «58 materias · 150 UC» y, a la derecha, una **mini silueta** del pensum (10 barras, una por semestre, con `silueta` de `indice.json`) en el color de la carrera.
3. **Selector** Todas · Ingenierías · Licenciaturas, con una pastilla que se desliza.
4. **Tu carrera**: la cifra a 56 sp (Inter 200), los UC y el semestre a la derecha, el avance en **10 segmentos** (uno por semestre) y tres datos debajo: por inscribir, la fecha y cursando.
5. **Muelle flotante** de vidrio vivo: Inicio, Mapa, Por hacer y Horario.
6. **Aurora y grano**: tres gradientes radiales del color de tu carrera y una textura de ruido al 5–7 %.

## Iconos (Phosphor, MIT)

| Carrera | Icono | Antes |
|---|---|---|
| Sistemas | `cpu` (chip) | monitor con consola |
| Administración | `presentation-chart` (pizarra con gráfica) | organigrama |
| Petróleo | `drop-half-bottom` (gota a medias) | balancín |
| Agronómica | `grains` (espiga) | espiga de Lucide |
| Ambiental | `tree` (árbol) | hoja |
| Producción Animal | `cow` (res) | cabeza de res propia |
| Contaduría | `calculator` | calculadora de Lucide |
| Tecnología de Alimentos | `flask` (matraz) | matraz de Lucide |
| Recursos Humanos | `users-three` (personas) | apretón de manos |

- **Tres pesos por icono:** duotono en reposo, relleno para la tuya o la activa, y ligero en texto y filas pequeñas. Es como SF Symbols.
- **Cómo entran en la app:** se copian solo los trazos usados a `componentes/Iconos.kt`. Phosphor usa una rejilla de 256 y se dibuja con relleno, no con trazo, así que hace falta un `iconoRelleno(paths, alfaDuo)`: la capa marcada con `opacity=".2"` va al 28 %. No se añade ninguna librería.
- **Descartadas:**
  - Solar (CC BY 4.0, pide atribución; le faltan vaca y trigo);
  - Hugeicons (no tiene res);
  - MingCute (Apache 2.0 con la condición «no venderlos»).

## Tecnologías

| Pieza | Veredicto | Nota |
|---|---|---|
| Haze 1.6 (ya está) | sí | Desenfoque vivo solo en lo que flota. La 1.7 pide Compose 1.11 |
| Cristal pintado | sí | Gradiente blanco al 3–9 %, luz arriba, filo especular de 1 dp a 150° y brillo radial del color |
| Aurora | sí | Tres `Brush.radialGradient` en `drawWithCache`. Al cambiar de carrera, `animateColorAsState` |
| Grano | sí | PNG de 128 px en mosaico con `ImageShader(TileMode.Repeated)` |
| Odómetro | sí | `AnimatedContent` por dígito, con cifras tabulares |
| Resortes y háptica | sí | Al pulsar, escala 0,97 con `spring(0.8f, 380f)`; `LocalHapticFeedback` |
| Elemento compartido | sí | `SharedTransitionLayout`: la tecla viaja de la fila a la cabecera |
| Liquid Glass (Kyant Backdrop 2.0.1, Apache 2.0) | más adelante | Pide Compose 1.11 (la app está en 1.10.3) y RuntimeShader (Android 13+). Para el muelle y el buscador, con Haze como respaldo |
| Shaders AGSL animados | no | Coste continuo y no existen por debajo de Android 13 |
| Lottie o Rive | no | Dependencia pesada para un adorno |

## Presupuesto de rendimiento

- **Una sola capa de desenfoque vivo por pantalla:** el muelle o la barra de arriba, nunca las dos a la vez.
- **Sin desenfoque en la lista ni en la tarjeta.**
- **Solo se anima `graphicsLayer`:** translación, escala y opacidad. Los valores animados se leen en lambdas.
- **La aurora y el grano se pintan una vez.**
- **El modo ligero** (`medirModesto`) quita el desenfoque, el grano y el odómetro.
- **Meta:** arranque frío por debajo de 600 ms y ningún cuadro de más de 16 ms al deslizar, medido con Macrobenchmark.
