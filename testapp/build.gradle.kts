plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "de.belkis.spyhound.testspy"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.belkis.spyhound.testspy"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {}
