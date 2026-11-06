// Genera las fixtures doradas del layout del mapa: posiciones, nombres
// partidos, cables ruteados, relaciones y la franja de electivas, todo
// calculado por el JS de la web. Las compara MapaDoradoTest.kt.
//
// Uso: node herramientas/fixturesMapa.mjs "C:/Users/Sam/Documents/Proyecto Pensum"
import fs from 'node:fs'
import path from 'node:path'
import { register } from 'node:module'
import { pathToFileURL } from 'node:url'

// La web importa './constantes' sin extensión (Vite la resuelve); Node necesita que se la pongan
register(
  'data:text/javascript,' +
    encodeURIComponent(`
export async function resolve(especificador, contexto, siguiente) {
  if (especificador.startsWith('.') && !/\\.[a-z]+$/i.test(especificador)) {
    return siguiente(especificador + '.js', contexto)
  }
  return siguiente(especificador, contexto)
}`),
)

const web = process.argv[2]
if (!web) throw new Error('Falta la ruta del repo web')
const modulo = (ruta) => import(pathToFileURL(path.join(web, 'src', ruta)).href)

const { calcularLayout } = await modulo('layout/calcularLayout.js')
const { cadenaDe, pesoDesbloqueo } = await modulo('layout/relaciones.js')
const { calcularFranja, adoptarMarcadas, casillasLibres } = await modulo('layout/franjaElectivas.js')
const { FRANJA } = await modulo('layout/constantes.js')

const aqui = path.dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Z]:)/, '$1'))
const salida = path.resolve(aqui, '../composeApp/src/androidUnitTest/fixtures/mapa')
fs.mkdirSync(salida, { recursive: true })
const carpetaCarreras = path.join(web, 'src/data/carreras')
// Las marcas y casillas de los casos de la lógica: el mismo avance para las dos pruebas
const carpetaCasos = path.resolve(aqui, '../composeApp/src/androidUnitTest/fixtures')

for (const archivo of fs.readdirSync(carpetaCarreras).filter((f) => f.endsWith('.json') && f !== 'indice.json').sort()) {
  const carrera = JSON.parse(fs.readFileSync(path.join(carpetaCarreras, archivo), 'utf8'))
  const layout = calcularLayout(carrera.asignaturas, carrera.grupos)
  const relaciones = layout.relaciones
  const peso = pesoDesbloqueo(relaciones)

  // La franja, solo en las carreras sin ruta completa, con cada avance de los casos de la lógica
  const casos = JSON.parse(fs.readFileSync(path.join(carpetaCasos, archivo), 'utf8')).casos
  const franjas = carrera.electivasEnCasillas
    ? []
    : casos.map((caso) => {
        // Unas casillas libres puestas a mano, y luego las electivas marcadas que se adoptan
        const libres = [...casillasLibres(carrera.grupos).keys()]
        const aMano = {}
        carrera.grupos.forEach((g, i) => {
          const e = g.asignaturas[(caso.nombre.length + i) % Math.max(1, g.asignaturas.length)]
          if (e && caso.nombre !== 'vacio') aMano[`libre-${g.clave}-${2}`] = e.codigo
        })
        const elegidas = adoptarMarcadas(aMano, carrera.grupos, caso.marcas)
        const franja = calcularFranja(carrera.grupos, elegidas, layout.columnas.map((c) => c.x), layout.finSemestres + FRANJA.corredor)
        return {
          caso: caso.nombre,
          marcas: caso.marcas,
          aMano,
          elegidas,
          libres: libres.length,
          nodos: franja.nodos.map((n) => [n.codigo, n.nombre, n.grupo, n.x, n.y]),
          filas: franja.filas.map((f) => [f.clave, f.titulo, f.cuota, f.opciones, f.elegidas, f.x, f.y]),
          alto: franja.alto,
        }
      })

  const datos = {
    slug: carrera.slug,
    nodos: layout.nodos.map((n) => [n.codigo, n.x, n.y, n.lineasNombre]),
    electivas: layout.electivas.map((n) => [n.codigo, n.lineasNombre]),
    columnas: layout.columnas.map((c) => [c.semestre, c.x, c.cantidad, c.uc]),
    finSemestres: layout.finSemestres,
    maxFilas: layout.maxFilas,
    ancho: layout.ancho,
    alto: layout.alto,
    aristas: layout.aristas.map((a) => [a.id, a.area ?? null, a.cruzaColumnas, a.d]),
    cadenas: Object.fromEntries([...relaciones.atras.keys()].map((c) => [c, [...cadenaDe(c, relaciones)].sort()])),
    peso: Object.fromEntries(peso),
    franjas,
  }
  fs.writeFileSync(path.join(salida, archivo), JSON.stringify(datos))
  console.log(archivo, datos.nodos.length, 'nodos,', datos.aristas.length, 'cables,', franjas.length, 'franjas')
}
