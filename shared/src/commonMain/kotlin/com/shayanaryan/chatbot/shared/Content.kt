package com.shayanaryan.chatbot.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Who wrote a message. */
enum class Role { User, Assistant }

/**
 * A single piece of a message. It's sent to the API, persisted, and rendered.
 *
 * Modelled as a list on the message types rather than a bare string so additional block types can
 * be added without reshaping them.
 */
@Serializable
sealed interface ContentBlock {
    @Serializable
    @SerialName("text")
    data class Text(
        val text: String,
    ) : ContentBlock
}

/**
 * A message's content as text a person reads: every [ContentBlock.Text] in order, concatenated.
 *
 * This is the single answer to which blocks count as a message's text.
 *
 * @return the joined text, empty when the message carries no text block.
 */
fun List<ContentBlock>.textContent(): String =
    filterIsInstance<ContentBlock.Text>().joinToString(separator = "") {
        it.text
    }
