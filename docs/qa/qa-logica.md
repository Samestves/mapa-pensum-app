# QA de lógica, datos, persistencia y rendimiento

Solo lectura. Alcance: `logica/`, `datos/`, `plataforma/`, `androidMain/`, `EstadoCarrera`, `EstadoHorario`, `App.kt`, los `remember` pesados de `carrera/` y los JSON.

Cómo se verificó:
- **Verificado (JSON)**: script `integridad.py` sobre los JSON reales.
- **Verificado (port)**: `sim.py`, port a Python de `estimarGraduacion`, `pendientesDe`, `cargaHabitual` y `Simulador.margen`, ejecutado con datos reales. Es un port mío: la lógica es la misma, pero no es el Kotlin ejecutado.
- **Lectura**: razonado leyendo el código.
- **Sospecha**: no demostrado.

Los scripts están en `C:\Users\Sam\AppData\Local\Temp\claude\5640b6dbb13df89b\2f7971bd-9e3a-4f69-82cd-166cc13cdcbc\scratchpad\` (`integridad.py`, `sim.py`, `t_amb.py`, `t_prox.py`).

**Resumen:** no hay cierres demostrables en el alcance. Todos los `!!` y `getValue` están protegidos por invariantes que se cumplen en los 9 JSON. Hay dos errores de fecha o estado a la vista (C1 y A1) y un problema de pérdida de trabajo (A2).

---

## Crítico

### C1. Ingeniería Ambiental: elegir electivas en la franja no descuenta las casillas de los semestres (verificado, port)
- **Dónde**: `logica/Graduacion.kt:184-228` (`pendientesDe`, rama sin cuota) y `mapa/FranjaElectivas.kt:51-61` (`adoptarMarcadas`). Los efectos se ven en `Avance.kt:capsulasDe` y en `Semestres.kt:marcasDeSemestres`.
- **Qué pasa**: Ambiental es la única carrera con las dos cosas a la vez: 5 casillas `esHueco` en los semestres 5, 7, 8 y 9, y franja libre (`electivasEnCasillas` es false). `pendientesDe` solo resta de `vacias` las electivas hechas que NO están en `casillas`: `hechasSinCasilla = hechas.count { it.codigo !in puestas.values }`. Una electiva puesta en `libre-tecnica-1` está en `puestas.values`, así que no consume ninguna casilla de semestre. Las 5 genéricas siguen pendientes aunque el estudiante tenga las 5 electivas aprobadas.
- **Cómo se reproduce** (port, Ambiental con todas las obligatorias aprobadas):

  | Caso | Pendientes | Semestres restantes |
  |---|---|---|
  | 3 técnicas + 2 humanísticas aprobadas, marcadas desde la lista o la ficha (sin casilla) | 0 | 0 |
  | Las mismas, puestas en la franja `libre-*` con el selector | 5 | 1 |
  | Las mismas, puestas en casillas de semestre | 0 | 0 |
  | Sin nada elegido, y luego elegir UNA en la franja (sin aprobarla) | 5 → 6 | |

  Elegir en la franja sube lo pendiente en vez de bajarlo.
- **Efecto a la vista**: la fecha de graduación suma un período de más y "terminado" no llega nunca. El anillo, `materiasRestantes` y las cápsulas siguen mostrando electivas pendientes.
- **Incoherencia del mapa en la misma carrera**: `adoptarMarcadas` solo coloca las electivas marcadas en casillas `libre-*` y nunca en las del semestre. Con las 5 electivas aprobadas desde la lista, las casillas de los semestres 5, 7, 8 y 9 siguen vacías (`FALTA_ELEGIR`). Pero `pendientesDe` sí cuenta esas electivas como hechas y da 0 pendientes. En `semestreActual(semestres)` el semestre 5 nunca queda completo, así que la barra de cápsulas se queda en "semestre 5" mientras no cursas nada.
- **Arreglo propuesto**: que el cálculo y el dibujo usen una sola regla. Elegir una: (a) para grupos sin cuota con huecos en semestres, cada electiva del grupo, hecha o elegida, consume una casilla de semestre, esté donde esté. En `pendientesDe`: `vacias.drop(hechas.size + elegidas.size - enHuecos)`, o más simple, genéricas = `max(0, huecosDelGrupo - hechas.size - elegidas.size)`, y las `elegidas` pendientes se cuentan solo como pendientes propios; (b) hacer que `adoptarMarcadas` rellene primero los huecos de semestre del grupo y solo después las libres; (c) ocultar la franja en las carreras que tienen casillas de semestre. La opción (c) es la más limpia: una sola vía de elegir. Añadir un test con Ambiental.
- **Esfuerzo**: M. Verificado.

---

## Alto

### A1. `estadosDe` ignora `requisitoEspecial` ("120 UC aprobadas"): el Proyecto de Trabajo de Grado sale inscribible desde el día 1 (verificado, JSON + lectura)
- **Dónde**: `logica/Estados.kt:estadosDe`. El dato está en `ingenieria-ambiental.json`, materia `0895593`, semestre 9, `requisitoEspecial: "120 UC aprobadas"` y sin prelaciones. `Graduacion.kt:149-158,325` sí lo respeta con `ucMinimas`.
- **Qué pasa**: un estudiante de Ambiental sin marcas ve esa materia como `DISPONIBLE` / `INSCRIBIBLE`. Sale en el mapa como "Puedes inscribirla", suma a "N por inscribir" de la portada (`progreso.disponibles`), aparece en "TAMBIÉN PUEDES" del próximo semestre (`inscribiblesDelProximo`) y entra en `seAbrenAlAprobar`. Pero el plan de graduación la deja para cuando se llegue a 120 UC. Dos verdades distintas sobre la misma materia.
- **Arreglo**: parsear `ucMinimas` una vez, como propiedad de `Asignatura` (por ejemplo `val ucMinimas: Int? by lazy` o resuelta en `leerPensum`). Dar a `estadosDe` las UC aprobadas: `val uc = todas.sumOf { if (marcas[it.codigo] == APROBADA) it.uc ?: 0 else 0 }`, y exigir `a.ucMinimas == null || uc >= a.ucMinimas` para `DISPONIBLE`. Reusar ese valor en `Graduacion.pendienteDe` y quitar la `Regex UC_MINIMAS` de ahí. Ojo: la simulación cuenta los cursando como hechos (`ucHechas`), y `estadosDe` solo aprobados; está bien, pero hay que decirlo en un comentario.
- **Esfuerzo**: S-M. Verificado.

### A2. Girar el teléfono destruye `EstadoHorario`: se pierde una lectura de captura ya gastada (verificado, lectura)
- **Dónde**: `carrera/Interior.kt:92` (`remember(pensum, horizontal) { EstadoCarrera(...) }`) y `EstadoHorario.kt:~447` (`remember(estado) { EstadoHorario(estado) }`).
- **Qué pasa**: `horizontal` es parte de la clave, así que al rotar nace un `EstadoCarrera` nuevo y, con él, un `EstadoHorario` nuevo. Se pierden `lectura` (foto, `leidas`, fase), `hoja` (el `Borrador` de una clase a medio editar), `dia` y el estado de la `LaunchedEffect` de `PasoLeyendo`. `configChanges` evita recrear la actividad, pero Compose igual descarta el estado.
- **Reproducción**: Intradace → "Subir mi horario" → elegir foto → leer; con las materias leídas en pantalla ("Antes de guardar"), rotar. Vuelve el horario vacío y hay que leer otra vez, gastando otra de las 20 lecturas diarias de Gemini. Si se rota durante "Leyendo", la petición HTTP bloqueante sigue viva en IO pero su resultado se descarta.
- **Efecto secundario**: cada rotación recalcula `calcularLayout` y `generarAristas` en el hilo principal.
- **Arreglo**: sacar `Lectura`, `hoja` y `dia` de `EstadoHorario` a un holder `rememberSaveable` o a uno en `Interior` clavado solo a `slug`. Para `EstadoCarrera`, mantener las marcas fuera de la clave. Lo más limpio: `EstadoCarrera` recibe `compacto` como parámetro mutable y recalcula `layout` con `derivedStateOf`, sin recrearse. `EstadoHorario` va con `remember(slug)`. Contenerlo en `rememberSaveable` solo si se quiere sobrevivir a la muerte del proceso (no hace falta).
- **Esfuerzo**: M. Verificado por lectura (no probado en dispositivo).

### A3. La carga "habitual" se desploma con pocas aprobadas y la fecha se va a 2050 (verificado, port)
- **Dónde**: `logica/Graduacion.kt:241-249` (`cargaHabitual`): `ritmo = max(1, round(aprobadas / semestreMásAltoAprobado))`.
- **Reproducción** (Sistemas, port): una sola materia de semestre 5 aprobada → carga 1 (`RITMO`) → 56 períodos → graduación en 2054. Semestre 1 completo (6 materias) + una de semestre 5 → carga 1, 50 períodos → 2051. Con 2 cursando del semestre 9 y nada aprobado → carga 6 (`PENSUM`), 11 períodos: ahí sí va bien.
- **Por qué importa**: un estudiante que marca solo lo que recuerda (o que convalida una materia alta) ve "Te gradúas 2054" en la portada y en la hoja. Es el caso típico del primer uso.
- **Arreglo**: dividir por semestres realmente recorridos y poner un suelo. Por ejemplo, `ritmo = max(redondear(aprobadas / recorridos), pensumMediana / 2)`, o caer a `PENSUM` si `aprobadas < recorridos`. Documentar el criterio y añadir test "1 aprobada de semestre alto".
- **Esfuerzo**: S. Verificado.

### A4. Cuatro definiciones de "semestre actual" en pantalla (verificado, lectura)
| Dónde | Regla |
|---|---|
| Portada: `Avance.kt:120` `ResumenPortada.semestreActual(silueta)` | primer semestre con menos aprobadas que su silueta; ignora cursando y casillas |
| Barra de cápsulas: `Recomendacion.kt:316` `semestreActual(semestres)` | el último donde cursas algo; si no, el primero incompleto con casillas |
| Lista: `Lista.kt:153` | primer semestre con alguna obligatoria no APROBADA (cursando cuenta como no aprobada) |
| Mapa: `MapaCarrera.kt:semestreFrente` | el menor semestre con INSCRIBIBLE o CURSANDO |

- **Reproducción**: Sistemas con el semestre 1 aprobado y cursando 3 materias del semestre 3. Portada: semestre 2. Barra: semestre 3. Lista: semestre 2. Mapa: semestre 2 si hay inscribibles allí. Ambiental con electivas aprobadas desde la lista: la barra y la lista se quedan en el semestre 5 (ver C1).
- **Arreglo**: una sola función `semestreActual(...)` en `logica/Semestres.kt` que reciba `CuentaSemestre` (ya existe `marcasDeSemestres`) y que todas usen. Decidir antes cuál es la regla (la de la barra, "el último donde cursas", es la más intuitiva). Hay que entregar a la portada un `semestreActual` calculado en `resumenParaPortada` en vez de la silueta.
- **Esfuerzo**: M. Verificado.

---

## Medio

### M1. Importar un horario sobre otro lo duplica sin avisar (verificado, lectura)
- **Dónde**: `LecturaRemota.kt:112` llama `revisar(filas, materias)` sin `yaGuardadas`, aunque `revisar` y `marcarChoques` ya lo admiten. `HojasHorario.kt:441` `Confirmar` solo calcula `choquesDe(todas)` entre lo leído. `EstadoHorario.kt:guardarLectura` hace `sesiones + nuevas` con `idNuevo` (sufijo `-b`).
- **Reproducción**: leer la misma captura dos veces: cada clase queda duplicada (mismo código, día y hora, ids `X-0-420` y `X-0-420-b`) y `choquesDe` las marca como choque en la semana, sin que la hoja de confirmación avise.
- **Arreglo**: en `guardarLectura`, descartar la sesión nueva si ya hay una igual (código, día, inicio, fin) y avisar en `Confirmar` si choca con las ya guardadas (pasar `horario.sesiones` a `choquesDe`). O preguntar "Reemplazar / Sumar".
- **Esfuerzo**: S.

### M2. Siete de nueve carreras: las electivas no entran ni en el avance ni en la fecha (verificado en datos; el criterio es sospecha de dominio)
- **Dónde**: `Graduacion.kt:222-225` y `Avance.kt:83`. Gerencia RR.HH., Agronómica, Petróleo, Producción Animal, Administración, Contaduría y Alimentos tienen `cuota = null`, 0 casillas y `creditos = null`. Resultado: `pendientesDe` no crea pendientes de electivas y `porcentaje` es null (se usa aprobadas/obligatorias).
- **Qué pasa**: la app dice "Te gradúas Jun 2029" y "100 %" con cero electivas aprobadas. Es lo que documenta el código ("no se inventa nada"), pero el título sí las exige en el pensum real (cada carrera trae 16-49 electivas).
- **Sospecha**: no consta cuántas exige cada pensum; los JSON no lo dicen. Conviene un aviso en la tarjeta ("No cuento tus electivas") o añadir `cuota` cuando se conozca.
- **Esfuerzo**: S (aviso) / M (datos).

### M3. `Periodo.actual()` leído dentro de `derivedStateOf`; fecha vieja con la app viva (verificado, lectura)
- **Dónde**: `EstadoCarrera.kt:71`. El valor se cachea mientras no cambie `marcas`, `casillas` ni `carga`. La hoja y los detalles usan su propio `hoy` (`AvanceCarrera.kt:193`, `remember(apertura)`).
- **Efecto**: cruzando 30-jun → 1-jul (o 31-dic → 1-ene) con la app abierta, `estado.graduacion` queda un período atrás de lo que calcula el detalle (anillo, cadena, próximo).
- **Arreglo**: pasar `hoy` como argumento a `graduacion` (un `State<Periodo>` en `EstadoCarrera`) y que todos lean el mismo. Bajo en la práctica, pero es una fuente de incoherencia gratuita.
- **Esfuerzo**: S.

### M4. El mando de carga "elegida" se desliza (verificado, lectura)
- **Dónde**: `DetalleGraduacion.kt:83-84,114` y `Guardado.kt:LIMITES_CARGA`.
- Si el chip pulsado es igual a `habitual`, se guarda `null`. Más tarde, al marcar más cursando, la "habitual" cambia sin que el estudiante haya tocado nada y su carga elegida deja de serlo.
- `habitual` se acota a 1..8 (`coerceIn(1, MAX_CARGA)`) pero `estimarGraduacion` usa la real: quien cursa 10 ve el chip "8" mientras el hero usa 10, y pulsar 8 guarda `null` (la habitual real, 10). `Guardado.LIMITES_CARGA = 1..9` no coincide con `MAX_CARGA = 8`.
- **Arreglo**: guardar siempre lo pulsado y mostrar "habitual" solo como marca visual; un único tope (8 o 9) para ambos.
- **Esfuerzo**: S.

### M5. `capsulasDe` y `pendientesDe` usan dos "UC típicas" distintas (verificado, lectura)
- `ucTipicaDe` (la moda, `Recomendacion.kt:274`) frente a `mediana(...)` (`Graduacion.kt:200`). Hoy coinciden en Sistemas (3 y 2); con otra mezcla de UC darían barras distintas del plan. Ver Redundancia.
- **Esfuerzo**: S.

---

## Bajo

- **B1. Datos viejos se depuran al leer pero el archivo no se reescribe** (`Guardado.kt:marcas/casillas`). Al siguiente `marcar`, `guardarMarcas` escribe el mapa depurado: un código que desaparece del pensum se borra para siempre. Aceptable y razonable. Solo afecta si se vuelve a una versión vieja. Sin acción.
- **B2. Una excepción en un solo JSON mata la portada** (`datos/Carreras.kt:60-67`): `leerPensum` dentro de `mapNotNull` sin `runCatching`. Con los 9 JSON actuales no falla (verificado). Un `runCatching { }.getOrNull()` por carrera lo blinda. S.
- **B3. La portada vuelve a parsear los JSON de las carreras con marcas cada vez que aparece** (`rememberResumenes`, fuera del hilo principal, 8-14 KB cada uno): despreciable, pero se puede cachear por `slug` + hash de marcas crudas.
- **B4. `ABRE = 6:00` y `CIERRA = 19:00`** (`Horario.kt:13-14`): una clase nocturna de la UDO (si la hay) se marca "fuera" al importar y `HorarioGuardado.leer` la descarta en silencio al releer. Sospecha: no consta que existan clases después de las 19:00.
- **B5. `aHora("00:30")` da 12:30** (`ImportarHorario.kt:112`): sin meridiano, una hora antes de ABRE se pasa a la tarde, incluida la 0:xx. Irrelevante en la práctica.
- **B6. `Periodo` fija lapso I = enero-junio y II = julio-diciembre** (`Graduacion.kt:56-62`). Es un supuesto documentado, no el calendario real de la UDO. Hoy (noviembre) la app calcula desde el lapso II. Sospecha: puede correr la fecha un período respecto a la realidad.
- **B7. `fechaSiRepruebas` para una electiva marcada sin casilla** en carreras sin cuota: al quitarle la marca no vuelve a ser pendiente (no hay casilla que la reclame), así que no cuenta en el retraso. Menor.
- **B8. `hoy` distinto**: `Periodo.actual()` se llama en tres sitios (`EstadoCarrera`, `HojaAvance`, `DetalleX` por parámetro). Ver M3.
- **B9. `Preferencias.leerTexto` usa `getString`**: si algún día la misma clave guardara otro tipo, lanza `ClassCastException`. Hoy todas las claves `mapa-pensum:*` son texto (verificado). Sin acción.

---

## Integridad de los JSON (verificado)

Salida resumida de `integridad.py` (`python -I`, sin cambios en el repo). `indice.json` lista **9 carreras** (no 8); los 9 archivos existen y coinciden con el índice.

| Comprobación | Resultado |
|---|---|
| Códigos duplicados (obligatorias + casillas + electivas) | ninguno |
| Prelaciones a códigos inexistentes, a casillas o a sí mismas | ninguna |
| Ciclos | ninguno |
| Prelación a un semestre igual o posterior | ninguna |
| Obligatorias sin `semestre` o fuera de 1..N | ninguna |
| Suma de UC obligatorias frente a `ucObligatorias` y `indice.uc` | coincide en las 9 |
| `semestres[].cantidad`, `.huecos`, `.uc` frente a los datos | coincide |
| `indice`: `semestres`, `asignaturas`, `electivas`, `silueta` | coincide (la silueta cuenta obligatorias, sin huecos) |
| Sistemas: 132 UC obligatorias + cuotas 15 + 6 = 153 = `creditos.titulo` | coincide |
| Cuotas imposibles (UC disponibles < cuota) | ninguna (Sistemas: técnicas 73 UC disponibles para 15; humanísticas 32 para 6) |
| Casillas de Sistemas por grupo | técnica: 5 (sem. 7, 8, 9, 9, 9) × 3 UC = 15; humanística: 3 (sem. 2, 3, 4) × 2 UC = 6 |
| Cadena de prelaciones más larga | coincide con `profundidadMaxima` (4 a 9) |

Observaciones, no errores:
- **7 carreras con `cuota = null`, 0 casillas y `creditos = null`** (todas menos Sistemas y Ambiental): ver M2.
- **Ambiental**: 5 casillas de semestre + franja libre, sin cuota ni créditos (ver C1). El aviso de las 2 claves duplicadas del documento escaneado (`0895683`, `0895193`) ya está separado en los datos.
- **`0214100 Servicio Comunitario`** tiene `uc: 0` y **`9099999 Áreas de Grado`** (comodín) tiene `uc: null`. Ambos son obligatorias y cuentan 1 materia en el avance por materias; con `?: 0` no rompen ningún cálculo (verificado en las rutas de `Avance.kt`, `Graduacion.kt` y `capsulasDe`, que usa `maxOf(1, uc)`).
- **Ambiental** es la única con `requisitoEspecial` (ver A1).
- Textos con codificación correcta en los archivos (la consola de Windows los muestra con `�`; no hay `U+FFFD` reales).

## Casos límite de la estimación (verificado, port)

Resultados de `sim.py` en las 9 carreras:
- **Sin marcas**: carga 4-6 (`PENSUM`), 9-11 períodos; `semestresRestantes >= minimoPorPrelaciones` en todas.
- **Carga 1 y 8**: 45-60 períodos y 6-9 períodos respectivamente; sin errores ni bucles (el tope de 200 períodos no se alcanza).
- **Todo aprobado**: 0 restantes, `periodoFinal = null` (en Ambiental y Sistemas quedan las casillas de electiva sin elegir como pendientes: 5 y 8, por diseño).
- **Todo cursando**: `semestresRestantes = 1`, `periodoFinal = hoy`.
- **Una sola pendiente**: 1 período (2 en Ambiental y Sistemas por las casillas).
- **Cursando con prelaciones sin aprobar**: se dan por hechas al final del período, y sus dependientes se abren en el período siguiente. Coherente con `situacionDe` (PROXIMA).
- No hay `!!`, `.first()` ni índices sin proteger que puedan fallar con estos datos. Revisados: `alturaDesdeSemestre[p.semestre!!]` (solo `marcaNivel`, que implica `semestre != null`), `vacias.getOrNull(min(genericas, vacias.lastIndex))` (`getOrNull(-1)` es seguro), `delGrupo.getValue(codigo)` (filtrado por `it in delGrupo`), `planes[elegida - 1]` (acotado 1..8), `Periodo` `require` (siempre construido con `floorDiv/mod`).

---

## Rendimiento

**Cuántas simulaciones corre cada pantalla** (`estimarGraduacion` completa; contadas con el port, en datos reales):

| Pantalla | Simulaciones |
|---|---|
| Abrir la hoja de avance (`estado.graduacion` + `rememberRapida` = `planesPorCarga`) | 1 + 8 = **9** |
| Volver a la hoja desde un detalle (`ContenidoHoja` se recompone entera y `rememberRapida` no sobrevive) | otra vez **8** |
| `DetalleGraduacion` (`planesPorCarga` + `habitual`) | **9**, idénticas a las 8 de `rememberRapida` (mismas marcas, casillas y `hoy`) |
| `DetalleProximo` (`proximoSemestre`): alumno nuevo | **28-74** (`margen` de cada recomendada y de cada opcional, hasta 9 simulaciones por materia) |
| `DetalleProximo`: alumno con s1-3 aprobados y s4 cursando | **16-54** |
| `DetalleCadena` (`cadenaExplicada`): alumno nuevo | **88-134** (`vigilar` mide el margen de todas las pendientes fuera de la cadena) |
| `DetalleCadena`: alumno intermedio | **38-79** |
| `DetalleEsteSemestre` | 1 por materia que cursas (hasta 8) |

- **Coste de una simulación**: el port en Python tarda unos 0,4 ms. El Kotlin compilado va muy por debajo en un PC; estimo 20-100 µs en JVM caliente, 5-10× más en un teléfono de gama baja (unos 0,2-1 ms, más en frío porque ART interpreta antes de compilar). Con eso, `DetalleCadena` en el peor caso cuesta aproximadamente **25-130 ms** en el hilo principal justo cuando empieza la transición de 340 ms, y abrir la hoja cuesta **2-9 ms**. No es un cuelgue, pero sí se pierden cuadros en la animación de entrada en gama baja. Estas cifras son estimación, no medición.
- **Todo en el hilo principal durante la composición**: los `remember { planesPorCarga / proximoSemestre / cadenaExplicada }` se ejecutan al componer.
- **Sin caché compartida**: `rememberRapida` y `DetalleGraduacion` calculan lo mismo por separado, y cada vuelta a la hoja repite las 8.
- **`margen(codigo)` se repite**: `proximoSemestre` lo calcula para `recomendadas` y `opcionales` por separado, y `cadenaExplicada` calcula `margen(primero)` además de la simulación con veto que ya hizo.
- **`Pensum.obligatorias` y `Pensum.todas` son getters** (`Pensum.kt:30,33`) que filtran y concatenan listas en cada acceso; 27 usos fuera de `Pensum.kt`, varios dentro de bucles de simulación (`cargaHabitual`, `pendientesDe`, `estadosDe`) y de composición (`rememberEstadoHorario` los pasa a `rememberLectorCaptura(estado.pensum.todas)` en cada recomposición).
- **`BarraCapsulas`** mide 10 textos (`medidor.measure`) en cada recomposición sin `remember`. Barato, pero evitable.
- **Rotar** recrea `EstadoCarrera` y su `layout` (ver A2).
- **Arreglo propuesto, en orden**:
  1. `val obligatorias`/`todas` con `by lazy` en `Pensum` (S).
  2. Un `planes by derivedStateOf` en `EstadoCarrera`, clave `marcas`/`casillas`/`hoy`, que usen `rememberRapida`, `DetalleGraduacion` y `habitual` (S).
  3. Subir `margen` a un cache `Map<String, Int>` dentro de `Simulador` y compartirlo entre `proximoSemestre` y `cadenaExplicada` (S).
  4. Mover los tres detalles pesados a `produceState(initial = null) { withContext(Dispatchers.Default) { … } }` con un esqueleto mientras llega (M).
  5. Ordenar las pendientes una sola vez con `cadena` precalculada y no recalcular `altura` por comparación (S, ganancia pequeña).
- **Algoritmos O(n²)**: `choquesDe` (`Horario.kt:119`) es O(n²) con `drop(i + 1)` copiando listas; con ≤ 40 clases es inocuo. `pendientesDe` y `prioridad` no pasan de unas decenas de elementos.
- **Asignaciones en bucles de dibujo**: en lo revisado (`BarraCapsulas`, `CabecerasSemestres`) no hay asignaciones en el bucle de `Canvas`; los cables del mapa los prepara `drawWithCache`.

## Persistencia

- **Por carrera, sin choques**: las claves `mapa-pensum:{marcas|casillas|carga|horario}:<slug>` no colisionan entre carreras ni con las de ajustes (`tema`, `acento`, `texto`, `luzFondo`, `movimiento`, `codigos`, `ligero`, `bienvenida`, `carrera`). Cambiar de carrera o reiniciar no pierde nada (verificado, lectura).
- **Escritura**: `SharedPreferences.apply()` (asíncrona, no bloquea). Cada marca reescribe el mapa entero de esa carrera (decenas de entradas): inocuo. `rememberResumenes` lee desde `Dispatchers.Default`: seguro.
- **Datos corruptos**: `leerMapa` y `HorarioGuardado.leer` están envueltos en `runCatching`; `"null"` en `carga` se lee como sin carga. Una `Marca` desconocida, un código borrado, una casilla huérfana o una electiva de otro grupo se descartan al leer (`depurarMarcas`, `depurarCasillas`).
- **Pérdida**: solo la de A2 (estado en memoria) y B1 (códigos que ya no existen).

## Redundancia

- **Cuatro "semestre actual"**: ver A4.
- **Dos "UC típicas"**: `ucTipicaDe` (moda) y la `mediana` de `pendientesDe`. Dejar una.
- **Tres formas de limpiar el título de un grupo**: `tituloGrupo` (`FranjaElectivas.kt:29`), el `replace(Regex("^Areas\\b"), "Áreas")` de `Lista.kt:210` y la poda de "as" de `nombresDelPlan`. Unificar en `tituloGrupo`.
- **Tres solapes de horario**: `solapan(Sesion, Sesion)`, `solapa(Int?, ...)` en `ImportarHorario.kt:234` y el filtro de `choqueCon`. Reusar `solapan` con un adaptador.
- **`nombresDelPlan` y `ucsDelPlan`** (`AvanceCarrera.kt`), `EstadoHorario.porCodigo` y `layout.porCodigo` construyen el mismo `associateBy { codigo }` de materias y electivas.
- **`estimarGraduacion(...).carga` y `Guardado.carga`/`habitual`**: ver M4.
- **`conCursandoAprobadas`** convierte TODAS las marcas en APROBADA: funciona porque `Marca` solo tiene dos valores, pero el nombre y la firma prometen otra cosa. Si se añade una tercera marca, se rompe en silencio. Filtrar por `CURSANDO` explícitamente.
- **Código sin uso en producción** (solo lo tocan los tests): `pesoDesbloqueo` (`Relaciones.kt:38`), `horaEnPunto` (`Horario.kt:70`), `Progreso.ucTotales`, `Progreso.porcentajeObligatorias`, `Progreso.paraInscribir` (solo para `disponibles`) y `EstimacionGraduacion.cadenaDe` (se calcula en cada simulación, incluida cada una de las ~130 de `cadenaExplicada`, y solo lo lee un test). Quitarlos, o dejar `cadenaDe` fuera de la estimación.
- **Documentación huérfana**: `Horario.kt:117-118` tiene dos KDoc seguidos; el primero pertenece a `choqueCon` (línea 138) y `diaDelMes` (127) no pertenece a este archivo.
- **`LIMITES_CARGA = 1..9` y `MAX_CARGA = 8`**: dos topes para el mismo dato. Ver M4.
