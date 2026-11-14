/*
 * El fondo: una cuadrícula de mosaicos de vidrio, como el historial de
 * contribuciones de GitHub pero en el azul de la marca, y encima la placa de
 * vidrio esmerilado donde se posa el logo. Todo en un lienzo 2D.
 */
import { ALTO, ANCHO, TAU, limita, mezcla, onda, pulso, suave } from './tiempo.js';

const CELDA = 40;
const HUECO = 9;
const PASO = CELDA + HUECO;
const RADIO = 9;
const COLS = Math.ceil(ANCHO / PASO) + 1;
const FILAS = Math.ceil(ALTO / PASO) + 1;
const X0 = (ANCHO - (COLS * PASO - HUECO)) / 2;
const Y0 = (ALTO - (FILAS * PASO - HUECO)) / 2;

/** La placa del centro */
export const PLACA = { cx: ANCHO / 2, cy: ALTO / 2, lado: 640 };

/** Los cinco niveles, de vacío a lleno: r, g, b, alfa */
const NIVELES = [
  [150, 175, 255, 0.05],
  [38, 66, 150, 0.5],
  [37, 75, 190, 0.78],
  [79, 132, 245, 0.92],
  [150, 188, 255, 1],
];

/** Un azar fijo por celda: siempre el mismo, para que el bucle cierre */
function azar(i, j, s) {
  const x = Math.sin(i * 127.1 + j * 311.7 + s * 74.7) * 43758.5453;
  return x - Math.floor(x);
}

const celdas = [];
for (let j = 0; j < FILAS; j++) {
  for (let i = 0; i < COLS; i++) {
    celdas.push({
      x: X0 + i * PASO,
      y: Y0 + j * PASO,
      fase: azar(i, j, 1) * TAU,
      vueltas: (1 + Math.floor(azar(i, j, 2) * 3)) * (azar(i, j, 3) < 0.5 ? -1 : 1),
      peso: azar(i, j, 4),
      fase2: azar(i, j, 5) * TAU,
    });
  }
}

function lienzo(w, h) {
  const c = document.createElement('canvas');
  c.width = w;
  c.height = h;
  return c;
}

function rutaCelda(ctx, x, y, lado, r) {
  ctx.beginPath();
  ctx.roundRect(x, y, lado, lado, r);
}

/** El brillo de vidrio de un mosaico: luz arriba y un filo fino. Se dibuja una vez */
const brilloCelda = (() => {
  const c = lienzo(CELDA, CELDA);
  const ctx = c.getContext('2d');
  rutaCelda(ctx, 0, 0, CELDA, RADIO);
  ctx.clip();
  let g = ctx.createLinearGradient(0, 0, 0, CELDA);
  g.addColorStop(0, 'rgba(255,255,255,0.30)');
  g.addColorStop(0.45, 'rgba(255,255,255,0.04)');
  g.addColorStop(1, 'rgba(0,10,40,0.16)');
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, CELDA, CELDA);
  g = ctx.createLinearGradient(0, 0, CELDA, CELDA);
  g.addColorStop(0, 'rgba(255,255,255,0.75)');
  g.addColorStop(0.5, 'rgba(255,255,255,0.10)');
  g.addColorStop(1, 'rgba(255,255,255,0.28)');
  ctx.strokeStyle = g;
  ctx.lineWidth = 2;
  rutaCelda(ctx, 0, 0, CELDA, RADIO);
  ctx.stroke();
  return c;
})();

/** El halo de un mosaico lleno */
const HALO = 150;
const haloCelda = (() => {
  const c = lienzo(HALO, HALO);
  const ctx = c.getContext('2d');
  const g = ctx.createRadialGradient(HALO / 2, HALO / 2, 8, HALO / 2, HALO / 2, HALO / 2);
  g.addColorStop(0, 'rgba(90,140,255,0.55)');
  g.addColorStop(0.4, 'rgba(70,120,255,0.16)');
  g.addColorStop(1, 'rgba(60,110,255,0)');
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, HALO, HALO);
  return c;
})();

