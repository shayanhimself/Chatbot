package com.shayanaryan.chatbot.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * The only thing that mutates the back stack. Every rule that rewrites it lives here as a named
 * operation, so features keep receiving lambdas and knowing nothing about navigation.
 *
 * @param hasApiKeyAtStart the gate value the stack was built from. Seeding it here is what makes a
 *   restore, which rebuilds this object against the flag as it stands, report no transition and
 *   leave the restored stack alone.
 */
internal class ChatbotNavigator(
    private val backStack: NavBackStack<NavKey>,
    hasApiKeyAtStart: Boolean,
) {
    private var appliedHasApiKey: Boolean = hasApiKeyAtStart

    fun openConversation(id: Long) = openChat(ChatKey(id))

    fun openNewChat() = openChat(ChatKey())

    fun back() {
        backStack.removeLastOrNull()
    }

    /**
     * Serves an intent from outside the app. Unlike [openConversation] it can assume nothing about
     * what is on the stack, so it discards it and builds the two entries a notification should
     * land on.
     *
     * A notification can outlive the key that scheduled it. With no key there is nothing to resume
     * into and onboarding is the only destination the app may show, so the intent is dropped
     * rather than held until a key arrives.
     */
    fun openConversationFromDeepLink(id: Long) {
        if (!appliedHasApiKey) return
        backStack.clear()
        backStack.add(ConversationListKey)
        backStack.add(ChatKey(id))
    }

    /**
     * Rewrites the stack when the gate value changes, and only then. The effect that drives this
     * runs on entering composition against a navigator built in the same composition from the same
     * value, so equality is the ordinary case on every launch and a change is the rare one.
     *
     * Storing a key lands on the list plus a new chat, where the user can immediately use what
     * they just set up. Removing one closes the gate again, which is the same rule read backwards.
     */
    fun resetForApiKeyState(hasApiKey: Boolean) {
        if (hasApiKey == appliedHasApiKey) return
        appliedHasApiKey = hasApiKey
        backStack.clear()
        if (hasApiKey) {
            backStack.add(ConversationListKey)
            backStack.add(ChatKey())
        } else {
            backStack.add(OnboardingKey)
        }
    }

    /**
     * The list is always the entry below a chat, so a second chat replaces the first rather than
     * stacking on it. That is what gives a different conversation a different ViewModel.
     */
    private fun openChat(key: ChatKey) {
        if (backStack.lastOrNull() is ChatKey) {
            backStack[backStack.lastIndex] = key
        } else {
            backStack.add(key)
        }
    }

    companion object {
        /**
         * Where a launch lands, given whether a key is stored. Without one, onboarding is the only
         * reachable destination; with one, the list is the home screen.
         *
         * A launch seeds the list alone, while [resetForApiKeyState] adds a new chat above it. A
         * cold start belongs on the list; storing a key belongs on what the user just set up.
         */
        fun startKeyFor(hasApiKey: Boolean): NavKey =
            if (hasApiKey) ConversationListKey else OnboardingKey
    }
}
