package com.shayanaryan.chatbot.shared.database

import kotlinx.serialization.json.Json

/**
 * Codec for values stored as text in the database. Deliberately separate from the codec used on
 * the Messages API: an on-disk format has no migration story for a settings change made for HTTP
 * reasons,
 * and a stored value that stops decoding must fail loudly rather than be coerced to a default.
 */
internal val storageJson =
    Json {
        // A row written by a newer version still reads on an older one.
        ignoreUnknownKeys = true
        // Pins the stored shape against a change of library default.
        classDiscriminator = "type"
    }
