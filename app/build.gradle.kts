plugins {
    alias(libs.plugins.chatbot.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.shayanaryan.chatbot"
    defaultConfig {
        applicationId = "com.shayanaryan.chatbot"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "com.shayanaryan.chatbot.HiltTestRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    buildFeatures {
        aidl = false
        buildConfig = true
        shaders = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// The Accessibility Test Framework the checks run on pulls a 2020 Material Components release,
// whose appcompat chain wants a newer fragment than the one the app itself resolves. The device
// test classpath must agree with the app's, and no check the app's screens hit reads a Material
// Components view.
configurations.androidTestImplementation {
    exclude(group = "com.google.android.material", module = "material")
}

dependencies {
    implementation(projects.shared)
    implementation(projects.core.ui)
    implementation(projects.feature.chat)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.settings)
    implementation(projects.feature.reminders)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.adaptive.navigation3)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    kspAndroidTest(libs.hilt.compiler)

    testImplementation(projects.shared.testing)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(projects.shared)
    androidTestImplementation(projects.shared.testing)
    androidTestImplementation(projects.core.testing)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // Carries the Accessibility Test Framework and the rule extension that runs its checks over
    // the node tree a screen reader is handed.
    androidTestImplementation(libs.androidx.compose.ui.test.junit4.accessibility)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(enforcedPlatform(libs.okhttp.bom))
    androidTestImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.okhttp.tls)
}
