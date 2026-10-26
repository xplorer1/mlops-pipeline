package org.ttpod.recreation

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tracks", indices = [Index(value = ["uri"], unique = true)])
data class Track(@PrimaryKey val id: String, val uri: String, val title: String, val artist: String, val folder: String, val duration: Long, val source: String, val available: Boolean = true)
@Entity(tableName = "lyrics")
data class SavedLyric(@PrimaryKey val trackUri: String, val bytes: ByteArray, val delayMs: Long = 0)
@Entity(tableName = "queue_snapshot")
data class SavedQueue(@PrimaryKey val id: Int = 1, val json: String)
@Entity(tableName = "playlists")
data class Playlist(@PrimaryKey val id: String, val name: String, val createdAt: Long)
@Entity(
    tableName = "playlist_entries",
    foreignKeys = [ForeignKey(entity = Playlist::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("playlistId"), Index(value = ["playlistId", "position"], unique = true)]
)
data class PlaylistEntry(@PrimaryKey val id: String, val playlistId: String, val trackUri: String, val position: Int)
data class PlaylistSummary(@Embedded val playlist: Playlist, @ColumnInfo(name = "songCount") val songCount: Int)

@Dao
interface LibraryDao {
    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE") fun observeTracks(): Flow<List<Track>>
    @Upsert suspend fun saveTracks(tracks: List<Track>)
    @Query("SELECT * FROM tracks WHERE uri = :uri LIMIT 1") suspend fun track(uri: String): Track?
    @Query("UPDATE tracks SET uri = :newUri, title = :title, artist = :artist, folder = :folder, duration = :duration, source = :source, available = 1 WHERE id = :id")
    suspend fun replaceTrackLocation(id: String, newUri: String, title: String, artist: String, folder: String, duration: Long, source: String)
    @Query("UPDATE playlist_entries SET trackUri = :newUri WHERE trackUri = :oldUri") suspend fun relinkPlaylistEntries(oldUri: String, newUri: String)
    @Query("DELETE FROM lyrics WHERE trackUri = :uri") suspend fun deleteLyric(uri: String)
    @Query("UPDATE tracks SET available = 0 WHERE source = :source") suspend fun markSourceUnavailable(source: String)
    @Transaction suspend fun reconcileSource(source: String, tracks: List<Track>) { markSourceUnavailable(source); saveTracks(tracks) }
    @Query("SELECT * FROM lyrics WHERE trackUri = :uri") suspend fun lyric(uri: String): SavedLyric?
    @Upsert suspend fun saveLyric(lyric: SavedLyric)
    @Query("SELECT * FROM queue_snapshot WHERE id = 1") suspend fun queue(): SavedQueue?
    @Upsert suspend fun saveQueue(queue: SavedQueue)
    @Transaction suspend fun relinkTrack(oldUri: String, replacement: Track) {
        val existing = track(oldUri) ?: error("The missing track is no longer in the library.")
        require(track(replacement.uri) == null || replacement.uri == oldUri) { "That file is already in the library." }
        val savedLyric = lyric(oldUri)
        relinkPlaylistEntries(oldUri, replacement.uri)
        if (savedLyric != null && replacement.uri != oldUri) {
            deleteLyric(oldUri)
            saveLyric(savedLyric.copy(trackUri = replacement.uri))
        }
        replaceTrackLocation(existing.id, replacement.uri, replacement.title, replacement.artist,
            replacement.folder, replacement.duration, replacement.source)
    }
    @Query("SELECT playlists.*, COUNT(playlist_entries.id) AS songCount FROM playlists LEFT JOIN playlist_entries ON playlists.id = playlist_entries.playlistId GROUP BY playlists.id ORDER BY playlists.name COLLATE NOCASE")
    fun observePlaylists(): Flow<List<PlaylistSummary>>
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE") suspend fun playlists(): List<Playlist>
    @Query("SELECT * FROM playlist_entries ORDER BY playlistId, position") suspend fun playlistEntries(): List<PlaylistEntry>
    @Insert suspend fun createPlaylist(playlist: Playlist)
    @Upsert suspend fun savePlaylists(playlists: List<Playlist>)
    @Upsert suspend fun savePlaylistEntries(entries: List<PlaylistEntry>)
    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId") suspend fun clearPlaylistEntries(playlistId: String)
    @Transaction suspend fun restorePlaylists(playlists: List<Playlist>, entries: List<PlaylistEntry>) {
        savePlaylists(playlists)
        playlists.forEach { clearPlaylistEntries(it.id) }
        savePlaylistEntries(entries)
    }
    @Query("SELECT * FROM playlists WHERE id = :id") suspend fun playlist(id: String): Playlist?
    @Query("SELECT tracks.* FROM playlist_entries JOIN tracks ON tracks.uri = playlist_entries.trackUri WHERE playlist_entries.playlistId = :playlistId ORDER BY playlist_entries.position")
    fun observePlaylistTracks(playlistId: String): Flow<List<Track>>
    @Query("SELECT * FROM playlist_entries WHERE playlistId = :playlistId ORDER BY position") fun observePlaylistEntries(playlistId: String): Flow<List<PlaylistEntry>>
    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM playlist_entries WHERE playlistId = :playlistId") suspend fun nextPlaylistPosition(playlistId: String): Int
    @Insert suspend fun addPlaylistEntry(entry: PlaylistEntry)
    @Query("UPDATE playlists SET name = :name WHERE id = :id") suspend fun renamePlaylist(id: String, name: String)
    @Query("DELETE FROM playlist_entries WHERE id = :id") suspend fun deletePlaylistEntry(id: String)
    @Query("UPDATE playlist_entries SET position = :position WHERE id = :id") suspend fun setPlaylistPosition(id: String, position: Int)
    @Query("SELECT * FROM playlist_entries WHERE playlistId = :playlistId AND position = :position LIMIT 1") suspend fun playlistEntryAt(playlistId: String, position: Int): PlaylistEntry?
    @Transaction suspend fun movePlaylistEntry(playlistId: String, from: Int, to: Int) {
        if (from == to) return
        val moving = playlistEntryAt(playlistId, from) ?: return
        val displaced = playlistEntryAt(playlistId, to) ?: return
        setPlaylistPosition(moving.id, -1)
        setPlaylistPosition(displaced.id, from)
        setPlaylistPosition(moving.id, to)
    }
    @Query("DELETE FROM playlists WHERE id = :id") suspend fun deletePlaylist(id: String)
}

@Database(entities = [Track::class, SavedLyric::class, SavedQueue::class, Playlist::class, PlaylistEntry::class], version = 3, exportSchema = true)
abstract class LibraryDatabase : RoomDatabase() { abstract fun library(): LibraryDao }
