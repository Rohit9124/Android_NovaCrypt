package com.example.myapplication.Storage

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import androidx.security.crypto.EncryptedSharedPreferences
import java.io.File
import java.io.IOException
import java.util.*

data class PrivateItem(
    val filename: String,         // internal storage filename (sanitized)
    val displayName: String,      // original name saved in metadata
    val sizeBytes: Long,
    val mime: String?,
    val savedAt: Long             // timestamp when saved
)

class PrivateStorage(private val context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val privateDir: File = File(context.filesDir, "private").apply {
        if (!exists()) mkdirs()
    }

    // encrypted prefs for metadata (original display names, mime types, timestamps)
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "private_metadata",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun sanitizeFilename(name: String): String {
        // keep it simple: timestamp + safe chars
        val cleaned = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return "${System.currentTimeMillis()}_$cleaned"
    }

    /** Try to detect MIME type based on extension if not provided */
    private fun detectMimeFromName(name: String, fallback: String? = null): String? {
        val extension = name.substringAfterLast('.', "").lowercase(Locale.getDefault())
        return if (extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: fallback
        } else {
            fallback
        }
    }

    /** Save bytes to an encrypted file in the private folder and record metadata. */
    @Throws(IOException::class)
    fun saveFile(displayName: String, data: ByteArray, mime: String? = null): String {
        val internalName = sanitizeFilename(displayName)
        val outFile = File(privateDir, internalName)

        val encryptedFile = EncryptedFile.Builder(
            context,
            outFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().use { it.write(data) }

        // detect mime if not provided
        val finalMime = mime ?: detectMimeFromName(displayName, "application/octet-stream")

        // store metadata
        prefs.edit()
            .putString("${internalName}_display", displayName)
            .putString("${internalName}_mime", finalMime ?: "")
            .putLong("${internalName}_time", System.currentTimeMillis())
            .apply()

        return internalName
    }

    /** Convenience helpers for text / images */
    @Throws(IOException::class)
    fun saveText(displayName: String, text: String): String {
        return saveFile(displayName, text.toByteArray(Charsets.UTF_8), "text/plain")
    }

    @Throws(IOException::class)
    fun saveBytes(displayName: String, bytes: ByteArray, mime: String?): String {
        return saveFile(displayName, bytes, mime)
    }

    /** List saved items with metadata */
    fun listItems(): List<PrivateItem> {
        return privateDir.listFiles()
            ?.map { file ->
                val key = file.name
                val display = prefs.getString("${key}_display", key) ?: key
                val mime = prefs.getString("${key}_mime", "")
                val savedAt = prefs.getLong("${key}_time", file.lastModified())
                PrivateItem(
                    filename = key,
                    displayName = display,
                    sizeBytes = file.length(),
                    mime = if (mime.isNullOrBlank()) detectMimeFromName(display) else mime,
                    savedAt = savedAt
                )
            } ?: emptyList()
    }

    /** Read bytes back (decrypted transparently) */
    @Throws(IOException::class)
    fun readFileBytes(internalFilename: String): ByteArray {
        val f = File(privateDir, internalFilename)
        val encryptedFile = EncryptedFile.Builder(
            context,
            f,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        encryptedFile.openFileInput().use { return it.readBytes() }
    }

    /** Delete file & metadata */
    fun delete(internalFilename: String): Boolean {
        val f = File(privateDir, internalFilename)
        prefs.edit()
            .remove("${internalFilename}_display")
            .remove("${internalFilename}_mime")
            .remove("${internalFilename}_time")
            .apply()
        return f.delete()
    }
}
