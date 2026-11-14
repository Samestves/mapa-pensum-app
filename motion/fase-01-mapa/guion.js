/*
 * El guion de la fase 01, en segundos. Dura lo mismo que el bucle del inicio
 * (20 s) y empieza y acaba en su mismo cuadro, así que en Resolume se puede
 * encadenar con la fase 00 por delante o por detrás sin que se note el corte.
 *
 *   0,0  El logo sobre su placa, como en el inicio
 *   0,5  La placa se abre en un panel de vidrio y el logo sube a la cabecera
 *   1,2  El mapa se arma: las materias entran por semestres y los cables se tienden
 *   6,5  Un toque en una materia: se enciende todo lo que pide y lo que abre
 *  11,6  Se aprueba un semestre: las tarjetas pasan a verde y el avance sube
 *  15,5  La carrera entera, de un vistazo, y una luz que la recorre
 *  17,7  El panel se cierra y vuelve el logo
 */
import { limita, mezcla, suave } from '../comun/tiempo.js';

export const ABRE = [0.5, 1.75];
export const CIERRA = [17.75, 19.2];
export const TOQUE = 6.5;
export const FOCO = [6.5, 9.9];
export const BARRIDO = 15.5;
/** La materia que se toca: Programación Orientada a Objetos, de las que más abren */
export const ELEGIDA = '0722103';

export const expoFuera = (x) => (x >= 1 ? 1 : 1 - Math.pow(2, -10 * x));
export const cubica = (x) => (x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2);
export const rebote = (x, s = 1.25) => 1 + (s + 1) * Math.pow(x - 1, 3) + s * Math.pow(x - 1, 2);

/** Cuánto está abierto el panel: 0 es la placa del logo, 1 el panel entero (se pasa un pelo al abrir) */
export function apertura(t) {
  if (t < 10) return rebote(limita((t - ABRE[0]) / (ABRE[1] - ABRE[0])), 0.9);
  return 1 - cubica(limita((t - CIERRA[0]) / (CIERRA[1] - CIERRA[0])));
}

/** Lo mismo para el logo, que viaja sin rebote */
export function viajeLogo(t) {
  if (t < 10) return expoFuera(limita((t - ABRE[0]) / 1.05));
  return 1 - cubica(limita((t - CIERRA[0] - 0.1) / (CIERRA[1] - CIERRA[0] - 0.1)));
}

/** La cámara: centro en el plano y zoom. Pasa por estos puntos sin detenerse en ninguno */
const CAMARA = [
  [0.0, 690, 438, 1.0],
  [1.2, 700, 438, 1.0],
  [5.3, 1230, 440, 1.0],
  [6.6, 1010, 590, 1.34],
  [9.7, 1780, 500, 0.6],
  [11.6, 1120, 480, 0.94],
  [14.6, 1330, 480, 0.84],
  [16.7, 1843, 505, 0.445],
  [20.0, 1843, 505, 0.4],
];

export function camara(t) {
  let i = 0;
  while (i < CAMARA.length - 2 && t > CAMARA[i + 1][0]) i++;
  const a = CAMARA[i];
  const b = CAMARA[i + 1];
  const antes = CAMARA[Math.max(0, i - 1)];
  const despues = CAMARA[Math.min(CAMARA.length - 1, i + 2)];
  const dt = b[0] - a[0];
  const x = limita((t - a[0]) / dt);
  // Hermite con tangentes de Catmull-Rom: la velocidad es continua de un tramo al siguiente
  const h = (k) => {
    const v = (p) => (k === 3 ? Math.log(p[3]) : p[k]);
    const m0 = ((v(b) - v(antes)) / (b[0] - antes[0] || 1)) * dt;
    const m1 = ((v(despues) - v(a)) / (despues[0] - a[0] || 1)) * dt;
    const x2 = x * x;
    const x3 = x2 * x;
    return (2 * x3 - 3 * x2 + 1) * v(a) + (x3 - 2 * x2 + x) * m0 + (-2 * x3 + 3 * x2) * v(b) + (x3 - x2) * m1;
  };
  return { x: h(1), y: h(2), z: Math.exp(h(3)) };
}

