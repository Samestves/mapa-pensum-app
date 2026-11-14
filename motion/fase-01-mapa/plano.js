/*
 * El plano del mapa: dónde va cada materia y por dónde pasa cada cable. Es la
 * misma geometría de la app (composeApp/.../mapa/Geometria.kt), con las mismas
 * constantes, para que el video enseñe el mapa que la gente va a ver.
 */
export const NODO = { ANCHO: 224, ALTO: 96, RADIO: 7, PAD_IZQ: 18, PAD_DER: 16 };
const COLUMNA = 150;
const FILA = 26;
const MARGEN = { ARRIBA: 24, DERECHA: 48, ABAJO: 72, IZQUIERDA: 48 };
export const ALTO_ENCABEZADO = 84;
const TENSION = 0.45;
const TENSION_MINIMA = 44;
const ZONA_PUERTOS = 0.46;

function partirEnLineas(texto, ancho = NODO.ANCHO - NODO.PAD_IZQ - NODO.PAD_DER, tam = 14, max = 3) {
  const mide = (t) => t.length * tam * 0.5;
  const lineas = [];
  let actual = '';
  for (const palabra of texto.split(' ')) {
    const tentativa = actual ? `${actual} ${palabra}` : palabra;
    if (mide(tentativa) <= ancho || !actual) actual = tentativa;
    else {
      lineas.push(actual);
      actual = palabra;
    }
  }
  if (actual) lineas.push(actual);
  return lineas.slice(0, max);
}

function puerto(indice, total) {
  if (total <= 1) return NODO.ALTO / 2;
  const util = NODO.ALTO * ZONA_PUERTOS;
  return (NODO.ALTO - util) / 2 + (util * indice) / (total - 1);
}

/** Un punto de la curva de Bézier cúbica */
function bezier(p0, p1, p2, p3, t) {
  const m = 1 - t;
  return [
    m * m * m * p0[0] + 3 * m * m * t * p1[0] + 3 * m * t * t * p2[0] + t * t * t * p3[0],
    m * m * m * p0[1] + 3 * m * m * t * p1[1] + 3 * m * t * t * p2[1] + t * t * t * p3[1],
  ];
}

/** Curvas suaves entre columnas y tramos rectos por los pasillos, ya en puntos */
function trazar(x1, y1, x2, y2, cruces) {
  const pts = [[x1, y1]];
  let cx = x1;
  let cy = y1;
  const curva = (ax, ay, bx, by, k) => {
    for (let i = 1; i <= 28; i++) pts.push(bezier([ax, ay], [ax + k, ay], [bx - k, by], [bx, by], i / 28));
  };
  for (const c of cruces) {
    curva(cx, cy, c.entrada, c.y, Math.max((c.entrada - cx) * TENSION, 26));
    pts.push([c.salida, c.y]);
    cx = c.salida;
    cy = c.y;
  }
  curva(cx, cy, x2, y2, Math.max((x2 - cx) * TENSION, TENSION_MINIMA));
  return pts;
}

