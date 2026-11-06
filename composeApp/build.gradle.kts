import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("com.android.application")
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
    testOptions {
        // Las capturas necesitan los recursos de la app: fuentes y pensums
        unitTests.isIncludeAndroidResources = true
    }
    buildTypes {
        release {
            // R8 encoge y optimiza: en un teléfono modesto es lo que más se nota frente a la versión de depuración
            isMinifyEnabled = true
            isShrinkResources = true
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
