/*
 * El logo en 3D: el mapa de papel plegado en tres paneles, con la ruta azul
 * pintada en una textura que se redibuja en cada cuadro (el pulso que viaja y
 * la meta que late). Las medidas salen del PNG de la marca
 * (docs/marca/logo-app-2026-11.png), desplegadas: cada panel mide 225 × 556.
 */
import * as THREE from 'three';
import { CICLO, TAU, limita, mezcla, onda, pulso, suave } from './tiempo.js';

const PANEL = 225;
const ALTO_MAPA = 556;
const ANCHO_MAPA = PANEL * 3;
const ESCALA_TEX = 2;

// La ruta sobre el mapa desplegado (x a la derecha, y hacia abajo)
const RUTA = [[100, 459], [225, 352], [338, 287], [450, 226], [577, 125]];
const SALIDA = RUTA[0];
const MEDIO = RUTA[2];
const META = RUTA[4];

const TRAMOS = RUTA.slice(1).map((p, i) => Math.hypot(p[0] - RUTA[i][0], p[1] - RUTA[i][1]));
const LARGO = TRAMOS.reduce((a, b) => a + b, 0);
/** A qué fracción de la ruta cae el punto del medio */
const FRAC_MEDIO = (TRAMOS[0] + TRAMOS[1]) / LARGO;

function puntoEnRuta(f) {
  let d = limita(f) * LARGO;
  for (let i = 0; i < TRAMOS.length; i++) {
    if (d <= TRAMOS[i] || i === TRAMOS.length - 1) {
      const t = limita(d / TRAMOS[i]);
      return [mezcla(RUTA[i][0], RUTA[i + 1][0], t), mezcla(RUTA[i][1], RUTA[i + 1][1], t)];
    }
    d -= TRAMOS[i];
  }
  return META;
}

