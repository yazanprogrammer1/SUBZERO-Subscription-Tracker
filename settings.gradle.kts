pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com[.]android.*")
                includeGroupByRegex("com[.]google.*")
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
        google {
            content {
                includeGroupByRegex("com[.]android.*")
                includeGroupByRegex("com[.]google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "SUBZERO"

// Enable type-safe project accessors: projects.core.domain instead of project(":core:domain")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

include(":core:common")
include(":core:domain")
include(":core:data")
include(":core:designsystem")
include(":core:navigation")
include(":core:testing")

include(":feature:onboarding")
include(":feature:home")
include(":feature:subscriptions")
include(":feature:calendar")
include(":feature:insights")
include(":feature:settings")
