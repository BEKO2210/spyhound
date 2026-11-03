import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
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
        versionCode = 14
        versionName = "0.3.9"
        // Shizuku reuses a running user service while this number stays the same. Derive it from the
        // service's interface and code, so a changed service is always restarted after an update.
        val shellSources = listOf("src/main/aidl/de/belkis/spyhound/shell/IShellService.aidl", "src/main/kotlin/de/belkis/spyhound/shell/Shell.kt")
            .joinToString("") { file(it).readText() }
        buildConfigField("int", "SHELL_SERVICE_VERSION", (shellSources.hashCode() and 0x7fffffff).toString())
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
            // Phones are ARM; the x86 builds of the native libraries only serve emulators and doubled the APK.
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
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
        unitTests.isIncludeAndroidResources = true
    }
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    // Screenshot capture keeps large bitmaps in memory; 512m regularly crashes the worker.
    maxHeapSize = "2048m"
    // Keep one JVM fork for deterministic binary test result writing.
    maxParallelForks = 1
    // Isolates parameterized screenshot cases to avoid classloader leakage between methods.
    forkEvery = 1
}

// Robolectric brings its own desktop Conscrypt; the Android build of it has no native library on the JVM.
configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
    exclude(group = "org.conscrypt", module = "conscrypt-android")
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)
    implementation(libs.appcompat)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.libadb.android)
    implementation(libs.conscrypt.android)
    implementation(libs.work.runtime)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.work.testing)
    debugImplementation(libs.compose.ui.test.manifest)
}
