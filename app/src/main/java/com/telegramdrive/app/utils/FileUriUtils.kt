package com.telegramdrive.app.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object FileUriUtils {

    /**
     * Resolves an Android [Uri] (content:// or file://) to an absolute local filesystem path
     * that TDLib native C++ engine (`libtdjson.so`) can open via POSIX file calls.
     *
     * 1. If direct file path exists and is readable, returns it directly (zero-copy).
     * 2. If it's a content URI, copies stream into `context.cacheDir/upload_cache/`.
     */
    fun resolveUriToLocalPath(context: Context, uri: Uri, fallbackName: String): String {
        // Direct file scheme
        if (uri.scheme.equals("file", ignoreCase = true)) {
            uri.path?.let { path ->
                val f = File(path)
                if (f.exists() && f.canRead()) {
                    return f.absolutePath
                }
            }
        }

        // Check if content URI can be resolved directly from MediaStore DATA column
        if (uri.scheme.equals("content", ignoreCase = true)) {
            try {
                val projection = arrayOf(MediaStore.MediaColumns.DATA)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val colIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                        if (colIdx != -1) {
                            val dataPath = cursor.getString(colIdx)
                            if (!dataPath.isNullOrBlank()) {
                                val f = File(dataPath)
                                if (f.exists() && f.canRead()) {
                                    return f.absolutePath
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Scoped storage or security restriction may prevent direct reading of DATA
            }

            // Stream copy to app cache
            val uploadDir = File(context.cacheDir, "upload_cache").apply { mkdirs() }
            val cleanName = fallbackName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(uploadDir, "${System.currentTimeMillis()}_$cleanName")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IllegalStateException("Unable to open stream for URI: $uri")

            return targetFile.absolutePath
        }

        return uri.path ?: uri.toString()
    }

    /**
     * Extract display name from ContentResolver or fallback.
     */
    fun getDisplayName(context: Context, uri: Uri, fallback: String): String {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        val name = cursor.getString(index)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return uri.lastPathSegment ?: fallback
    }

    /**
     * Delete files in `upload_cache` older than [maxAgeMs] to prevent storage accumulation.
     */
    fun cleanupOldUploadCache(context: Context, maxAgeMs: Long = 24 * 60 * 60 * 1000L) {
        try {
            val uploadDir = File(context.cacheDir, "upload_cache")
            if (!uploadDir.exists() || !uploadDir.isDirectory) return
            val now = System.currentTimeMillis()
            uploadDir.listFiles()?.forEach { file ->
                if (now - file.lastModified() > maxAgeMs) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
