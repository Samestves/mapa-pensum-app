# Inicio · prototipo navegable

Prototipo: `docs/prototipo/inicio.html`. Se abre en Chrome del móvil (a pantalla completa) o en un escritorio (en un marco de teléfono). Viene de `inicio-premium.md`, depurado con criterio de Apple: una idea por bloque y ningún dato repetido.

## Qué se quitó (y por qué)

| Antes | Problema | Ahora |
|---|---|---|
| «BUENAS TARDES» + «Mapa de Pensum» + campana + engranaje + buscador | Cinco piezas para una cabecera; el título era el logo con otra letra; la campana no tenía función | El saludo y la fecha a la izquierda, el engranaje a la derecha. Al deslizar, una barra de vidrio con el nombre de tu carrera |
| El buscador fijo arriba | Ocupaba sitio en cada visita para una acción ocasional | Una lupa en el muelle, como en iOS 26 |
| Tarjeta de 8 datos («TU CARRERA», flecha, 41 %, 54 / 132 UC, semestre, cursando dos veces, segmentos y tres cifras) | Repetía «cursando» y competía consigo misma | **Portada inmersiva**: icono, nombre, tipo, una acción, tres cifras y la regla de semestres |
| Barras de colores en cada carrera | Dos señales de color por fila (la tecla y las barras) y un dato que no se entiende sin leyenda | Una fila limpia: tecla, nombre y materias. La silueta vive en la ficha de cada carrera |
| El selector Todas · Ingenierías · Licenciaturas | Filtraba 8 filas que caben en dos grupos | Dos grupos con su rótulo |

## La portada

- **Campo de color:** cuatro manchas radiales en el color de la carrera y sus vecinos de tono, con grano. Se pinta una vez en un canvas.
- **Constelación:** tu pensum (un punto por materia, una columna por semestre); lo aprobado brilla más. Es la «foto» de la portada, como en la referencia de perfil.
- **Fundido desenfocado** hacia el lienzo, debajo del nombre grande (Jost 300, 46 sp).
- **Una sola acción** blanca, «Seguir donde ibas», con el horario al lado.
- **Tres cifras** que entran rodando (odómetro): avance, por inscribir y fecha de grado.
- **Regla de semestres:** diez trazos finos y un punto sobre el semestre actual. Escala técnica, guiño a las interfaces de Marathon.

## Interacciones del prototipo

- Deslizar: la cabecera se vuelve vidrio y la portada sube a media velocidad.
- Tocar una carrera: su ficha abre con transición de vista (la tecla viaja). Desde ahí, «Hacerla mía» la pone en la portada, con su color.
- La lupa abre la búsqueda; el engranaje abre Ajustes con el tema oscuro o claro.
- Las pestañas del muelle se deslizan con un resorte y vibran al tocar en Android.
