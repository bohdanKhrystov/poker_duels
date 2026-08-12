pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "poker-duels"

include(":poker-engine")
include(":poker-ai")
include(":poker-server")
