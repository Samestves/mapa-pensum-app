# Inicio nuevo · tres propuestas

Maqueta: `docs/maquetas/inicio-nuevo.html` (PNG al lado). Se recomienda **A · Resumen**. B (Hoy) y C (Ordenada) son las alternativas.

La referencia es la app de la Univalle: saludo, tarjeta héroe de la carrera, dos cifras y accesos rápidos. El lenguaje es el de Mapa de Pensum: Jost y Inter, la silueta del pensum, los iconos de carrera (`Iconos.kt`) y el color de cada una (`indice.json`).

## A · Resumen (recomendada)

Como la pestaña Resumen de Salud o Actividad de iPhone.

1. **Cabecera:** la fecha en Rótulo («LUNES 9 DE NOVIEMBRE») y el engranaje. Debajo, el saludo según la hora («Buenas tardes») en Titular. Al deslizar, el saludo se encoge a una barra de vidrio.
2. **Tarjeta héroe:** en el color de tu carrera, radio 28. Lleva la tecla con su icono, el tipo y el nombre, un **anillo de avance** (41 %) y la silueta del pensum en blanco (aprobado lleno, cursando en contorno, lo demás apagado). Debajo van las pastillas «Semestre 5» y «54 / 132 UC», el botón blanco «Seguir donde ibas» y el atajo al horario.
3. **Dos tarjetas cortas:** «Dic 2029 · Te gradúas» (birrete) y «6 materias · Puedes inscribir» (candado, azul).
4. **Accesos rápidos:** Mapa, Por hacer, Horario y Progreso, en teclas de 60 dp.
5. **Las nueve carreras:** rejilla de 3 × 3 con iconos de app de 64 dp en el color de cada carrera. La tuya va marcada con un aro.

## B · Hoy

Como la pestaña Hoy de la App Store. La tarjeta grande usa la silueta como ilustración. Debajo van la **siguiente clase** (del horario) y un carrusel de carteles con las demás carreras. Es más vistosa, pero las nueve no se ven a la vez.

## C · Ordenada

Como Ajustes de iPhone: título grande y buscador, una tarjeta compacta con barra de avance y tres cifras, y la lista agrupada por Ingenierías y Licenciaturas. Es la que mejor aguanta 320 dp y letra grande, pero es la menos emocionante.

## Pendiente de decidir

- **Saludo con nombre** («Hola, Juan»): pide un campo opcional en la bienvenida. Mientras no exista, se saluda según la hora.
- **Choque con `reglas-diseno.md`:** la tarjeta héroe en color de carrera rompe «el color es estado». Se propone como excepción de identidad, igual que el horario.
