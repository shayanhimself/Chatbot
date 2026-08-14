plugins {
    alias(libs.plugins.chatbot.android.library.screenshot)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.shayanaryan.chatbot.feature.chat"
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
    // Supplies androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel, including the chat detail
    // ViewModel needs. hilt-navigation-compose would drag Navigation 2 in.
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)

    testImplementation(projects.core.testing)
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

    screenshotTestImplementation(projects.core.testing)
    // Provides the @PreviewTest annotation; screenshot plugin does not add it to the classpath itself.
    screenshotTestImplementation(libs.screenshot.validation.api)
}
