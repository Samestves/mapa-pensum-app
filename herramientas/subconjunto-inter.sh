#!/bin/sh
# Recorta Inter a latín (más flechas y el euro) conservando los ejes wght y opsz: de unos 876 KB a unos 200 KB.
# Uso: sh herramientas/subconjunto-inter.sh   (necesita fonttools: pip install fonttools brotli)
set -e
fuente=composeApp/src/commonMain/composeResources/font/inter.ttf
pyftsubset "$fuente" \
    --unicodes="U+0000-00FF,U+2010-2027,U+20AC,U+2192" \
    --layout-features="*" \
    --output-file="$fuente.recortada"
mv "$fuente.recortada" "$fuente"
