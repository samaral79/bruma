pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // O GeckoView não está no Maven Central nem no repositório da Google.
        maven("https://maven.mozilla.org/maven2") {
            content { includeGroupByRegex("org\\.mozilla.*") }
        }
    }
}

rootProject.name = "Bruma"
include(":app")
