package org.ttpod.recreation

import android.content.ComponentName
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.ttpod.recreation.domain.QueueTrack
import org.ttpod.recreation.playback.PlaybackService
import org.ttpod.recreation.playback.toMediaItem
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Physical-device gate for the user-supplied acceptance album copied by the test runbook. */
@UnstableApi
@RunWith(AndroidJUnit4::class)
class CollectionAcceptanceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val app get() = context.applicationContext as PlayerApplication

    @Test fun allTwelveAlbumMp3sDecodeAndAdvance() = runBlocking {
        app.library.scan()
        val album = app.db.library().observeTracks().first()
            .filter { it.folder.contains("boomtrack-acceptance", ignoreCase = true) }
            .sortedBy { it.title }
        assertEquals("Expected the complete 12-track acceptance album", 12, album.size)

        lateinit var future: com.google.common.util.concurrent.ListenableFuture<MediaController>
        instrumentation.runOnMainSync {
            future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
        }
        val controller = future.get(15, TimeUnit.SECONDS)
        try {
            album.forEach { track ->
                instrumentation.runOnMainSync {
                    controller.setMediaItem(QueueTrack(UUID.randomUUID().toString(), track.uri, track.title, track.artist).toMediaItem())
                    controller.prepare(); controller.play()
                }
                var advanced = false
                repeat(50) {
                    Thread.sleep(100)
                    instrumentation.runOnMainSync { advanced = controller.isPlaying && controller.currentPosition > 500 }
                    if (advanced) return@repeat
                }
                assertTrue("${track.title} did not decode and advance", advanced)
            }
        } finally { instrumentation.runOnMainSync { controller.release() } }
    }
}
