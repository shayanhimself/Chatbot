package com.shayanaryan.chatbot.shared.claude

import java.io.File
import java.util.Properties

private const val API_KEY_ENVIRONMENT_VARIABLE = "ANTHROPIC_API_KEY"
private const val LOCAL_PROPERTIES_PATH = "../local.properties"
private const val API_KEY_PROPERTY = "anthropic.api.key"

/** Printed by a test that found no key, so the run reads as skipped rather than as passed. */
internal const val SKIP_MESSAGE = "SKIPPED: no dev key in ANTHROPIC_API_KEY or local.properties."

/**
 * A developer's own key, from the environment or `local.properties`, and null when neither holds
 * one.
 *
 * Only the tests that hit the real API read it.
 */
internal fun devApiKey(): String? {
    System.getenv(API_KEY_ENVIRONMENT_VARIABLE)?.takeIf { it.isNotBlank() }?.let { return it }
    val properties = File(LOCAL_PROPERTIES_PATH).takeIf { it.exists() } ?: return null
    return properties
        .inputStream()
        .use { Properties().apply { load(it) } }
        .getProperty(API_KEY_PROPERTY)
        ?.takeIf { it.isNotBlank() }
}
