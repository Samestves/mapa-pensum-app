# El equipo de Mapa de Pensum: lo que todo experto debe saber

Este documento lo leen primero todos los expertos (agentes) antes de tocar
nada. Lo mantiene el orquestador.

## El producto

- **Mapa de Pensum** (UDO, Núcleo de Monagas): convierte el pensum de una
  carrera en un mapa de materias conectadas por sus prelaciones. El estudiante
  marca lo aprobado y lo que cursa, y ve qué puede inscribir.
- **App nativa** en `C:\Users\Sam\Documents\mapa-pensum-kmp`: Kotlin 2.3 +
  Compose Multiplatform 1.10 (solo Android por ahora) y Haze 1.6.10 para el
  cristal. Es un port de la web (`C:\Users\Sam\Documents\Proyecto Pensum`,
  React, en mapa-pensum.vercel.app).
- Paquetes:
  - `bienvenida/`: onboarding de 4 pasos.
  - `portada/`: home y ajustes.
  - `carrera/`: interior, barra Núcleo, ficha, hojas.
  - `mapa/`: mapa, cámara, gestos, efectos.
  - `logica/`: estados, avance, semestres.
  - `datos/`: pensums, guardado.
  - `componentes/`: iconos, silueta, rosa.
  - `tema/`: paleta y fuentes.

## Quién decide

Sam es el dueño del producto: diseñador exigente con gusto tipo Apple, Tesla
y Steam. Escribe en español, directo. Prueba cada entrega en un Android modesto.

## Reglas de estilo (ya decididas; no reabrirlas)

- **Paleta Acero**, oscuro: lienzo `#0B0C0E`, panel `#121316`, panelSuave
  `#16181B`, borde `#262A2F`, tinta `#ECEEF1`, suave `#B6BBC2`, tenue
  `#858B94`, apagada `#5D636C`.
  - El **único azul** es lo inscribible: `#5B8AF5`.
  - Verde de aprobada `#2EC27E`, ámbar de cursando `#F0A53A`, rojo apagado
    `#E5403A`.
  - Cada carrera tiene su color (`colorDe(carrera.color.oscuro)`), solo de acento.
  - Tokens en `tema/Tema.kt` (`Paleta`).
- **Fuentes:** Jost para la interfaz y los títulos (pesos Light/Normal),
  Inter para el texto de apoyo y los botones, IBM Plex Mono para códigos y UC.
- **Minimalismo premium.** Lo que Sam ya rechazó:
  - burbujas, volumen o reflejos ("too much");
  - iconos por materia;
  - toasts o banners informativos;
  - cajas con borde para avisos;
  - puntos de luz decorativos;
  - etiquetas que mueven el layout;
  - el título grande estilo Apple en la portada;
  - la brújula del onboarding.
- **Lo que sí le gusta:**
  - cristal sutil (Haze) con un filo de luz de 1 px;
  - texto sobre velo oscuro, al estilo TLOU 2;
  - mayúsculas finas y espaciadas para etiquetas;
  - un solo botón lleno en tinta (blanco) por pantalla;
  - movimiento lento y fluido, sin rebotes de juguete.
- **Rendimiento:** debe ir fluido en un teléfono de gama baja.
  - Animar en `graphicsLayer` o en `draw`, nunca recomponiendo cada cuadro.
  - Sin desenfoques grandes en cada cuadro.
- **Código limpio:** en español, comentado como el resto (por qué, no qué),
  con los mismos nombres e idiomas del código vecino.

### Lo aprendido con la tarjeta "Tu carrera" (2026-10-26)

- La versión con anillo, datos y barras de semestres: "abrumante".
- "Titular" (nombre, una línea de dato y la cápsula Continuar): rechazada por
  "simplona, muy AI slop, no va con el proyecto".
- Lo que pide: algo **moderno 2026**, no una tarjeta simple, que resuma dónde
  quedaste la última vez.
- Sus referencias:
  1. Mosaico de baldosas de cristal esmerilado sobre un degradado vivo (verde
     a lima) que brilla detrás.
  2. Tarjeta editorial: número enorme ("2,3m."), gráfico de matriz de puntos
     en un panel oscuro con rejilla de puntitos, y abajo una ficha con etiqueta
     "• GOOGLE FONTS", nombre grande y un bloque de color sólido con "Aa".
  3. Placa "69°": cristal oscuro tipo squircle, resplandor cálido desde abajo,
     número enorme con degradado, dial de marcas con aguja y grano fino.
