package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.chat.ChatEngine
import com.shayanaryan.chatbot.shared.database.ChatbotDatabase
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Clock

/**
 * Assembles the repository over a database. The DAOs stay inside this module; callers pass the
 * database as an opaque handle.
 *
 * @param externalScope where a turn runs. It must outlive any screen, since a reply is persisted
 *   whether anything is still collecting it, and it must be supervised — turns are siblings, so
 *   under a regular job one that throws would take every later turn down with it.
 * @param clock injected so tests can assert exact timestamps.
 */
fun createConversationRepository(
    database: ChatbotDatabase,
    engine: ChatEngine,
    externalScope: CoroutineScope,
    clock: Clock = Clock.System,
): ConversationRepository =
    DefaultConversationRepository(
        engine = engine,
        conversationDao = database.conversationDao(),
        messageDao = database.messageDao(),
        externalScope = externalScope,
        clock = clock,
    )
