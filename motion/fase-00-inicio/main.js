import { CUADROS, FPS } from '../comun/tiempo.js';
import { PLACA, crearFondo, pintarGrano } from '../comun/fondo.js';
import { crearLogo } from '../comun/logo.js';

const LADO_LOGO = 900;
const lienzoLogo = document.getElementById('logo');
lienzoLogo.style.left = `${PLACA.cx - LADO_LOGO / 2}px`;
lienzoLogo.style.top = `${PLACA.cy - LADO_LOGO / 2}px`;

const logo = crearLogo(lienzoLogo, LADO_LOGO);
const fondo = crearFondo(document.getElementById('fondo'));
pintarGrano(document.getElementById('grano'));

/** Dibuja el cuadro `f` del bucle. El render lo llama cuadro a cuadro */
window.cuadro = (f) => {
  const u = (((f % CUADROS) + CUADROS) % CUADROS) / CUADROS;
  const { metaX, metaY, sube } = logo.cuadro(u);
  const origen = { x: PLACA.cx - LADO_LOGO / 2 + metaX * LADO_LOGO, y: PLACA.cy - LADO_LOGO / 2 + metaY * LADO_LOGO };
  fondo(u, origen, { ...PLACA, sube });
};
window.CUADROS = CUADROS;
window.listo = true;

// Abierto en el navegador (sin ?quieto) se reproduce solo, para verlo
if (!new URLSearchParams(location.search).has('quieto')) {
  const t0 = performance.now();
  const vuelta = (t) => {
    window.cuadro(Math.floor(((t - t0) / 1000) * FPS));
    requestAnimationFrame(vuelta);
  };
  requestAnimationFrame(vuelta);
} else {
  window.cuadro(0);
}
