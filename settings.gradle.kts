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

// Type-safe project accessors.
// Still a feature preview on Gradle 9.6.1, so it stays opt-in.
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "chatbot"

include(
    ":app",
    ":core:ui",
    ":shared",
    ":shared:testing",
    ":feature:conversation",
    ":feature:onboarding",
    ":feature:settings",
    ":feature:reminders",
)
