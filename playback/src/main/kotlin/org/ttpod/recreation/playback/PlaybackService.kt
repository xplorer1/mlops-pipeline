package org.ttpod.recreation.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ttpod.recreation.domain.*

interface PlaybackDependencies { val queueStore: QueueStore }

@UnstableApi
class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val saveMutex = Mutex()
    private var ready = false
    private var failureCount = 0
    private var lastHistoryUri: String? = null
    private val uiPreferences by lazy { getSharedPreferences("classic-ui", MODE_PRIVATE) }
    private val probe = PcmProbe()
    private val equalizer = EqualizerProcessor()
    private val store get() = (application as PlaybackDependencies).queueStore

    override fun onCreate() {
        super.onCreate()
        val renderers = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioOutputPlaybackParams: Boolean): AudioSink =
                DefaultAudioSink.Builder(context).setAudioProcessors(arrayOf(equalizer, probe)).build()
        }
        player = ExoPlayer.Builder(this, renderers).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
            trackSelectionParameters = trackSelectionParameters.buildUpon().setAudioOffloadPreferences(
                TrackSelectionParameters.AudioOffloadPreferences.Builder()
                    .setAudioOffloadMode(TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED).build()
            ).build()
        }
        equalizer.setPreset(EffectPreset.from(uiPreferences.getString("effectPreset", null)))
        uiPreferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        val callback = object : MediaSession.Callback {
            override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                if (controller.packageName != packageName && !controller.isTrusted)
                    return MediaSession.ConnectionResult.reject()
                return super.onConnect(session, controller)
            }
            override fun onAddMediaItems(session: MediaSession, controller: MediaSession.ControllerInfo, items: List<MediaItem>): ListenableFuture<List<MediaItem>> {
                // The first build only accepts content URIs supplied by its own UI.
                val safe = if (controller.packageName == packageName) items.filter {
                    it.localConfiguration?.uri?.scheme == "content"
                } else emptyList()
                return Futures.immediateFuture(safe)
            }
        }
        val launch = packageManager.getLaunchIntentForPackage(packageName)!!
        session = MediaSession.Builder(this, player).setCallback(callback)
            .setSessionActivity(PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)).build()
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (ready) checkpoint()
                val uri = player.currentMediaItem?.localConfiguration?.uri?.toString()
                uiPreferences.edit()
                    .putString("widgetTitle", player.currentMediaItem?.mediaMetadata?.title?.toString() ?: "No song selected")
                    .putString("widgetArtist", player.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "boomtrack")
                    .putBoolean("widgetPlaying", player.isPlaying).apply()
                sendBroadcast(Intent("org.ttpod.recreation.WIDGET_REFRESH").setPackage(packageName))
                if (player.isPlaying && uri != null && uri != lastHistoryUri) {
                    lastHistoryUri = uri
                    val old = runCatching { org.json.JSONArray(uiPreferences.getString("recent", "[]")) }.getOrDefault(org.json.JSONArray())
                    val recent = (listOf(uri) + List(old.length()) { old.getString(it) }.filterNot { it == uri }).take(100)
                    uiPreferences.edit().putString("recent", org.json.JSONArray(recent).toString()).apply()
                }
            }
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_READY) failureCount = 0 }
            override fun onPlayerError(error: PlaybackException) {
                if (++failureCount < player.mediaItemCount && player.hasNextMediaItem()) {
                    player.seekToNextMediaItem(); player.prepare()
                } else player.pause()
            }
        })
        scope.launch {
            val restored = runCatching { store.load() }.getOrNull()
            if (player.mediaItemCount == 0 && restored != null && restored.tracks.isNotEmpty()) {
                val items = restored.tracks.filter { Uri.parse(it.uri).scheme == "content" }.map { it.toMediaItem() }
                if (items.isNotEmpty()) {
                    player.setMediaItems(items, restored.index.coerceIn(items.indices), restored.positionMs.coerceAtLeast(0))
                    player.repeatMode = restored.repeatMode.coerceIn(0, 2)
                    val order = restored.shuffleOrder
                    player.setShuffleOrder(if (order.sorted() == items.indices.toList())
                        ShuffleOrder.DefaultShuffleOrder(order.toIntArray(), restored.shuffleSeed)
                        else ShuffleOrder.DefaultShuffleOrder(items.size, restored.shuffleSeed))
                    player.shuffleModeEnabled = restored.shuffle
                    // Restore the queue paused. Decoding starts only after an explicit user command.
                }
            }
            ready = true
            var checkpointTicks = 0
            while (isActive) {
                delay(1000)
                val deadline = uiPreferences.getLong("sleepDeadline", 0)
                if (deadline > 0 && System.currentTimeMillis() >= deadline) {
                    player.pause(); uiPreferences.edit().remove("sleepDeadline").apply()
                }
                if (++checkpointTicks % 5 == 0 && player.isPlaying) checkpoint()
            }
        }
    }

    private fun checkpoint() {
        val order = mutableListOf<Int>()
        val timeline = player.currentTimeline
        var window = timeline.getFirstWindowIndex(true)
        while (window != C.INDEX_UNSET && order.size < player.mediaItemCount) {
            order.add(window)
            window = timeline.getNextWindowIndex(window, Player.REPEAT_MODE_OFF, true)
        }
        val snapshot = QueueSnapshot((0 until player.mediaItemCount).map {
            val item = player.getMediaItemAt(it)
            QueueTrack(item.mediaId, item.localConfiguration?.uri.toString(), item.mediaMetadata.title?.toString().orEmpty(), item.mediaMetadata.artist?.toString().orEmpty())
        }, player.currentMediaItemIndex.coerceAtLeast(0), player.currentPosition.coerceAtLeast(0), player.repeatMode, player.shuffleModeEnabled, shuffleOrder = order)
        scope.launch { saveMutex.withLock { runCatching { store.save(snapshot) } } }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onDestroy() {
        ready = false
        uiPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        session?.release(); player.release(); scope.cancel()
        super.onDestroy()
    }

    private val preferenceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == "effectPreset") equalizer.setPreset(EffectPreset.from(prefs.getString(key, null)))
    }

    override fun dump(fd: java.io.FileDescriptor, writer: java.io.PrintWriter, args: Array<out String>?) {
        super.dump(fd, writer, args)
        writer.println("boomtrack pcmBytes=${probe.bytesProcessed.get()} queue=${player.mediaItemCount} playing=${player.isPlaying} positionMs=${player.currentPosition}")
    }
}

fun QueueTrack.toMediaItem(): MediaItem = MediaItem.Builder().setMediaId(entryId).setUri(uri)
    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist)
        .setExtras(android.os.Bundle().apply { putString("trackUri", uri) }).build()).build()
