package com.brahmadeo.supertonic.tts.utils

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class RecentBook(
    val title: String,
    val uri: String
)

object EbookManager {
    private const val PREFS_NAME = "EbookPrefs"
    private const val KEY_RECENT_BOOKS = "recent_books"
    private const val MAX_BOOKS = 15

    fun getRecentBooks(context: Context): List<RecentBook> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_RECENT_BOOKS, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        val books = mutableListOf<RecentBook>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            books.add(RecentBook(obj.getString("title"), obj.getString("uri")))
        }
        return books
    }

    fun addBook(context: Context, title: String, uri: String) {
        val books = getRecentBooks(context).toMutableList()
        // Remove if exists to re-add at top
        books.removeAll { it.uri == uri }
        // Add at top
        books.add(0, RecentBook(title, uri))
        
        // Trim to max
        val trimmedBooks = if (books.size > MAX_BOOKS) books.take(MAX_BOOKS) else books
        
        val jsonArray = JSONArray()
        trimmedBooks.forEach {
            val obj = JSONObject()
            obj.put("title", it.title)
            obj.put("uri", it.uri)
            jsonArray.put(obj)
        }
        
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECENT_BOOKS, jsonArray.toString())
            .apply()
    }
}
