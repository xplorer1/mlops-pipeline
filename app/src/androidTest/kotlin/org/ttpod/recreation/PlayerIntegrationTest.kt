package org.ttpod.recreation

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.ttpod.recreation.domain.QueueTrack
import org.ttpod.recreation.domain.LrcParser
import org.ttpod.recreation.playback.PlaybackService
import org.ttpod.recreation.playback.toMediaItem
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@UnstableApi
@RunWith(AndroidJUnit4::class)
class PlayerIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val app get() = context.applicationContext as PlayerApplication

    @Test fun v1MigrationPreservesDataAndSupportsDuplicatePlaylistEntries() = runBlocking {
        val name = "migration-${UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { raw ->
            raw.execSQL("CREATE TABLE tracks (id TEXT NOT NULL, uri TEXT NOT NULL, title TEXT NOT NULL, artist TEXT NOT NULL, folder TEXT NOT NULL, duration INTEGER NOT NULL, source TEXT NOT NULL, PRIMARY KEY(id))")
            raw.execSQL("CREATE UNIQUE INDEX index_tracks_uri ON tracks(uri)")
            raw.execSQL("CREATE TABLE lyrics (trackUri TEXT NOT NULL, bytes BLOB NOT NULL, delayMs INTEGER NOT NULL, PRIMARY KEY(trackUri))")
            raw.execSQL("CREATE TABLE queue_snapshot (id INTEGER NOT NULL, json TEXT NOT NULL, PRIMARY KEY(id))")
            raw.execSQL("INSERT INTO tracks VALUES ('old','content://old','Old song','Old artist','Music',1000,'mediastore')")
            raw.execSQL("INSERT INTO lyrics VALUES ('content://old',X'5B30303A30312E30305D4F6C64',250)")
            raw.execSQL("INSERT INTO queue_snapshot VALUES (1,'{}')")
            raw.version = 1
        }
        val migrated = Room.databaseBuilder(context, LibraryDatabase::class.java, name)
            .addMigrations(PlayerApplication.MIGRATION_1_2, PlayerApplication.MIGRATION_2_3).build()
        try {
            val dao = migrated.library()
            val old = dao.observeTracks().first().single()
            assertEquals("Old song", old.title); assertTrue(old.available)
            assertEquals(250L, dao.lyric("content://old")?.delayMs); assertNotNull(dao.queue())
            val playlist = Playlist("list", "Duplicates", 1)
            dao.createPlaylist(playlist)
            dao.addPlaylistEntry(PlaylistEntry("one", playlist.id, old.uri, 0))
            dao.addPlaylistEntry(PlaylistEntry("two", playlist.id, old.uri, 1))
            assertEquals(2, dao.observePlaylistTracks(playlist.id).first().size)
            dao.movePlaylistEntry(playlist.id, 0, 1)
            assertEquals(listOf("two", "one"), dao.observePlaylistEntries(playlist.id).first().map { it.id })
            dao.renamePlaylist(playlist.id, "Renamed")
            assertEquals("Renamed", dao.playlist(playlist.id)?.name)
            dao.deletePlaylistEntry("two")
            assertEquals(listOf("one"), dao.observePlaylistEntries(playlist.id).first().map { it.id })
            dao.reconcileSource("mediastore", emptyList())
            assertFalse(dao.observeTracks().first().single().available)
            assertEquals("Missing-file reconciliation must preserve organization", 1, dao.observePlaylistTracks(playlist.id).first().size)
            dao.relinkTrack(old.uri, old.copy(uri = "content://replacement", title = "Replacement", available = true))
            assertEquals("content://replacement", dao.observeTracks().first().single().uri)
            assertEquals("content://replacement", dao.observePlaylistEntries(playlist.id).first().single().trackUri)
            assertNotNull("Saved lyrics must follow a relinked track", dao.lyric("content://replacement"))
            assertNull(dao.lyric("content://old"))
        } finally { migrated.close(); context.deleteDatabase(name) }
    }

    @Test fun skinImportValidatesAndRollsBack() {
        fun skinZip(file: File, entryName: String, manifest: String) {
            ZipOutputStream(file.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry(entryName)); zip.write(manifest.toByteArray()); zip.closeEntry()
            }
        }
        val id = "test.skin.${UUID.randomUUID()}"
        val valid = File(context.cacheDir, "valid-skin.zip")
        val invalid = File(context.cacheDir, "invalid-skin.zip")
        try {
            skinZip(valid, "manifest.json", """{"version":1,"id":"$id","name":"Test Skin","topColor":"#112233","bottomColor":"#223344"}""")
            val repository = SkinRepository(context)
            assertEquals(id, repository.import(Uri.fromFile(valid)).id)
            skinZip(invalid, "../manifest.json", "{}")
            runCatching { repository.import(Uri.fromFile(invalid)) }.onSuccess { fail("Unsafe skin path was accepted") }
            assertTrue(repository.installed().any { it.id == id })
            assertFalse(File(context.filesDir, "skins").listFiles().orEmpty().any { it.name.startsWith(".staging-") })
        } finally {
            valid.delete(); invalid.delete(); File(context.filesDir, "skins/$id").deleteRecursively()
        }
    }

    @Test fun backupRoundTripRestoresOrganizationAndReportsMissingFiles() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java).build()
        val file = File(context.cacheDir, "boomtrack-backup-${UUID.randomUUID()}.json")
        val prefs = context.getSharedPreferences("classic-ui", android.content.Context.MODE_PRIVATE)
        try {
            val dao = db.library()
            val playlist = Playlist("backup-list", "Backup list", 10)
            dao.createPlaylist(playlist)
            dao.addPlaylistEntry(PlaylistEntry("backup-entry", playlist.id, "content://missing", 0))
            prefs.edit().putStringSet("favorites", setOf("content://favorite")).putString("effectPreset", "ROCK").commit()
            val backup = BackupRepository(context, dao)
            backup.export(Uri.fromFile(file))
            dao.deletePlaylist(playlist.id); prefs.edit().clear().commit()
            val result = backup.import(Uri.fromFile(file))
            assertEquals(1, result.playlists); assertEquals(1, result.entries); assertEquals(2, result.missingUris)
            assertEquals("Backup list", dao.playlist(playlist.id)?.name)
            assertEquals(setOf("content://favorite"), prefs.getStringSet("favorites", emptySet()))
            assertEquals("ROCK", prefs.getString("effectPreset", null))
        } finally { db.close(); file.delete(); prefs.edit().clear().commit() }
    }


    private fun views(view: android.view.View): List<android.view.View> = listOf(view) +
        if (view is android.view.ViewGroup) (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()
    private fun clickText(activity: android.app.Activity, text: String) {
        var view = views(activity.window.decorView).first { it.isShown && it is android.widget.TextView && it.text.toString() == text }
        while (!view.isClickable && view.parent is android.view.View) view = view.parent as android.view.View
        assertTrue("No clickable destination for $text", view.performClick())
    }
    @Test fun categoriesSearchAndBackUsePages() {
        runBlocking { app.library.scan() }
        val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            Thread.sleep(1500)
            instrumentation.runOnMainSync {
                activity.findViewById<android.view.View>(R.id.localMusic).performClick()
                activity.findViewById<android.view.View>(R.id.artists).performClick()
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                val songs = activity.findViewById<android.widget.ListView>(R.id.songs)
                assertTrue("Artists must be an inline list", songs.count > 0)
                assertFalse(activity.findViewById<android.view.View>(R.id.randomPlay).isShown)
                songs.performItemClick(songs.getChildAt(0), 0, songs.adapter.getItemId(0))
                assertTrue(activity.findViewById<android.view.View>(R.id.randomPlay).isShown)
                activity.onBackPressedDispatcher.onBackPressed()
                assertFalse(activity.findViewById<android.view.View>(R.id.randomPlay).isShown)
                activity.findViewById<android.view.View>(R.id.scan).performClick()
                assertFalse("Covered library must not remain interactive", activity.findViewById<android.view.View>(R.id.localPanel).isShown)
                val query = views(activity.window.decorView).filterIsInstance<android.widget.EditText>().first { it.isShown }
                query.setText("Twinkle")
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                assertTrue(views(activity.window.decorView).filterIsInstance<android.widget.TextView>().any { it.isShown && it.text.contains("Twinkle") })
                clickText(activity, "Play all")
                assertTrue(activity.findViewById<android.view.View>(R.id.playerPanel).isShown)
                activity.onBackPressedDispatcher.onBackPressed()
                assertTrue("Search query must survive visiting player", views(activity.window.decorView).filterIsInstance<android.widget.EditText>().any { it.isShown && it.text.toString() == "Twinkle" })
                activity.onBackPressedDispatcher.onBackPressed()
                assertTrue(activity.findViewById<android.view.View>(R.id.artists).isShown)
                assertFalse(activity.findViewById<android.view.View>(R.id.randomPlay).isShown)
                activity.findViewById<android.view.View>(R.id.miniPlay).performClick()
            }
        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }

    @Test fun lyricImportIsDurableAndFailedReplacementPreservesSelection() = runBlocking {
        val file = File(context.cacheDir, "import-test.lrc")
        try {
            file.writeText("[offset:500]\n[00:10.00]Known cue")
            app.library.importLyrics("test:track", Uri.fromFile(file))
            val saved = app.db.library().lyric("test:track")!!
            assertEquals(9500L, LrcParser.parse(saved.bytes).lines.single().timeMs)
            file.writeText("[ar:only metadata]")
            try { app.library.importLyrics("test:track", Uri.fromFile(file)); fail("Invalid lyrics accepted") }
            catch (_: IllegalArgumentException) { /* expected */ }
            assertArrayEquals(saved.bytes, app.db.library().lyric("test:track")!!.bytes)
        } finally { file.delete() }
    }

    @Test fun explicitOnlineLyricsBuildDeclaresInternetPermission() {
        val permissions = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertTrue(permissions.contains("android.permission.INTERNET"))
    }

    @Test fun scannedAudioPlaysThroughServiceAndContinuesWithoutActivity() {
        val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        // Exercise the real scan feedback too: API 23's Toast layout must not
        // be inflated through the Activity's AppCompat view factory.
        val automation = instrumentation.uiAutomation
        instrumentation.runOnMainSync { activity.findViewById<android.view.View>(R.id.homeMenu).performClick() }
        instrumentation.waitForIdleSync()
        var found: android.view.accessibility.AccessibilityNodeInfo? = null
        for (attempt in 0 until 30) {
            found = automation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Scan Media")?.firstOrNull()
            if (found != null) break
            Thread.sleep(100)
        }
        var scanItem = checkNotNull(found) { "Scan Media menu did not become accessible" }
        while (!scanItem.isClickable && scanItem.parent != null) scanItem = scanItem.parent
        assertTrue(scanItem.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))
        Thread.sleep(500)
        instrumentation.runOnMainSync { clickText(activity, "Fast scan") }
        Thread.sleep(500)
        val track = runBlocking {
            app.library.scan()
            app.db.library().observeTracks().first().firstOrNull { it.title.contains("Twinkle", true) }
                ?: error("Push and media-scan tools' TwinkleDemo.wav fixture before running this test")
        }
        lateinit var future: com.google.common.util.concurrent.ListenableFuture<MediaController>
        instrumentation.runOnMainSync {
            future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
        }
        val controller = future.get(15, TimeUnit.SECONDS)
        try {
            instrumentation.runOnMainSync {
                controller.setMediaItem(QueueTrack("integration-entry", track.uri, track.title, track.artist).toMediaItem())
                controller.prepare(); controller.play()
            }
            var playing = false
            repeat(40) {
                Thread.sleep(250)
                instrumentation.runOnMainSync { playing = controller.isPlaying && controller.currentPosition > 500 }
                if (playing) return@repeat
            }
            assertTrue("Playback did not advance", playing)
            instrumentation.runOnMainSync { activity.finish() }
            Thread.sleep(1500)
            instrumentation.runOnMainSync { assertTrue(controller.isPlaying); assertTrue(controller.currentPosition > 1500) }
            val prefs = context.getSharedPreferences("classic-ui", android.content.Context.MODE_PRIVATE)
            val history = org.json.JSONArray(prefs.getString("recent", "[]"))
            assertTrue("Background service did not record history", (0 until history.length()).any { history.getString(it) == track.uri })
            prefs.edit().putLong("sleepDeadline", System.currentTimeMillis() + 500).commit()
            Thread.sleep(2000)
            instrumentation.runOnMainSync { assertFalse("Sleep timer must pause after Activity closes", controller.isPlaying) }
            assertEquals(0L, prefs.getLong("sleepDeadline", 0))
            val saved = runBlocking { app.queueStore.load() }
            assertEquals("integration-entry", saved?.tracks?.single()?.entryId)
        } finally { instrumentation.runOnMainSync { controller.release(); activity.finish() } }
    }

    @Test fun indexedWavAndOggBothPlay() = runBlocking {
        val discovered = app.library.scan().let { app.db.library().observeTracks().first() }
        val candidates = listOf("TwinkleDemo", "Daisy Bell").map { expected ->
            discovered.firstOrNull { it.title.contains(expected, true) }
                ?: error("Missing seeded $expected audio fixture")
        }
        lateinit var future: com.google.common.util.concurrent.ListenableFuture<MediaController>
        instrumentation.runOnMainSync {
            future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
        }
        val controller = future.get(15, TimeUnit.SECONDS)
        try {
            candidates.forEach { track ->
                instrumentation.runOnMainSync {
                    controller.setMediaItem(QueueTrack(UUID.randomUUID().toString(), track.uri, track.title, track.artist).toMediaItem())
                    controller.prepare(); controller.play()
                }
                var advanced = false
                repeat(40) {
                    Thread.sleep(100)
                    instrumentation.runOnMainSync { advanced = controller.isPlaying && controller.currentPosition > 250 }
                    if (advanced) return@repeat
                }
                assertTrue("${track.title} did not play", advanced)
            }
        } finally { instrumentation.runOnMainSync { controller.release() } }
    }
}