/** La superelipse del icono de la app (exponente 4) */
function squircle(ctx, cx, cy, lado) {
  const a = lado / 2;
  ctx.beginPath();
  for (let i = 0; i <= 160; i++) {
    const t = (TAU * i) / 160;
    const c = Math.cos(t);
    const s = Math.sin(t);
    const x = cx + a * Math.sign(c) * Math.pow(Math.abs(c), 0.5);
    const y = cy + a * Math.sign(s) * Math.pow(Math.abs(s), 0.5);
    if (i) ctx.lineTo(x, y);
    else ctx.moveTo(x, y);
  }
  ctx.closePath();
}

export function crearFondo(destino) {
  const ctx = destino.getContext('2d');
  const capa = lienzo(ANCHO, ALTO);
  const cc = capa.getContext('2d');

  function manchas(u) {
    let g = cc.createLinearGradient(0, 0, 0, ALTO);
    g.addColorStop(0, '#111a33');
    g.addColorStop(0.5, '#0a1022');
    g.addColorStop(1, '#05070f');
    cc.fillStyle = g;
    cc.fillRect(0, 0, ANCHO, ALTO);
    // Tres luces grandes que se pasean despacio por detrás del vidrio
    const luces = [
      [0.26 + 0.14 * onda(u, 1, 0.3), 0.30 + 0.12 * onda(u, 1, 1.9), 900, '37,75,190', 0.5],
      [0.76 + 0.12 * onda(u, -1, 2.2), 0.66 + 0.14 * onda(u, 1, 0.2), 980, '60,110,245', 0.36],
      [0.52 + 0.20 * onda(u, 1, 4.0), 0.92 + 0.08 * onda(u, 2, 1.0), 760, '92,70,215', 0.26],
    ];
    cc.globalCompositeOperation = 'lighter';
    for (const [fx, fy, r, color, a] of luces) {
      g = cc.createRadialGradient(fx * ANCHO, fy * ALTO, 0, fx * ANCHO, fy * ALTO, r);
      g.addColorStop(0, `rgba(${color},${a})`);
      g.addColorStop(0.5, `rgba(${color},${a * 0.32})`);
      g.addColorStop(1, `rgba(${color},0)`);
      cc.fillStyle = g;
      cc.fillRect(0, 0, ANCHO, ALTO);
    }
    cc.globalCompositeOperation = 'source-over';
  }

  /** Cuánto vale la celda en este cuadro, de 0 a 4 */
  function nivel(c, u, origen, desdeLlegada) {
    const cx = c.x + CELDA / 2;
    const cy = c.y + CELDA / 2;
    // Dos frentes anchos que cruzan la cuadrícula y el latido propio de la celda
    const frente =
      0.5 * onda(u, 1, cx * 0.0042 + cy * 0.0021) +
      0.3 * onda(u, -2, cx * 0.0026 - cy * 0.0047 + 1.3) +
      0.2 * onda(u, 1, Math.hypot(cx - ANCHO / 2, cy - ALTO / 2) * 0.006);
    const latido = onda(u, c.vueltas, c.fase) * 0.6 + onda(u, c.vueltas * 2, c.fase2) * 0.4;
    let v = 0.31 + 0.27 * frente + (0.14 + 0.36 * c.peso) * latido;
    v = Math.pow(limita(v), 1.35);
    // La onda que sale de la meta cada vez que llega el pulso
    const d = Math.hypot(cx - origen.x, cy - origen.y);
    const radio = 70 + desdeLlegada * 470;
    const anillo = Math.exp(-Math.pow((d - radio) / 95, 2));
    const fuerza = Math.exp(-desdeLlegada * 0.55) * suave(0, 0.15, desdeLlegada);
    v += 0.85 * anillo * fuerza;
    // Escalones suaves: la celda descansa en un nivel y pasa al siguiente sin saltar
    const n = limita(v) * 4;
    const base = Math.floor(n);
    return { n: Math.min(4, base + suave(0.3, 0.7, n - base)), golpe: anillo * fuerza };
  }

  function cuadricula(u, origen) {
    const { desdeLlegada } = pulso(u);
    const estados = celdas.map((c) => nivel(c, u, origen, desdeLlegada));
    // Primero los halos, para que queden por debajo de todos los mosaicos
    cc.globalCompositeOperation = 'lighter';
    celdas.forEach((c, k) => {
      const a = suave(2.4, 4, estados[k].n);
      if (a <= 0.01) return;
      cc.globalAlpha = a * 0.9;
      cc.drawImage(haloCelda, c.x + CELDA / 2 - HALO / 2, c.y + CELDA / 2 - HALO / 2);
    });
    cc.globalCompositeOperation = 'source-over';
    celdas.forEach((c, k) => {
      const { n, golpe } = estados[k];
      const i = Math.min(3, Math.floor(n));
      const t = n - i;
      const a = NIVELES[i];
      const b = NIVELES[i + 1];
      const lado = CELDA * (0.9 + 0.1 * (n / 4) + 0.06 * golpe);
      const o = (CELDA - lado) / 2;
      cc.globalAlpha = 1;
      cc.fillStyle = `rgba(${mezcla(a[0], b[0], t) | 0},${mezcla(a[1], b[1], t) | 0},${mezcla(a[2], b[2], t) | 0},${mezcla(a[3], b[3], t)})`;
      rutaCelda(cc, c.x + o, c.y + o, lado, RADIO * (lado / CELDA));
      cc.fill();
      cc.globalAlpha = 0.22 + 0.78 * (n / 4);
      cc.drawImage(brilloCelda, c.x + o, c.y + o, lado, lado);
    });
    cc.globalAlpha = 1;
    // Las esquinas se apagan: el ojo se queda en el centro
    const g = cc.createRadialGradient(ANCHO / 2, ALTO / 2, ALTO * 0.3, ANCHO / 2, ALTO / 2, ANCHO * 0.66);
    g.addColorStop(0, 'rgba(3,5,12,0)');
    g.addColorStop(1, 'rgba(3,5,12,0.72)');
    cc.fillStyle = g;
    cc.fillRect(0, 0, ANCHO, ALTO);
  }

  function placa(u, sube) {
    const { desdeLlegada } = pulso(u);
    const { cx, lado } = PLACA;
    const cy = PLACA.cy - sube * 0.25;
    const llegada = Math.exp(-desdeLlegada * 1.6);

    // La sombra con que la placa se despega de la cuadrícula
    ctx.save();
    ctx.shadowColor = 'rgba(1,3,12,0.62)';
    ctx.shadowBlur = 110;
    ctx.shadowOffsetY = 44;
    ctx.fillStyle = '#060913';
    squircle(ctx, cx, cy, lado - 8);
    ctx.fill();
    ctx.restore();

    ctx.save();
    squircle(ctx, cx, cy, lado);
    ctx.clip();
    // Lo de atrás, desenfocado y un poco más grande: el vidrio esmerilado
    ctx.filter = 'blur(15px) saturate(160%) brightness(0.95)';
    const z = 1.06;
    ctx.drawImage(capa, cx - cx * z, cy - cy * z, ANCHO * z, ALTO * z);
    ctx.filter = 'none';
    // El tinte: más claro arriba, azul noche abajo
    let g = ctx.createLinearGradient(0, cy - lado / 2, 0, cy + lado / 2);
    g.addColorStop(0, 'rgba(130,160,240,0.14)');
    g.addColorStop(0.4, 'rgba(18,27,62,0.38)');
    g.addColorStop(1, 'rgba(5,8,20,0.6)');
    ctx.fillStyle = g;
    ctx.fillRect(cx - lado / 2, cy - lado / 2, lado, lado);
    // Luz que entra por la esquina de arriba a la izquierda
    g = ctx.createRadialGradient(cx - lado * 0.36, cy - lado * 0.44, 0, cx - lado * 0.36, cy - lado * 0.44, lado * 0.8);
    g.addColorStop(0, 'rgba(255,255,255,0.20)');
    g.addColorStop(1, 'rgba(255,255,255,0)');
    ctx.fillStyle = g;
    ctx.fillRect(cx - lado / 2, cy - lado / 2, lado, lado);
    // Un reflejo en diagonal que cruza la placa una vez por bucle
    const s = ((u + 0.35) % 1) * 2.6 - 0.8;
    const bx = cx - lado / 2 + s * lado;
    g = ctx.createLinearGradient(bx - 190, cy - 100, bx + 190, cy + 100);
    g.addColorStop(0, 'rgba(255,255,255,0)');
    g.addColorStop(0.5, 'rgba(225,236,255,0.13)');
    g.addColorStop(1, 'rgba(255,255,255,0)');
    ctx.fillStyle = g;
    ctx.fillRect(cx - lado / 2, cy - lado / 2, lado, lado);
    // El resplandor azul bajo el mapa y su sombra de apoyo
    const py = cy + lado * 0.335;
    ctx.save();
    ctx.translate(cx, py);
    ctx.scale(1, 0.24);
    g = ctx.createRadialGradient(0, 0, 0, 0, 0, lado * 0.43);
    g.addColorStop(0, `rgba(70,120,255,${0.5 + 0.22 * llegada})`);
    g.addColorStop(0.5, `rgba(50,95,235,${0.2 + 0.1 * llegada})`);
    g.addColorStop(1, 'rgba(40,80,220,0)');
    ctx.fillStyle = g;
    ctx.beginPath();
    ctx.arc(0, 0, lado * 0.43, 0, TAU);
    ctx.fill();
    const rs = lado * (0.3 - sube * 0.0012);
    g = ctx.createRadialGradient(0, -30, 0, 0, -30, rs);
    g.addColorStop(0, `rgba(2,4,14,${0.5 - sube * 0.006})`);
    g.addColorStop(1, 'rgba(2,4,14,0)');
    ctx.fillStyle = g;
    ctx.beginPath();
    ctx.arc(0, -30, rs, 0, TAU);
    ctx.fill();
    ctx.restore();
    ctx.restore();

    // El canto: recoge luz arriba y un poco abajo
    g = ctx.createLinearGradient(cx - lado * 0.2, cy - lado / 2, cx + lado * 0.2, cy + lado / 2);
    g.addColorStop(0, 'rgba(255,255,255,0.78)');
    g.addColorStop(0.22, 'rgba(210,225,255,0.22)');
    g.addColorStop(0.6, 'rgba(160,185,255,0.07)');
    g.addColorStop(1, 'rgba(150,185,255,0.34)');
    ctx.strokeStyle = g;
    ctx.lineWidth = 2;
    squircle(ctx, cx, cy, lado - 2);
    ctx.stroke();
    ctx.strokeStyle = 'rgba(255,255,255,0.05)';
    ctx.lineWidth = 1;
    squircle(ctx, cx, cy, lado - 12);
    ctx.stroke();
  }

  /** `origen` es dónde cae la meta del mapa en pantalla: de ahí sale la onda */
  return function cuadro(u, origen, sube) {
    manchas(u);
    cuadricula(u, origen);
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.drawImage(capa, 0, 0);
    placa(u, sube);
  };
}

/** Un grano fijo y muy leve: quita las bandas de los degradados oscuros en la pantalla LED */
export function pintarGrano(destino) {
  const ctx = destino.getContext('2d');
  const img = ctx.createImageData(ANCHO, ALTO);
  let s = 1234567;
  for (let i = 0; i < img.data.length; i += 4) {
    s = (s * 1664525 + 1013904223) >>> 0;
    const v = s >>> 24;
    img.data[i] = img.data[i + 1] = img.data[i + 2] = v;
    img.data[i + 3] = 7;
  }
  ctx.putImageData(img, 0, 0);
}
