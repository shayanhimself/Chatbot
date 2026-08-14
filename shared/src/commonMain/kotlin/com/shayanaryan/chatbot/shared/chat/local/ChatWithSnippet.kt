package com.shayanaryan.chatbot.shared.chat.local

import androidx.room.Embedded
import com.shayanaryan.chatbot.shared.ContentBlock

/**
 * A chat row plus the content of its last complete message, projected by the query rather
 * than stored. Nothing writes [snippet], so it cannot drift from the messages it summarizes.
 *
 * @property chat the `chats` row itself, embedded so the column list lives in one
 *   place rather than being restated here.
 * @property snippet null for a chat whose only messages failed or were cancelled, and for
 *   one whose first turn has not finished.
 */
internal data class ChatWithSnippet(
    @Embedded val chat: ChatEntity,
    val snippet: List<ContentBlock>?,
)
