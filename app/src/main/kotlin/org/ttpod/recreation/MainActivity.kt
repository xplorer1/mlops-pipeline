package org.ttpod.recreation

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.MotionEvent
import android.graphics.Color
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import org.ttpod.recreation.databinding.ActivityMainBinding
import org.ttpod.recreation.domain.*
import org.ttpod.recreation.playback.*
import java.util.UUID

@UnstableApi
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val repo get() = (application as PlayerApplication).library
    private val skins by lazy { SkinRepository(this) }
    // Retained only for source compatibility with the dormant legacy lookup method.
    // The local-only UI does not expose it and the manifest has no network permission.
    private val lyricsProvider: LyricsProvider = FallbackLyricsProvider()
    private val backup by lazy { BackupRepository(this, repo.dao) }
    private var controller: MediaController? = null
    private var connection: ListenableFuture<MediaController>? = null
    private var tracks = emptyList<Track>()
    private var shown = emptyList<Track>()
    private var folder: String? = null
    private var artistFilter: String? = null
    private var albumFilter: String? = null
    private var favoritesOnly = false
    private var localPage = false
    private lateinit var pages: LibraryPages
    private var groupTab = 0
    private var scanJob: Job? = null
    private var albumJob: Job? = null
    private var groupNames = emptyList<String>()
    private val albumNames = mutableMapOf<String, String>()
    private val preferences by lazy { getSharedPreferences("classic-ui", MODE_PRIVATE) }
    private fun favorites() = preferences.getStringSet("favorites", emptySet()).orEmpty().toSet()
    private var currentUri: String? = null
    private var lyric: Lyrics? = null
    private var savedLyric: SavedLyric? = null
    private var lyricJob: Job? = null
    private var lyricPage = false
    private var visualizerPage = false
    private var playerPage = false
    private var dragging = false
    private var lastLine = Int.MIN_VALUE
    private var importFor: String? = null
    private var relinkFor: Track? = null
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { updatePlayer(); binding.recentMusicCount.setChangedText("${pages.recentCount()} songs") }
    }
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) scan() else { pages.scanResult("Music access was not granted. Choose individual files instead."); message("Music access was not granted. You can still choose individual audio files.") }
    }
    private val chooseAudio = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        lifecycleScope.launch {
            for (uri in uris) {
                runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                runCatching { repo.importAudio(uri) }.onFailure { message(it.message ?: "Could not read audio.") }
            }
        }
    }
    private val chooseMusicFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            lifecycleScope.launch {
                message("Indexing selected music folder…")
                runCatching { repo.importAudioTree(uri) }
                    .onSuccess { result ->
                        preferences.edit().putString("musicTree", uri.toString()).apply()
                        val skipped = if (result.skipped > 0) "; ${result.skipped} unreadable entries skipped" else ""
                        message("Indexed ${result.imported} songs$skipped.")
                    }
                    .onFailure { message("Folder import failed. ${it.message.orEmpty()}") }
            }
        }
    }
    private val chooseReplacement = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val missing = relinkFor
        relinkFor = null
        if (uri != null && missing != null) lifecycleScope.launch {
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            runCatching {
                repo.relinkAudio(missing.uri, uri)
                migrateUriPreferences(missing.uri, uri.toString())
            }.onSuccess { message("Relinked ${missing.title}. Playlists, favorites, history and lyrics were preserved.") }
                .onFailure { message("Could not relink ${missing.title}. ${it.message.orEmpty()}") }
        }
    }
    private val chooseLyrics = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = importFor
        if (uri != null && target != null) lifecycleScope.launch {
            try {
                val skipped = repo.importLyrics(target, uri)
                if (currentUri == target) loadTrack(target)
                message(if (skipped == 0) "Lyrics saved for this song." else "Lyrics saved; $skipped malformed cues skipped.")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                message("Import failed. Previous lyrics kept. ${e.message.orEmpty()}")
            }
        }
    }
    private val chooseSkin = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) lifecycleScope.launch(Dispatchers.IO) {
            runCatching { skins.import(uri) }.onSuccess { skin ->
                withContext(Dispatchers.Main) {
                    preferences.edit().putString("playerSkin", skin.id).apply(); applySkin(skin.id)
                    pages.open("Background & Skin"); message("${skin.name} imported.")
                }
            }.onFailure { error -> withContext(Dispatchers.Main) { message("Skin import failed. ${error.message.orEmpty()}") } }
        }
    }
    private val chooseLyricFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            preferences.edit().putString("lyricTree", uri.toString()).apply()
            message("Lyrics folder saved. Use Find sidecar lyrics from the player menu.")
        }
    }
    private val createBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) lifecycleScope.launch { runCatching { withContext(Dispatchers.IO) { backup.export(uri) } }.onSuccess { message("boomtrack backup saved.") }.onFailure { message("Backup failed. ${it.message.orEmpty()}") } }
    }
    private val openBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) lifecycleScope.launch { runCatching { withContext(Dispatchers.IO) { backup.import(uri) } }.onSuccess { result ->
            applySkin(preferences.getString("playerSkin", "classic") ?: "classic"); pages.refresh(); message("Imported ${result.playlists} playlists and ${result.entries} entries; ${result.missingUris} file references need relinking.")
        }.onFailure { message("Import failed. Existing data kept where possible. ${it.message.orEmpty()}") } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater); setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
        }
        pages = LibraryPages(this, binding.root, binding.libraryPanel, binding.miniPlay.parent as View, binding.homePanel.parent as android.widget.FrameLayout,
            { tracks }, { favorites() }, { list, index -> playSelection(list, index) },
            { requestScan() }, { chooseAudio.launch(arrayOf("audio/*")) }, { chooseMusicFolder.launch(null) },
            { showPlayer(false); clearFilters(); showLibrary(true); showTracks() },
            { controller }, { playerPage }, { showPlayer(it) },
            { skin -> applySkin(skin) }, repo.dao,
            { chooseSkin.launch(arrayOf("application/zip", "application/octet-stream")) }, { skins.installed() },
            { chooseLyricFolder.launch(null) }, { preferences.contains("lyricTree") },
            { createBackup.launch("boomtrack-backup.json") }, { openBackup.launch(arrayOf("application/json", "text/plain")) })
        localPage = savedInstanceState?.getBoolean("local") ?: false
        playerPage = savedInstanceState?.getBoolean("player") ?: false
        lyricPage = savedInstanceState?.getBoolean("lyrics") ?: false
        visualizerPage = savedInstanceState?.getBoolean("visualizer") ?: false
        importFor = savedInstanceState?.getString("importFor")
        groupTab = savedInstanceState?.getInt("groupTab") ?: 0
        folder = savedInstanceState?.getString("folder"); artistFilter = savedInstanceState?.getString("artistFilter"); albumFilter = savedInstanceState?.getString("albumFilter")
        binding.scan.setOnClickListener { searchLibrary() }
        binding.addAudio.setOnClickListener { chooseAudio.launch(arrayOf("audio/*")) }
        binding.allSongs.setOnClickListener { clearFilters(); showTracks() }
        binding.folders.setOnClickListener { showGroups(3) }
        binding.manageLabel.setOnClickListener { libraryMenu() }
        binding.dismissTip.setOnClickListener { binding.libraryTip.visibility = View.GONE; preferences.edit().putBoolean("tipDismissed", true).apply() }
        if (preferences.getBoolean("tipDismissed", false)) binding.libraryTip.visibility = View.GONE
        binding.allSongs.post { binding.tabIndicator.layoutParams = binding.tabIndicator.layoutParams.apply { width = binding.allSongs.width } }
        binding.localMusic.setOnClickListener { clearFilters(); showLibrary(true); showTracks() }
        binding.libraryBack.setOnClickListener { libraryBack() }
        binding.miniArt.setOnClickListener { showPlayer(true) }
        binding.miniInfo.setOnClickListener { showPlayer(true) }
        binding.miniQueue.setOnClickListener { showQueue() }
        binding.homeMenu.setOnClickListener { pages.drawer() }
        binding.libraryActions.setOnClickListener { libraryMenu() }
        binding.search.setOnClickListener { searchLibrary() }
        binding.discoveryTab.setOnClickListener { unavailable("Discovery") }
        binding.recommendTab.setOnClickListener { unavailable("Recommendations") }
        binding.audioDownloads.setOnClickListener { unavailable("Audio downloads") }
        binding.videoDownloads.setOnClickListener { unavailable("Music video downloads") }
        binding.recentMusic.setOnClickListener { pages.open("Recent play") }
        binding.openFavorites.setOnClickListener { pages.open("Favorite songs") }
        binding.artists.setOnClickListener { chooseGroup(false) }
        binding.albums.setOnClickListener { chooseGroup(true) }
        binding.randomPlay.setOnClickListener { if (shown.isNotEmpty()) { playTracks(shown.indices.random()); controller?.shuffleModeEnabled = true } }
        binding.playerMenu.setOnClickListener { playerMenu() }
        binding.detailsPage.setOnClickListener { lyricPage = false; visualizerPage = true; showPages() }
        binding.favorite.setOnClickListener { toggleFavorite() }
        binding.share.setOnClickListener { shareTrack() }
        installSwipe(binding.artwork) { lyricPage = true; showPages() }
        installSwipe(binding.lyricsScroll) { lyricPage = false; showPages() }
        binding.miniHint.setOnClickListener { showPlayer(true) }
        installSwipe(binding.miniTitle) { direction -> if (direction < 0) controller?.seekToNextMediaItem() else controller?.seekToPreviousMediaItem() }
        binding.songs.setOnItemClickListener { _, _, position, _ ->
            if (groupTab != 0 && folder == null && artistFilter == null && albumFilter == null) {
                val name = groupNames.getOrNull(position) ?: return@setOnItemClickListener
                when(groupTab) { 1 -> artistFilter = name; 2 -> albumFilter = name; 3 -> folder = name }
                showTracks()
            } else if (shown[position].available) playTracks(position) else offerRelink(shown[position])
        }
        binding.miniTitle.setOnClickListener { showPlayer(true) }
        binding.back.setOnClickListener { showPlayer(false) }
        binding.miniPlay.setOnClickListener { togglePlay() }; binding.play.setOnClickListener { togglePlay() }
        binding.previous.setOnClickListener { controller?.seekToPrevious() }
        binding.next.setOnClickListener { controller?.seekToNextMediaItem() }
        binding.repeat.setOnClickListener { controller?.let { it.repeatMode = (it.repeatMode + 1) % 3 } }
        binding.repeat.setOnLongClickListener { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }; true }
        binding.queue.setOnClickListener { showQueue() }
        binding.artPage.setOnClickListener { lyricPage = false; visualizerPage = false; showPages() }
        binding.lyricPage.setOnClickListener { lyricPage = true; visualizerPage = false; showPages() }
        binding.importLyrics.setOnClickListener {
            importFor = currentUri
            if (importFor == null) message("Choose a song first.") else chooseLyrics.launch(arrayOf("*/*"))
        }
        binding.earlier.setOnClickListener { changeDelay(-500) }; binding.later.setOnClickListener { changeDelay(500) }
        binding.delayLabel.setOnLongClickListener { changeDelay(-(savedLyric?.delayMs ?: 0)); true }
        binding.seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(bar: SeekBar) { dragging = true }
            override fun onStopTrackingTouch(bar: SeekBar) {
                controller?.let { if (it.duration > 0) it.seekTo(it.duration * bar.progress / 1000) }; dragging = false
            }
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.elapsed.text = time((controller?.duration ?: 0).coerceAtLeast(0) * progress / 1000)
            }
        })
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if (pages.closeDrawer()) Unit else if (playerPage) showPlayer(false) else if (pages.back()) Unit else if (localPage) libraryBack() else { isEnabled = false; onBackPressedDispatcher.onBackPressed() } }
        })
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { repo.dao.observeTracks().collect { tracks = it; showTracks(); pages.refresh(); if (groupTab == 2) loadAlbums() } }
            launch { while (isActive) { if (controller?.isPlaying == true) updatePlayer(); delay(100) } }
        } }
        applySkin(preferences.getString("playerSkin", "classic") ?: "classic")
        showLibrary(localPage); showPlayer(playerPage); showPages(); pages.restore(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        val future = MediaController.Builder(this, SessionToken(this, ComponentName(this, PlaybackService::class.java))).buildAsync()
        connection = future
        future.addListener({
            if (connection !== future) return@addListener
            runCatching { future.get() }.onSuccess { controller = it; it.addListener(listener); updatePlayer() }
                .onFailure { message("The playback service could not connect.") }
        }, ContextCompat.getMainExecutor(this))
    }
    override fun onStop() {
        controller?.removeListener(listener); controller = null
        connection?.let { MediaController.releaseFuture(it) }; connection = null
        super.onStop()
    }
    override fun onSaveInstanceState(out: Bundle) {
        out.putBoolean("local", localPage); out.putBoolean("player", playerPage); out.putBoolean("lyrics", lyricPage); out.putBoolean("visualizer", visualizerPage); out.putString("importFor", importFor)
        pages.save(out)
        out.putInt("groupTab", groupTab); out.putString("folder", folder); out.putString("artistFilter", artistFilter); out.putString("albumFilter", albumFilter)
        super.onSaveInstanceState(out)
    }

    private fun requestScan() {
        val p = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED) scan() else permission.launch(p)
    }
    private fun scan() { if (scanJob?.isActive == true) return
        scanJob = lifecycleScope.launch {
        pages.scanResult("Scanning accessible audio…"); binding.scan.isEnabled = false; binding.status.text = "Scanning accessible audio…"
        try { val count = repo.scan(); pages.scanResult("$count songs found."); message("$count songs found.") } catch (e: Exception) { if (e is CancellationException) throw e; pages.scanResult("Scan failed. Existing library kept."); message("Scan failed. Existing library kept. ${e.message.orEmpty()}") }
        finally { binding.scan.isEnabled = true }
    } }
    private fun showTracks() {
        binding.localMusicCount.text = "${tracks.size} songs"
        binding.recentMusicCount.text = "${pages.recentCount()} songs"
        if (groupTab != 0 && folder == null && artistFilter == null && albumFilter == null) { renderGroups(); return }
        (binding.randomPlay.parent as View).visibility = View.VISIBLE
        selectTab(groupTab)
        shown = tracks.filter { (folder == null || it.folder.ifBlank { "Music" } == folder) &&
            (artistFilter == null || it.artist == artistFilter) && (albumFilter == null || albumNames[it.uri] == albumFilter) &&
            (!favoritesOnly || it.uri in favorites()) }
        binding.songs.adapter = object : ArrayAdapter<Track>(this, R.layout.song_row, R.id.songTitle, shown) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                view.findViewById<TextView>(R.id.songNumber).text = (position + 1).toString()
                view.findViewById<TextView>(R.id.songTitle).text = shown[position].title
                view.findViewById<TextView>(R.id.songArtist).text = if (shown[position].available) shown[position].artist else "File unavailable • ${shown[position].artist}"
                view.alpha = if (shown[position].available) 1f else .55f
                // Samsung's ListView does not reliably dispatch the item click when
                // the row also contains the focusable options button.
                view.setOnClickListener {
                    shown.getOrNull(position)?.let { track ->
                        if (track.available) playTracks(position) else offerRelink(track)
                    }
                }
                view.findViewById<View>(R.id.songMore).setOnClickListener {
                    val track = shown[position]
                    val actions = if (track.available) arrayOf("Play", "Play next", "Add to queue", "Add to playlist") else arrayOf("Locate missing file", "Add to playlist")
                    AlertDialog.Builder(this@MainActivity).setTitle(track.title)
                        .setItems(actions) { _, action ->
                            if (!track.available) {
                                if (action == 0) startRelink(track) else addToPlaylist(track)
                                return@setItems
                            }
                            when (action) {
                                0 -> playTracks(position)
                                1, 2 -> controller?.let { c ->
                                    val t = shown[position].let { QueueTrack(UUID.randomUUID().toString(), it.uri, it.title, it.artist).toMediaItem() }
                                    if (action == 1) c.addMediaItem((c.currentMediaItemIndex + 1).coerceAtLeast(0), t) else c.addMediaItem(t)
                                } ?: message("Player is connecting. Try again shortly.")
                                3 -> addToPlaylist(shown[position])
                            }
                        }.show()
                }
                return view
            }
        }
        val count = "${shown.size} ${if (shown.size == 1) "song" else "songs"}"
        binding.status.text = count
        binding.localMusicCount.text = "${tracks.size} ${if (tracks.size == 1) "song" else "songs"}"
        binding.libraryTitle.text = folder ?: artistFilter ?: albumFilter ?: if (favoritesOnly) "Favorite songs" else "Local music"
        binding.recentMusicTitle.text = "Recent play"
        binding.recentMusicCount.text = "${pages.recentCount()} songs"

    }
    private fun playTracks(index: Int) = playSelection(shown, index)
    private fun playSelection(selection: List<Track>, index: Int) {
        val c = controller ?: return message("Player is connecting. Try again shortly.")
        val selected = selection.getOrNull(index) ?: return
        if (!selected.available) return offerRelink(selected)
        val playable = selection.filter { it.available }
        val playableIndex = playable.indexOfFirst { it.id == selected.id }
        c.setMediaItems(playable.map { QueueTrack(UUID.randomUUID().toString(), it.uri, it.title, it.artist).toMediaItem() }, playableIndex, 0)
        c.prepare(); c.play(); showPlayer(true)
    }
    private fun offerRelink(track: Track) {
        AlertDialog.Builder(this).setTitle("Missing audio file")
            .setMessage("boomtrack kept ${track.title} in your library so its playlists and lyrics would not be lost. Choose its replacement file now.")
            .setPositiveButton("Locate file") { _, _ -> startRelink(track) }
            .setNegativeButton("Cancel", null).show()
    }
    private fun startRelink(track: Track) {
        relinkFor = track
        chooseReplacement.launch(arrayOf("audio/*", "application/ogg", "application/octet-stream"))
    }
    private fun migrateUriPreferences(oldUri: String, newUri: String) {
        val oldFavorites = favorites()
        val newFavorites = if (oldUri in oldFavorites) oldFavorites - oldUri + newUri else oldFavorites
        val recent = runCatching { org.json.JSONArray(preferences.getString("recent", "[]")) }.getOrDefault(org.json.JSONArray())
        val migratedRecent = org.json.JSONArray(List(recent.length()) { index -> if (recent.getString(index) == oldUri) newUri else recent.getString(index) })
        preferences.edit().putStringSet("favorites", newFavorites).putString("recent", migratedRecent.toString()).apply()
        lifecycleScope.launch(Dispatchers.IO) {
            repo.dao.queue()?.let { saved ->
                val root = runCatching { org.json.JSONObject(saved.json) }.getOrNull() ?: return@let
                val items = root.optJSONArray("tracks") ?: return@let
                for (i in 0 until items.length()) items.getJSONObject(i).let { if (it.optString("uri") == oldUri) it.put("uri", newUri) }
                repo.dao.saveQueue(saved.copy(json = root.toString()))
            }
        }
    }
    private fun togglePlay() {
        controller?.let { if (it.isPlaying) it.pause() else if (it.mediaItemCount > 0) { it.prepare(); it.play() } }
    }
    private fun showPlayer(show: Boolean) {
        playerPage = show
        binding.libraryPanel.visibility = if (show) View.GONE else View.VISIBLE
        binding.playerPanel.visibility = if (show) View.VISIBLE else View.GONE
        window.statusBarColor = Color.parseColor(if (show) "#172536" else "#1D9CFA")
        window.navigationBarColor = Color.parseColor(if (show) "#030608" else "#1D9CFA")
        binding.root.setBackgroundColor(Color.parseColor(if (show) "#172536" else "#1D9CFA"))
    }
    private fun showLibrary(show: Boolean) {
        localPage = show
        binding.localPanel.visibility = if (show) View.VISIBLE else View.GONE
        binding.homePanel.visibility = if (show) View.GONE else View.VISIBLE
        if (show) binding.allSongs.post {
            binding.tabIndicator.layoutParams = binding.tabIndicator.layoutParams.apply { width = binding.allSongs.width }; selectTab(groupTab)
        }
    }
    private fun showPages() {
        binding.artwork.visibility = if (lyricPage || visualizerPage) View.GONE else View.VISIBLE
        binding.lyricsScroll.visibility = if (lyricPage) View.VISIBLE else View.GONE
        binding.waveform.visibility = if (visualizerPage) View.VISIBLE else View.GONE
        binding.delayPanel.visibility = View.GONE
        binding.artPage.setImageResource(if (!lyricPage && !visualizerPage) R.drawable.classic_page_active else R.drawable.classic_page_dot)
        binding.lyricPage.setImageResource(if (lyricPage) R.drawable.classic_page_active else R.drawable.classic_page_dot)
        binding.detailsPage.setImageResource(if (visualizerPage) R.drawable.classic_page_active else R.drawable.classic_page_dot)
        binding.artPage.alpha = if (!lyricPage && !visualizerPage) 1f else .65f
        binding.lyricPage.alpha = if (lyricPage) 1f else .65f; binding.detailsPage.alpha = if (visualizerPage) 1f else .65f
        lastLine = Int.MIN_VALUE; updatePlayer()
    }
    private fun updatePlayer() {
        val c = controller ?: return
        val item = c.currentMediaItem
        binding.title.setChangedText(item?.mediaMetadata?.title ?: "Choose a song")
        binding.artist.setChangedText(item?.mediaMetadata?.artist?.toString().orEmpty())
        binding.miniTitle.setChangedText(item?.mediaMetadata?.title ?: "No song selected")
        val playing = c.isPlaying
        for (button in listOf(binding.play, binding.miniPlay)) { if (button.tag != playing) {
            button.tag = playing; button.setImageResource(if (playing) R.drawable.ic_classic_pause else R.drawable.ic_classic_play)
            button.contentDescription = if (playing) "Pause" else "Play"
        } }
        binding.repeat.setImageResource(if (c.shuffleModeEnabled) R.drawable.ic_classic_shuffle else R.drawable.ic_classic_repeat)
        binding.repeat.alpha = if (c.repeatMode == Player.REPEAT_MODE_OFF && !c.shuffleModeEnabled) .65f else 1f
        binding.repeat.contentDescription = "Repeat ${when(c.repeatMode) { 1 -> "one"; 2 -> "all"; else -> "off" }}"
        binding.favorite.contentDescription = if (currentUri in favorites()) "Remove from favorites" else "Add to favorites"
        binding.favorite.setColorFilter(if (currentUri in favorites()) 0xffff6b81.toInt() else 0xff1d9cfa.toInt())
        val displayDuration = if (c.duration > 0) c.duration else tracks.firstOrNull { it.uri == currentUri }?.duration ?: 0
        binding.seek.isEnabled = c.isCurrentMediaItemSeekable && c.duration > 0
        if (!dragging) { binding.elapsed.text = time(c.currentPosition); binding.seek.progress = if (displayDuration > 0) (c.currentPosition * 1000 / displayDuration).toInt() else 0 }
        binding.duration.setChangedText(time(displayDuration))
        binding.playerError.visibility = if (c.playerError != null) View.VISIBLE else View.GONE
        binding.playerError.text = "This song cannot be played. Check file access or choose another song."
        val uri = item?.mediaMetadata?.extras?.getString("trackUri") ?: item?.localConfiguration?.uri?.toString()
        if (uri != currentUri) { currentUri = uri; loadTrack(uri) }
        val delayMs = savedLyric?.delayMs ?: 0L
        binding.delayLabel.text = "Delay %.1fs".format(delayMs / 1000.0)
        val index = lyric?.activeIndex(c.currentPosition, delayMs) ?: -1
        if (lyricPage && index != lastLine) {
            lastLine = index
            val l = lyric
            if (l == null) binding.lyrics.text = "No lyrics selected\n\nImport a local LRC or text file."
            else if (l.lines.isEmpty()) binding.lyrics.text = l.plainText
            else {
                val start = (index - 2).coerceAtLeast(0); val end = (index + 4).coerceAtMost(l.lines.size)
                val texts = l.lines.subList(start, end.coerceAtLeast(start)).map { it.text }
                val s = SpannableString(texts.joinToString("\n"))
                if (index >= start && index < end) {
                    val offset = texts.take(index - start).sumOf { it.length + 1 }
                    s.setSpan(ForegroundColorSpan(0xff31adff.toInt()), offset, offset + texts[index - start].length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                binding.lyrics.text = s
            }
        }
    }
    private fun loadTrack(uri: String?) {
        lyricJob?.cancel(); lyric = null; savedLyric = null; lastLine = Int.MIN_VALUE
        binding.artwork.setImageResource(org.ttpod.recreation.R.drawable.classic_disc)
        if (uri == null) return
        lyricJob = lifecycleScope.launch {
            val saved = repo.dao.lyric(uri)
            val parsed = withContext(Dispatchers.Default) { saved?.let { runCatching { LrcParser.parse(it.bytes) }.getOrNull() } }
            if (currentUri != uri) return@launch
            savedLyric = saved; lyric = parsed; lastLine = Int.MIN_VALUE; updatePlayer()
            val bitmap = withContext(Dispatchers.IO) {
                val r = MediaMetadataRetriever()
                try {
                    r.setDataSource(this@MainActivity, Uri.parse(uri))
                    val bytes = r.embeddedPicture ?: return@withContext null
                    if (bytes.size > 8 * 1024 * 1024) return@withContext null
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    if (options.outWidth.toLong() * options.outHeight > 24_000_000) return@withContext null
                    options.inSampleSize = (maxOf(options.outWidth, options.outHeight) / 768).coerceAtLeast(1)
                    options.inJustDecodeBounds = false
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                } catch (_: Exception) { null } finally { r.release() }
            }
            if (currentUri == uri && bitmap != null) binding.artwork.setImageBitmap(bitmap)
        }
    }
    private fun changeDelay(delta: Long) {
        val saved = savedLyric ?: return message("Import lyrics first.")
        val updated = saved.copy(delayMs = (saved.delayMs + delta).coerceIn(-60_000, 60_000))
        savedLyric = updated; lastLine = Int.MIN_VALUE
        lifecycleScope.launch { repo.dao.saveLyric(updated) }; updatePlayer()
    }
    private fun showQueue() {
        val c = controller ?: return
        val dialog = android.app.Dialog(this)
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
        val heading = TextView(this).apply {
            textSize = 16f; setTextColor(0xff454c50.toInt()); setBackgroundColor(0xfff2f2f2.toInt())
            gravity = android.view.Gravity.CENTER_VERTICAL; setPadding(dp(18), 0, dp(12), 0)
            contentDescription = "Queue playback mode"; setOnClickListener { binding.repeat.performClick(); text = "playing/${c.mediaItemCount} songs" }
        }
        panel.addView(heading, LinearLayout.LayoutParams(-1, dp(40)))
        val list = ListView(this).apply { divider = android.graphics.drawable.ColorDrawable(0xffeeeeee.toInt()); dividerHeight = dp(1) }
        panel.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        fun refresh() {
            heading.text = "playing/${c.mediaItemCount} ${if (c.mediaItemCount == 1) "song" else "songs"}"
            list.adapter = object : BaseAdapter() {
                override fun getCount() = c.mediaItemCount
                override fun getItem(position: Int) = c.getMediaItemAt(position)
                override fun getItemId(position: Int) = position.toLong()
                override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                    val row = LinearLayout(this@MainActivity).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
                    val number = TextView(this@MainActivity).apply { text = if (position == c.currentMediaItemIndex) "Ⅱ" else "${position+1}"; textSize = 16f; gravity = android.view.Gravity.CENTER; setTextColor(if(position == c.currentMediaItemIndex) 0xff1d9cfa.toInt() else 0xff888888.toInt()) }
                    row.addView(number, LinearLayout.LayoutParams(dp(52), dp(48)))
                    val title = TextView(this@MainActivity).apply { text = getItem(position).mediaMetadata.title; textSize = 16f; setTextColor(0xff454c50.toInt()); setSingleLine(); ellipsize = android.text.TextUtils.TruncateAt.END
                        setOnClickListener { c.seekToDefaultPosition(position); c.prepare(); c.play(); refresh() } }
                    row.addView(title, LinearLayout.LayoutParams(0, dp(48), 1f))
                    val up = TextView(this@MainActivity).apply {
                        text = "↑"; textSize = 20f; gravity = android.view.Gravity.CENTER
                        setTextColor(if (position > 0) 0xff1d9cfa.toInt() else 0xffcccccc.toInt())
                        isEnabled = position > 0; contentDescription = "Move ${getItem(position).mediaMetadata.title} up"
                        setOnClickListener { c.moveMediaItem(position, position - 1); refresh() }
                    }
                    row.addView(up, LinearLayout.LayoutParams(dp(36), dp(48)))
                    val down = TextView(this@MainActivity).apply {
                        text = "↓"; textSize = 20f; gravity = android.view.Gravity.CENTER
                        setTextColor(if (position < c.mediaItemCount - 1) 0xff1d9cfa.toInt() else 0xffcccccc.toInt())
                        isEnabled = position < c.mediaItemCount - 1; contentDescription = "Move ${getItem(position).mediaMetadata.title} down"
                        setOnClickListener { c.moveMediaItem(position, position + 1); refresh() }
                    }
                    row.addView(down, LinearLayout.LayoutParams(dp(36), dp(48)))
                    val remove = TextView(this@MainActivity).apply { text = "⊗"; textSize = 22f; gravity = android.view.Gravity.CENTER; setTextColor(0xffcccccc.toInt()); contentDescription = "Remove ${getItem(position).mediaMetadata.title}"; setOnClickListener { c.removeMediaItem(position); refresh() } }
                    row.addView(remove, LinearLayout.LayoutParams(dp(48), dp(48)))
                    return row
                }
            }
        }
        val footer = LinearLayout(this).apply { gravity = android.view.Gravity.CENTER_VERTICAL; setBackgroundColor(0xfff5f9fa.toInt()) }
        val disc = ImageView(this).apply { setImageResource(R.drawable.classic_disc) }
        footer.addView(disc, LinearLayout.LayoutParams(dp(50), dp(50)).apply { setMargins(dp(9), 0, dp(12), 0) })
        val song = TextView(this).apply { text = c.currentMediaItem?.mediaMetadata?.title ?: "No song selected"; textSize = 16f; setTextColor(0xff454c50.toInt()); setSingleLine(); ellipsize = android.text.TextUtils.TruncateAt.END; setOnClickListener { dialog.dismiss(); showPlayer(true) } }
        footer.addView(song, LinearLayout.LayoutParams(0, dp(60), 1f)); song.gravity = android.view.Gravity.CENTER_VERTICAL
        val toggle = ImageButton(this).apply {
            setBackgroundColor(Color.TRANSPARENT); setColorFilter(0xff1d9cfa.toInt()); setPadding(dp(20),dp(20),dp(20),dp(20))
            setImageResource(if(c.isPlaying) R.drawable.ic_classic_pause else R.drawable.ic_classic_play)
            contentDescription = if(c.isPlaying) "Pause" else "Play"
            setOnClickListener { togglePlay(); setImageResource(if(c.isPlaying) R.drawable.ic_classic_pause else R.drawable.ic_classic_play); contentDescription = if(c.isPlaying) "Pause" else "Play" }
        }
        footer.addView(toggle, LinearLayout.LayoutParams(dp(60),dp(60)))
        val close = ImageButton(this).apply { setBackgroundColor(Color.TRANSPARENT); setImageResource(R.drawable.ic_classic_queue); setColorFilter(0xff1d9cfa.toInt()); setPadding(dp(19),dp(19),dp(19),dp(19)); contentDescription = "Close queue"; setOnClickListener { dialog.dismiss() } }
        footer.addView(close, LinearLayout.LayoutParams(dp(60),dp(60)))
        panel.addView(footer, LinearLayout.LayoutParams(-1, dp(60)))
        dialog.setContentView(panel); dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setGravity(android.view.Gravity.BOTTOM)
        dialog.show(); dialog.window?.setLayout(-1, minOf(dp(252), resources.displayMetrics.heightPixels / 2))
        refresh()
    }
    private fun applySkin(skin: String) {
        when (skin) {
            "classic" -> binding.playerPanel.setBackgroundResource(R.drawable.player_background)
            "violet" -> binding.playerPanel.setBackgroundResource(R.drawable.player_background_violet)
            else -> skins.find(skin)?.let { installed ->
                val drawable = installed.background?.let { android.graphics.drawable.BitmapDrawable(resources, it.path) }
                    ?: android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(installed.topColor, installed.bottomColor))
                binding.playerPanel.background = drawable
            } ?: run {
                preferences.edit().putString("playerSkin", "classic").apply()
                binding.playerPanel.setBackgroundResource(R.drawable.player_background)
            }
        }
    }
    private fun addToPlaylist(track: Track) {
        lifecycleScope.launch {
            val lists = repo.dao.playlists()
            if (lists.isEmpty()) return@launch message("Create a playlist first from Manage music.")
            AlertDialog.Builder(this@MainActivity).setTitle("Add ${track.title}")
                .setItems(lists.map { it.name }.toTypedArray()) { _, index ->
                    lifecycleScope.launch {
                        val playlist = lists[index]
                        repo.dao.addPlaylistEntry(PlaylistEntry(UUID.randomUUID().toString(), playlist.id, track.uri, repo.dao.nextPlaylistPosition(playlist.id)))
                        message("Added to ${playlist.name}.")
                    }
                }.show()
        }
    }
    private fun selectTab(index: Int) {
        val tabs = listOf(binding.allSongs, binding.artists, binding.albums, binding.folders)
        tabs.forEachIndexed { i, tab -> tab.setTextColor(if (i == index) 0xff3ca5f9.toInt() else 0xff333333.toInt()) }
        binding.tabIndicator.translationX = tabs[index].x
    }
    private fun clearFilters() { groupTab = 0; folder = null; artistFilter = null; albumFilter = null; favoritesOnly = false; selectTab(0) }
    private fun libraryMenu() { pages.open("Manage music") }
    private fun unavailable(name: String) { pages.open(name) }
    private fun playerMenu() {
        AlertDialog.Builder(this).setTitle("Now playing").setItems(arrayOf("Import local lyrics", "Find sidecar lyrics", "Find lyrics online", "Lyric timing", "Artwork", "Lyrics", "Shuffle", "Track information")) { _, i ->
            when (i) { 0 -> binding.importLyrics.performClick()
                1 -> findSidecarLyrics(); 2 -> findLyricsOnline(); 3 -> { lyricPage = true; visualizerPage = false; showPages(); binding.delayPanel.visibility = View.VISIBLE }
                4 -> binding.artPage.performClick(); 5 -> binding.lyricPage.performClick()
                6 -> controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }; 7 -> trackInformation() }
        }.show()
    }
    private fun findSidecarLyrics() {
        val uri = currentUri ?: return message("Choose a song first.")
        val track = tracks.firstOrNull { it.uri == uri } ?: return message("This song is not in the local library.")
        val tree = preferences.getString("lyricTree", null)?.let(Uri::parse) ?: return message("Choose a lyrics folder from Manage music first.")
        lifecycleScope.launch {
            val result = runCatching { repo.importSidecarLyrics(track, tree) }
            result.onSuccess { found -> if (found) { loadTrack(uri); lyricPage = true; visualizerPage = false; showPages(); message("Matching sidecar lyrics saved.") } else message("No matching .lrc file was found in the selected folder.") }
                .onFailure { message("Lyrics folder is unavailable. Select it again. ${it.message.orEmpty()}") }
        }
    }
    private fun findLyricsOnline() {
        val uri = currentUri ?: return message("Choose a song first.")
        val track = tracks.firstOrNull { it.uri == uri } ?: return message("This song is not in the local library.")
        message("Searching lyrics providers...")
        lifecycleScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { lyricsProvider.find(track) } }
            result.onFailure { message("Lyrics search failed. ${it.message.orEmpty()}") }
            val match = result.getOrNull()
            if (match != null) {
                AlertDialog.Builder(this@MainActivity).setTitle("Use these lyrics?")
                    .setMessage("${match.title}\n${match.artist}\n\nSource: ${match.providerId.substringBefore(':')}")
                    .setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ ->
                        lifecycleScope.launch {
                            repo.dao.saveLyric(SavedLyric(uri, match.bytes)); loadTrack(uri)
                            lyricPage = true; visualizerPage = false; showPages(); message("Lyrics saved from ${match.providerId.substringBefore(':')} for offline use.")
                        }
                    }.show()
            } else if (result.isSuccess) message("No matching lyrics were found.")
        }
    }
    private fun trackInformation() { pages.open("Track information") }
    private fun toggleFavorite() {
        val uri = currentUri ?: return
        val values = favorites().toMutableSet()
        if (!values.add(uri)) values.remove(uri)
        preferences.edit().putStringSet("favorites", values).apply()
        updatePlayer(); pages.refresh(); if (favoritesOnly) showTracks()
    }
    private fun shareTrack() {
        val uri = currentUri?.let(Uri::parse) ?: return
        try { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("audio/*")
            .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Share audio")) }
        catch (_: android.content.ActivityNotFoundException) { message("No sharing app is available.") }
    }
    private fun searchLibrary() { pages.open("Search music") }
    private fun chooseGroup(albums: Boolean) { showGroups(if (albums) 2 else 1) }
    private fun libraryBack() {
        if (groupTab != 0 && (folder != null || artistFilter != null || albumFilter != null)) {
            folder = null; artistFilter = null; albumFilter = null; showTracks()
        } else showLibrary(false)
    }
    private fun showGroups(tab: Int) {
        clearFilters(); groupTab = tab; showTracks()
        if (tab == 2) loadAlbums()
    }
    private fun loadAlbums() {
        albumJob?.cancel()
        albumJob = lifecycleScope.launch {
            val names = withContext(Dispatchers.IO) { tracks.associate { track ->
                val r = MediaMetadataRetriever()
                val name = try { r.setDataSource(this@MainActivity, Uri.parse(track.uri)); r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown album" }
                catch (_: Exception) { "Unknown album" } finally { r.release() }
                track.uri to name
            } }
            albumNames.putAll(names); if (groupTab == 2) showTracks()
        }
    }
    private fun renderGroups() {
        selectTab(groupTab); binding.libraryTitle.text = "Local music"
        (binding.randomPlay.parent as View).visibility = View.GONE
        val grouped = tracks.groupBy { when(groupTab) { 1 -> it.artist; 2 -> albumNames[it.uri] ?: "Unknown album"; else -> it.folder.ifBlank { "Music" } } }
        groupNames = grouped.keys.sorted(); shown = emptyList()
        binding.songs.adapter = object : BaseAdapter() {
            override fun getCount() = groupNames.size
            override fun getItem(position: Int) = groupNames[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View =
                pages.groupRow(getItem(position), "${grouped[getItem(position)]?.size ?: 0} songs", groupTab)
        }
    }
    private fun installSwipe(view: View, action: (Float) -> Unit) {
        var downX = 0f; var downY = 0f
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = event.x; downY = event.y; true }
                MotionEvent.ACTION_UP -> {
                    val dx = event.x - downX
                    if (kotlin.math.abs(dx) > 60 * resources.displayMetrics.density && kotlin.math.abs(dx) > kotlin.math.abs(event.y-downY)) action(dx)
                    else view.performClick()
                    true
                }
                else -> true
            }
        }
    }
    private fun TextView.setChangedText(value: CharSequence) { if (text.toString() != value.toString()) text = value }
    private fun message(text: String) { binding.status.text = text; Toast.makeText(applicationContext, text, Toast.LENGTH_LONG).show() }
    private fun time(ms: Long): String { val seconds = ms.coerceAtLeast(0) / 1000; return "%02d:%02d".format(seconds / 60, seconds % 60) }
}
