plugins {
    id("chatbot.kover")
}

tasks.withType<Test>().configureEach {
    javaLauncher.set(
        project.extensions
            .getByType<JavaToolchainService>()
            .launcherFor {
                languageVersion.set(JavaLanguageVersion.of(TEST_JDK_VERSION))
            },
    )
}
