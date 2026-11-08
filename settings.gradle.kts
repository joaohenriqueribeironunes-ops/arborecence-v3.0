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
    }
}

rootProject.name = "ArborecenceV3"
include(":app")
include(":core:common")
include(":core:domain")
include(":core:data")
include(":core:ui")
include(":feature:home")
include(":feature:voice-dump")
include(":feature:notes")
include(":feature:focus")
include(":feature:review")
include(":feature:learning")
include(":feature:assistant")
include(":feature:settings")
include(":feature:onboarding")
