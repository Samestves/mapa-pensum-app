/*
 * IMCULMAT · bucle de 20 s para la pantalla LED (1920 × 1280, 60 fps).
 * El logo en el centro, separado en capas (grecas, ola, texto) y con las diez
 * estrellas dibujadas aparte; detrás, tres luces en los colores de la bandera
 * que se pasean despacio, un polvo dorado que sube y el reflejo del logo en
 * el suelo. Todo es periódico en el bucle: el último cuadro empalma con el primero.
 */
import { ALTO, ANCHO, CUADROS, FPS, TAU, limita, onda, suave } from '../comun/tiempo.js';
import { pintarGrano } from '../comun/fondo.js';

const cargar = (n) => new Promise((ok) => { const i = new Image(); i.onload = () => ok(i); i.src = `./capas/${n}.png`; });
const [roja, azul, ola, texto] = await Promise.all(['roja', 'azul', 'ola', 'texto'].map(cargar));
const datos = await (await fetch('./capas/logo.json')).json();

const ctx = document.getElementById('lienzo').getContext('2d');
pintarGrano(document.getElementById('grano'));

const ESC = 0.54;
const LW = datos.ancho * ESC;
const LH = datos.alto * ESC;
const LX = (ANCHO - LW) / 2;
const LY = (ALTO - LH) / 2 - 30;

// Una capa de trabajo para el brillo que cruza el texto
const capa = document.createElement('canvas');
capa.width = LW; capa.height = LH;
const cc = capa.getContext('2d');

// El polvo: cada mota sube un número entero de pantallas por bucle, así cierra
const motas = Array.from({ length: 70 }, (_, i) => {
  const r = (k) => { const x = Math.sin(i * 91.7 + k * 37.3) * 43758.5453; return x - Math.floor(x); };
  return { x: r(1) * ANCHO, y: r(2) * ALTO, v: 1 + Math.floor(r(3) * 2), t: 0.6 + r(4) * 1.8, a: 0.15 + r(5) * 0.45, f: r(6) * TAU, d: r(7) * 30 };
});

function fondo(u) {
  let g = ctx.createLinearGradient(0, 0, 0, ALTO);
  g.addColorStop(0, '#0d0f17'); g.addColorStop(0.55, '#08090e'); g.addColorStop(1, '#040407');
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, ANCHO, ALTO);
  ctx.globalCompositeOperation = 'lighter';
  const luces = [
    [0.22 + 0.08 * onda(u, 1, 0.4), 0.36 + 0.1 * onda(u, 1, 2.0), 820, '206,42,54', 0.2],
    [0.52 + 0.1 * onda(u, -1, 1.1), 0.22 + 0.06 * onda(u, 2, 0.3), 760, '246,190,40', 0.14],
    [0.8 + 0.07 * onda(u, 1, 3.2), 0.62 + 0.1 * onda(u, -1, 0.9), 900, '34,104,182', 0.24],
  ];
  for (const [fx, fy, r, c, a] of luces) {
    g = ctx.createRadialGradient(fx * ANCHO, fy * ALTO, 0, fx * ANCHO, fy * ALTO, r);
    g.addColorStop(0, `rgba(${c},${a})`); g.addColorStop(0.5, `rgba(${c},${a * 0.3})`); g.addColorStop(1, `rgba(${c},0)`);
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, ANCHO, ALTO);
  }
  // El halo cálido detrás del logo
  g = ctx.createRadialGradient(ANCHO / 2, LY + LH / 2, 0, ANCHO / 2, LY + LH / 2, 760);
  g.addColorStop(0, `rgba(255,236,200,${0.07 + 0.015 * onda(u, 2)})`); g.addColorStop(1, 'rgba(255,236,200,0)');
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, ANCHO, ALTO);
  // Polvo dorado
  for (const m of motas) {
    const y = (((m.y - u * m.v * ALTO) % ALTO) + ALTO) % ALTO;
    const x = m.x + m.d * onda(u, 2, m.f);
    const a = m.a * (0.6 + 0.4 * onda(u, 3, m.f)) * suave(0, 120, y) * (1 - suave(ALTO - 120, ALTO, y));
    ctx.fillStyle = `rgba(255,222,150,${a})`;
    ctx.beginPath(); ctx.arc(x, y, m.t, 0, TAU); ctx.fill();
  }
  ctx.globalCompositeOperation = 'source-over';
  // Viñeta
  g = ctx.createRadialGradient(ANCHO / 2, ALTO / 2, ALTO * 0.35, ANCHO / 2, ALTO / 2, ANCHO * 0.7);
  g.addColorStop(0, 'rgba(0,0,0,0)'); g.addColorStop(1, 'rgba(0,0,0,0.6)');
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, ANCHO, ALTO);
}

function estrella(x, y, r, color) {
  ctx.beginPath();
  for (let i = 0; i < 10; i++) {
    const a = -Math.PI / 2 + (i * Math.PI) / 5;
    const rr = i % 2 ? r * 0.45 : r;
    ctx.lineTo(x + rr * Math.cos(a), y + rr * Math.sin(a));
  }
  ctx.closePath();
  ctx.fillStyle = color;
  ctx.fill();
}

