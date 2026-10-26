package org.ttpod.recreation

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

data class ImportSummary(val playlists: Int, val entries: Int, val missingUris: Int)

class BackupRepository(private val context: Context, private val dao: LibraryDao) {
    private val prefs get() = context.getSharedPreferences("classic-ui", Context.MODE_PRIVATE)

    suspend fun export(uri: Uri) {
        val root = JSONObject().put("format", "boomtrack-backup").put("version", 1)
        root.put("favorites", JSONArray(prefs.getStringSet("favorites", emptySet()).orEmpty()))
        root.put("settings", JSONObject().put("playerSkin", prefs.getString("playerSkin", "classic"))
            .put("effectPreset", prefs.getString("effectPreset", "OFF")))
        root.put("playlists", JSONArray().apply { dao.playlists().forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("createdAt", it.createdAt)) } })
        root.put("entries", JSONArray().apply { dao.playlistEntries().forEach { put(JSONObject().put("id", it.id).put("playlistId", it.playlistId).put("trackUri", it.trackUri).put("position", it.position)) } })
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(root.toString(2)) }
            ?: error("Backup destination could not be opened.")
    }

    suspend fun import(uri: Uri): ImportSummary {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: error("Backup could not be opened.")
        require(text.length <= 5_000_000) { "Backup exceeds 5 MB." }
        val root = JSONObject(text)
        require(root.getString("format") == "boomtrack-backup" && root.getInt("version") == 1) { "Unsupported backup format." }
        val playlistsJson = root.getJSONArray("playlists")
        val playlists = List(playlistsJson.length()) { i -> playlistsJson.getJSONObject(i).let { Playlist(it.getString("id"), it.getString("name").take(100), it.getLong("createdAt")) } }
        val validIds = playlists.map { it.id }.toSet()
        val entriesJson = root.getJSONArray("entries")
        val entries = List(entriesJson.length()) { i -> entriesJson.getJSONObject(i).let { PlaylistEntry(it.getString("id"), it.getString("playlistId"), it.getString("trackUri"), it.getInt("position")) } }
            .filter { it.playlistId in validIds }
        require(playlists.size <= 1_000 && entries.size <= 100_000) { "Backup contains too much organization data." }
        dao.restorePlaylists(playlists, entries)
        val favoritesJson = root.optJSONArray("favorites") ?: JSONArray()
        val favorites = (0 until favoritesJson.length()).map { favoritesJson.getString(it) }.toSet()
        val settings = root.optJSONObject("settings") ?: JSONObject()
        prefs.edit().putStringSet("favorites", favorites)
            .putString("playerSkin", settings.optString("playerSkin", "classic"))
            .putString("effectPreset", settings.optString("effectPreset", "OFF")).apply()
        val available = dao.observeTracks().first().map { it.uri }.toSet()
        return ImportSummary(playlists.size, entries.size, (entries.map { it.trackUri } + favorites).toSet().count { it !in available })
    }
}
