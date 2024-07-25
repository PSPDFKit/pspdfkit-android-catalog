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
        // The PSPDFKit library is loaded from the PSPDFKit Maven repository, added by this configuration.
        maven(url = "https://customers.pspdfkit.com/maven/")
    }

    versionCatalogs {
    }
}

rootProject.name = "catalog"
include(":app")
