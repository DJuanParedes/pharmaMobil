import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

// Sesión 13: el complemento lee google-services.json (se descarga de la consola de Firebase
// y va en androidApp/). Sin el archivo el proyecto compila igual y la telemetría queda inactiva.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.googleServices.get().pluginId)
}

// Sesión 14, paso 3: las credenciales de firma viven en keystore.properties (raíz, ignorado por Git).
// Quien clona sin ese archivo compila igual: la depuración funciona y solo la entrega queda sin firmar.
val firma = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val hayFirma = !firma.getProperty("storeFile").isNullOrBlank()

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("androidx.lifecycle:lifecycle-viewmodel:2.11.0-beta01")
    androidTestImplementation(libs.ktor.client.core)
    androidTestImplementation("io.ktor:ktor-client-auth:${libs.versions.ktor.get()}")
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "pe.edu.upeu.pharmamobil"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        if (hayFirma) {
            create("release") {
                storeFile = file(firma.getProperty("storeFile"))
                storePassword = firma.getProperty("storePassword")
                keyAlias = firma.getProperty("keyAlias")
                keyPassword = firma.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        // Sesión 14: identificador y versión de la entrega (sección 6 de la guía)
        applicationId = "pe.upeu.pharmamobil"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Sesión 14: dirección de PharmaSoft. El emulador usa 10.0.2.2; un teléfono real necesita
        // la IP de la máquina en la red local: ./gradlew ... -PpharmaApiUrl=http://192.168.1.40:8080/api/v1/
        manifestPlaceholders["pharmaApiUrl"] = (findProperty("pharmaApiUrl") as String?)
            ?: System.getenv("PHARMA_API_URL")?.takeIf { it.isNotBlank() }
            ?: "http://10.0.2.2:8080/api/v1/"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            if (hayFirma) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}
