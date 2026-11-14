"""Separa el logo de IMCULMAT en capas nítidas a 6x: grecas, ola, estrellas y texto."""
import colorsys, json, sys
from PIL import Image, ImageFilter
import numpy as np

im = np.asarray(Image.open(sys.argv[1]).convert('RGB')).astype(float)
X0, Y0, X1, Y1 = 22, 150, 425, 272
im = im[Y0:Y1, X0:X1]
dist = np.sqrt(((255 - im) ** 2).sum(axis=2))  # lo lejos que está cada píxel del blanco
alfa = np.clip(dist / 40, 0, 1)
h = np.zeros(dist.shape); s = np.zeros(dist.shape)
for y in range(im.shape[0]):
    for x in range(im.shape[1]):
        hh, ss, _ = colorsys.rgb_to_hsv(*(im[y, x] / 255)); h[y, x] = hh * 360; s[y, x] = ss
gris = (s < 0.18) & (dist > 25)
amar = (s >= 0.18) & (h > 35) & (h < 75)
rojo = (s >= 0.18) & ((h < 25) | (h > 330))
azul = (s >= 0.18) & (h > 180) & (h < 250)
xs = np.arange(alfa.shape[1])[None, :].repeat(alfa.shape[0], 0)
capas = {
    'texto': (gris, (242, 239, 232), 1),
    'roja': (rojo, None, 1.15), 'azul': (azul, None, 1.15),
    'ola': (amar & (xs < 95), None, 1.15), 'estrellas': (amar & (xs >= 95), None, 1.2),
}
E = 6
for nombre, (m, fijo, gan) in capas.items():
    d = dist[m]
    lleno = np.percentile(d, 70) if d.size else 1
    a = np.where(m, np.clip(dist / lleno, 0, 1), 0)
    c = fijo or tuple(int(v) for v in np.median(im[m & (s >= np.percentile(s[m], 75))], axis=0))
    big = Image.fromarray((a * 255).astype(np.uint8)).resize((a.shape[1] * E, a.shape[0] * E), Image.LANCZOS).filter(ImageFilter.GaussianBlur(2.2))
    b = np.asarray(big).astype(float) / 255
    b = np.clip((b - 0.5) * 3.2 + 0.5, 0, 1)  # borde firme, antialias suave
    rgba = np.zeros(b.shape + (4,), np.uint8); rgba[..., :3] = c; rgba[..., 3] = (b * 255).astype(np.uint8)
    Image.fromarray(rgba).save(f'capas/{nombre}.png')
    print(nombre, c)
# Centros de las estrellas, de izquierda a derecha
ms = capas['estrellas'][0] & (alfa > 0.4)
vistos = np.zeros(ms.shape, bool); cs = []
for y0, x0 in zip(*np.nonzero(ms)):
    if vistos[y0, x0]: continue
    pila = [(y0, x0)]; vistos[y0, x0] = True; pts = []
    while pila:
        y, x = pila.pop(); pts.append((y, x))
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            v, u = y + dy, x + dx
            if 0 <= v < ms.shape[0] and 0 <= u < ms.shape[1] and ms[v, u] and not vistos[v, u]:
                vistos[v, u] = True; pila.append((v, u))
    if len(pts) > 6: cs.append(tuple(np.mean(pts, axis=0)))
cs.sort(key=lambda p: p[1]); n = len(cs)
json.dump({'escala': E, 'ancho': alfa.shape[1] * E, 'alto': alfa.shape[0] * E,
           'estrellas': [[round(x * E), round(y * E)] for y, x in cs if True]}, open('capas/logo.json', 'w'))
print(n, 'estrellas')
