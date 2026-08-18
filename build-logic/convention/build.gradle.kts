plugins {
    `kotlin-dsl`
}

group = "com.shayanaryan.chatbot.buildlogic"

kotlin {
    // The literal is unavoidable: a build script cannot see the constants in the source set it
    // compiles. Keep it in step with JDK_VERSION.
    jvmToolchain(17)
}

dependencies {
    // compileOnly, not implementation: the build that applies these convention plugins already
    // carries the Android and Kotlin plugins, so shipping them again would duplicate them there.
    compileOnly(libs.android.gradlePlugin)
    // Pinning the version of Kotlin gradle plugin, otherwise AGP decides the version
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)

    // implementation, not compileOnly: these are applied by id at execution time, so they have
    // to reach the classpath of the build that applies the convention plugin.
    implementation(libs.screenshot.gradlePlugin)
    implementation(libs.kover.gradlePlugin)
}
