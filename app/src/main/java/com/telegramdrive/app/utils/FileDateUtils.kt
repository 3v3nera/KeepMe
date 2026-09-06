package com.telegramdrive.app.utils

import android.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FileDateUtils {

    private val EXIF_DATE_FORMAT = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)

    // Regex patterns for extracting dates from file names
    // 1. 13-digit timestamp (epoch millis e.g. 1781315860188)
    private val EPOCH_MILLIS_REGEX = Regex("""\b(1[5-9]\d{11})\b""")

    // 2. 10-digit timestamp (epoch seconds e.g. 1781315860)
    private val EPOCH_SECONDS_REGEX = Regex("""\b(1[5-9]\d{8})\b""")

    // 3. YYYYMMDD_HHMMSS, YYYYMMDD-HHMMSS, or YYYY-MM-DD-HH-MM-SS
    // e.g. PXL_20260613_025516911, Screenshot_20260613-072224, IMG_20251005_114015_763
    private val DATETIME_REGEX = Regex("""(19\d\d|20\d\d)[-_]?(0[1-9]|1[0-2])[-_]?(0[1-9]|[12]\d|3[01])[-_T\s]?(?:([01]\d|2[0-3])[-_:]?([0-5]\d)[-_:]?([0-5]\d))?""")

    // 4. Month name and year, e.g. apr-2026, october-2025
    private val MONTH_NAME_YEAR_REGEX = Regex("""(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*[-_ ]?(20\d\d)""", RegexOption.IGNORE_CASE)

    // 5. Isolated 4-digit year at word boundary (e.g. 2025_PYQs...)
    private val ISOLATED_YEAR_REGEX = Regex("""(?:^|[^0-9a-zA-Z])(20[12]\d)(?:[^0-9a-zA-Z]|$)""")

    fun extractDateTaken(
        fileName: String,
        localFilePath: String? = null,
        thumbnailPath: String? = null
    ): Long? {
        // 1. Try local file EXIF first if file exists
        localFilePath?.let { path ->
            extractDateFromExif(path)?.let { return it }
        }

        // 2. Try thumbnail EXIF if available
        thumbnailPath?.let { path ->
            extractDateFromExif(path)?.let { return it }
        }

        // 3. Try filename patterns
        return extractDateFromFileName(fileName)
    }

    fun extractDateFromFileName(fileName: String): Long? {
        val name = fileName.substringBeforeLast('.')

        // 1. 13-digit epoch millis
        EPOCH_MILLIS_REGEX.find(name)?.let { match ->
            match.groupValues[1].toLongOrNull()?.let { millis ->
                if (isValidEpochMillis(millis)) return millis
            }
        }

        // 2. 10-digit epoch seconds
        EPOCH_SECONDS_REGEX.find(name)?.let { match ->
            match.groupValues[1].toLongOrNull()?.let { seconds ->
                val millis = seconds * 1000L
                if (isValidEpochMillis(millis)) return millis
            }
        }

        // 3. Standard YYYYMMDD_HHMMSS or YYYY-MM-DD
        DATETIME_REGEX.find(name)?.let { match ->
            val y = match.groupValues[1].toIntOrNull()
            val mo = match.groupValues[2].toIntOrNull()
            val d = match.groupValues[3].toIntOrNull()
            val h = match.groupValues.getOrNull(4)?.toIntOrNull() ?: 12
            val mi = match.groupValues.getOrNull(5)?.toIntOrNull() ?: 0
            val s = match.groupValues.getOrNull(6)?.toIntOrNull() ?: 0

            if (y != null && mo != null && d != null) {
                try {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, y)
                        set(Calendar.MONTH, mo - 1)
                        set(Calendar.DAY_OF_MONTH, d)
                        set(Calendar.HOUR_OF_DAY, h)
                        set(Calendar.MINUTE, mi)
                        set(Calendar.SECOND, s)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val millis = cal.timeInMillis
                    if (isValidEpochMillis(millis)) return millis
                } catch (_: Exception) {}
            }
        }

        // 4. Month name + Year (e.g. apr-2026)
        MONTH_NAME_YEAR_REGEX.find(name)?.let { match ->
            val monthStr = match.groupValues[1].lowercase()
            val year = match.groupValues[2].toIntOrNull()
            val monthIndex = when (monthStr) {
                "jan" -> 0; "feb" -> 1; "mar" -> 2; "apr" -> 3
                "may" -> 4; "jun" -> 5; "jul" -> 6; "aug" -> 7
                "sep" -> 8; "oct" -> 9; "nov" -> 10; "dec" -> 11
                else -> null
            }
            if (year != null && monthIndex != null) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, monthIndex)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val millis = cal.timeInMillis
                if (isValidEpochMillis(millis)) return millis
            }
        }

        // 5. Isolated year (e.g. 2025_PYQs...)
        ISOLATED_YEAR_REGEX.find(name)?.let { match ->
            match.groupValues[1].toIntOrNull()?.let { year ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, 0)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val millis = cal.timeInMillis
                if (isValidEpochMillis(millis)) return millis
            }
        }

        return null
    }

    private fun extractDateFromExif(filePath: String): Long? {
        try {
            val file = File(filePath)
            if (!file.exists() || !file.canRead() || file.length() < 128) return null
            val exif = ExifInterface(filePath)
            val dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

            if (!dateStr.isNullOrBlank()) {
                val parsed = EXIF_DATE_FORMAT.parse(dateStr)
                if (parsed != null && isValidEpochMillis(parsed.time)) {
                    return parsed.time
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun isValidEpochMillis(millis: Long): Boolean {
        // Between Jan 1, 2000 and Jan 1, 2040
        return millis in 946684800000L..2208988800000L
    }

    /**
     * Format a month header string.
     * If the year is the current calendar year, returns "Month" (e.g. "September", "June").
     * If the year is different, returns "Month Year" (e.g. "October 2025", "January 2023").
     */
    fun formatMonthHeader(timestamp: Long): String {
        if (timestamp <= 0L) return "Recent"
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val currentCal = Calendar.getInstance()
        return if (cal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR)) {
            SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(timestamp))
        } else {
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    /**
     * Format a full date and time for file viewers and info dialogs.
     */
    fun formatFullDateTime(timestamp: Long): String {
        if (timestamp <= 0L) return "Unknown date"
        return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    /**
     * Professional relative time formatting.
     */
    fun formatRelativeTime(timestamp: Long): String {
        if (timestamp <= 0L) return "Recent"
        val diff = System.currentTimeMillis() - timestamp
        if (diff < 0) return "Recent"
        val minutes = diff / 60_000
        val hours = diff / 3_600_000
        val days = diff / 86_400_000
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            days < 30 -> "${days / 7}w ago"
            days < 365 -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
