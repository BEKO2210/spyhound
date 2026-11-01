import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release key lives outside the checkout (~/.config/spyhound), never in git.
val releasePropertiesFile = providers.gradleProperty("spyhound.keystoreProperties")
    .map { rootProject.file(it) }
    .getOrElse(file("${System.getProperty("user.home")}/.config/spyhound/keystore.properties"))
val releaseProperties = Properties().apply {
    if (releasePropertiesFile.isFile) releasePropertiesFile.inputStream().use { load(it) }
}
val signingKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
val hasReleaseSigning = signingKeys.all { !releaseProperties.getProperty(it).isNullOrBlank() }

android {
    namespace = "de.belkis.spyhound"
    compileSdk = 37

    defaultConfig {
        applicationId = "de.belkis.spyhound"
        minSdk = 29
        // Sideload-only app (no Play Store): QUERY_ALL_PACKAGES, READ_SMS and
        // MANAGE_EXTERNAL_STORAGE are used without Play policy constraints.
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (hasReleaseSigning) create("release") {
            storeFile = releasePropertiesFile.parentFile.resolve(releaseProperties.getProperty("storeFile"))
            storePassword = releaseProperties.getProperty("storePassword")
            keyAlias = releaseProperties.getProperty("keyAlias")
            keyPassword = releaseProperties.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }
    androidResources {
        // Indicator bundles are read as-is; compressing them only costs startup time.
        noCompress += listOf("stix2", "txt", "json")
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
