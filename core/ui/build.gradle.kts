plugins {
    alias(libs.plugins.chatbot.android.library.screenshot)
}

android {
    namespace = "com.shayanaryan.chatbot.core.ui"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Provides the @PreviewTest annotation; screenshot plugin does not add it to the classpath itself.
    screenshotTestImplementation(libs.screenshot.validation.api)
}
