pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // The Nutrient library is loaded from the Nutrient Maven repository, added by this configuration.
        maven(url = "https://my.nutrient.io/maven/")
    }

    versionCatalogs {
    }
}

rootProject.name = "catalog"
include(":app")
