plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "com.shayanaryan.chatbot.feature.onboarding"
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
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)

    testImplementation(projects.core.testing)
    testImplementation(projects.shared.testing)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    screenshotTestImplementation(projects.core.testing)
    screenshotTestImplementation(libs.screenshot.validation.api)
}
