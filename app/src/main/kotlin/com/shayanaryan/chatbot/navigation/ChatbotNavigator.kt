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

    /** Opens conversation [id], replacing the open chat rather than stacking on it. */
    fun openConversation(id: Long) = openChat(ChatKey(id))

    /** Opens a new chat, replacing the open chat rather than stacking. */
    fun openNewChat() = openChat(ChatKey())

    /** Pops the top entry, and does nothing on a stack with none. */
    fun back() {
        backStack.removeLastOrNull()
    }

    /**
     * Opens the conversation an intent from outside the app asked for.
     *
     * Unlike [openConversation] this can assume nothing about the stack it arrives against, so it
     * discards what is there and builds the two entries a notification should land on.
     */
    fun openConversationFromDeepLink(id: Long) {
        // A notification can outlive the key that scheduled it. With no key there is nothing to
        // resume into and onboarding is the only destination the app may show, so the intent is
        // dropped rather than held until a key arrives.
        if (!appliedHasApiKey) return
        backStack.clear()
        backStack.add(ConversationListKey)
        backStack.add(ChatKey(id))
    }

    /**
     * Moves the app between onboarding and the conversation list when a key is stored or removed.
     *
     * @param hasApiKey the current gate value. Nothing happens while it matches the last value
     *   applied, which is the ordinary case: the effect driving this runs on entering composition,
     *   against a navigator built in the same composition from the same value.
     */
    fun resetForApiKeyState(hasApiKey: Boolean) {
        if (hasApiKey == appliedHasApiKey) return
        appliedHasApiKey = hasApiKey
        backStack.clear()
        if (hasApiKey) {
            // A new chat above the list, so the user can immediately use what they just set up.
            backStack.add(ConversationListKey)
            backStack.add(ChatKey())
        } else {
            backStack.add(OnboardingKey)
        }
    }

    /**
     * Opens [key], replacing the chat already on top rather than stacking on it.
     *
     * The list is always the entry below a chat, and replacing is what gives a different
     * conversation a different ViewModel.
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
         * Chooses the destination a launch starts on.
         *
         * A launch seeds this key alone, while [resetForApiKeyState] lands on a new chat above the
         * list. A cold start belongs on the list, and a key just stored belongs on what the user
         * set up with it.
         *
         * @return [OnboardingKey] without a stored key, which is then the only destination the app
         *   may show, and [ConversationListKey] with one.
         */
        fun startKeyFor(hasApiKey: Boolean): NavKey =
            if (hasApiKey) ConversationListKey else OnboardingKey
    }
}