export function crearLogo(lienzo, lado) {
  const render = new THREE.WebGLRenderer({ canvas: lienzo, antialias: true, alpha: true, preserveDrawingBuffer: true });
  render.setPixelRatio(2);
  render.setSize(lado, lado, false);
  lienzo.style.width = lienzo.style.height = `${lado}px`;
  render.toneMapping = THREE.NoToneMapping;
  render.setClearColor(0x000000, 0);

  const escena = new THREE.Scene();
  const camara = new THREE.PerspectiveCamera(16, 1, 100, 20000);
  const DIST = 4750;
  const PICADO = THREE.MathUtils.degToRad(25);
  camara.position.set(0, DIST * Math.sin(PICADO), DIST * Math.cos(PICADO));
  camara.lookAt(0, 0, 0);

  // La luz viene de la derecha: los paneles de los lados quedan blancos y el del centro en sombra
  escena.add(new THREE.AmbientLight(0xeceff6, 1.72));
  const sol = new THREE.DirectionalLight(0xffffff, 1.85);
  sol.position.set(1.0, 0.85, 1.25);
  escena.add(sol);
  // El resplandor azul de abajo, como en el icono
  const rebote = new THREE.PointLight(0x3f6dff, 1.3, 0, 0);
  rebote.position.set(0, -520, 420);
  escena.add(rebote);

  // La textura del mapa desplegado
  const papel = document.createElement('canvas');
  papel.width = ANCHO_MAPA * ESCALA_TEX;
  papel.height = ALTO_MAPA * ESCALA_TEX;
  const ctx = papel.getContext('2d');
  const textura = new THREE.CanvasTexture(papel);
  textura.colorSpace = THREE.SRGBColorSpace;
  textura.anisotropy = 8;
  const material = new THREE.MeshStandardMaterial({ map: textura, roughness: 0.82, metalness: 0 });
  const canto = new THREE.MeshStandardMaterial({ color: 0xe9edf5, roughness: 0.6 });

  const mapa = new THREE.Group();
  const paneles = [0, 1, 2].map((i) => {
    const g = new THREE.Group();
    const malla = new THREE.Mesh(geometriaPanel(i), [material, canto]);
    g.add(malla);
    mapa.add(g);
    return g;
  });
  const flota = new THREE.Group();
  flota.add(mapa);
  escena.add(flota);

  const metaLocal = new THREE.Vector3();

  function plegar(angulo) {
    // Zigzag: sube, baja, sube. Cada panel gira sobre su pliegue de la izquierda
    const giros = [angulo, -angulo, angulo];
    let x = 0;
    let z = 0;
    const esquinas = [];
    giros.forEach((g, i) => {
      paneles[i].position.set(x, 0, z);
      paneles[i].rotation.y = g;
      esquinas.push([x, z]);
      x += PANEL * Math.cos(g);
      z -= PANEL * Math.sin(g);
    });
    // Centrado: la mitad entre los extremos
    const cx = x / 2;
    const cz = (Math.min(...esquinas.map((e) => e[1]), z) + Math.max(...esquinas.map((e) => e[1]), z)) / 2;
    mapa.position.set(-cx, 0, -cz);
    // La meta está en el tercer panel
    const lx = META[0] - PANEL * 2;
    const p = paneles[2].position;
    metaLocal.set(p.x + lx * Math.cos(giros[2]) - cx, ALTO_MAPA / 2 - META[1], p.z - lx * Math.sin(giros[2]) - cz);
  }

  function pintar(u) {
    const e = ESCALA_TEX;
    const { avance, viajando, desdeLlegada } = pulso(u);
    ctx.setTransform(e, 0, 0, e, 0, 0);

    // El papel: blanco que cae un poco hacia abajo
    let g = ctx.createLinearGradient(0, 0, 0, ALTO_MAPA);
    g.addColorStop(0, '#ffffff');
    g.addColorStop(1, '#eef1f6');
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, ANCHO_MAPA, ALTO_MAPA);
    // El panel del centro se oscurece hacia el segundo pliegue; el derecho sale gris del pliegue
    g = ctx.createLinearGradient(PANEL, 0, PANEL * 2, 0);
    g.addColorStop(0, 'rgba(120,132,160,0.02)');
    g.addColorStop(1, 'rgba(120,132,160,0.20)');
    ctx.fillStyle = g;
    ctx.fillRect(PANEL, 0, PANEL, ALTO_MAPA);
    g = ctx.createLinearGradient(PANEL * 2, 0, PANEL * 2 + 80, 0);
    g.addColorStop(0, 'rgba(120,132,160,0.30)');
    g.addColorStop(1, 'rgba(120,132,160,0)');
    ctx.fillStyle = g;
    ctx.fillRect(PANEL * 2, 0, 80, ALTO_MAPA);

    // La ruta
    g = ctx.createLinearGradient(SALIDA[0], SALIDA[1], META[0], META[1]);
    g.addColorStop(0, '#1e40a6');
    g.addColorStop(0.5, '#254bbe');
    g.addColorStop(1, '#4f84f5');
    ctx.lineCap = ctx.lineJoin = 'round';
    ctx.lineWidth = 25;
    ctx.strokeStyle = g;
    ctx.beginPath();
    RUTA.forEach(([x, y], i) => (i ? ctx.lineTo(x, y) : ctx.moveTo(x, y)));
    ctx.stroke();

    // Cada punto se hincha un poco cuando el pulso pasa por él
    const paso = (f) => Math.exp(-Math.pow((avance - f) / 0.07, 2)) * (viajando ? 1 : 0);
    const sale = Math.exp(-Math.pow((desdeLlegada - (CICLO - 2.1)) / 0.22, 2));
    const llegada = Math.exp(-desdeLlegada * 2.2);

    disco(SALIDA, 37 * (1 + 0.1 * sale), '#1e40a6');
    disco(MEDIO, 41 * (1 + 0.1 * paso(FRAC_MEDIO)), '#254bbe');

    // El pulso: una estela clara que corre por la ruta
    if (viajando) {
      const vis = suave(0, 0.06, avance) * (1 - suave(0.93, 1, avance));
      const cola = 0.2;
      const pasos = 26;
      for (let i = 0; i < pasos; i++) {
        const a = avance - cola * (1 - i / pasos);
        const b = avance - cola * (1 - (i + 1) / pasos);
        if (b <= 0) continue;
        const [x0, y0] = puntoEnRuta(Math.max(0, a));
        const [x1, y1] = puntoEnRuta(b);
        const k = (i + 1) / pasos;
        ctx.strokeStyle = `rgba(214,230,255,${0.85 * k * k * vis})`;
        ctx.lineWidth = 6 + 7 * k;
        ctx.beginPath();
        ctx.moveTo(x0, y0);
        ctx.lineTo(x1, y1);
        ctx.stroke();
      }
      const [px, py] = puntoEnRuta(avance);
      g = ctx.createRadialGradient(px, py, 0, px, py, 30);
      g.addColorStop(0, `rgba(255,255,255,${0.95 * vis})`);
      g.addColorStop(0.35, `rgba(205,225,255,${0.55 * vis})`);
      g.addColorStop(1, 'rgba(160,195,255,0)');
      ctx.fillStyle = g;
      ctx.beginPath();
      ctx.arc(px, py, 30, 0, TAU);
      ctx.fill();
    }

    // La meta: al llegar el pulso suelta un aro que se abre sobre el papel
    const aro = limita(desdeLlegada / 1.5);
    if (aro < 1) {
      ctx.strokeStyle = `rgba(100,154,254,${0.55 * Math.pow(1 - aro, 2)})`;
      ctx.lineWidth = 9 * (1 - aro) + 2;
      ctx.beginPath();
      ctx.arc(META[0], META[1], 62 + 78 * (1 - Math.pow(1 - aro, 3)), 0, TAU);
      ctx.stroke();
    }
    const rm = 62 * (1 + 0.09 * llegada);
    disco(META, rm, mezclaColor([100, 154, 254], [150, 190, 255], llegada));
    disco(META, rm * 0.56, '#ffffff');
    disco(META, rm * 0.26 * (1 + 0.35 * llegada), mezclaColor([100, 154, 254], [70, 125, 250], llegada));

    textura.needsUpdate = true;
  }

  function disco([x, y], r, color) {
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.arc(x, y, r, 0, TAU);
    ctx.fill();
  }

  const v = new THREE.Vector3();

  /** Dibuja el cuadro y devuelve dónde cae la meta en el lienzo (0..1) y cuánto flota el mapa */
  function cuadro(u) {
    const { desdeLlegada } = pulso(u);
    plegar(THREE.MathUtils.degToRad(26 + 4 * onda(u, 2, 0.6)));
    flota.rotation.y = 0.2 * onda(u, 1);
    flota.rotation.x = 0.07 * onda(u, 2, 1.2);
    flota.rotation.z = 0.018 * onda(u, 1, 2.1);
    const sube = 16 * onda(u, 2, -0.5);
    flota.position.y = sube + 14;
    flota.scale.setScalar(1 + 0.016 * Math.exp(-desdeLlegada * 3));
    // El sol se pasea un poco para que el papel no tenga nunca la misma luz
    sol.position.set(1.0 + 0.35 * onda(u, 1, 1.0), 0.85 + 0.2 * onda(u, 2), 1.25);
    rebote.intensity = 1.3 + 1.0 * Math.exp(-desdeLlegada * 1.6);
    pintar(u);
    escena.updateMatrixWorld(true);
    render.render(escena, camara);
    v.copy(metaLocal);
    mapa.localToWorld(v);
    v.project(camara);
    return { metaX: (v.x + 1) / 2, metaY: (1 - v.y) / 2, sube };
  }

  return { cuadro };
}

