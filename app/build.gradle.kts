import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// A password do keystore vive em keystore.properties (chmod 600, fora do git).
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "pt.shrek.bruma"
    // O GeckoView 155 exige compilar contra 37.1 e recusa-se a ser consumido
    // por um projeto em 37.0 (verificação de metadados do AAR).
    compileSdk = 37
    compileSdkMinor = 1
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "pt.shrek.bruma"
        // O GeckoView 155 exige 26; o tor embutido 21; o Compose 23.
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.2"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")

                // O v2 chega para instalar em tudo o que a app suporta, e é o
                // que o AGP escolheu sozinho. Mas o v3 é o que permite **rodar
                // a chave** um dia: trocar a chave de assinatura sem obrigar
                // toda a gente a desinstalar e perder os dados. Uma app que
                // pode viver anos não deve fechar essa porta na v1.0.
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            // O kmp-tor resolve classes nativas por nome; encolher com o R8 sem
            // regras próprias parte o arranque do tor. Fica desligado até haver
            // um perfil de manutenção que justifique afiná-lo.
            isMinifyEnabled = false
            isShrinkResources = false
            // O uBlock Origin vem descompactado nos assets e é lido pelo Gecko
            // por resource://android/assets/ — nada disto pode ser encolhido.
            if (keystoreProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    // O GeckoView traz ~60 MB de biblioteca nativa por arquitetura, e o tor mais
    // ~10 MB. Sem divisão por ABI o APK levaria três motores que o telemóvel
    // nunca executa. O universal fica na mesma, para instalar sem saber a ABI.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        // O aapt descarta por omissão qualquer diretório começado por `_`
        // (padrão `<dir>_*`). O uBlock Origin guarda as traduções em
        // `_locales/`, por isso essa pasta nunca chegava ao APK e o Gecko
        // rejeitava a extensão inteira com um obscuro "Extension is invalid" —
        // o erro verdadeiro, `NS_ERROR_FILE_NOT_FOUND` em
        // `_locales/en/messages.json`, só aparecia no GeckoConsole.
        // Isto é o padrão original sem a regra `<dir>_*`.
        ignoreAssetsPatterns += listOf(
            "!.svn", "!.git", "!.ds_store", "!*.scc", ".*", "!CVS",
            "!thumbs.db", "!picasa.ini", "!*~",
        )
    }

    packaging {
        // O resource-exec-tor precisa que o binário do tor seja desempacotado
        // para o nativeLibDir na instalação, para poder ser executado.
        jniLibs.useLegacyPackaging = true
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/INDEX.LIST",
            )
        }
    }
}

// Extensão de topo: dentro de android { } o Kotlin 2.4 não a reconhece.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.vm)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.geckoview)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.okhttp)
    implementation(libs.coroutines.android)

    // Tor embutido: runtime + serviço em primeiro plano + binários do tor.
    implementation(libs.kmptor.runtime)
    implementation(libs.kmptor.service.ui)
    implementation(libs.kmptor.resource.exec)

    testImplementation("junit:junit:4.13.2")
}
