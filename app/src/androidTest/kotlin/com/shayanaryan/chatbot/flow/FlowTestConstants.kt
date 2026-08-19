package com.shayanaryan.chatbot.flow


/**
 * Why a test that types before it clicks is ignored.
 *
 * Focusing a field opens the soft keyboard, and the inset animation that follows moves the layout
 * for several frames. Nothing reports that motion: the composition is idle throughout, so the tap
 * is injected at the coordinates the target held when it was aimed and lands beside it. The screen
 * is then left with the typed text still in the field and nothing sent.
 */
internal const val IME_ANIMATION_RACE =
    "Flaky: the soft keyboard's inset animation moves the target between aiming and clicking."
