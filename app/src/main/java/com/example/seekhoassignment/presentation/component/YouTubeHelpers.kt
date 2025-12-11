package com.example.seekhoassignment.presentation.component

import android.util.Log

/**
 * Helpers to extract youtube id and build standard watch url.
 * These are unit-test-friendly and used by both repo (to store id) and UI.
 */

/** Extracts 11-character YouTube id from common forms. Returns null if not found. */
fun extractYoutubeId(input: String?): String? {
    if (input.isNullOrBlank()) return null
    val s = input.trim()

    // Common regexes that capture the 11-char id
    val patterns = listOf(
        Regex("/embed/([A-Za-z0-9_-]{11})"),
        Regex("[?&]v=([A-Za-z0-9_-]{11})"),
        Regex("youtu\\.be/([A-Za-z0-9_-]{11})"),
        Regex("/v/([A-Za-z0-9_-]{11})")
    )

    for (re in patterns) {
        val m = re.find(s)
        if (m != null) return m.groupValues[1]
    }

    // If the string *is* an id
    if (s.length == 11 && s.all { it.isLetterOrDigit() || it == '-' || it == '_' }) return s

    return null
}
