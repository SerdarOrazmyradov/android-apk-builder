package com.gateway.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, phone TEXT UNIQUE)")
        db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {}

    fun addUser(phone: String): Boolean {
        return try {
            val db = this.writableDatabase
            val values = ContentValues().apply {
                put("phone", phone)
            }
            val result = db.insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            result != -1L
        } catch (e: Exception) {
            false
        }
    }

    fun isUserAllowed(phone: String): Boolean {
        return try {
            val db = this.readableDatabase
            val cursor = db.rawQuery("SELECT * FROM users WHERE phone = ?", arrayOf(phone))
            val exists = cursor.count > 0
            cursor.close()
            exists
        } catch (e: Exception) {
            false
        }
    }

    fun saveApiKey(apiKey: String) {
        try {
            val db = this.writableDatabase
            val cv = ContentValues().apply {
                put("key", "gemini_api_key")
                put("value", apiKey)
            }
            db.insertWithOnConflict("settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getApiKey(): String {
        return try {
            val db = this.readableDatabase
            val cursor = db.rawQuery("SELECT value FROM settings WHERE key = 'gemini_api_key'", null)
            var key = ""
            if (cursor.moveToFirst()) {
                key = cursor.getString(0)
            }
            cursor.close()
            key
        } catch (e: Exception) {
            ""
        }
    }

    companion object {
        private const val DB_NAME = "gateway.db"
        private const val DB_VERSION = 1
    }
}
