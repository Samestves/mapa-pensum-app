package mpensum.tema

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Las medidas de la interfaz, sacadas de las que más se repetían en las
 * pantallas hechas a 412 dp. Migrar a ellas no debe cambiar el aspecto: si un
 * valor no está aquí, se redondea al vecino solo si la diferencia es de 2 dp o
 * 1 sp; si no, se queda como constante local con nombre. La guía está en
 * docs/sistema-diseno.md.
 */

/** Rellenos, huecos y separaciones: márgenes, `spacedBy`, `Spacer` */
object Espacio {
    /** Un hilo: separa un texto de su subrayado o un icono de su punto */
    val hilo = 2.dp

    /** Entre un icono y su texto, o entre renglones de un bloque */
    val xxs = 4.dp

    /** Entre elementos de una misma fila o pila (el más usado) */
    val xs = 8.dp

    /** Relleno de chips y filas compactas */
    val s = 12.dp

    /** Relleno de tarjetas; margen de pantalla en ventana compacta */
    val m = 16.dp

    /** Margen de pantalla en ventana normal; relleno de hojas */
    val l = 20.dp

    /** Separación entre bloques; margen de pantalla en ventana amplia */
    val xl = 24.dp

    /** Separación entre secciones de una hoja o de la portada */
    val xxl = 32.dp
}

/** Los radios de esquina; los demás (8 dp de las pastillas de la ficha, 2 dp de las barritas) se quedan donde se usan */
object Radio {
    /** Las pastillas de icono y los botones cuadrados (40 dp): 12 dp */
    val Chip = 12.dp

    /** Los campos y botones de las hojas (Personalizar, ficha): 16 dp */
    val Campo = 16.dp

    /** Las tarjetas y bandejas de la hoja: 18 dp */
    val Tarjeta = 18.dp

    /** Las opciones de Personalizar y la tarjeta de semestre del avance: 20 dp */
    val Opcion = 20.dp

    /** Los bloques del avance: 22 dp */
    val Bloque = 22.dp

    /** Las tarjetas grandes del detalle de graduación y de Personalizar: 24 dp */
    val Grande = 24.dp

    /** La barra de la carrera y las bandejas del horario: 26 dp */
    val Barra = 26.dp

    /** La hoja inferior y los paneles del avance: 28 dp */
    val Hoja = 28.dp
}

/** Altos de toque y de fila; se aplican con `heightIn(min = ...)` para que la letra grande pueda crecerlos */
object Alto {
    /** Un chip o pastilla de una línea */
    val chip = 36.dp

    /** Un botón de icono cuadrado */
    val icono = 40.dp

    /** El toque mínimo: ningún botón o fila pulsable por debajo */
    val toque = 44.dp

    /** Una fila de lista o de ajustes */
    val fila = 48.dp

    /** Un botón principal a lo ancho */
    val boton = 52.dp

    /** Un botón o fila destacada (Abrir, Guardar) */
    val grande = 56.dp
}

/** Los tamaños de letra que se repiten; la familia (jost, inter, plex) la pone cada estilo */
object Tipo {
    /** Etiquetas mínimas: códigos, unidades, pies de gráfica */
    val xxs = 11.sp

    /** Notas y subtítulos de fila */
    val xs = 12.sp

    /** El texto corriente de la app (el más usado) */
    val s = 13.sp

    /** Texto de lectura en hojas y fichas */
    val m = 14.sp

    /** Acciones y nombres en fila */
    val l = 15.sp

    /** Nombres de materia y títulos de fila */
    val xl = 17.sp

    /** Títulos de hoja y de sección */
    val xxl = 22.sp

    /** Titulares de pantalla */
    val titulo = 24.sp

    /** Cifras grandes (porcentajes, créditos) */
    val cifra = 32.sp

    /** La cifra protagonista del anillo y de la portada */
    val gigante = 40.sp
}
