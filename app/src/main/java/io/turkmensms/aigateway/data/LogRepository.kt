package io.turkmensms.aigateway.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LogRepository(context: Context) : SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp INTEGER NOT NULL,
                direction TEXT NOT NULL,
                peer TEXT NOT NULL,
                subject TEXT NOT NULL,
                snippet TEXT NOT NULL,
                status TEXT NOT NULL,
                detail TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    fun insert(
        direction: String,
        peer: String,
        subject: String,
        snippet: String,
        status: String,
        detail: String = ""
    ) {
        val values = ContentValues().apply {
            put("timestamp", System.currentTimeMillis())
            put("direction", direction)
            put("peer", peer)
            put("subject", subject.take(200))
            put("snippet", snippet.take(300))
            put("status", status)
            put("detail", detail.take(400))
        }
        writableDatabase.insert(TABLE, null, values)
        trim()
    }

    fun latest(limit: Int = 200): List<BridgeLog> {
        val result = ArrayList<BridgeLog>()
        val cursor = readableDatabase.query(
            TABLE,
            null,
            null,
            null,
            null,
            null,
            "timestamp DESC",
            limit.toString()
        )
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow("id")
            val tsIdx = it.getColumnIndexOrThrow("timestamp")
            val dirIdx = it.getColumnIndexOrThrow("direction")
            val peerIdx = it.getColumnIndexOrThrow("peer")
            val subIdx = it.getColumnIndexOrThrow("subject")
            val snipIdx = it.getColumnIndexOrThrow("snippet")
            val stIdx = it.getColumnIndexOrThrow("status")
            val detIdx = it.getColumnIndexOrThrow("detail")
            while (it.moveToNext()) {
                result.add(
                    BridgeLog(
                        id = it.getLong(idIdx),
                        timestamp = it.getLong(tsIdx),
                        direction = it.getString(dirIdx),
                        peer = it.getString(peerIdx),
                        subject = it.getString(subIdx),
                        snippet = it.getString(snipIdx),
                        status = it.getString(stIdx),
                        detail = it.getString(detIdx)
                    )
                )
            }
        }
        return result
    }

    private fun trim() {
        writableDatabase.execSQL(
            "DELETE FROM $TABLE WHERE id NOT IN (SELECT id FROM $TABLE ORDER BY timestamp DESC LIMIT $MAX_ROWS)"
        )
    }

    companion object {
        private const val DB_NAME = "bridge_logs.db"
        private const val DB_VERSION = 1
        private const val TABLE = "bridge_log"
        private const val MAX_ROWS = 500
    }
}
