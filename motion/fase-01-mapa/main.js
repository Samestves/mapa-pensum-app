import { CUADROS, FPS, limita, mezcla, suave } from '../comun/tiempo.js';
import { PLACA, crearFondo, pintarGrano } from '../comun/fondo.js';
import { crearLogo } from '../comun/logo.js';
import { calcularPlano } from './plano.js';
import { apertura, crearHistoria, viajeLogo } from './guion.js';
import { crearDibujo } from './dibujo.js';

const LADO_LOGO = 900;
/** El panel abierto y dónde queda el logo en su cabecera */
const PANEL = { cx: 960, cy: 640, w: 1716, h: 1092, r: 76 };
const LOGO_CABECERA = { x: PANEL.cx - PANEL.w / 2 + 92, y: PANEL.cy - PANEL.h / 2 + 76, escala: 0.155 };

// Las letras de la app y el pensum de verdad
const fuentes = [
  new FontFace('Jost', 'url(/fuentes/jost.ttf)', { weight: '100 900' }),
  new FontFace('Plex', 'url(/fuentes/plex_mono_light.ttf)', { weight: '300 400' }),
];
await Promise.all(fuentes.map((f) => f.load().then((c) => document.fonts.add(c))));
const pensum = await (await fetch('/carreras/ingenieria-de-sistemas.json')).json();
const plano = calcularPlano(pensum);
plano.ucObligatorias = pensum.ucObligatorias;
const historia = crearHistoria(plano);

const lienzoLogo = document.getElementById('logo');
lienzoLogo.style.left = `${PLACA.cx - LADO_LOGO / 2}px`;
lienzoLogo.style.top = `${PLACA.cy - LADO_LOGO / 2}px`;
const logo = crearLogo(lienzoLogo, LADO_LOGO);
const fondo = crearFondo(document.getElementById('fondo'));
const dibujo = crearDibujo(document.getElementById('mapa'), plano, historia, pensum);
pintarGrano(document.getElementById('grano'));

window.cuadro = (n) => {
  const c = ((n % CUADROS) + CUADROS) % CUADROS;
  const u = c / CUADROS;
  const t = c / FPS;
  const e = apertura(t);
  const abierto = limita(e);
  const v = viajeLogo(t);

  const { metaX, metaY, sube } = logo.cuadro(u);
  // El logo viaja de la placa a la cabecera del panel, encogiéndose
  const escala = mezcla(1, LOGO_CABECERA.escala, v);
  const dx = mezcla(0, LOGO_CABECERA.x - PLACA.cx, v);
  const dy = mezcla(0, LOGO_CABECERA.y - PLACA.cy, v);
  lienzoLogo.style.transform = `translate(${dx}px, ${dy}px) scale(${escala})`;
  const origen = {
    x: PLACA.cx + dx + (metaX - 0.5) * LADO_LOGO * escala,
    y: PLACA.cy + dy + (metaY - 0.5) * LADO_LOGO * escala,
  };

  const f = {
    cx: PANEL.cx, cy: PANEL.cy,
    w: mezcla(PLACA.w, PANEL.w, e), h: mezcla(PLACA.h, PANEL.h, e), r: mezcla(PLACA.r, PANEL.r, abierto),
  };
  fondo(u, origen, { ...f, sube: sube * (1 - abierto), apoyo: 1 - suave(0, 0.3, abierto), oscuro: abierto });
  dibujo(t, f, abierto);
};
window.CUADROS = CUADROS;
window.listo = true;

if (!new URLSearchParams(location.search).has('quieto')) {
  const t0 = performance.now();
  const vuelta = (ahora) => {
    window.cuadro(Math.floor(((ahora - t0) / 1000) * FPS));
    requestAnimationFrame(vuelta);
  };
  requestAnimationFrame(vuelta);
} else {
  window.cuadro(0);
}
