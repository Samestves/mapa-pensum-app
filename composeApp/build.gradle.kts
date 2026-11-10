import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("com.android.application")
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.animation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            // El cristal de la cabecera. La 1.7 ya pide Compose 1.11; la 1.6 va con la nuestra
            implementation("dev.chrisbanes.haze:haze:1.6.10")
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.10.1")
            // Instala el perfil de arranque al abrir la app aunque no venga de Play (ya llegaba por Compose; aquí queda explícito)
            implementation("androidx.profileinstaller:profileinstaller:1.4.0")
        }
        // Las pruebas corren en la JVM: la lógica lee las fixtures del disco, y
        // Robolectric pinta pantallas de verdad para ver el mapa sin teléfono
        androidUnitTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.robolectric:robolectric:4.17")
            implementation("androidx.compose.ui:ui-test-junit4-android:1.10.5")
            implementation("io.github.takahirom.roborazzi:roborazzi:1.77.0")
            implementation("io.github.takahirom.roborazzi:roborazzi-compose:1.77.0")
        }
    }
}

composeCompiler {
    // Qué es estable de hecho (las listas y los datos de la app se comparan por equals), y los informes para revisarlo
    stabilityConfigurationFiles.add(layout.projectDirectory.file("estabilidad-compose.conf"))
    reportsDestination.set(layout.buildDirectory.dir("informes-compose"))
    metricsDestination.set(layout.buildDirectory.dir("informes-compose"))
}

compose.resources {
    packageOfResClass = "mpensum.recursos"
}

android {
    namespace = "com.mapapensum.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.mapapensum.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    androidResources {
        // Las fuentes van sin comprimir: Android las mapea del APK sin copiarlas a memoria en cada peso
        noCompress += "ttf"
    }
    sourceSets {
        // El perfil de arranque propio, escrito a mano: en KMP la fuente de Android es src/androidMain, no src/main
        getByName("main").baselineProfiles.srcDir("src/androidMain/baselineProfiles")
    }
    testOptions {
        // Las capturas necesitan los recursos de la app: fuentes y pensums
        unitTests.isIncludeAndroidResources = true
    }
    buildTypes {
        release {
            // R8 encoge y optimiza: en un teléfono modesto es lo que más se nota frente a la versión de depuración
            isMinifyEnabled = true
            isShrinkResources = true
            // Lo de serie en release, escrito para que nadie lo active sin querer: depurable, ART no compila con el perfil
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Firmado con la clave de depuracion para poder instalarlo a mano
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    // La actividad vacía donde las pruebas de pantalla montan lo que capturan
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.10.5")
}

// Linter mínimo: las reglas viven en .editorconfig (solo imports, espacios y líneas en blanco).
// Las capturas generadas y el código de otros no se miran.
ktlint {
    version.set("1.7.1")
    android.set(true)
    ignoreFailures.set(false)
    filter {
        exclude { it.file.path.contains("${File.separator}build${File.separator}") }
    }
}

// Pruebas rápidas por defecto: la lógica y los recorridos Qa*. Las capturas y los informes de coste
// (Captura*, CosteCuadros, Rendimiento) no afirman nada: solo detectan cuelgues y escriben PNG o cifras.
// Corren con `./gradlew :composeApp:capturas`, o con -Pcapturas junto a testDebugUnitTest.
val lentas = listOf("**/*Captura*", "**/*CosteCuadros*", "**/*Rendimiento*")

tasks.register("capturas") {
    group = "verification"
    description = "Corre solo las capturas y los informes de coste (las pruebas que no afirman nada)"
    dependsOn("testDebugUnitTest")
}

// Se decide al configurar, mirando qué tareas se pidieron: más tarde la prueba ya tiene su lista de clases
val soloLentas = gradle.startParameter.taskNames.any { it == "capturas" || it.endsWith(":capturas") }
val todas = soloLentas || providers.gradleProperty("capturas").isPresent
tasks.withType<Test>().configureEach {
    if (soloLentas) include(lentas) else if (!todas) exclude(lentas)
}
