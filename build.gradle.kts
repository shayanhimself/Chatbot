// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.spotless)
}

allprojects {
    apply(
        plugin =
            rootProject.libs.plugins.spotless
                .get()
                .pluginId,
    )

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        // Style is configured in .editorconfig, so the IDE and the build agree.
        kotlin {
            target("src/**/*.kt")
            ktlint(
                rootProject.libs.versions.ktlint
                    .get(),
            )
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint(
                rootProject.libs.versions.ktlint
                    .get(),
            )
        }
    }
}
