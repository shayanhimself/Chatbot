package com.shayanaryan.chatbot.shared.chat

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
