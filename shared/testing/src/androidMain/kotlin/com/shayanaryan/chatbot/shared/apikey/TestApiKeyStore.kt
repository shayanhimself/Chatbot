package com.shayanaryan.chatbot.shared.apikey

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.ExternalResource
import java.util.concurrent.atomic.AtomicInteger

private const val STORE_NAME_PREFIX = "test_api_key_store"

/**
 * Numbers the file each store gets. Every store a process builds needs a name no other has taken.
 */
private val storeCount = AtomicInteger()

/**
 * A real key store over a file of the test's own, deleted when the test ends.
 * The test app holds one store over one file.
 *
 * `@Singleton` is no help: it scopes to a component, and a Hilt test builds a fresh one per test
 * method, so the second method to reach the app's store opens a file the first method still holds.
 *
 * The store is built as the rule is constructed rather than in `before`, so that a Hilt test may
 * hand [repository] straight to a `@BindValue` field, which is read while the test class is still
 * being constructed.
 *
 * @param context whose files directory holds the store, the application under test by default.
 * @param cipher what the store encrypts through, the device's Keystore by default.
 */
class TestApiKeyStore(
    context: Context = ApplicationProvider.getApplicationContext(),
    cipher: KeyCipher = createKeystoreKeyCipher(),
) : ExternalResource() {
    private val file =
        context.applicationContext.preferencesDataStoreFile(
            "$STORE_NAME_PREFIX-${storeCount.incrementAndGet()}",
        )

    /** The store itself, for a test asserting on the bytes rather than on what the repository reports. */
    val dataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { file })

    /** The repository the app would hold, over [dataStore]. */
    val repository: ApiKeyRepository =
        createApiKeyRepository(dataStore = dataStore, cipher = cipher)

    override fun after() {
        file.delete()
    }
}
