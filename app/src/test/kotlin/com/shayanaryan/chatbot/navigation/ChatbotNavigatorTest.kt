package com.shayanaryan.chatbot.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

private const val CHAT_ID = 7L
private const val OTHER_CHAT_ID = 9L

class ChatbotNavigatorTest {
    private fun backStack(vararg keys: NavKey) = NavBackStack(*keys)

    private fun navigator(
        backStack: NavBackStack<NavKey>,
        hasApiKeyAtStart: Boolean,
    ) = ChatbotNavigator(backStack, hasApiKeyAtStart)

    @Test
    fun `a missing key starts on onboarding`() {
        assertEquals(OnboardingKey, ChatbotNavigator.startKeyFor(hasApiKey = false))
    }

    @Test
    fun `a stored key starts on the chat list`() {
        assertEquals(ChatListKey, ChatbotNavigator.startKeyFor(hasApiKey = true))
    }

    @Test
    fun `storing a key rewrites the stack to the list and a new chat`() {
        val stack = backStack(OnboardingKey)
        val navigator = navigator(stack, hasApiKeyAtStart = false)

        navigator.resetForApiKeyState(hasApiKey = true)

        assertEquals(listOf(ChatListKey, ChatDetailKey()), stack.toList())
    }

    @Test
    fun `removing a key rewrites the stack to onboarding`() {
        val stack = backStack(ChatListKey, ChatDetailKey(CHAT_ID))
        val navigator = navigator(stack, hasApiKeyAtStart = true)

        navigator.resetForApiKeyState(hasApiKey = false)

        assertEquals(listOf(OnboardingKey), stack.toList())
    }

    @Test
    fun `a repeated value leaves the stack untouched`() {
        val stack = backStack(ChatListKey, ChatDetailKey(CHAT_ID))
        val navigator = navigator(stack, hasApiKeyAtStart = true)

        navigator.resetForApiKeyState(hasApiKey = true)

        assertEquals(listOf(ChatListKey, ChatDetailKey(CHAT_ID)), stack.toList())
    }

    @Test
    fun `a navigator built against the current value treats it as no transition`() {
        // What a restore looks like: the stack came back from saved state and the flag has not
        // moved, so nothing may discard where the user was.
        val stack = backStack(ChatListKey, ChatDetailKey(CHAT_ID))
        val navigator = navigator(stack, hasApiKeyAtStart = true)

        navigator.resetForApiKeyState(hasApiKey = true)
        navigator.resetForApiKeyState(hasApiKey = true)

        assertEquals(listOf(ChatListKey, ChatDetailKey(CHAT_ID)), stack.toList())
    }

    @Test
    fun `opening a chat from the list pushes a chat`() {
        val stack = backStack(ChatListKey)
        val navigator = navigator(stack, hasApiKeyAtStart = true)

        navigator.openChat(CHAT_ID)

        assertEquals(listOf(ChatListKey, ChatDetailKey(CHAT_ID)), stack.toList())
    }

    @Test
    fun `opening another chat replaces the open chat`() {
        val stack = backStack(ChatListKey)
        val navigator = navigator(stack, hasApiKeyAtStart = true)

        navigator.openChat(CHAT_ID)
        navigator.openChat(OTHER_CHAT_ID)

        assertEquals(listOf(ChatListKey, ChatDetailKey(OTHER_CHAT_ID)), stack.toList())
    }

    @Test
    fun `a new chat replaces an open chat rather than stacking on it`() {
        val stack = backStack(ChatListKey, ChatDetailKey(CHAT_ID))
        val navigator = navigator(stack, hasApiKeyAtStart = true)

        navigator.openNewChat()

        assertEquals(listOf(ChatListKey, ChatDetailKey()), stack.toList())
    }

    @Test
    fun `back pops the top entry`() {
        val stack = backStack(ChatListKey, ChatDetailKey(CHAT_ID))
        val navigator = navigator(stack, hasApiKeyAtStart = true)

        navigator.back()

        assertEquals(listOf(ChatListKey), stack.toList())
    }

    @Test
    fun `back leaves the last entry in place`() {
        val stack = backStack(ChatListKey)
        val navigator = navigator(stack, hasApiKeyAtStart = true)

        navigator.back()

        assertEquals(listOf(ChatListKey), stack.toList())
    }

    @Test
    fun `a second back before the screen leaves keeps the stack usable`() {
        val stack = backStack(ChatListKey, ChatDetailKey(CHAT_ID))
        val navigator = navigator(stack, hasApiKeyAtStart = true)

        navigator.back()
        navigator.back()

        assertEquals(listOf(ChatListKey), stack.toList())
    }

    @Test
    fun `a deep link flattens a stack that already has a chat`() {
        val stack = backStack(ChatListKey, ChatDetailKey(OTHER_CHAT_ID))
        val navigator = navigator(stack, hasApiKeyAtStart = true)

        navigator.openChatFromDeepLink(CHAT_ID)

        assertEquals(listOf(ChatListKey, ChatDetailKey(CHAT_ID)), stack.toList())
    }

    @Test
    fun `a deep link builds both entries from an arbitrary stack`() {
        val stack = backStack(OnboardingKey)
        val navigator = navigator(stack, hasApiKeyAtStart = true)

        navigator.openChatFromDeepLink(CHAT_ID)

        assertEquals(listOf(ChatListKey, ChatDetailKey(CHAT_ID)), stack.toList())
    }

    @Test
    fun `a deep link arriving with no key is dropped`() {
        val stack = backStack(OnboardingKey)
        val navigator = navigator(stack, hasApiKeyAtStart = false)

        navigator.openChatFromDeepLink(CHAT_ID)

        assertEquals(listOf(OnboardingKey), stack.toList())
    }
}
