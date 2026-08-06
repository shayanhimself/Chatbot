package com.shayanaryan.chatbot.core.ui.viewmodel

/**
 * How long a `stateIn` pipeline keeps collecting after its last subscriber leaves. Long enough to
 * survive a configuration change, short enough that a backgrounded screen stops reading its
 * sources.
 *
 * Every feature's ViewModels share one value, so a screen's collection behaviour cannot drift from
 * another's by nothing more than a differently written literal.
 */
const val SUBSCRIPTION_TIMEOUT_MILLIS: Long = 5_000L
