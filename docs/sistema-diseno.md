# Sistema de diseño: tokens y reglas responsive

La app se hizo a medida de 412 dp. Esta guía es para migrar las pantallas a tokens y que aguanten 320-360 dp y letra grande.

## Qué token usar (`tema/Medidas.kt`)
- **`Espacio`** (rellenos, `spacedBy`, `Spacer`): `hilo` 2, `xxs` 4, `xs` 8, `s` 12, `m` 16, `l` 20, `xl` 24, `xxl` 32. Los 6 y 10 sueltos van al vecino que mejor respire (6→`xxs`/`xs`, 10→`xs`/`s`).
- **`Radio`**: `Chip` 12, `Campo` 16, `Tarjeta` 18, `Opcion` 20, `Bloque` 22, `Grande` 24, `Barra` 26, `Hoja` 28. Los 8 y 2 de pastillas y barritas se quedan locales.
- **`Alto`** (siempre con `heightIn(min = ...)`): `chip` 36, `icono` 40, `toque` 44 (mínimo de cualquier pulsable), `fila` 48, `boton` 52, `grande` 56.
- **`Tipo`** (tamaño de letra; la familia la pone el estilo): `xxs` 11, `xs` 12, `s` 13, `m` 14, `l` 15, `xl` 17, `xxl` 22, `titulo` 24, `cifra` 32, `gigante` 40. Los 12,5 / 9,5 / 8,5 sp se quedan como constante local.
- **Ventana** (`componentes/Ventana.kt`): `ventana()` devuelve `Ventana(ancho, alto, escalaLetra)` con `anchura` (`Compacta` < 360, `Normal`, `Amplia` ≥ 600), `apaisado`, `compacta`, `letraGrande` (≥ 1,3), `margen` y `columnas(minimo, maximo)`. `margenPantalla()` da 16/20/24. App la provee una vez en `LocalVentana`; `esApaisado()` sigue siendo válido (es fachada).

## Reglas responsive
1. Nada de anchos fijos de pantalla ni de bloques: `fillMaxWidth()` + `widthIn(max = ...)`, nunca `width(300.dp)`. Los anchos fijos solo para iconos y pastillas.
2. Altos con `heightIn(min = Alto.x)`, no `height()`: con letra grande la fila debe poder crecer.
3. Todo texto variable (nombres de materia, subtítulos) lleva `maxLines` + `TextOverflow.Ellipsis`, o `autoSize` (`TextAutoSize.StepBased(minFontSize = Tipo.xs)`) para cifras y titulares, o se deja saltar de línea. Nunca un texto en una `Row` sin `Modifier.weight(1f)`.
4. Filas que no caben (chips de estado, cifra + etiqueta): `FlowRow`, o pasar a dos líneas cuando `ventana().compacta || ventana().letraGrande`.
5. Rejillas: `ventana().columnas(minimo = 150.dp)` en lugar de un número fijo de columnas.
6. Respetar `fontScale`: nunca convertir sp a dp a mano para medir texto; si hay que medir, `rememberTextMeasurer`.
7. Márgenes laterales de pantalla y hoja: `margenPantalla()`, no `20.dp`.

## Migrar sin cambiar el aspecto en 412 dp
- Sustituir solo cuando el token valga lo mismo (o ±2 dp / ±1 sp). En 412 dp `margenPantalla()` es 20, igual que antes.
- Lo adaptativo va detrás de `if (ventana().compacta)` o `letraGrande`: la rama normal debe quedar idéntica.
- Verificar con `CapturaResponsiveTest` (320, 360, 412 con letra 1,3) y comparar la de 412 con las capturas que ya había.

## Qué NO tocar
- Los dp de dibujo dentro de `Canvas`/`drawBehind`/`drawWithCache`: geometría del anillo de graduación, del mapa (tarjetas, cables, cámara) y de los iconos Phosphor. Se quedan como constantes locales con nombre (`private val GROSOR_ARO = 1.5.dp`), no pasan a `Espacio`.
- `ALTO_BARRA`, `ANCHO_PESTANA` y demás constantes con nombre que ya existen: solo se renombran si cambian de sitio.

## Fallos vistos en la matriz (build/capturas/resp-*.png), trabajo de los migradores
- **Avance 320:** el «%» del 41 se monta sobre «TE GRADÚAS / Dic 2029»; el título «Ingeniería de Sistemas» salta a dos líneas y empuja todo; «Ene – Jun 2027 · 6 m…» cortado.
- **Avance 412+1,3:** «Sociohumanísticas 0 / 6 UC» sin aire entre nombre y cifra.
- **Ficha 320 y 360:** la fila Aprobada/Cursando/Sin cursar no cabe: «Cursando» toca la pastilla «Sin cursar». Pide `FlowRow` o reparto con `weight`.
- **Lista 320 y 412+1,3:** nombres largos cortados con «…» («Inferencia y Diseño de Exper…»); preferible dos líneas.
- **Mapa 320:** la segunda columna queda cortada a la mitad en el borde derecho y la cabecera del semestre se aprieta contra el «0%»; la cámara inicial debería encuadrar una columna entera.
- **Graduación 412+1,3:** «Tu ritmo / materias por semestre» se parte en tres líneas y choca con «Lo más rápido 6»; los rótulos del anillo («23 aprobadas», «34 por aprobar») rozan el borde en 320.
- **Horario semana 320:** las clases solo caben abreviadas en 3 renglones («Infer. Dis. Exp.») y el aula queda al límite; la columna de horas come ancho útil.
- **Ajustes 320:** subtítulos cortados («Oscuro · tu carrera · text…», «Cómo funciona, en cuatr…»); con letra grande pasa lo mismo en 412.
- **Portada:** aguanta en los tres; en 320 el bloque «Tu carrera» queda justo, sin margen a la derecha del «Continuar».
