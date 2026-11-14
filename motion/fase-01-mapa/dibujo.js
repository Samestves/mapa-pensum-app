/*
 * El dibujo del mapa dentro del panel de vidrio: tarjetas, cables, cabeceras
 * y los efectos (toque, cadena encendida, aprobaciones, barrido de luz). Los
 * colores son los del tema Medianoche de la app y las áreas las suyas.
 */
import { forma } from '../comun/fondo.js';
import { TAU, limita, mezcla, suave } from '../comun/tiempo.js';
import { ALTO_ENCABEZADO, NODO, trozo } from './plano.js';
import {
  BARRIDO, CURSANDO, ELEGIDA, FOCO, HECHA, INSCRIBIBLE, LEJANA, PROXIMA, ROTULOS, TOQUE,
  camara, entrada, expoFuera, rebote,
} from './guion.js';

const P = {
  lienzo: [10, 16, 32], panel: [16, 25, 45], panelSuave: [21, 31, 55], tinta: [233, 238, 248],
  suave: [180, 191, 211], tenue: [135, 148, 174], apagada: [95, 108, 136], azul: [125, 164, 255], verde: [63, 207, 142],
};
const AREAS = {
  'ciencias-basicas': [91, 192, 235], estadistica: [167, 139, 250], computacion: [60, 207, 168],
  electronica: [242, 166, 90], sistemas: [91, 138, 245], gestion: [240, 115, 155], tesis: [232, 207, 79],
};
const GRIS = [139, 145, 153];
const area = (a) => AREAS[a] ?? GRIS;
const mix = (a, b, t) => a.map((v, i) => mezcla(v, b[i], t));
const rgba = (c, a = 1) => `rgba(${c[0] | 0},${c[1] | 0},${c[2] | 0},${a})`;
const avivar = (c) => mix(c, [255, 255, 255], 0.5);

/** Cómo se ve la tarjeta en cada situación: fondo, borde (y su alfa), grosor, nombre y color fuerte */
const ASPECTO = [];
ASPECTO[HECHA] = { fondo: mix(P.panel, P.verde, 0.09), borde: P.verde, alfa: 0.3, grosor: 1, nombre: P.suave, fuerte: P.verde };
ASPECTO[CURSANDO] = { fondo: P.panel, borde: P.tinta, alfa: 1, grosor: 1.5, nombre: P.tinta, fuerte: P.tinta };
ASPECTO[INSCRIBIBLE] = { fondo: mix(P.panel, P.tinta, 0.07), borde: P.tinta, alfa: 0.3, grosor: 1.25, nombre: P.tinta, fuerte: P.azul };
ASPECTO[PROXIMA] = { fondo: P.panelSuave, borde: P.tinta, alfa: 0.12, grosor: 1, nombre: P.suave, fuerte: P.suave };
ASPECTO[LEJANA] = { fondo: mix(P.lienzo, P.panel, 0.5), borde: P.tinta, alfa: 0.05, grosor: 1, nombre: mix(P.tenue, P.apagada, 0.3), fuerte: P.suave };

function aspectoMezclado(w) {
  const r = { fondo: [0, 0, 0], borde: [0, 0, 0], alfa: 0, grosor: 0, nombre: [0, 0, 0], fuerte: [0, 0, 0] };
  w.forEach((p, s) => {
    if (!p) return;
    const a = ASPECTO[s];
    for (const k of ['fondo', 'borde', 'nombre', 'fuerte']) for (let i = 0; i < 3; i++) r[k][i] += a[k][i] * p;
    r.alfa += a.alfa * p;
    r.grosor += a.grosor * p;
  });
  return r;
}

const JOST = 'Jost, sans-serif';
const PLEX = 'Plex, monospace';

