package com.shayanaryan.chatbot.shared.claude

import com.shayanaryan.chatbot.shared.claude.dto.ContentDeltaDto
import com.shayanaryan.chatbot.shared.claude.dto.SseEventDto
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

/** Shared codec for the Claude Messages API. */
@OptIn(ExperimentalSerializationApi::class)
internal val claudeJson =
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
