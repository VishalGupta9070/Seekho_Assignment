package com.example.seekhoassignment.util

fun extractYoutubeId(s: String?): String? {
    if (s.isNullOrBlank()) return null
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
    val trimmed = s.trim()
    if (trimmed.length == 11 && trimmed.all { it.isLetterOrDigit() || it == '-' || it == '_' }) return trimmed
    return null
}

fun buildYouTubeWatchUrl(embedOrWatchOrId: String): String {
    val id = extractYoutubeId(embedOrWatchOrId) ?: embedOrWatchOrId
    return "https://www.youtube.com/watch?v=$id"
}
