plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("chatbot.robolectric")
}

kotlin {
    jvmToolchain(JDK_VERSION)

    android {
        compileSdk = COMPILE_SDK
        minSdk = MIN_SDK
    }
}
