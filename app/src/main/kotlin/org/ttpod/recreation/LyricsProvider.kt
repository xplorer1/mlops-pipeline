package org.ttpod.recreation

import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class RemoteLyric(val providerId: String, val title: String, val artist: String, val bytes: ByteArray)

interface LyricsProvider { suspend fun find(track: Track): RemoteLyric? }

/** Tries independent providers in order. One unavailable service must not block the rest. */
class FallbackLyricsProvider(
    private val providers: List<LyricsProvider> = listOf(LrclibLyricsProvider(), LyricsOvhProvider())
) : LyricsProvider {
    override suspend fun find(track: Track): RemoteLyric? {
        val failures = mutableListOf<String>()
        providers.forEach { provider ->
            try {
                provider.find(track)?.let { return it }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                failures += error.message ?: provider.javaClass.simpleName
            }
        }
        if (failures.size == providers.size) error(failures.joinToString(" "))
        return null
    }
}

/** Explicit, single-request LRCLIB lookup. Local playback never invokes this provider. */
class LrclibLyricsProvider : LyricsProvider {
    override suspend fun find(track: Track): RemoteLyric? {
        fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
        val endpoint = "https://lrclib.net/api/get?track_name=${encode(track.title)}&artist_name=${encode(track.artist)}&duration=${(track.duration / 1000).coerceIn(1, 3600)}"
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000; connection.readTimeout = 15_000
        connection.setRequestProperty("Lrclib-Client", "boomtrack/0.4.1 (private Android client)")
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_NOT_FOUND -> return search(track)
                429 -> error("Lyrics provider rate limit reached. Try again after ${connection.getHeaderField("Retry-After") ?: "the requested delay"} seconds.")
                // Exact-match and search can fail independently on LRCLIB.
                503 -> return search(track)
                !in 200..299 -> error("Lyrics provider returned ${connection.responseCode}.")
            }
            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            require(text.length <= 1_100_000) { "Lyrics response is too large." }
            return parse(JSONObject(text), track)
        } finally { connection.disconnect() }
    }

    private fun search(track: Track): RemoteLyric? {
        fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
        val endpoint = "https://lrclib.net/api/search?track_name=${encode(track.title)}&artist_name=${encode(track.artist)}"
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000; connection.readTimeout = 15_000
        connection.setRequestProperty("Lrclib-Client", "boomtrack/0.4.1 (private Android client)")
        try {
            if (connection.responseCode !in 200..299) return null
            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            require(text.length <= 1_100_000) { "Lyrics response is too large." }
            val array = JSONArray(text)
            val target = track.duration / 1000.0
            val best = (0 until array.length()).map { array.getJSONObject(it) }
                .minByOrNull { kotlin.math.abs(it.optDouble("duration", target) - target) }
                ?: return null
            return parse(best, track)
        } finally { connection.disconnect() }
    }

    private fun parse(json: JSONObject, track: Track): RemoteLyric? {
        if (json.optBoolean("instrumental")) return null
        val lyrics = json.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" }
            ?: json.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" }
            ?: return null
        val bytes = lyrics.toByteArray(Charsets.UTF_8)
        require(bytes.size <= org.ttpod.recreation.domain.LrcParser.MAX_BYTES) { "Lyrics exceed the local import limit." }
        org.ttpod.recreation.domain.LrcParser.parse(bytes)
        return RemoteLyric("lrclib:${json.getLong("id")}", json.optString("trackName", track.title), json.optString("artistName", track.artist), bytes)
    }
}

/** Plain (unsynchronised) lyrics fallback using the public lyrics.ovh API. */
class LyricsOvhProvider : LyricsProvider {
    override suspend fun find(track: Track): RemoteLyric? {
        if (track.artist.isBlank() || track.artist.equals("Unknown artist", ignoreCase = true)) return null
        fun encode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
        val endpoint = "https://api.lyrics.ovh/v1/${encode(track.artist)}/${encode(track.title)}"
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000; connection.readTimeout = 15_000
        connection.setRequestProperty("User-Agent", "boomtrack/0.4.1 (private Android client)")
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_NOT_FOUND -> return null
                429 -> error("lyrics.ovh rate limit reached.")
                503 -> error("lyrics.ovh is busy.")
                !in 200..299 -> error("lyrics.ovh returned ${connection.responseCode}.")
            }
            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            require(text.length <= 1_100_000) { "Lyrics response is too large." }
            val lyrics = JSONObject(text).optString("lyrics").takeIf { it.isNotBlank() && it != "null" }
                ?: return null
            val bytes = lyrics.toByteArray(Charsets.UTF_8)
            require(bytes.size <= org.ttpod.recreation.domain.LrcParser.MAX_BYTES) { "Lyrics exceed the local import limit." }
            org.ttpod.recreation.domain.LrcParser.parse(bytes)
            return RemoteLyric("lyrics.ovh", track.title, track.artist, bytes)
        } finally { connection.disconnect() }
    }
}
