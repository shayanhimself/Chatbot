plugins {
    id("com.android.library")
}

android {
    compileSdk = COMPILE_SDK
    defaultConfig {
        minSdk = MIN_SDK
    }
    compileOptions {
        targetCompatibility = JavaVersion.toVersion(JDK_VERSION)
    }
}
