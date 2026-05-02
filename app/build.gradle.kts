import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(keystorePropertiesFile.inputStream())
    }
}

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        load(versionPropsFile.inputStream())
    }
}

val versionMajor = (versionProps["VERSION_MAJOR"] as String?)?.toIntOrNull() ?: 1
val versionMinor = (versionProps["VERSION_MINOR"] as String?)?.toIntOrNull() ?: 0
val appVersionCode = (versionProps["VERSION_CODE"] as String?)?.toIntOrNull() ?: 1

android {
    namespace = "com.investhelp.app"
    compileSdk = 35

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }

    defaultConfig {
        applicationId = "com.investhelp.app"
        minSdk = 29
        targetSdk = 35
        versionCode = appVersionCode
        versionName = "$versionMajor.$versionMinor"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Core
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DocumentFile (for folder picker)
    implementation(libs.androidx.documentfile)

    // Splash Screen
    implementation(libs.androidx.splashscreen)

    // Image loading
    implementation(libs.coil.compose)
}

tasks.configureEach {
    if (name.matches(Regex("assemble(Debug|Release)"))) {
        doLast {
            val props = Properties().apply {
                versionPropsFile.inputStream().use { load(it) }
            }
            val major = (props["VERSION_MAJOR"] as String).toInt()
            val minor = (props["VERSION_MINOR"] as String).toInt()
            val code = (props["VERSION_CODE"] as String).toInt()
            val newMinor = minor + 1
            val newCode = code + 1
            props["VERSION_MINOR"] = newMinor.toString()
            props["VERSION_CODE"] = newCode.toString()
            versionPropsFile.outputStream().use { props.store(it, null) }
            println("Version bumped: $major.$minor (code $code) -> $major.$newMinor (code $newCode)")
        }
    }
}
