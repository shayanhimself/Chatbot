package com.shayanaryan.chatbot.shared.conversation.local

import androidx.room.Embedded
import com.shayanaryan.chatbot.shared.chat.ContentBlock

/**
 * A conversation row plus the content of its last complete message, projected by the query rather
 * than stored. Nothing writes [snippet], so it cannot drift from the messages it summarizes.
 *
 * @property conversation the `conversations` row itself, embedded so the column list lives in one
 *   place rather than being restated here.
 * @property snippet null for a conversation whose only messages failed or were cancelled, and for
 *   one whose first turn has not finished.
 */
internal data class ConversationWithSnippet(
    @Embedded val conversation: ConversationEntity,
    val snippet: List<ContentBlock>?,
)
