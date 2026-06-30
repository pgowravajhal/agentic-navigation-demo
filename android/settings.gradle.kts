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
        // HERE SDK repository
        maven {
            url = uri("https://repo.here.com/artifactory/artifact-cache")
        }
    }
}

rootProject.name = "AgenticNavigator"
include(":app")
