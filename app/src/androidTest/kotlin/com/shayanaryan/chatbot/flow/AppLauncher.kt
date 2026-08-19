package com.shayanaryan.chatbot.flow

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import com.shayanaryan.chatbot.MainActivity

/**
 * Starts and stops the Activity that one flow test drives, so no test leaves one running behind it.
 */
internal class AppLauncher {
    private var scenario: ActivityScenario<MainActivity>? = null

    /**
     * Starts the app, on [intent] when one is given and on a plain launch otherwise.
     *
     * @return the scenario, for a test that drives the Activity itself rather than its screens.
     */
    fun launch(intent: Intent? = null): ActivityScenario<MainActivity> {
        val started =
            intent
                ?.let { ActivityScenario.launch(it) }
                ?: ActivityScenario.launch(MainActivity::class.java)
        scenario = started
        return started
    }

    /** Ends the Activity this test started, and does nothing when it never started one. */
    fun close() {
        scenario?.close()
        scenario = null
    }
}
