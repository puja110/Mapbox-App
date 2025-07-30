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
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                // Do not change the username below.
                // This should always be ⁠ mapbox ⁠ (not your username).
                username = "mapbox"
                // Use the secret token you stored in gradle.properties as the password
                password = "pk.eyJ1IjoiY2hldHRyaXIxIiwiYSI6ImNtOTZrYzYzcTFoc20ya29qM25mazgzNjYifQ.M0J8vVHgTKMje0KjJxWSWg"
            }
        }
    }
}

rootProject.name = "Mapbox Demo"
include(":app")
