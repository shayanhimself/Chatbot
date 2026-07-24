package com.shayanaryan.chatbot.shared.chat

/**
 * Server-sent event streams used by the engine tests. [HAPPY_PATH] is a verbatim capture from the
 * live Messages API (see `scripts/record-sse-fixture.sh`), normalized only to strip the server's
 * cosmetic line padding; the rest are edits of it. It carries four `text_delta` frames so tests
 * prove delta accumulation, and the full server usage shape so the parser is exercised against the
 * unknown fields (`cache_creation`, `service_tier`, `inference_geo`, ...) it must tolerate.
 */
internal object SseFixtures {
    val HAPPY_PATH =
        """
        event: message_start
        data: {"type":"message_start","message":{"model":"claude-sonnet-5","id":"msg_011CdLU4SeaXXEvj6ns4KHHo","type":"message","role":"assistant","content":[],"stop_reason":null,"stop_sequence":null,"stop_details":null,"usage":{"input_tokens":28,"cache_creation_input_tokens":0,"cache_read_input_tokens":0,"cache_creation":{"ephemeral_5m_input_tokens":0,"ephemeral_1h_input_tokens":0},"output_tokens":1,"service_tier":"standard","inference_geo":"global"}}}

        event: content_block_start
        data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}

        event: ping
        data: {"type":"ping"}

        event: content_block_delta
        data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"**"}}

        event: content_block_delta
        data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"SSE (Server-Sent Events)** is a web technology that lets a server push real-time updates to a client over a single,"}}

        event: content_block_delta
        data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":" long-lived HTTP connection. It's a one-way communication channel—only the"}}

        event: content_block_delta
        data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":" server sends data to the client—commonly used for live feeds, notifications, or streaming updates."}}

        event: content_block_stop
        data: {"type":"content_block_stop","index":0}

        event: message_delta
        data: {"type":"message_delta","delta":{"stop_reason":"end_turn","stop_sequence":null,"stop_details":null},"usage":{"input_tokens":28,"cache_creation_input_tokens":0,"cache_read_input_tokens":0,"output_tokens":93,"output_tokens_details":{"thinking_tokens":0}}}

        event: message_stop
        data: {"type":"message_stop"}

        """.trimIndent()

    val REFUSAL = HAPPY_PATH.replace("\"stop_reason\":\"end_turn\"", "\"stop_reason\":\"refusal\"")

    val THINKING_ONLY_DELTA =
        """
        event: message_start
        data: {"type":"message_start","message":{"id":"msg_01Think","type":"message","role":"assistant","model":"claude-sonnet-5","content":[],"stop_reason":null,"usage":{"input_tokens":9,"output_tokens":1}}}

        event: content_block_delta
        data: {"type":"content_block_delta","index":0,"delta":{"type":"thinking_delta","thinking":""}}

        event: content_block_delta
        data: {"type":"content_block_delta","index":1,"delta":{"type":"text_delta","text":"Done"}}

        event: message_delta
        data: {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"output_tokens":3}}

        event: message_stop
        data: {"type":"message_stop"}

        """.trimIndent()

    val MID_STREAM_ERROR =
        """
        event: message_start
        data: {"type":"message_start","message":{"id":"msg_01Err","type":"message","role":"assistant","model":"claude-sonnet-5","content":[],"stop_reason":null,"usage":{"input_tokens":11,"output_tokens":1}}}

        event: content_block_delta
        data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hel"}}

        event: error
        data: {"type":"error","error":{"type":"overloaded_error","message":"Overloaded"}}

        """.trimIndent()

    val TRUNCATED =
        """
        event: message_start
        data: {"type":"message_start","message":{"id":"msg_01Cut","type":"message","role":"assistant","model":"claude-sonnet-5","content":[],"stop_reason":null,"usage":{"input_tokens":11,"output_tokens":1}}}

        event: content_block_delta
        data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hel"}}

        """.trimIndent()

    val MALFORMED_JSON =
        """
        event: message_start
        data: {"type":"message_start","message":

        """.trimIndent()
}
