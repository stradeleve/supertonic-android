package com.brahmadeo.supertonic.tts.utils

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class RecentBook(
    val title: String,
    val path: String // Changed from uri to local path
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
            books.add(RecentBook(obj.getString("title"), obj.getString("path")))
        }
        return books
    }

    fun addBook(context: Context, title: String, path: String) {
        val books = getRecentBooks(context).toMutableList()
        books.removeAll { it.path == path }
        books.add(0, RecentBook(title, path))
        
        val trimmedBooks = if (books.size > MAX_BOOKS) {
            // Optional: delete the file of the book that fell off the list
            // But we might want to keep it if multiple paths point to same file
            books.take(MAX_BOOKS)
        } else books
        
        val jsonArray = JSONArray()
        trimmedBooks.forEach {
            val obj = JSONObject()
            obj.put("title", it.title)
            obj.put("path", it.path)
            jsonArray.put(obj)
        }
        
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECENT_BOOKS, jsonArray.toString())
            .apply()
    }

    fun importBook(context: Context, uri: Uri): String? {
        try {
            val contentResolver = context.contentResolver
            val fileName = "book_${System.currentTimeMillis()}.epub" // Simplified
            val destFile = File(context.filesDir, "ebooks/$fileName")
            destFile.parentFile?.mkdirs()

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            return destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
