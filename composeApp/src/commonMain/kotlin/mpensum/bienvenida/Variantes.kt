package mpensum.bienvenida

/*
 * Las variantes en prueba de las pantallas 2 y 3 (docs/bienvenida-animaciones-v2.md).
 * Solo las cambian las capturas, antes de montar la bienvenida; la app usa la
 * recomendada. Cuando Sam elija, se quedan la elegida y este archivo se va.
 */

/** El objeto que hace de «foto» en la pantalla 2 */
internal enum class VarianteFoto {
    /** Una instantánea con su marco, inclinada, que sube desde abajo como si la soltaras */
    INSTANTANEA,
    /** La galería: se elige la miniatura, se pulsa Subir y la miniatura crece hasta la foto */
    GALERIA,
}

/** El concepto de la pantalla 3 */
internal enum class VarianteAvance {
    /** A: la tarjeta de la hoja «Tu avance», que se cuenta sola */
    TARJETA,
    /** B: el anillo del grado, una marca por materia hasta la meta y el birrete */
    ANILLO,
    /** C: el título, que se va firmando conforme avanza el % */
    TITULO,
}

internal object Variantes {
    var foto = VarianteFoto.INSTANTANEA
    var avance = VarianteAvance.TITULO
}
