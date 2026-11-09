// Genera el vector de la rosa (la marca dentro de la app) a partir de los trazos de la web.
// El icono de la app ya no sale de aquí: lo genera herramientas/icono.mjs.
// Uso: node herramientas/rosa.mjs "<ruta a Proyecto Pensum>"
import { pathToFileURL } from 'node:url'
import { writeFileSync, mkdirSync } from 'node:fs'
import { join } from 'node:path'

const web = process.argv[2]
const { ROSA, NODOS } = await import(pathToFileURL(join(web, 'src/data/logoTrazos.js')).href)
const raiz = new URL('..', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1')

// Caja de un trazo con solo M, c y z (los nodos son asi), en coordenadas del calco
function caja(d) {
  const t = d.match(/[a-zA-Z]|-?\d+(\.\d+)?/g)
  let x = 0, y = 0, i = 0, cmd = ''
  const xs = [], ys = []
  while (i < t.length) {
    if (/[a-zA-Z]/.test(t[i])) { cmd = t[i++]; if (cmd === 'z' || cmd === 'Z') continue }
    if (cmd === 'M') { x = +t[i++]; y = +t[i++]; xs.push(x); ys.push(y) }
    else if (cmd === 'c') {
      for (let k = 0; k < 3; k++) { const dx = +t[i++], dy = +t[i++]; xs.push(x + dx); ys.push(y + dy); if (k === 2) { x += dx; y += dy } }
    } else i++
  }
  const minX = Math.min(...xs), maxX = Math.max(...xs), minY = Math.min(...ys), maxY = Math.max(...ys)
  // Al sistema de la caja: x' = x/10, y' = 1278 - y/10; y respecto a la caja -3 -10
  const cx = (minX + maxX) / 20 + 3, cy = 1278 - (minY + maxY) / 20 + 10
  const r = ((maxX - minX) + (maxY - minY)) / 40
  return { cx: +cx.toFixed(1), cy: +cy.toFixed(1), r: +r.toFixed(1) }
}

const grupo = (paths, color) => paths.map((d) => `        <path android:fillColor="${color}" android:pathData="${d}" />`).join('\n')

const vector = (tam, pad, cuerpo, nodos) => `<?xml version="1.0" encoding="utf-8"?>
<!-- Generado por herramientas/rosa.mjs a partir de src/data/logoTrazos.js de la web -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="${tam}dp" android:height="${tam}dp"
    android:viewportWidth="${1230 + pad * 2}" android:viewportHeight="${1230 + pad * 2}">
    <group android:translateX="${pad + 3}" android:translateY="${pad + 10}">
    <group android:translateY="1278" android:scaleX="0.1" android:scaleY="-0.1">
${grupo(ROSA, cuerpo)}
${nodos ? grupo(NODOS, nodos) : ''}
    </group>
    </group>
</vector>
`
const comun = join(raiz, 'composeApp/src/commonMain/composeResources/drawable')
const res = join(raiz, 'composeApp/src/androidMain/res')
mkdirSync(comun, { recursive: true })
writeFileSync(join(comun, 'rosa.xml'), vector(48, 0, '#FFECEEF1', null))
console.log(JSON.stringify(NODOS.map(caja)))
