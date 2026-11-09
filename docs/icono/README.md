# El icono de la app

El mapa plegado con la ruta de tu carrera: dos materias y la meta. Los azules salen del escudo de la UDO.

| Archivo | Para qué |
|---|---|
| `icono.svg` | El original completo, en vector |
| `icono-512.png`, `icono-1024.png` | La ficha de Google Play y otros usos |
| `fondo.svg`, `frente.svg` | Las dos capas del icono adaptable de Android |

En la app, el icono es adaptable (`res/mipmap-anydpi-v26/ic_launcher.xml`):

- **Fondo y frente:** PNG por densidad en `res/mipmap-*/`. Van en PNG y no en vector porque el vector de Android no admite las sombras ni los brillos difuminados.
- **Monocromático:** `res/drawable/ic_launcher_monochrome.xml`, para los iconos con tema de Android 13 en adelante. El mapa va tenue y la ruta, llena.

Todo sale de `herramientas/icono.mjs` (`node herramientas/icono.mjs`, con Playwright instalado). Para cambiar el dibujo se edita ese archivo y se vuelve a generar.

Los bocetos previos están en `docs/maquetas/icono-app*.html`.
