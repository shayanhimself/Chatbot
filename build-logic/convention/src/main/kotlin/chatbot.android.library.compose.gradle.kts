plugins {
    // Listed alongside the convention plugin that applies it, so the `android` accessor resolves.
    id("com.android.library")
    id("chatbot.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    buildFeatures {
        compose = true
    }
}
