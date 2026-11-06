// Versiones en un solo sitio: el modulo de la app solo aplica los plugins
plugins {
    kotlin("multiplatform") version "2.3.20" apply false
    kotlin("plugin.compose") version "2.3.20" apply false
    kotlin("plugin.serialization") version "2.3.20" apply false
    id("org.jetbrains.compose") version "1.10.3" apply false
    id("com.android.application") version "8.13.1" apply false
}
