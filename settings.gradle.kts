rootProject.name = "liftly"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":androidApp")
include(":composeApp")
include(":core:designsystem")
include(":core:domain")
include(":core:data")
include(":core:protocol")
include(":core:network")
include(":core:datastore")
include(":core:platform")
include(":core:resources")
include(":feature:home")
include(":feature:workout")
include(":feature:exercises")
include(":feature:routines")
include(":feature:history")
include(":feature:settings")
include(":server")
