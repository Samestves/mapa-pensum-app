// Genera las fixtures doradas de la lógica: ejecuta el JS de la web sobre las
// nueve carreras con varios avances y guarda lo que devuelve cada función.
// Las pruebas de Kotlin (androidUnitTest/.../LogicaDoradaTest.kt) comparan
// contra esto, así la app y la web dicen lo mismo de cada materia.
//
// Uso: node herramientas/fixtures.mjs "C:/Users/Sam/Documents/Proyecto Pensum"
import fs from 'node:fs'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const web = process.argv[2]
if (!web) throw new Error('Falta la ruta del repo web')
const modulo = (ruta) => import(pathToFileURL(path.join(web, 'src', ruta)).href)

const { estadosDe, depurarMarcas, avanceDeGrupos, progresoDe, avanceDe, resumenParaPortada } = await modulo('data/avance.js')
const { situacionDe, tramoDe } = await modulo('layout/situacion.js')
const { marcasDeSemestres, accionDeSemestre } = await modulo('data/semestre.js')

const carpetaCarreras = path.join(web, 'src/data/carreras')
const salida = path.resolve(path.dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Z]:)/, '$1')), '../composeApp/src/androidUnitTest/fixtures')
fs.mkdirSync(salida, { recursive: true })

// Azar con semilla: las fixtures salen iguales en cada ejecución
function azar(semilla) {
  let s = semilla >>> 0
  return () => {
    s = (s + 0x6d2b79f5) >>> 0
    let t = s
    t = Math.imul(t ^ (t >>> 15), t | 1)
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61)
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

function casos(carrera, n) {
  const obligatorias = carrera.asignaturas.filter((a) => !a.esHueco)
  const electivas = carrera.grupos.flatMap((g) => g.asignaturas)
  const huecos = carrera.asignaturas.filter((a) => a.esHueco)
  const lista = []

  lista.push({ nombre: 'vacio', marcas: {}, casillas: {} })

  // Los tres primeros semestres aprobados y dos del cuarto en curso, con basura que hay que depurar
  const primeros = {}
  obligatorias.filter((a) => a.semestre <= 3).forEach((a) => { primeros[a.codigo] = 'aprobada' })
  obligatorias.filter((a) => a.semestre === 4).slice(0, 2).forEach((a) => { primeros[a.codigo] = 'cursando' })
  primeros['NO-EXISTE'] = 'aprobada'
  if (obligatorias[0]) primeros[obligatorias[obligatorias.length - 1].codigo] = 'disponible'
  lista.push({ nombre: 'primeros', marcas: primeros, casillas: {} })

  // Varios al azar, con electivas aprobadas y casillas elegidas
  for (let k = 0; k < 4; k++) {
    const r = azar(n * 100 + k + 1)
    const marcas = {}
    for (const a of obligatorias) {
      const x = r()
      if (x < 0.4) marcas[a.codigo] = 'aprobada'
      else if (x < 0.5) marcas[a.codigo] = 'cursando'
    }
    for (const e of electivas) if (r() < 0.15) marcas[e.codigo] = r() < 0.8 ? 'aprobada' : 'cursando'
    const casillas = {}
    const usadas = new Set()
    for (const h of huecos) {
      if (r() < 0.5) continue
      const grupo = carrera.grupos.find((g) => g.clave === h.grupo)
      const libres = (grupo?.asignaturas ?? []).filter((e) => !usadas.has(e.codigo))
      if (!libres.length) continue
      const e = libres[Math.floor(r() * libres.length)]
      usadas.add(e.codigo)
      casillas[h.codigo] = e.codigo
    }
    lista.push({ nombre: 'azar' + (k + 1), marcas, casillas })
  }

  // Todo aprobado, con las electivas necesarias para cubrir cada cuota
  const todo = {}
  obligatorias.forEach((a) => { todo[a.codigo] = 'aprobada' })
  const casillasTodo = {}
  for (const g of carrera.grupos) {
    let uc = 0
    for (const e of g.asignaturas) {
      if (g.cuota != null && uc >= g.cuota) break
      todo[e.codigo] = 'aprobada'
      uc += e.uc ?? 0
    }
  }
  const puestas = new Set()
  for (const h of huecos) {
    const e = carrera.grupos.find((g) => g.clave === h.grupo)?.asignaturas.find((x) => !puestas.has(x.codigo))
    if (e) { puestas.add(e.codigo); casillasTodo[h.codigo] = e.codigo }
  }
  lista.push({ nombre: 'todo', marcas: todo, casillas: casillasTodo })
  return lista
}

const archivos = fs.readdirSync(carpetaCarreras).filter((f) => f.endsWith('.json') && f !== 'indice.json').sort()
archivos.forEach((archivo, n) => {
  const carrera = JSON.parse(fs.readFileSync(path.join(carpetaCarreras, archivo), 'utf8'))
  const obligatorias = carrera.asignaturas.filter((a) => !a.esHueco)
  const todas = [...obligatorias, ...carrera.grupos.flatMap((g) => g.asignaturas)]
  const porCodigo = Object.fromEntries(todas.map((a) => [a.codigo, a]))
  const validos = new Set(todas.map((a) => a.codigo))

  const resultados = casos(carrera, n).map(({ nombre, marcas: crudas, casillas }) => {
    const marcas = depurarMarcas(crudas, validos)
    const estados = estadosDe(todas, marcas)
    const situaciones = Object.fromEntries(todas.map((a) => [a.codigo, situacionDe(a.codigo, a.prerrequisitos, estados)]))
    const tramos = []
    for (const a of todas) for (const p of a.prerrequisitos ?? []) tramos.push([p, a.codigo, tramoDe(situaciones[p], situaciones[a.codigo])])
    const grupos = avanceDeGrupos(carrera.grupos, marcas)
    const progreso = progresoDe(obligatorias, estados, carrera.creditos, grupos)
    const enCasilla = (codigo) => (casillas[codigo] ? porCodigo[casillas[codigo]] ?? null : null)
    const semestres = [...marcasDeSemestres(carrera.asignaturas, enCasilla, estados).entries()].map(([s, c]) => ({ semestre: s, ...c }))
    const acciones = carrera.semestres.map((s) => ({ semestre: s.numero, accion: accionDeSemestre(carrera.asignaturas, s.numero, enCasilla, estados) }))
    return {
      nombre, crudas, casillas, marcas, estados, situaciones, tramos,
      grupos: Object.fromEntries(Object.entries(grupos).map(([k, g]) => [k, { uc: g.uc, completa: g.completa, excedente: g.excedente, elegidas: g.elegidas.map((e) => e.codigo) }])),
      progreso: { ...progreso, paraInscribir: progreso.paraInscribir.map((a) => a.codigo) },
      avance: avanceDe(progreso),
      portada: resumenParaPortada(carrera, crudas),
      semestres, acciones,
    }
  })
  fs.writeFileSync(path.join(salida, archivo), JSON.stringify({ slug: carrera.slug, casos: resultados }))
  console.log(archivo, resultados.length, 'casos')
})
