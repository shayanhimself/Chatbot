// dependencyResolutionManagement has been @Incubating since Gradle 6.8 and is the only
// supported way to centralize repositories. The warning is not actionable.
@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // This build sits outside the main one, so its catalog is not discovered automatically.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"

include(":convention")
