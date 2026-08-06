package com.shayanaryan.chatbot.core.testing

import android.content.Context
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider

/**
 * The copy a test asserts on, read from the resource the screen itself resolves rather than
 * repeated as a literal, so rewording is a one-file change and a test can never assert copy the
 * app stopped shipping.
 *
 * Robolectric supplies the context, so this needs no Activity and works under a plain
 * `createComposeRule`.
 *
 * @param formatArgs the arguments of a formatted string, in the order the resource declares them.
 */
fun string(
    @StringRes id: Int,
    vararg formatArgs: Any,
): String = ApplicationProvider.getApplicationContext<Context>().getString(id, *formatArgs)
