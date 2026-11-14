/*
 * Saca el bucle cuadro a cuadro con Chromium y lo monta con ffmpeg.
 *
 *   node render.mjs                 el video entero  → out/mapa-pensum-fondo-logo-3x2.mp4
 *   node render.mjs --cuadros 0,300 solo esos cuadros → out/prueba-0000.png …
 *   node render.mjs --servir        solo sirve la escena para verla en el navegador
 */
import { createServer } from 'node:http';
import { readFile, mkdir, rm } from 'node:fs/promises';
import { spawn } from 'node:child_process';
import { createRequire } from 'node:module';
import path from 'node:path';
import os from 'node:os';
import { fileURLToPath } from 'node:url';

const AQUI = path.dirname(fileURLToPath(import.meta.url));
const SALIDA = path.join(AQUI, 'out');
const CUADROS_DIR = path.join(SALIDA, 'cuadros');
const TIPOS = { '.html': 'text/html', '.js': 'text/javascript', '.mjs': 'text/javascript', '.png': 'image/png' };
const args = process.argv.slice(2);
const opcion = (n) => (args.includes(n) ? args[args.indexOf(n) + 1] ?? true : null);

const servidor = createServer(async (req, res) => {
  const ruta = path.join(AQUI, decodeURIComponent(new URL(req.url, 'http://x').pathname).replace(/^\/$/, '/index.html'));
  if (!ruta.startsWith(AQUI)) return res.writeHead(403).end();
  try {
    const datos = await readFile(ruta);
    res.writeHead(200, { 'content-type': TIPOS[path.extname(ruta)] ?? 'application/octet-stream' }).end(datos);
  } catch {
    res.writeHead(404).end();
  }
});
await new Promise((ok) => servidor.listen(Number(opcion('--puerto')) || 0, '127.0.0.1', ok));
const url = `http://127.0.0.1:${servidor.address().port}/`;

if (opcion('--servir')) {
  console.log(`Escena en ${url}`);
} else {
  const { chromium } = createRequire(import.meta.url)('playwright');
  await mkdir(CUADROS_DIR, { recursive: true });
  const navegador = await chromium.launch({ args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });

  async function pagina() {
    const p = await navegador.newPage({ viewport: { width: 1920, height: 1280 }, deviceScaleFactor: 1 });
    p.on('pageerror', (e) => console.error('Error en la página:', e.message));
    await p.goto(`${url}?quieto`);
    await p.waitForFunction('window.listo === true');
    return p;
  }

  const sueltos = opcion('--cuadros');
  if (sueltos) {
    const p = await pagina();
    for (const f of String(sueltos).split(',').map(Number)) {
      await p.evaluate((n) => window.cuadro(n), f);
      await p.screenshot({ path: path.join(SALIDA, `prueba-${String(f).padStart(4, '0')}.png`) });
      console.log('cuadro', f);
    }
  } else {
    const total = await (await pagina()).evaluate('window.CUADROS');
    const obreros = Number(opcion('--obreros')) || Math.max(1, os.cpus().length);
    let siguiente = 0;
    let hechos = 0;
    await Promise.all(
      Array.from({ length: obreros }, async () => {
        const p = await pagina();
        for (let f = siguiente++; f < total; f = siguiente++) {
          await p.evaluate((n) => window.cuadro(n), f);
          await p.screenshot({ path: path.join(CUADROS_DIR, `${String(f).padStart(5, '0')}.png`) });
          if (++hechos % 60 === 0) console.log(`${hechos}/${total}`);
        }
      }),
    );
    const mp4 = path.join(SALIDA, 'mapa-pensum-fondo-logo-3x2.mp4');
    // H.264 con todos los cuadros cerca de ser clave: Resolume lo recorre y lo pone en bucle sin tirones
    await new Promise((ok, mal) => {
      const ff = spawn(
        'ffmpeg',
        ['-y', '-loglevel', 'error', '-framerate', '60', '-i', path.join(CUADROS_DIR, '%05d.png'),
          '-c:v', 'libx264', '-preset', 'slow', '-crf', '13', '-g', '30', '-pix_fmt', 'yuv420p',
          '-colorspace', 'bt709', '-color_primaries', 'bt709', '-color_trc', 'bt709', '-movflags', '+faststart', mp4],
        { stdio: 'inherit' },
      );
      ff.on('exit', (c) => (c === 0 ? ok() : mal(new Error(`ffmpeg salió con ${c}`))));
    });
    if (!opcion('--conservar')) await rm(CUADROS_DIR, { recursive: true, force: true });
    console.log('Listo:', mp4);
  }
  await navegador.close();
  servidor.close();
}
