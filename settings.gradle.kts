// dependencyResolutionManagement has been @Incubating since Gradle 6.8 and is the only
// supported way to centralize repositories. The warning is not actionable.
@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "chatbot"

include(
    ":app",
    ":core:ui",
    ":shared",
    ":feature:conversation",
    ":feature:onboarding",
    ":feature:settings",
    ":feature:reminders",
)
