plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "com.shayanaryan.chatbot.feature.conversation"
    compileSdk = 37
    defaultConfig {
        minSdk = 31
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    // Gates screenshotTest source-set creation
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Robolectric needs a Java 21 runtime for SDK 36; compile toolchain stays 17.
tasks.withType<Test>().configureEach {
    javaLauncher.set(
        project.extensions.getByType<JavaToolchainService>().launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        },
    )
}

dependencies {
    implementation(projects.shared)
    implementation(projects.core.ui)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    // Supplies androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel, including the
    // creationCallback overload the assisted-inject chat ViewModel needs. Deliberately not
    // hilt-navigation-compose, which would drag navigation-compose (Navigation 2) in.
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)

    testImplementation(projects.shared.testing)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    // Carries the JVM actuals for kotlin.test's annotations. This module is a plain Android
    // library, so nothing substitutes the framework variant the way KMP does in :shared, and
    // without this every kotlin.test import in the tests below is unresolved.
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Provides the @PreviewTest annotation; screenshot plugin does not add it to the classpath itself.
    screenshotTestImplementation(libs.screenshot.validation.api)
}
