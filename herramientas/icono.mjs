// Genera el icono de la app: el mapa plegado con la ruta (docs/maquetas/icono-app-i.html, «Después · pulida»).
// Uso: node herramientas/icono.mjs   (necesita Playwright con Chromium: npm i playwright)
// Escribe las dos capas del icono adaptable (fondo y frente) en PNG para cada densidad, la capa
// monocromática en vector, y los originales en docs/icono/ (SVG y PNG de 512 y 1024 para la tienda).
import { createRequire } from 'node:module'
import { writeFileSync, mkdirSync } from 'node:fs'
import { join } from 'node:path'

const raiz = new URL('..', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1')
const res = join(raiz, 'composeApp/src/androidMain/res')
const docs = join(raiz, 'docs/icono')

// El dibujo vive en una caja de 512: es lo que deja ver la máscara del icono (72 de los 108 dp).
// La capa adaptable completa mide 768 (108 dp), con el dibujo desplazado 128.
const RUTA = 'M152 340 206 288 306 248 360 192'

const defsFondo = `
 <linearGradient id="fondo" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="0" y2="512"><stop stop-color="#1d2026"/><stop offset="1" stop-color="#040405"/></linearGradient>
 <radialGradient id="luz" gradientUnits="userSpaceOnUse" cx="256" cy="0" r="461"><stop stop-color="#6699FF" stop-opacity=".24"/><stop offset="1" stop-color="#6699FF" stop-opacity="0"/></radialGradient>`
const fondo = (x, y, lado) => `<rect x="${x}" y="${y}" width="${lado}" height="${lado}" fill="url(#fondo)"/><rect x="${x}" y="${y}" width="${lado}" height="${lado}" fill="url(#luz)"/>`

const defsFrente = `
 <linearGradient id="hoja" x1="0" y1="0" x2="0" y2="1"><stop stop-color="#FFFFFF"/><stop offset="1" stop-color="#E9EDF3"/></linearGradient>
 <linearGradient id="centro" x1="0" y1="0" x2="1" y2="0"><stop stop-color="#DCE0E8"/><stop offset="1" stop-color="#BCC3CF"/></linearGradient>
 <linearGradient id="derecha" x1="0" y1="0" x2="1" y2="0"><stop stop-color="#E4E8EE"/><stop offset=".35" stop-color="#FFFFFF"/></linearGradient>
 <linearGradient id="ruta" gradientUnits="userSpaceOnUse" x1="152" y1="340" x2="360" y2="192"><stop stop-color="#1F3FA6"/><stop offset=".5" stop-color="#2F5BE0"/><stop offset="1" stop-color="#6699FF"/></linearGradient>
 <radialGradient id="halo"><stop stop-color="#6699FF" stop-opacity=".5"/><stop offset="1" stop-color="#6699FF" stop-opacity="0"/></radialGradient>
 <radialGradient id="reflejo"><stop stop-color="#3D6BF0" stop-opacity=".35"/><stop offset="1" stop-color="#3D6BF0" stop-opacity="0"/></radialGradient>
 <filter id="sombra" x="-20%" y="-20%" width="140%" height="150%"><feDropShadow dx="0" dy="14" stdDeviation="14" flood-color="#000" flood-opacity=".55"/></filter>
 <filter id="relieve" x="-30%" y="-30%" width="160%" height="170%"><feDropShadow dx="0" dy="3" stdDeviation="3.5" flood-color="#0B1E5C" flood-opacity=".28"/></filter>`
const frente = `
 <ellipse cx="256" cy="418" rx="150" ry="26" fill="url(#reflejo)"/>
 <g filter="url(#sombra)" stroke-linejoin="round" stroke-width="14">
  <path d="M116 158 206 124V362L116 396Z" fill="url(#hoja)" stroke="url(#hoja)"/>
  <path d="M206 124 306 158V396L206 362Z" fill="url(#centro)" stroke="url(#centro)"/>
  <path d="M306 158 396 124V362L306 396Z" fill="url(#derecha)" stroke="url(#derecha)"/></g>
 <path d="M206 120V366" stroke="#fff" stroke-width="2.5" stroke-linecap="round" opacity=".9"/>
 <path d="M306 154V400" stroke="#6B7484" stroke-width="2" stroke-linecap="round" opacity=".28"/>
 <circle cx="360" cy="192" r="52" fill="url(#halo)"/>
 <g filter="url(#relieve)">
  <path d="${RUTA}" fill="none" stroke="url(#ruta)" stroke-width="13" stroke-linecap="round" stroke-linejoin="round"/>
  <circle cx="152" cy="340" r="15" fill="#1F3FA6"/>
  <circle cx="256" cy="268" r="18" fill="#2F5BE0"/>
  <circle cx="360" cy="192" r="22" fill="#fff" stroke="#6699FF" stroke-width="12"/></g>
 <circle cx="360" cy="192" r="7" fill="#6699FF"/>`

const svg = (caja, defs, cuerpo) => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${caja}"><defs>${defs}</defs>${cuerpo}</svg>\n`
const CAPA = '-128 -128 768 768'
const svgs = {
  icono: svg('0 0 512 512', defsFondo + defsFrente, fondo(0, 0, 512) + frente),
  fondo: svg(CAPA, defsFondo, fondo(-128, -128, 768)),
  frente: svg(CAPA, defsFrente, frente),
}

// La capa monocromática: el sistema solo usa su alfa, así que el mapa va tenue y la ruta, llena
const monocromo = `<?xml version="1.0" encoding="utf-8"?>
<!-- Generado por herramientas/icono.mjs -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="768" android:viewportHeight="768">
    <group android:translateX="128" android:translateY="128">
        <path android:fillColor="#FFFFFF" android:fillAlpha="0.38" android:pathData="M116,158 L206,124 V362 L116,396 Z" />
        <path android:fillColor="#FFFFFF" android:fillAlpha="0.24" android:pathData="M206,124 L306,158 V396 L206,362 Z" />
        <path android:fillColor="#FFFFFF" android:fillAlpha="0.38" android:pathData="M306,158 L396,124 V362 L306,396 Z" />
        <path android:strokeColor="#FFFFFF" android:strokeWidth="14" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M152,340 L206,288 L306,248 L360,192" />
        <path android:fillColor="#FFFFFF" android:pathData="M137,340 a15,15 0 1,0 30,0 a15,15 0 1,0 -30,0 Z" />
        <path android:fillColor="#FFFFFF" android:pathData="M238,268 a18,18 0 1,0 36,0 a18,18 0 1,0 -36,0 Z" />
        <path android:fillColor="#FFFFFF" android:fillType="evenOdd" android:pathData="M332,192 a28,28 0 1,0 56,0 a28,28 0 1,0 -56,0 Z M344,192 a16,16 0 1,0 32,0 a16,16 0 1,0 -32,0 Z M353,192 a7,7 0 1,0 14,0 a7,7 0 1,0 -14,0 Z" />
    </group>
</vector>
`
const adaptable = `<?xml version="1.0" encoding="utf-8"?>
<!-- Generado por herramientas/icono.mjs -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
`

mkdirSync(docs, { recursive: true })
mkdirSync(join(res, 'drawable'), { recursive: true })
mkdirSync(join(res, 'mipmap-anydpi-v26'), { recursive: true })
for (const [nombre, texto] of Object.entries(svgs)) writeFileSync(join(docs, `${nombre}.svg`), texto)
writeFileSync(join(res, 'drawable/ic_launcher_monochrome.xml'), monocromo)
writeFileSync(join(res, 'mipmap-anydpi-v26/ic_launcher.xml'), adaptable)

const { chromium } = createRequire(import.meta.url)('playwright')
const navegador = await chromium.launch()
async function png(texto, lado, destino, transparente) {
  const pagina = await navegador.newPage({ viewport: { width: lado, height: lado } })
  await pagina.setContent(`<body style="margin:0;background:transparent">${texto.replace('<svg ', `<svg width="${lado}" height="${lado}" style="display:block" `)}</body>`)
  await pagina.screenshot({ path: destino, omitBackground: transparente })
  await pagina.close()
}
const DENSIDADES = { mdpi: 108, hdpi: 162, xhdpi: 216, xxhdpi: 324, xxxhdpi: 432 }
for (const [densidad, lado] of Object.entries(DENSIDADES)) {
  const carpeta = join(res, `mipmap-${densidad}`)
  mkdirSync(carpeta, { recursive: true })
  await png(svgs.fondo, lado, join(carpeta, 'ic_launcher_background.png'), false)
  await png(svgs.frente, lado, join(carpeta, 'ic_launcher_foreground.png'), true)
}
await png(svgs.icono, 512, join(docs, 'icono-512.png'), false)
await png(svgs.icono, 1024, join(docs, 'icono-1024.png'), false)
await navegador.close()
console.log('Icono generado')
