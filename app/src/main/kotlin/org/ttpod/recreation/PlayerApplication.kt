package org.ttpod.recreation

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import org.ttpod.recreation.domain.*
import org.ttpod.recreation.playback.PlaybackDependencies

class PlayerApplication : Application(), PlaybackDependencies {
    val db by lazy { Room.databaseBuilder(this, LibraryDatabase::class.java, "library.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3).build() }
    val library by lazy { LibraryRepository(this, db.library()) }
    override val queueStore: QueueStore by lazy {
        object : QueueStore {
            override suspend fun load(): QueueSnapshot? {
                val raw = db.library().queue() ?: return null
                val j = JSONObject(raw.json); val list = j.getJSONArray("tracks")
                return QueueSnapshot((0 until list.length()).map {
                    val t = list.getJSONObject(it)
                    QueueTrack(t.getString("id"), t.getString("uri"), t.getString("title"), t.getString("artist"))
                }, j.getInt("index"), j.getLong("position"), j.getInt("repeat"), j.getBoolean("shuffle"), j.optLong("seed", 42),
                    j.optJSONArray("order")?.let { order -> (0 until order.length()).map { order.getInt(it) } }.orEmpty())
            }
            override suspend fun save(snapshot: QueueSnapshot) {
                val tracks = JSONArray()
                snapshot.tracks.forEach { tracks.put(JSONObject().put("id", it.entryId).put("uri", it.uri).put("title", it.title).put("artist", it.artist)) }
                val j = JSONObject().put("tracks", tracks).put("index", snapshot.index).put("position", snapshot.positionMs)
                    .put("repeat", snapshot.repeatMode).put("shuffle", snapshot.shuffle).put("seed", snapshot.shuffleSeed)
                    .put("order", JSONArray(snapshot.shuffleOrder))
                db.library().saveQueue(SavedQueue(json = j.toString()))
            }
        }
    }

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `playlist_entries` (`id` TEXT NOT NULL, `playlistId` TEXT NOT NULL, `trackUri` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_entries_playlistId` ON `playlist_entries` (`playlistId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_playlist_entries_playlistId_position` ON `playlist_entries` (`playlistId`, `position`)")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tracks` ADD COLUMN `available` INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