/** Los rótulos de abajo: de cuándo a cuándo y qué dicen */
export const ROTULOS = [
  [2.2, 5.3, 'Tu carrera, en un mapa.'],
  [6.3, 9.8, 'Toca una materia: mira qué pide y qué abre.'],
  [11.4, 14.8, 'Marca lo aprobado. El mapa se pone al día.'],
  [15.6, 17.6, 'Todo tu pensum, de un vistazo.'],
];

export const HECHA = 0;
export const CURSANDO = 1;
export const INSCRIBIBLE = 2;
export const PROXIMA = 3;
export const LEJANA = 4;

/**
 * La historia de las materias: quien usa la app lleva dos semestres aprobados
 * y cursa el tercero. En el video aprueba el tercero y se inscribe en el cuarto.
 */
export function crearHistoria(plano) {
  const hecha = new Map();
  const cursa = new Map();
  for (const n of plano.nodos) {
    if (n.hueco) continue;
    const s = n.a.semestre;
    if (s <= 2) hecha.set(n.codigo, -Infinity);
    if (s === 3) {
      cursa.set(n.codigo, -Infinity);
      hecha.set(n.codigo, 11.7 + n.fila * 0.27);
    }
    if (s === 4) cursa.set(n.codigo, 13.45 + n.fila * 0.2);
  }
  const estaHecha = (c, t) => t >= (hecha.get(c) ?? Infinity);
  const estaCursando = (c, t) => t >= (cursa.get(c) ?? Infinity);

  function situacion(n, t) {
    if (estaHecha(n.codigo, t)) return HECHA;
    if (estaCursando(n.codigo, t)) return CURSANDO;
    const pre = n.a.prerrequisitos;
    if (pre.every((c) => estaHecha(c, t))) return INSCRIBIBLE;
    if (pre.every((c) => estaHecha(c, t) || estaCursando(c, t))) return PROXIMA;
    return LEJANA;
  }

  /** Cuánto de cada situación tiene la tarjeta ahora: el cambio de una a otra dura un tercio de segundo */
  function pesos(n, t) {
    const w = [0, 0, 0, 0, 0];
    const M = 8;
    for (let i = 0; i < M; i++) w[situacion(n, t - (0.34 * i) / (M - 1))] += 1 / M;
    return w;
  }

  /** Cuándo se abre una materia (pasa a poder inscribirse) dentro del video, si pasa */
  function seAbre(n) {
    if (n.hueco || hecha.has(n.codigo) && hecha.get(n.codigo) === -Infinity) return null;
    const pre = n.a.prerrequisitos;
    if (!pre.length) return null;
    const t = Math.max(...pre.map((c) => hecha.get(c) ?? Infinity));
    return Number.isFinite(t) ? t : null;
  }

  const total = plano.nodos.reduce((s, n) => s + (n.a.uc ?? 0), 0);
  /** Las UC aprobadas, subiendo de a poco con cada materia */
  function ucHechas(t) {
    let s = 0;
    for (const n of plano.nodos) {
      const th = hecha.get(n.codigo);
      if (th === undefined) continue;
      s += (n.a.uc ?? 0) * (th === -Infinity ? 1 : suave(th, th + 0.55, t));
    }
    return s;
  }

  return { situacion, pesos, hecha, cursa, seAbre, ucHechas, ucTotal: pensumUC(plano, total) };
}

function pensumUC(plano, suma) {
  return plano.ucObligatorias ?? suma;
}

/** Cuándo entra cada tarjeta al armarse el mapa */
export const entrada = (n) => 1.15 + n.columna * 0.26 + n.fila * 0.05;

export { limita, mezcla, suave };
