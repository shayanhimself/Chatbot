package com.shayanaryan.chatbot.shared.model

/**
 * A Claude model the user can pick for a chat.
 *
 * @param id the wire identifier sent as the API's `model` field.
 * @param displayName the name shown to the user. It lives here rather than in a feature's
 *   `strings.xml` because it is a product identifier, not copy: byte-identical in every locale,
 *   and the untranslatable half of the same pair as [id]. Keeping it on the model means every
 *   screen that names a model reads one source instead of duplicating the table.
 */
enum class ClaudeModel(
    val id: String,
    val displayName: String,
) {
    Sonnet("claude-sonnet-5", "Sonnet 5"),
    Haiku("claude-haiku-4-5", "Haiku 4.5"),
    Opus("claude-opus-5", "Opus 5"),
    ;

    companion object {
        val Default = Sonnet
    }
}