export function calcularPlano(pensum) {
  const materias = pensum.asignaturas;
  const semestres = [...new Set(materias.map((a) => a.semestre))].sort((a, b) => a - b);
  const nodos = [];
  const columnas = [];
  let maxFilas = 0;
  semestres.forEach((semestre, indice) => {
    const suyas = materias.filter((a) => a.semestre === semestre);
    const x = MARGEN.IZQUIERDA + indice * (NODO.ANCHO + COLUMNA);
    columnas.push({ semestre, indice, x, materias: suyas, uc: suyas.reduce((s, a) => s + (a.uc ?? 0), 0) });
    suyas.forEach((a, fila) => {
      nodos.push({
        a, codigo: a.codigo, columna: indice, fila, x,
        y: MARGEN.ARRIBA + ALTO_ENCABEZADO + fila * (NODO.ALTO + FILA),
        lineas: partirEnLineas(a.nombre), hueco: !!a.esHueco,
      });
    });
    maxFilas = Math.max(maxFilas, suyas.length);
  });
  const porCodigo = new Map(nodos.map((n) => [n.codigo, n]));

  // Los pasillos libres de cada columna: entre dos tarjetas y bajo la última
  const pasillos = new Map();
  for (const c of columnas) {
    const ys = nodos.filter((n) => n.columna === c.indice).map((n) => n.y).sort((a, b) => a - b);
    const centros = ys.slice(1).map((y, i) => (ys[i] + NODO.ALTO + y) / 2);
    centros.push(ys[ys.length - 1] + NODO.ALTO + FILA / 2);
    pasillos.set(c.semestre, { x: c.x, centros, usados: new Set() });
  }
  const elegir = (p, y) => {
    const orden = p.centros.map((v, i) => [v, i]).sort((a, b) => Math.abs(a[0] - y) - Math.abs(b[0] - y));
    const libre = orden.find(([, i]) => !p.usados.has(i)) ?? orden[0];
    p.usados.add(libre[1]);
    return libre[0];
  };

  const salidas = new Map();
  const entradas = new Map();
  for (const d of nodos) {
    for (const pre of d.a.prerrequisitos) {
      if (!porCodigo.has(pre)) continue;
      if (!salidas.has(pre)) salidas.set(pre, []);
      if (!entradas.has(d.codigo)) entradas.set(d.codigo, []);
      salidas.get(pre).push(d.codigo);
      entradas.get(d.codigo).push(pre);
    }
  }
  const porAltura = (a, b) => porCodigo.get(a).y - porCodigo.get(b).y;
  salidas.forEach((l) => l.sort(porAltura));
  entradas.forEach((l) => l.sort(porAltura));

  const aristas = [];
  for (const d of nodos) {
    for (const pre of d.a.prerrequisitos) {
      const o = porCodigo.get(pre);
      if (!o) continue;
      const sal = salidas.get(pre);
      const ent = entradas.get(d.codigo);
      const x1 = o.x + NODO.ANCHO;
      const y1 = o.y + puerto(sal.indexOf(d.codigo), sal.length);
      const x2 = d.x;
      const y2 = d.y + puerto(ent.indexOf(pre), ent.length);
      const cruces = [];
      for (let s = o.a.semestre + 1; s < d.a.semestre; s++) {
        const p = pasillos.get(s);
        if (p) cruces.push({ entrada: p.x, salida: p.x + NODO.ANCHO, y: elegir(p, (y1 + y2) / 2) });
      }
      const pts = trazar(x1, y1, x2, y2, cruces);
      // Largo acumulado: con él se dibuja cualquier trozo del cable
      const largos = [0];
      for (let i = 1; i < pts.length; i++) largos.push(largos[i - 1] + Math.hypot(pts[i][0] - pts[i - 1][0], pts[i][1] - pts[i - 1][1]));
      aristas.push({ origen: pre, destino: d.codigo, area: o.a.area, pts, largos, largo: largos[largos.length - 1] });
    }
  }

  /** Todo lo que una materia pide (hacia atrás) o abre (hacia delante), con la distancia a ella */
  function cadena(codigo) {
    const niveles = new Map([[codigo, 0]]);
    const andar = (inicio, vecinos, signo) => {
      let frente = [inicio];
      let n = 0;
      while (frente.length) {
        n++;
        const sig = [];
        for (const c of frente) for (const v of vecinos(c)) if (!niveles.has(v)) { niveles.set(v, signo * n); sig.push(v); }
        frente = sig;
      }
    };
    andar(codigo, (c) => entradas.get(c) ?? [], -1);
    andar(codigo, (c) => salidas.get(c) ?? [], 1);
    return niveles;
  }

  const columnasN = Math.max(semestres.length, 1);
  const fin = MARGEN.ARRIBA + ALTO_ENCABEZADO + maxFilas * NODO.ALTO + (maxFilas - 1) * FILA;
  return {
    nodos, columnas, aristas, porCodigo, cadena, salidas, entradas,
    ancho: MARGEN.IZQUIERDA + columnasN * NODO.ANCHO + (columnasN - 1) * COLUMNA + MARGEN.DERECHA,
    alto: fin + MARGEN.ABAJO,
  };
}

/** El trozo de un cable entre dos largos, como lista de puntos */
export function trozo(arista, desde, hasta) {
  const { pts, largos, largo } = arista;
  const a = Math.max(0, Math.min(largo, desde));
  const b = Math.max(0, Math.min(largo, hasta));
  if (b <= a) return [];
  const en = (d) => {
    let i = 1;
    while (i < largos.length - 1 && largos[i] < d) i++;
    const t = (d - largos[i - 1]) / (largos[i] - largos[i - 1] || 1);
    return [pts[i - 1][0] + (pts[i][0] - pts[i - 1][0]) * t, pts[i - 1][1] + (pts[i][1] - pts[i - 1][1]) * t];
  };
  const out = [en(a)];
  for (let i = 0; i < pts.length; i++) if (largos[i] > a && largos[i] < b) out.push(pts[i]);
  out.push(en(b));
  return out;
}
