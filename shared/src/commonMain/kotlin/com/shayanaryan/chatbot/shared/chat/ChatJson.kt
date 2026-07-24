package com.shayanaryan.chatbot.shared.chat

import com.shayanaryan.chatbot.shared.chat.dto.ContentDeltaDto
import com.shayanaryan.chatbot.shared.chat.dto.SseEventDto
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

/** Shared codec for the Messages API. */
@OptIn(ExperimentalSerializationApi::class)
internal val chatJson =
    Json {
        // Keeps new server fields from breaking us.
        ignoreUnknownKeys = true
        // Emits `stream: true`.
        encodeDefaults = true
        // Omits an absent `system`.
        explicitNulls = false
        // Folds an unknown or null `stop_reason` onto StopReason.Unknown.
        coerceInputValues = true
        serializersModule =
            SerializersModule {
                // Fallback deserializers used when an incoming `type` discriminator matches no
                // registered variant, so an unknown frame or delta kind decodes instead of throwing.
                polymorphicDefaultDeserializer(
                    SseEventDto::class,
                ) { SseEventDto.Unknown.serializer() }
                polymorphicDefaultDeserializer(
                    ContentDeltaDto::class,
                ) { ContentDeltaDto.Other.serializer() }
            }
    }
