plugins {
    alias(libs.plugins.chatbot.android.library)
}

android {
    namespace = "com.shayanaryan.chatbot.core.testing"
}

dependencies {
    api(platform(libs.androidx.compose.bom))

    // api, not implementation: both helpers put their dependencies in the consumer's face. A
    // module annotating a preview with @FormFactorPreviews needs @Preview and Devices to
    // resolve, and one calling string() needs @StringRes, which arrives with the test artifact.
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.test.core)
}
