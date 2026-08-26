package com.shayanaryan.chatbot.shared.database

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

private const val SCHEMA_DIRECTORY =
    "schemas/com.shayanaryan.chatbot.shared.database.ChatbotDatabase"

private const val TABLE_NAME_PLACEHOLDER = "\${TABLE_NAME}"

/**
 * Builds a database file holding an older version of the schema, out of the JSON Room exported
 * while that version was current.
 *
 * Room's own migration helper reads those files from an APK's assets, which a host test has no
 * APK to package them into. The committed files are on disk either way.
 *
 * @param path where the database file is created.
 * @param version the exported version to build, which the file is then stamped with so Room
 *   migrates it on open.
 * @param populate runs against the finished database, for the rows a migration has to carry over.
 */
internal fun createDatabaseAtVersion(
    path: String,
    version: Int,
    populate: SQLiteConnection.() -> Unit,
) {
    val schema =
        Json
            .parseToJsonElement(File("$SCHEMA_DIRECTORY/$version.json").readText())
            .jsonObject
            .getValue("database")
            .jsonObject
    val connection = AndroidSQLiteDriver().open(path)
    try {
        schema.getValue("entities").jsonArray.forEach { element ->
            val entity = element.jsonObject
            val table = entity.getValue("tableName").jsonPrimitive.content
            connection.execSQL(entity.createSql(table))
            entity["indices"]?.jsonArray?.forEach { index ->
                connection.execSQL(index.jsonObject.createSql(table))
            }
        }
        // The identity hash Room compares the migrated schema against.
        schema.getValue("setupQueries").jsonArray.forEach { query ->
            connection.execSQL(query.jsonPrimitive.content)
        }
        connection.execSQL("PRAGMA user_version = $version")
        connection.populate()
    } finally {
        connection.close()
    }
}

/**
 * @return the exported statement with the placeholder Room writes in place of a table's own name
 *   substituted back.
 */
private fun JsonObject.createSql(table: String): String =
    getValue("createSql").jsonPrimitive.content.replace(TABLE_NAME_PLACEHOLDER, table)
