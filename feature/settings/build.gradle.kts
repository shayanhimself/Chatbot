plugins {
    alias(libs.plugins.chatbot.android.library.compose)
}

android {
    namespace = "com.shayanaryan.chatbot.feature.settings"
}

dependencies {
    implementation(projects.shared)
    implementation(projects.core.ui)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}
