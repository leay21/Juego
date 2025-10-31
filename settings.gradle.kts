pluginManagement {
    repositories {
        // Ya no filtramos el contenido de Google.
        // Esto permite que KSP (y KAPT) se descarguen.
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
    }
}

rootProject.name = "Juego"
include(":app")