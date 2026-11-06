package mpensum.plataforma

import java.time.LocalDate
import java.time.LocalDateTime

actual fun horaLocal(): HoraLocal {
    val t = LocalDateTime.now()
    // DayOfWeek va de 1 (lunes) a 7 (domingo); el horario cuenta desde 0
    return HoraLocal(t.dayOfWeek.value - 1, t.hour * 60 + t.minute, t.second, t.toLocalDate().toEpochDay())
}

actual fun mesLocal(): MesLocal {
    val hoy = LocalDate.now()
    return MesLocal(hoy.year, hoy.monthValue)
}