- Lección: minimalista no es pobre. Quiere una pieza con carácter (cifra
  protagonista, visualización propia del producto, luz y textura), no una
  tarjeta genérica de lista.
- Después, Matriz, Brasa y Cristal le parecieron cargadas. Pidió algo limpio
  como una wallet (la cifra es la página, sin caja) y, viendo «Cifra al aire»
  (un 41 % enorme), lo corrigió: «piensa cómo lo debe ver el usuario, es
  simple»: en qué carrera quedó, una invitación a continuar y un motion de
  los punticos de su carrera, como en la web («Seguir donde ibas»).
- Línea en curso, «Constelación»: sin caja y centrado. Tiene etiqueta, nombre,
  la silueta de la carrera en grande (lo aprobado en blanco, lo que cursa en
  ámbar respirando) sobre la luz de su color, un dato y el botón «Seguir
  donde ibas».
- Tampoco. «No me captas, empecemos de 0»: minimalista tipo Notion, con
  cristal de verdad, moderno 2026, algo nuevo hecho para la app. Propuesta
  «Ventana» (inspirada en el «Continuar» de las consolas y Rockstar): tu mapa
  real, inclinado y a la deriva, de fondo; encima, un cristal con tu carrera,
  el semestre, lo que puedes inscribir y una flecha; el filo de luz del
  cristal marca el avance. Debajo, las otras carreras como lista sobria.
  El cristal tiene sentido porque hay algo detrás: tu mapa.
- **APROBADA por Sam: «Sencilla»**, calcada de la web en móvil («es excelente
  como está en móvil»). Va así, en un cristal: la silueta de tu carrera en
  puntos (lo aprobado encendido), el % con «APROBADO», el nombre corto con
  el tipo de título en mayúsculas finas y el chip «Continuar →» en el color de
  la carrera. Debajo, «OTRAS CARRERAS» en filas (silueta tenue, nombre, tipo
  y ›). Se pulió con una crítica heurística: sin repetir el nombre, texto de
  11 sp como mínimo, rejilla de 4/8, la flecha ↗ solo para lo que abre fuera
  y una escala tipográfica de 11/13/17/20/24/36.
- Lección: Sam quiere lo simple, fiel a la web, con pocos elementos bien
  alineados. Cuando dude, mirar primero cómo está en la web en móvil.

## Métodos (los de expertos de verdad)

- **Diseño:**
  - jerarquía visual (un foco por bloque);
  - divulgación progresiva;
  - ley de Fitts (objetivos de 44 dp o más donde va el pulgar);
  - Gestalt (proximidad, alineación);
  - rejilla de 4/8 dp;
  - escala tipográfica;
  - contraste WCAG AA;
  - Apple HIG y Material 3 como referencias.
- **Movimiento:** duraciones y curvas de Material Motion y Apple (entradas
  de 250–500 ms, *emphasized decelerate*, resortes críticamente amortiguados),
  continuidad espacial y coreografía escalonada sin saturar.
- **Datos:** nada inventado. El avance sale de `resumenParaPortada`
  (`logica/Avance.kt`): porcentaje, llevas/de (UC o materias), disponibles,
  cursando, hechas por semestre.

## Cómo se verifica

- Capturas sin teléfono con Robolectric + Roborazzi: las pruebas `Captura*Test`
  guardan PNG en `composeApp/build/capturas/`.
- Compilar y probar: `./gradlew --no-daemon :composeApp:testDebugUnitTest`.
- APK para Sam: `:composeApp:assembleRelease`.

## Formato de entrega de un experto

- Una propuesta (o dos como mucho) con la recomendación primero.
- Valores exactos listos para Compose (dp, sp, pesos, colores de `Paleta`,
  duraciones y curvas) y el texto en español.
- Los estados: con datos, vacío, cargando y error si aplica.
- El porqué de cada decisión, en una línea y con el método que la respalda.
- Sin relleno. Si algo choca con una regla de arriba, se dice.
