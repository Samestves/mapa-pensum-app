# Plan: horario Día, nuevo avance y horizontal bien hecho

Noviembre 2026, de cara a la feria. Una fase a la vez: no se empieza la
siguiente hasta que la anterior compila, se ve bien en su captura y queda en
un commit. Si una fase usa agentes, todos trabajan en esa misma fase.

Verificación barata en cada fase: compilar y sacar solo las capturas de lo que
cambió (vertical 360 y horizontal 915×412). Nada de suites completas.

---

## Fase 1 — Horario: fuera «Semana», la vista Día como la única

**Por qué.** La rejilla semanal se ve fea en el teléfono y compite con la vista
Día, que ya trae la semana en miniatura. Sobran dos formas de ver lo mismo.

**Qué se hace**
1. Quitar el selector Día | Semana de la cabecera. La cabecera queda así: el
   rótulo del momento (EN CURSO / PRÓXIMA CLASE / HOY) y el «⋯».
2. Quitar `ModoHorario` de `EstadoHorario`. Al guardar una lectura se vuelve a
   la vista Día, y no a la Semana.
3. `SemanaRejilla` **se queda** solo para revisar lo leído de una captura
   (`LeerHorario`): ahí sí sirve ver la semana entera antes de guardar.
4. Pulir la vista Día:
   - Deslizar a los lados sobre el día elegido cambia de día (lunes ↔ viernes),
     y la lente de la semanita lo acompaña.
   - Arriba, bajo el rótulo, el resumen de la semana en una línea callada
     («5 clases · 10 h a la semana»), que antes solo vivía en la Semana.
5. Horizontal en dos columnas que se desplazan cada una por su lado:
   - Izquierda (unos 42 %): «Ahora y después» y la semanita.
   - Derecha: el día elegido, con su línea de tiempo y «Añadir clase».
6. Ajustar las capturas y pruebas que pedían el modo Semana.

**Hecho cuando.** No queda ninguna «Semana» en la vista; el día se cambia con
toque o deslizando; en horizontal no queda espacio muerto.

---

## Fase 2 — Avance: un héroe nuevo en lugar de la gráfica de trading

**Por qué.** La curva con años en el eje parece un gráfico de bolsa: no le
dice al estudiante, de forma humana, cuánto le falta. Además, los años se
montan unos sobre otros cuando la carrera es larga (2028…2039).

**Qué se hace**
1. Maquetas HTML de 3 direcciones, a 375 px y en horizontal, con los datos
   reales de Sistemas (8 %, 13/153 UC, Dic 2039). Sam elige una.
2. Implementar la elegida en `TarjetaHeroe` (AvanceCarrera.kt). La gráfica del
   detalle «¿Cuándo me gradúo?» se queda, pero sin los años montados: se marcan
   solo cada 2 o 3 años según quepan.
3. Respeta la Paleta Acero: el color de la carrera solo como acento, el azul
   solo para lo inscribible, el verde solo para lo aprobado.

**Hecho cuando.** Sam aprueba la maqueta y la tarjeta en el teléfono se ve
igual que ella.

---

## Fase 3 — Electivas que sirven

**Por qué.** Las filas «Técnicas 0/15 UC ›» tienen flecha pero no hacen nada.

**Qué se hace**
1. Tocar un grupo abre su detalle dentro de la misma hoja (como «¿Cuándo me
   gradúo?»): la barra de lo que llevas, cuántas te faltan, y cada casilla del
   plan con la electiva elegida o «Elegir electiva».
2. Elegir usa el `SelectorElectiva` que ya existe. Incluye «Ver en el mapa».
3. Es el mismo patrón que el detalle de graduación: atrás vuelve a la hoja.

**Hecho cuando.** Desde el avance se pueden ver y elegir las electivas de cada
grupo, y el contador se actualiza.

---

## Fase 4 — Lista en horizontal

**Por qué.** Acostada, la lista es una columna de 440 dp pegada al riel y el
resto de la pantalla queda vacío hasta que se toca una materia.

**Qué se hace.** Un patrón maestro-detalle, como Ajustes del iPad:
1. A la izquierda, la lista (filtros y semestres).
2. A la derecha, siempre algo:
   - Sin materia elegida: el resumen del semestre abierto (su anillo, sus UC
     aprobadas, cursando e inscribibles) y la pista «Toca una materia».
   - Con materia: la ficha, que ya existe como `PanelLateral`.
3. Repartir el ancho por proporción (unos 45/55, con mínimos y máximos), no
   con medidas fijas: se ve bien en teléfonos de 640 a 960 dp de ancho.

**Hecho cuando.** En horizontal no queda media pantalla vacía en ningún estado.

---

## Fase 5 — Ajustes de la Home en horizontal

**Por qué.** En dos columnas, la versión queda suelta en medio de la columna
izquierda, «El proyecto» queda cortado abajo, y las columnas no se equilibran.

**Qué se hace**
1. Izquierda: tu carrera y Personalizar. Derecha: Ayuda y El proyecto.
2. La versión, abajo y centrada, a todo el ancho.
3. La hoja se desplaza si no cabe, y la cabecera es más baja acostada.
4. Los rótulos de sección de las dos columnas quedan a la misma altura.

**Hecho cuando.** Todo se ve en una o dos pasadas de scroll, sin huecos raros.

---

## Fase 6 — Progreso en horizontal

Se hace después de la fase 2, porque depende del héroe nuevo.
1. El héroe a la izquierda, a alto completo, y las filas a la derecha con su
   propio scroll.
2. El detalle «¿Cuándo me gradúo?» y el de electivas usan las mismas dos
   columnas.

---

## Fase 7 — Ficha del mapa en horizontal

**Por qué.** Acostado, al tocar una materia sale una hoja desde abajo, con un
selector de marca enorme que tapa el mapa.

**Qué se hace**
1. En horizontal, la ficha sale como panel lateral a la derecha y a alto
   completo (el mismo `PanelLateral` de la lista). El mapa sigue visible a la
   izquierda y la cámara centra la materia en lo que queda libre.
2. El selector de marca va abajo en el panel: una fila compacta de 44 dp, sin
   el botón gigante.
3. Al deslizar el panel hacia la derecha, o al tocar el mapa, se cierra.

**Hecho cuando.** Al tocar una materia acostado, se sigue viendo el mapa y la
ficha no tapa más de un 40 % del ancho.

---

## Fase 8 — Cierre

Compilar el release, revisar el tamaño y que el perfil base sigue dentro,
entregar el APK y hacer push a `mapa-pensum-app`.
