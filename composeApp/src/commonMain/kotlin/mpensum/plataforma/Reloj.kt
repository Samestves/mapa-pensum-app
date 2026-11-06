package mpensum.plataforma

/** La hora del teléfono en su zona: día de la semana (0 lunes … 6 domingo), minuto del día y segundo */
class HoraLocal(val dia: Int, val minuto: Int, val segundo: Int, val epoca: Long = 0)

/** Lo lee el horario para saber qué toca ahora; en las pruebas se inyecta otro reloj */
expect fun horaLocal(): HoraLocal

/** El año y el mes del teléfono (1 enero … 12 diciembre): de ahí sale el período en curso */
class MesLocal(val anio: Int, val mes: Int)

/** Lo lee la estimación del grado para saber en qué período estás; las pruebas pasan el suyo */
expect fun mesLocal(): MesLocal
