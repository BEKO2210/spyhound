pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Build-time only (libadb-android is not on Maven Central); nothing else is resolved from here.
        maven("https://jitpack.io") {
            content { includeGroupAndSubgroups("com.github.MuntashirAkon") }
        }
    }
}
rootProject.name = "Spyhound"
include(":app")
include(":testapp")