export function crearDibujo(lienzo, plano, historia, pensum) {
  const ctx = lienzo.getContext('2d');
  const cadena = plano.cadena(ELEGIDA);
  const elegida = plano.porCodigo.get(ELEGIDA);
  const abre = new Map(plano.nodos.map((n) => [n.codigo, historia.seAbre(n)]));

  function redondo(x, y, w, h, r) {
    ctx.beginPath();
    ctx.roundRect(x, y, w, h, r);
  }

  // ------------------------------------------------------------- tarjetas
  function icono(s, x, y, lado, alfa, pop = 1) {
    if (alfa <= 0.01) return;
    const c = [x + lado / 2, y + lado / 2];
    const r = (lado / 2 - 1) * pop;
    ctx.save();
    ctx.globalAlpha *= alfa;
    ctx.lineWidth = 1.5;
    ctx.lineCap = ctx.lineJoin = 'round';
    if (s === HECHA) {
      ctx.fillStyle = rgba(P.verde);
      ctx.beginPath();
      ctx.arc(c[0], c[1], r, 0, TAU);
      ctx.fill();
      ctx.strokeStyle = rgba(P.lienzo);
      ctx.lineWidth = 1.8;
      ctx.beginPath();
      ctx.moveTo(c[0] - r * 0.42, c[1] + r * 0.02);
      ctx.lineTo(c[0] - r * 0.1, c[1] + r * 0.36);
      ctx.lineTo(c[0] + r * 0.46, c[1] - r * 0.32);
      ctx.stroke();
    } else if (s === CURSANDO) {
      ctx.strokeStyle = rgba(P.tinta);
      ctx.beginPath();
      ctx.arc(c[0], c[1], r, 0, TAU);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(c[0], c[1] - r * 0.5);
      ctx.lineTo(c[0], c[1]);
      ctx.lineTo(c[0] + r * 0.38, c[1] + r * 0.22);
      ctx.stroke();
    } else if (s === INSCRIBIBLE) {
      ctx.strokeStyle = rgba(P.azul);
      ctx.beginPath();
      ctx.arc(c[0], c[1], r, 0, TAU);
      ctx.stroke();
      ctx.fillStyle = rgba(P.azul);
      ctx.beginPath();
      ctx.arc(c[0], c[1], r * 0.36, 0, TAU);
      ctx.fill();
    } else {
      // El candado de lo que todavía no se puede cursar
      ctx.strokeStyle = rgba(s === PROXIMA ? P.tenue : P.apagada);
      ctx.lineWidth = 1.3;
      redondo(c[0] - r * 0.62, c[1] - r * 0.1, r * 1.24, r * 0.95, 2);
      ctx.stroke();
      ctx.beginPath();
      ctx.arc(c[0], c[1] - r * 0.12, r * 0.4, Math.PI, 0);
      ctx.stroke();
    }
    ctx.restore();
  }

  function tarjeta(n, t, zoom, foco) {
    const e = expoFuera(limita((t - entrada(n)) / 0.75));
    if (e <= 0.001) return;
    const enCadena = cadena.has(n.codigo);
    const apagada = enCadena ? 1 : 1 - 0.76 * foco;
    const detalle = suave(0.3, 0.5, zoom);
    const W = NODO.ANCHO;
    const H = NODO.ALTO;

    ctx.save();
    ctx.translate(n.x + W / 2, n.y + H / 2 + (1 - e) * 34);
    const esc = 0.9 + 0.1 * e;
    ctx.scale(esc, esc);
    ctx.translate(-W / 2, -H / 2);
    ctx.globalAlpha *= e * apagada;

    if (n.hueco) {
      ctx.strokeStyle = rgba(P.tinta, 0.22);
      ctx.lineWidth = 1.5;
      ctx.setLineDash([6, 5]);
      redondo(0.75, 0.75, W - 1.5, H - 1.5, NODO.RADIO);
      ctx.stroke();
      ctx.setLineDash([]);
      ctx.globalAlpha *= detalle;
      ctx.lineWidth = 1;
      redondo(W / 2 - 15, 14, 30, 30, 8);
      ctx.stroke();
      ctx.strokeStyle = rgba(P.suave);
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.moveTo(W / 2 - 6, 29);
      ctx.lineTo(W / 2 + 6, 29);
      ctx.moveTo(W / 2, 23);
      ctx.lineTo(W / 2, 35);
      ctx.stroke();
      ctx.fillStyle = rgba(P.tenue);
      ctx.font = `380 14px ${JOST}`;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(n.a.nombre, W / 2, 64);
      ctx.restore();
      return;
    }

    const w = historia.pesos(n, t);
    const a = aspectoMezclado(w);
    const sel = n.codigo === ELEGIDA ? foco : 0;
    const resalta = enCadena ? foco : 0;
    // La luz del barrido final pasa por la tarjeta
    const paso = Math.exp(-Math.pow((t - (BARRIDO + 0.25 + (n.x / plano.ancho) * 1.5)) / 0.22, 2));

    if (sel > 0) {
      ctx.strokeStyle = rgba(a.fuerte, 0.3 * sel);
      ctx.lineWidth = 2;
      redondo(-4, -4, W + 8, H + 8, NODO.RADIO + 4);
      ctx.stroke();
    }
    ctx.fillStyle = rgba(a.fondo, 0.94);
    redondo(0, 0, W, H, NODO.RADIO);
    ctx.fill();
    const g = a.grosor + 0.75 * sel;
    const bordeAlfa = mezcla(a.alfa, 1, Math.max(resalta, paso * 0.7));
    ctx.strokeStyle = rgba(mix(a.borde, a.fuerte, resalta), bordeAlfa);
    ctx.lineWidth = g;
    redondo(g / 2, g / 2, W - g, H - g, NODO.RADIO);
    ctx.stroke();

    // El punto de su área y el estado: se leen aunque la cámara esté lejos
    ctx.fillStyle = rgba(area(n.a.area));
    ctx.beginPath();
    ctx.arc(NODO.PAD_IZQ + 3, H - 16, 3, 0, TAU);
    ctx.fill();
    const th = historia.hecha.get(n.codigo);
    const pop = Number.isFinite(th) ? rebote(limita((t - th) / 0.45), 2.4) : 1;
    w.forEach((p, s) => icono(s, W - NODO.PAD_DER - 17.5, 10.5, 17.5, p, s === HECHA ? pop : 1));

    if (detalle > 0.01) {
      ctx.globalAlpha *= detalle;
      ctx.textAlign = 'left';
      ctx.textBaseline = 'alphabetic';
      ctx.fillStyle = rgba(P.tenue);
      ctx.font = `300 10px ${PLEX}`;
      ctx.letterSpacing = '0.4px';
      ctx.fillText(n.codigo, NODO.PAD_IZQ, 24);
      ctx.fillText(`${n.a.uc ?? 0} UC`, NODO.PAD_IZQ + 12, H - 12.5);
      ctx.letterSpacing = '0px';
      ctx.fillStyle = rgba(a.nombre);
      ctx.font = `380 14px ${JOST}`;
      ctx.textBaseline = 'middle';
      n.lineas.forEach((l, i) => ctx.fillText(l, NODO.PAD_IZQ, 56 + (i - (n.lineas.length - 1) / 2) * 16.5));
    }
    ctx.restore();

    // Los anillos: el del toque, el de aprobar y el destello de lo que se abre
    const anillo = (t0, color, dur, crece) => {
      const p = (t - t0) / dur;
      if (p <= 0 || p >= 1) return;
      const k = 1 - Math.pow(1 - p, 3);
      ctx.save();
      ctx.globalAlpha *= (1 - p) * 0.85;
      ctx.strokeStyle = rgba(color);
      ctx.lineWidth = 2 + 4 * (1 - k);
      redondo(n.x - crece * k, n.y - crece * k, W + 2 * crece * k, H + 2 * crece * k, NODO.RADIO + crece * k);
      ctx.stroke();
      ctx.restore();
    };
    if (n.codigo === ELEGIDA) anillo(TOQUE, P.tinta, 0.65, 16);
    if (Number.isFinite(th)) {
      anillo(th, P.verde, 0.8, 20);
      const brillo = Math.exp(-Math.max(0, t - th) * 5) * (t >= th ? 1 : 0);
      if (brillo > 0.01) {
        ctx.fillStyle = rgba(P.verde, 0.3 * brillo);
        redondo(n.x, n.y, W, H, NODO.RADIO);
        ctx.fill();
      }
    }
    const ta = abre.get(n.codigo);
    if (ta !== null) anillo(ta + 0.55, P.azul, 0.9, 12);
    const tc = historia.cursa.get(n.codigo);
    if (Number.isFinite(tc)) anillo(tc, P.tinta, 0.6, 12);
  }

  // --------------------------------------------------------------- cables
  function linea(pts) {
    if (pts.length < 2) return;
    ctx.beginPath();
    ctx.moveTo(pts[0][0], pts[0][1]);
    for (let i = 1; i < pts.length; i++) ctx.lineTo(pts[i][0], pts[i][1]);
    ctx.stroke();
  }

  /** Una luz que corre por el cable: cabeza en `f` (0..1) y una cola detrás */
  function cometa(a, f, color, fuerza = 1, cola = 0.3) {
    if (f <= 0 || f >= 1 + cola || fuerza <= 0.01) return;
    const N = 7;
    ctx.save();
    ctx.globalCompositeOperation = 'lighter';
    ctx.lineCap = 'round';
    for (let i = 0; i < N; i++) {
      const k = (i + 1) / N;
      ctx.strokeStyle = rgba(color, 0.75 * k * k * fuerza);
      ctx.lineWidth = 1.6 + 2.2 * k;
      linea(trozo(a, (f - cola * (1 - i / N)) * a.largo, (f - cola * (1 - k)) * a.largo));
    }
    if (f < 1) {
      const [[x, y]] = trozo(a, f * a.largo - 0.5, f * a.largo).slice(-1);
      const g = ctx.createRadialGradient(x, y, 0, x, y, 16);
      g.addColorStop(0, rgba([255, 255, 255], 0.9 * fuerza));
      g.addColorStop(0.3, rgba(color, 0.5 * fuerza));
      g.addColorStop(1, rgba(color, 0));
      ctx.fillStyle = g;
      ctx.beginPath();
      ctx.arc(x, y, 16, 0, TAU);
      ctx.fill();
    }
    ctx.restore();
  }

  function cables(t, foco) {
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    for (const a of plano.aristas) {
      const o = plano.porCodigo.get(a.origen);
      const d = plano.porCodigo.get(a.destino);
      const sale = limita((t - Math.max(entrada(o), entrada(d)) - 0.12) / 0.7);
      if (sale <= 0) continue;
      const color = area(a.area);
      const enCadena = cadena.has(a.origen) && cadena.has(a.destino);
      const hecho = historia.hecha.get(a.origen);
      const vivo = hecho === undefined ? 0 : hecho === -Infinity ? 1 : suave(hecho, hecho + 0.5, t);
      const alfa = mezcla(0.3, 0.52, vivo) * (enCadena ? 1 : 1 - 0.8 * foco);
      ctx.strokeStyle = rgba(color, alfa);
      ctx.lineWidth = 1.5;
      linea(trozo(a, 0, a.largo * expoFuera(sale)));

      if (enCadena && foco > 0) {
        // La cadena se enciende desde la materia tocada hacia fuera, nivel a nivel
        const no = cadena.get(a.origen);
        const nd = cadena.get(a.destino);
        const atras = nd <= 0;
        const nivel = Math.min(Math.abs(no), Math.abs(nd));
        const t0 = TOQUE + 0.12 + nivel * 0.26;
        const p = expoFuera(limita((t - t0) / 0.6));
        if (p > 0) {
          ctx.strokeStyle = rgba(avivar(color), 0.95 * foco);
          ctx.lineWidth = 2.4;
          linea(atras ? trozo(a, a.largo * (1 - p), a.largo) : trozo(a, 0, a.largo * p));
          const vuelta = ((t - t0) % 1.5) / 1.1;
          cometa(a, atras ? 1.3 - vuelta : vuelta, avivar(color), foco);
        }
      }
      // Al aprobar una materia, la luz corre hacia lo que abre
      if (Number.isFinite(hecho)) cometa(a, (t - hecho - 0.08) / 0.75, P.verde, 1, 0.4);
      // El barrido final: una luz recorre la carrera de izquierda a derecha
      cometa(a, (t - (BARRIDO + (o.x / plano.ancho) * 1.5)) / 0.8, avivar(color), 1, 0.45);
    }
  }

  // ------------------------------------------------------------ cabeceras
  function cabeceras(t, zoom) {
    const detalle = suave(0.3, 0.5, zoom);
    for (const c of plano.columnas) {
      const e = expoFuera(limita((t - (1.05 + c.indice * 0.26)) / 0.7));
      if (e <= 0.001) continue;
      ctx.save();
      ctx.globalAlpha *= e;
      ctx.translate(c.x, 24 + (1 - e) * 18);
      const suyas = c.materias.filter((m) => !m.esHueco);
      let hechas = 0;
      for (const m of suyas) {
        const th = historia.hecha.get(m.codigo);
        if (th !== undefined) hechas += th === -Infinity ? 1 : suave(th, th + 0.5, t);
      }
      ctx.fillStyle = rgba(P.tinta, 0.1);
      redondo(0, 62, NODO.ANCHO, 2, 1);
      ctx.fill();
      if (hechas > 0) {
        ctx.fillStyle = rgba(P.verde);
        redondo(0, 62, (NODO.ANCHO * hechas) / suyas.length, 2, 1);
        ctx.fill();
      }
      ctx.globalAlpha *= detalle;
      ctx.textAlign = 'left';
      ctx.textBaseline = 'alphabetic';
      ctx.fillStyle = rgba(P.tinta);
      ctx.font = `500 11.5px ${JOST}`;
      ctx.letterSpacing = '3.4px';
      ctx.fillText(`SEMESTRE ${c.semestre}`, 0, 30);
      ctx.fillStyle = rgba(P.tenue);
      ctx.font = `300 10px ${PLEX}`;
      ctx.letterSpacing = '0.4px';
      ctx.fillText(`${suyas.length} materias · ${c.uc} UC`, 0, 50);
      ctx.letterSpacing = '0px';
      ctx.restore();
    }
  }

  // --------------------------------------------------------- sobre el mapa
  function rotulos(t, f, abierto) {
    const x = f.cx - f.w / 2 + 76;
    const y = f.cy + f.h / 2 - 84;
    ctx.textAlign = 'left';
    ctx.textBaseline = 'alphabetic';
    ctx.fillStyle = rgba(P.azul, abierto);
    ctx.font = `400 15px ${PLEX}`;
    ctx.letterSpacing = '4px';
    ctx.fillText('01 — EL MAPA', x + 2, y - 82);
    ctx.letterSpacing = '0px';
    ctx.font = `300 62px ${JOST}`;
    for (const [t0, t1, texto] of ROTULOS) {
      if (t < t0 || t > t1 + 0.5) continue;
      const sale = suave(t1, t1 + 0.4, t);
      let px = x;
      texto.split(' ').forEach((palabra, i) => {
        const e = expoFuera(limita((t - t0 - i * 0.055) / 0.7));
        ctx.fillStyle = rgba(P.tinta, e * (1 - sale) * abierto);
        ctx.fillText(palabra, px, y + (1 - e) * 34 - sale * 16);
        px += ctx.measureText(`${palabra} `).width;
      });
    }
  }

  function cabecera(t, f, abierto) {
    const x0 = f.cx - f.w / 2;
    const y0 = f.cy - f.h / 2;
    const e = expoFuera(limita((t - 1.0) / 0.8)) * abierto;
    if (e <= 0.01) return;
    ctx.save();
    ctx.globalAlpha = e;
    ctx.textAlign = 'left';
    ctx.textBaseline = 'middle';
    ctx.fillStyle = rgba(P.tinta);
    ctx.font = `400 31px ${JOST}`;
    const xm = x0 + 146 - (1 - e) * 20;
    ctx.fillText('Mapa de Pensum', xm, y0 + 80);
    const w = ctx.measureText('Mapa de Pensum').width;
    ctx.fillStyle = rgba(P.tinta, 0.18);
    ctx.fillRect(xm + w + 22, y0 + 66, 1.5, 28);
    ctx.fillStyle = rgba(P.suave);
    ctx.font = `300 24px ${JOST}`;
    ctx.fillText(pensum.nombre, xm + w + 44, y0 + 81);

    // La isla del avance, arriba a la derecha
    const iw = 356;
    const ih = 92;
    const ix = x0 + f.w - 64 - iw + (1 - e) * 20;
    const iy = y0 + 34;
    ctx.fillStyle = 'rgba(160,185,255,0.09)';
    redondo(ix, iy, iw, ih, 26);
    ctx.fill();
    ctx.strokeStyle = 'rgba(255,255,255,0.16)';
    ctx.lineWidth = 1;
    redondo(ix + 0.5, iy + 0.5, iw - 1, ih - 1, 26);
    ctx.stroke();
    const uc = historia.ucHechas(t);
    const pct = (uc / historia.ucTotal) * 100;
    ctx.fillStyle = rgba(P.tinta);
    ctx.font = `200 56px ${JOST}`;
    ctx.textBaseline = 'alphabetic';
    const cifra = String(Math.round(pct));
    ctx.fillText(cifra, ix + 26, iy + 66);
    const wc = ctx.measureText(cifra).width;
    ctx.fillStyle = rgba(P.tenue);
    ctx.font = `300 24px ${JOST}`;
    ctx.fillText('%', ix + 32 + wc, iy + 66);
    const bx = ix + 150;
    ctx.font = `400 12px ${PLEX}`;
    ctx.letterSpacing = '2.6px';
    ctx.fillText('AVANCE', bx, iy + 34);
    ctx.letterSpacing = '0px';
    ctx.fillStyle = rgba(P.suave);
    ctx.font = `400 17px ${JOST}`;
    ctx.fillText(`${Math.round(uc)} de ${historia.ucTotal} UC`, bx, iy + 58);
    ctx.fillStyle = rgba(P.tinta, 0.12);
    redondo(bx, iy + 70, iw - 150 - 28, 3, 1.5);
    ctx.fill();
    ctx.fillStyle = rgba(P.verde);
    redondo(bx, iy + 70, ((iw - 150 - 28) * pct) / 100, 3, 1.5);
    ctx.fill();
    ctx.restore();
  }

  function dedo(t, aPantalla) {
    const p = limita((t - (TOQUE - 0.55)) / 0.55);
    const fuera = suave(TOQUE + 0.3, TOQUE + 0.6, t);
    if (p <= 0 || fuera >= 1) return;
    const k = expoFuera(p);
    const [x, y] = aPantalla(elegida.x + NODO.ANCHO * 0.62, elegida.y + NODO.ALTO * 0.6);
    const aprieta = 1 - 0.24 * Math.exp(-Math.pow((t - TOQUE) / 0.1, 2));
    const r = 30 * aprieta;
    const cx = x + (1 - k) * 150;
    const cy = y + (1 - k) * 190;
    ctx.save();
    ctx.globalAlpha = Math.min(1, p * 3) * (1 - fuera);
    ctx.fillStyle = 'rgba(255,255,255,0.2)';
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, TAU);
    ctx.fill();
    ctx.strokeStyle = 'rgba(255,255,255,0.7)';
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.restore();
  }

  /** Dibuja el mapa en el panel `f`, abierto `abierto` (0..1), en el segundo `t` */
  return function cuadro(t, f, abierto) {
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.clearRect(0, 0, lienzo.width, lienzo.height);
    if (abierto <= 0.02) return;
    const x0 = f.cx - f.w / 2;
    const y0 = f.cy - f.h / 2;
    const cam = camara(t);
    const zoom = cam.z * (0.72 + 0.28 * abierto);
    const visible = suave(0.45, 0.95, abierto);
    const foco = suave(FOCO[0], FOCO[0] + 0.35, t) * (1 - suave(FOCO[1], FOCO[1] + 0.5, t));
    const aPantalla = (x, y) => [f.cx + (x - cam.x) * zoom, f.cy + (y - cam.y) * zoom];

    ctx.save();
    forma(ctx, f, 3);
    ctx.clip();

    ctx.save();
    ctx.globalAlpha = visible;
    ctx.translate(f.cx, f.cy);
    ctx.scale(zoom, zoom);
    ctx.translate(-cam.x, -cam.y);
    // La rejilla de puntos del lienzo: delata el movimiento de la cámara
    const paso = 56;
    const medio = [f.w / 2 / zoom, f.h / 2 / zoom];
    ctx.fillStyle = 'rgba(150,175,235,0.16)';
    const lado = Math.max(1.6, 1.3 / zoom);
    for (let gx = Math.floor((cam.x - medio[0]) / paso) * paso; gx < cam.x + medio[0]; gx += paso) {
      for (let gy = Math.floor((cam.y - medio[1]) / paso) * paso; gy < cam.y + medio[1]; gy += paso) {
        ctx.fillRect(gx - lado / 2, gy - lado / 2, lado, lado);
      }
    }
    cabeceras(t, zoom);
    const capa = ctx.globalAlpha;
    cables(t, foco);
    for (const n of plano.nodos) {
      ctx.globalAlpha = capa;
      tarjeta(n, t, zoom, foco);
    }
    ctx.restore();

    // Velos arriba y abajo para que la cabecera y los rótulos se lean sobre el mapa
    let g = ctx.createLinearGradient(0, y0, 0, y0 + 236);
    g.addColorStop(0, `rgba(6,10,22,${0.97 * visible})`);
    g.addColorStop(0.6, `rgba(6,10,22,${0.94 * visible})`);
    g.addColorStop(1, 'rgba(6,10,22,0)');
    ctx.fillStyle = g;
    ctx.fillRect(x0, y0, f.w, 236);
    // Y a los lados: el mapa no se corta contra el canto, se pierde en el vidrio
    for (const [xa, xb] of [[x0, x0 + 120], [x0 + f.w, x0 + f.w - 120]]) {
      g = ctx.createLinearGradient(xa, 0, xb, 0);
      g.addColorStop(0, `rgba(6,10,22,${0.9 * visible})`);
      g.addColorStop(1, 'rgba(6,10,22,0)');
      ctx.fillStyle = g;
      ctx.fillRect(Math.min(xa, xb), y0, 120, f.h);
    }
    g = ctx.createLinearGradient(0, y0 + f.h - 330, 0, y0 + f.h);
    g.addColorStop(0, 'rgba(6,10,22,0)');
    g.addColorStop(0.6, `rgba(6,10,22,${0.8 * visible})`);
    g.addColorStop(1, `rgba(6,10,22,${0.95 * visible})`);
    ctx.fillStyle = g;
    ctx.fillRect(x0, y0 + f.h - 330, f.w, 330);

    dedo(t, aPantalla);
    cabecera(t, f, visible);
    rotulos(t, f, visible);
    ctx.restore();
  };
}