/** El logo entero en (0,0) a escala de pantalla; `reflejo` lo pinta sin efectos */
function logo(u, t, reflejo = false) {
  // Las grecas laten por turnos con un resplandor de su color
  const piezas = [[roja, '206,42,54', 0], [ola, '246,212,44', 0.12], [azul, '34,104,182', 0.24]];
  for (const [img, c, d] of piezas) {
    const k = Math.exp(-Math.pow(((t + 20 - 1.2 - d * 5) % 5) - 0.6, 2) / 0.18);
    if (!reflejo) { ctx.shadowColor = `rgba(${c},${0.35 + 0.45 * k})`; ctx.shadowBlur = 18 + 30 * k; }
    ctx.drawImage(img, 0, 0, LW, LH);
  }
  ctx.shadowBlur = 0;
  // El texto, con un brillo que lo cruza cada 10 s
  cc.globalCompositeOperation = 'source-over';
  cc.clearRect(0, 0, LW, LH);
  cc.drawImage(texto, 0, 0, LW, LH);
  if (!reflejo) {
    const p = (((t - 3) % 10) + 10) % 10 / 1.6;
    if (p < 1) {
      const x = -300 + p * (LW + 600);
      const g = cc.createLinearGradient(x - 160, 0, x + 160, LH);
      g.addColorStop(0, 'rgba(255,255,255,0)'); g.addColorStop(0.5, 'rgba(255,214,120,0.95)'); g.addColorStop(1, 'rgba(255,255,255,0)');
      cc.globalCompositeOperation = 'source-atop';
      cc.fillStyle = g;
      cc.fillRect(0, 0, LW, LH);
    }
  }
  ctx.drawImage(capa, 0, 0);
  // Las diez estrellas: una ola de luz las recorre de izquierda a derecha cada 5 s
  datos.estrellas.forEach(([sx, sy], i) => {
    const k = Math.exp(-Math.pow(((t % 5) - (0.4 + i * 0.13)) / 0.16, 2));
    const r = 19 * ESC * 1.9 * (1 + 0.28 * k);
    const x = sx * ESC;
    const y = sy * ESC;
    if (!reflejo && k > 0.02) {
      const g = ctx.createRadialGradient(x, y, 0, x, y, r * 3.2);
      g.addColorStop(0, `rgba(255,230,120,${0.55 * k})`); g.addColorStop(1, 'rgba(255,230,120,0)');
      ctx.fillStyle = g;
      ctx.beginPath(); ctx.arc(x, y, r * 3.2, 0, TAU); ctx.fill();
    }
    estrella(x, y, r, `rgb(${246},${Math.round(212 + 30 * k)},${Math.round(44 + 160 * k)})`);
  });
}

window.cuadro = (n) => {
  const c = ((n % CUADROS) + CUADROS) % CUADROS;
  const u = c / CUADROS;
  const t = c / FPS;
  ctx.setTransform(1, 0, 0, 1, 0, 0);
  fondo(u);
  const flota = 7 * onda(u, 2, 0.5);
  const esc = 1 + 0.008 * onda(u, 1, 1.4);
  // El reflejo en el suelo, cada vez más tenue
  const piso = LY + LH + 70;
  ctx.save();
  ctx.translate(ANCHO / 2, piso - flota * 0.5);
  ctx.scale(esc, -esc * 0.9);
  ctx.translate(-LW / 2, -LH);
  ctx.globalAlpha = 0.1;
  logo(u, t, true);
  ctx.restore();
  let g = ctx.createLinearGradient(0, piso, 0, piso + LH * 0.75);
  g.addColorStop(0, 'rgba(6,7,11,0.15)'); g.addColorStop(1, 'rgba(6,7,11,1)');
  ctx.fillStyle = g;
  ctx.fillRect(0, piso, ANCHO, ALTO - piso);
  // La línea del suelo, con una luz que la recorre
  const lx = ANCHO / 2 + (LW * 0.55) * onda(u, 1);
  g = ctx.createLinearGradient(ANCHO / 2 - LW * 0.6, 0, ANCHO / 2 + LW * 0.6, 0);
  g.addColorStop(0, 'rgba(255,230,180,0)'); g.addColorStop(0.5, 'rgba(255,230,180,0.16)'); g.addColorStop(1, 'rgba(255,230,180,0)');
  ctx.fillStyle = g;
  ctx.fillRect(ANCHO / 2 - LW * 0.6, piso, LW * 1.2, 1.5);
  g = ctx.createRadialGradient(lx, piso, 0, lx, piso, 160);
  g.addColorStop(0, 'rgba(255,226,160,0.5)'); g.addColorStop(1, 'rgba(255,226,160,0)');
  ctx.fillStyle = g;
  ctx.fillRect(lx - 160, piso - 2, 320, 4);
  // El logo
  ctx.save();
  ctx.translate(ANCHO / 2, LY + LH / 2 + flota);
  ctx.scale(esc, esc);
  ctx.translate(-LW / 2, -LH / 2);
  logo(u, t);
  ctx.restore();
};
window.CUADROS = CUADROS;
window.listo = true;

if (!new URLSearchParams(location.search).has('quieto')) {
  const t0 = performance.now();
  const vuelta = (a) => { window.cuadro(Math.floor(((a - t0) / 1000) * FPS)); requestAnimationFrame(vuelta); };
  requestAnimationFrame(vuelta);
} else window.cuadro(0);
