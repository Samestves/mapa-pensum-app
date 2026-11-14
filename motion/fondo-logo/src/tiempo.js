/*
 * El reloj del bucle. Todo lo que se mueve es una función periódica de `u`
 * (0 al empezar, 1 al acabar), así el último cuadro empalma con el primero.
 */
export const ANCHO = 1920;
export const ALTO = 1280;
export const FPS = 60;
export const SEGUNDOS = 20;
export const CUADROS = FPS * SEGUNDOS;

/** Cuántas veces recorre el pulso la ruta en un bucle, y cuánto dura cada vuelta */
export const PULSOS = 4;
export const CICLO = SEGUNDOS / PULSOS;
/** Lo que tarda el pulso en ir de la salida a la meta */
export const VIAJE = 2.1;

export const TAU = Math.PI * 2;
export const limita = (x, a = 0, b = 1) => Math.min(b, Math.max(a, x));
export const mezcla = (a, b, t) => a + (b - a) * t;
export const suave = (a, b, x) => {
  const t = limita((x - a) / (b - a));
  return t * t * (3 - 2 * t);
};
export const entraSale = (t) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);
/** Una onda que da `k` vueltas enteras por bucle */
export const onda = (u, k = 1, fase = 0) => Math.sin(TAU * (k * u) + fase);

/** El estado del pulso en el cuadro: dónde va por la ruta y cuánto hace que llegó a la meta */
export function pulso(u) {
  const t = ((u * PULSOS) % 1) * CICLO;
  return {
    avance: entraSale(limita(t / VIAJE)),
    viajando: t < VIAJE,
    // Segundos desde la última llegada (la de este ciclo o la del anterior)
    desdeLlegada: (t - VIAJE + CICLO) % CICLO,
  };
}