/** Un panel: rectángulo de esquinas redondas con un poco de grosor y el canto biselado */
function geometriaPanel(indice) {
  const r = 16;
  const w = PANEL;
  const h = ALTO_MAPA;
  const s = new THREE.Shape();
  s.moveTo(r, -h / 2);
  s.lineTo(w - r, -h / 2);
  s.quadraticCurveTo(w, -h / 2, w, -h / 2 + r);
  s.lineTo(w, h / 2 - r);
  s.quadraticCurveTo(w, h / 2, w - r, h / 2);
  s.lineTo(r, h / 2);
  s.quadraticCurveTo(0, h / 2, 0, h / 2 - r);
  s.lineTo(0, -h / 2 + r);
  s.quadraticCurveTo(0, -h / 2, r, -h / 2);
  const geo = new THREE.ExtrudeGeometry(s, {
    depth: 5, bevelEnabled: true, bevelThickness: 1.6, bevelSize: 1.6, bevelSegments: 3, curveSegments: 10,
  });
  geo.translate(0, 0, -5);
  // La textura es el mapa entero: a cada panel le toca su tercio
  const pos = geo.attributes.position;
  const uv = geo.attributes.uv;
  for (let i = 0; i < pos.count; i++) {
    uv.setXY(i, (indice + limita(pos.getX(i) / w)) / 3, limita((pos.getY(i) + h / 2) / h));
  }
  uv.needsUpdate = true;
  return geo;
}

function mezclaColor(a, b, t) {
  return `rgb(${a.map((x, i) => Math.round(mezcla(x, b[i], t))).join(',')})`;
}
