plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("chatbot.kover")
}

android {
    compileSdk = COMPILE_SDK
    defaultConfig {
        minSdk = MIN_SDK
        targetSdk = TARGET_SDK
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(JDK_VERSION)
        targetCompatibility = JavaVersion.toVersion(JDK_VERSION)
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(JDK_VERSION)
}
