package org.ttpod.recreation

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.doAfterTextChanged
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

/** Page navigation shares the existing library mini-player; native pickers and queue remain overlays. */
@UnstableApi
internal class LibraryPages(
    private val activity: MainActivity,
    private val root: FrameLayout,
    private val library: View,
    private val mini: View,
    private val content: FrameLayout,
    private val tracks: () -> List<Track>,
    private val favorites: () -> Set<String>,
    private val play: (List<Track>, Int) -> Unit,
    private val scan: () -> Unit,
    private val choose: () -> Unit,
    private val chooseFolder: () -> Unit,
    private val local: () -> Unit,
    private val controller: () -> MediaController?,
    private val isPlayer: () -> Boolean,
    private val showPlayer: (Boolean) -> Unit,
    private val applySkin: (String) -> Unit,
    private val dao: LibraryDao,
    private val chooseSkin: () -> Unit,
    private val installedSkins: () -> List<InstalledSkin>,
    private val chooseLyricFolder: () -> Unit,
    private val hasLyricFolder: () -> Boolean,
    private val exportData: () -> Unit,
    private val importData: () -> Unit,
) {
    private val blue = 0xff1d9cfa.toInt()
    private val ink = 0xff454c50.toInt()
    private val prefs = activity.getSharedPreferences("classic-ui", Context.MODE_PRIVATE)
    private val host = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); isClickable = true; visibility = View.GONE }
    private val stack = arrayListOf<String>()
    private val playerReturns = arrayListOf<Boolean>()
    private var drawerView: View? = null
    private val baseVisibility = mutableMapOf<View, Int>()
    private var query = ""
    private var scanStatus = "Find audio stored on this device. Files are kept in their original folders."
    private var searchResults: LinearLayout? = null
    private var playlists = emptyList<PlaylistSummary>()
    private var playlistEntries = emptyList<PlaylistEntry>()
    private var playlistJob: kotlinx.coroutines.Job? = null

    init {
        content.addView(host, FrameLayout.LayoutParams(-1, -1))
        activity.lifecycleScope.launch { dao.observePlaylists().collectLatest { playlists = it; if (stack.lastOrNull() == "Playlists") render() } }
    }
    private fun dp(n: Int) = (n * activity.resources.displayMetrics.density).toInt()
    private fun label(value: String, size: Float = 16f, color: Int = ink) = TextView(activity).apply {
        text = value; textSize = size; setTextColor(color); gravity = Gravity.CENTER_VERTICAL
    }
    private fun row(parent: LinearLayout, title: String, subtitle: String = "", action: (() -> Unit)? = null) {
        val line = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), 0, dp(16), 0) }
        val words = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        words.addView(label(title), LinearLayout.LayoutParams(-1, if (subtitle.isEmpty()) -1 else dp(28)))
        if (subtitle.isNotEmpty()) words.addView(label(subtitle, 12f, 0xff888888.toInt()), LinearLayout.LayoutParams(-1, -2))
        line.addView(words, LinearLayout.LayoutParams(0, -1, 1f))
        if (action != null) {
            line.addView(label("›", 26f, 0xff999999.toInt()), LinearLayout.LayoutParams(dp(24), -1))
            line.isFocusable = true; line.setBackgroundResource(android.R.drawable.list_selector_background)
            line.setOnClickListener { action() }
        }
        parent.addView(line, LinearLayout.LayoutParams(-1, dp(if (subtitle.isEmpty()) 52 else 68)))
        parent.addView(View(activity).apply { setBackgroundColor(0xffeeeeee.toInt()) }, LinearLayout.LayoutParams(-1, dp(1)))
    }
    private fun note(parent: LinearLayout, text: String) {
        parent.addView(label(text, 14f, 0xff888888.toInt()).apply { setPadding(dp(24), dp(26), dp(24), dp(26)); gravity = Gravity.CENTER; setLineSpacing(dp(4).toFloat(), 1f) }, LinearLayout.LayoutParams(-1, -2))
    }
    fun groupRow(name: String, count: String, kind: Int): View {
        val line = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), 0, dp(16), 0); layoutParams = AbsListView.LayoutParams(-1, dp(80)) }
        val icon = ImageView(activity).apply { setImageResource(if (kind == 1) R.drawable.classic_group_artist else if (kind == 2) R.drawable.classic_disc else R.drawable.ic_classic_folder); if (kind == 3) setColorFilter(blue) }
        line.addView(icon, LinearLayout.LayoutParams(dp(62), dp(62)))
        val words = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0); gravity = Gravity.CENTER_VERTICAL }
        words.addView(label(name, 18f).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }, LinearLayout.LayoutParams(-1, dp(36)))
        words.addView(label(count, 14f, 0xff888888.toInt()), LinearLayout.LayoutParams(-1, dp(28)))
        line.addView(words, LinearLayout.LayoutParams(0, -1, 1f)); line.addView(label("›", 28f, 0xff999999.toInt()))
        return line
    }
    fun open(route: String) {
        closeDrawer()
        val playerReturn = isPlayer()
        showPlayer(false)
        if (stack.lastOrNull() != route) { stack.add(route); playerReturns.add(playerReturn) }
        render()
    }
    fun back(): Boolean {
        if (stack.isEmpty()) return false
        hideKeyboard(); stack.removeAt(stack.lastIndex)
        val returnPlayer = playerReturns.removeAt(playerReturns.lastIndex)
        if (stack.isEmpty()) { host.visibility = View.GONE; restoreBase(); mini.visibility = View.VISIBLE }
        else render()
        if (returnPlayer) showPlayer(true)
        return true
    }
    private fun hideKeyboard() { (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(host.windowToken, 0); host.clearFocus() }
    private fun openLocal() { stack.clear(); host.visibility = View.GONE; restoreBase(); mini.visibility = View.VISIBLE; playerReturns.clear(); local() }
    fun refresh() { if (stack.lastOrNull() in listOf("Recent play", "Favorite songs", "Search music")) { if (stack.lastOrNull() == "Search music") renderSearchResults() else render() } }
    fun save(out: Bundle) { out.putStringArrayList("routes", stack); out.putString("searchQuery", query); out.putBooleanArray("routePlayerReturns", playerReturns.toBooleanArray()); out.putString("scanStatus", scanStatus) }
    fun restore(state: Bundle?) {
        query = state?.getString("searchQuery").orEmpty()
        scanStatus = state?.getString("scanStatus") ?: scanStatus
        if (scanStatus.startsWith("Scanning")) scanStatus = "Scan interrupted. Tap Fast scan to retry."
        stack.addAll(state?.getStringArrayList("routes").orEmpty()); playerReturns.addAll(state?.getBooleanArray("routePlayerReturns")?.toList() ?: List(stack.size) { false }); if (stack.isNotEmpty()) render()
    }
    fun scanResult(status: String) { scanStatus = status; if (stack.lastOrNull() == "Scan Media") render() }
    private fun history(): List<String> = runCatching { val json = JSONArray(prefs.getString("recent", "[]")); List(json.length()) { json.getString(it) } }.getOrDefault(emptyList())
    fun recentCount(): Int { val available = tracks().map { it.uri }.toSet(); return history().count { it in available } }
    private fun restoreBase() { baseVisibility.forEach { (view, visibility) -> view.visibility = visibility }; baseVisibility.clear() }
    private fun render() {
        if (baseVisibility.isEmpty()) for (i in 0 until content.childCount) {
            val view = content.getChildAt(i)
            if (view !== host) { baseVisibility[view] = view.visibility; view.visibility = View.INVISIBLE }
        }
        host.removeAllViews(); host.visibility = View.VISIBLE
        val route = stack.lastOrNull() ?: return
        mini.visibility = if (route in listOf("Sleep Mode", "Custom timer", "Settings", "About", "Scan Media", "Background & Skin", "Audio Effect", "Account")) View.GONE else View.VISIBLE
        val bar = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(blue) }
        val back = ImageButton(activity).apply { setImageResource(R.drawable.ic_classic_back); setColorFilter(Color.WHITE); setBackgroundColor(Color.TRANSPARENT); setPadding(dp(13), dp(13), dp(13), dp(13)); contentDescription = "Back"; setOnClickListener { back() } }
        bar.addView(back, LinearLayout.LayoutParams(dp(48), dp(48)))
        val heading = if (route.startsWith("Playlist:")) playlists.firstOrNull { it.playlist.id == route.substringAfter(':') }?.playlist?.name ?: "Playlist" else route
        if (route != "Search music") bar.addView(label(heading, 20f, Color.WHITE), LinearLayout.LayoutParams(0, -1, 1f))
        host.addView(bar, LinearLayout.LayoutParams(-1, dp(48)))
        val scroll = ScrollView(activity).apply { isFillViewport = true }
        val page = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        searchResults = null
        if (route in listOf("Search music", "Favorite songs", "Recent play", "Scan Media") || route.startsWith("Playlist:")) host.addView(page, LinearLayout.LayoutParams(-1, 0, 1f))
        else { scroll.addView(page, ViewGroup.LayoutParams(-1, -2)); host.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)) }
        when (route) {
            "Search music" -> {
                val input = EditText(activity).apply { hint = "Search"; textSize = 18f; setSingleLine(); setText(query); setBackgroundColor(Color.WHITE); setPadding(dp(10), 0, dp(10), 0); contentDescription = "Search songs and artists" }
                bar.addView(input, LinearLayout.LayoutParams(0, dp(30), 1f).apply { setMargins(dp(2), 0, dp(6), 0) })
                searchResults = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }.also { page.addView(it, LinearLayout.LayoutParams(-1, 0, 1f)) }
                input.doAfterTextChanged { query = it.toString(); renderSearchResults() }; renderSearchResults()
            }
            "Favorite songs" -> songList(page, tracks().filter { it.uri in favorites() }, "No favorite songs yet. Tap the heart in the player to add one.")
            "Playlists" -> {
                row(page, "Create playlist", "Make a new local playlist") { createPlaylist() }
                if (playlists.isEmpty()) note(page, "No playlists yet.")
                playlists.forEach { summary -> row(page, summary.playlist.name, "${summary.songCount} ${if (summary.songCount == 1) "song" else "songs"}") { open("Playlist:${summary.playlist.id}") } }
            }
            "Recent play" -> {
                val byUri = tracks().associateBy { it.uri }
                val recent = history().mapNotNull { byUri[it] }
                if (recent.isNotEmpty()) row(page, "Clear listening history") { prefs.edit().remove("recent").apply(); render() }
                songList(page, recent, "Your recently played songs will appear here.")
            }
            "Manage music" -> {
                row(page, "Scan Media", "Find music on this device") { open("Scan Media") }
                row(page, "Choose audio files", "Add files using the Android file picker", choose)
                row(page, "Choose music folder", "Index this folder and its subfolders", chooseFolder)
                row(page, "Local music", "${tracks().size} songs", ::openLocal)
                row(page, "Favorite songs", "${tracks().count { it.uri in favorites() }} songs") { open("Favorite songs") }
                row(page, "Playlists", "${playlists.size} playlists") { open("Playlists") }
                row(page, "Lyrics folder", if (hasLyricFolder()) "Folder selected" else "Choose a folder for automatic .lrc matching", chooseLyricFolder)
            }
            "Scan Media" -> {
                page.addView(ImageView(activity).apply { setImageResource(R.drawable.classic_scan_phone); setPadding(dp(30), dp(26), dp(30), dp(16)); scaleType = ImageView.ScaleType.FIT_CENTER }, LinearLayout.LayoutParams(-1, 0, 1f))
                page.addView(label(if (scanStatus.startsWith("Find audio")) "One key scan all the songs" else scanStatus, 18f).apply { gravity = Gravity.CENTER; setPadding(dp(16), 0, dp(16), 0) }, LinearLayout.LayoutParams(-1, dp(64)))
                page.addView(label("Fast scan", 20f, Color.WHITE).apply {
                    gravity = Gravity.CENTER; contentDescription = "Scan device"; isFocusable = true
                    background = android.graphics.drawable.GradientDrawable().apply { setColor(blue); cornerRadius = dp(30).toFloat() }
                    isEnabled = !scanStatus.startsWith("Scanning"); alpha = if (isEnabled) 1f else .5f; setOnClickListener { scan() }
                }, LinearLayout.LayoutParams(dp(200), dp(44)).apply { gravity = Gravity.CENTER; setMargins(0, dp(18), 0, dp(20)) })
                row(page, "Choose audio files", "Add music from a different location", choose)
                row(page, "Choose music folder", "Index a folder without copying its files", chooseFolder)
                row(page, "View local music", "${tracks().size} songs", ::openLocal)
            }
            "Audio downloads", "Music video downloads" -> {
                val tabs = LinearLayout(activity).apply { setBackgroundColor(0xfff2f2f2.toInt()) }
                val panel = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
                fun empty(completed: Boolean) { panel.removeAllViews(); note(panel, if (completed) "No completed downloads" else "No download tasks"); note(panel, "The original download service is unavailable. No download provider is connected in this build."); if (route == "Audio downloads") row(panel, "Choose local audio", "Listen to files already on your device", choose) }
                listOf("SONGS", "IN PROGRESS").forEachIndexed { i, title -> tabs.addView(label(title, 16f, blue).apply { gravity = Gravity.CENTER; isFocusable = true; setOnClickListener { empty(i == 0) } }, LinearLayout.LayoutParams(0, dp(44), 1f)) }
                page.addView(tabs); page.addView(panel); empty(true)
            }
            "Discovery", "Recommendations", "DiDi Find Song", "Music bag", "Account" -> {
                note(page, when(route) { "DiDi Find Song" -> "Song recognition is not connected."; "Account" -> "You are listening locally. Account sign-in is not available."; else -> "This online service is not connected." })
                note(page, "The old TTPod services cannot supply this page. Your local library remains available offline.")
                row(page, "Local music", "Browse songs on this device", ::openLocal)
            }
            "Background & Skin" -> {
                page.addView(View(activity).apply { setBackgroundResource(R.drawable.player_background) }, LinearLayout.LayoutParams(-1, dp(180)))
                val selected = prefs.getString("playerSkin", "classic") ?: "classic"
                row(page, "Classic blue", if (selected == "classic") "Selected" else "Built-in") {
                    prefs.edit().putString("playerSkin", "classic").apply(); applySkin("classic"); render()
                }
                row(page, "Midnight violet", if (selected == "violet") "Selected" else "Built-in") {
                    prefs.edit().putString("playerSkin", "violet").apply(); applySkin("violet"); render()
                }
                installedSkins().forEach { skin -> row(page, skin.name, if (selected == skin.id) "Selected • Imported" else "Imported") {
                    prefs.edit().putString("playerSkin", skin.id).apply(); applySkin(skin.id); render()
                } }
                row(page, "Import skin package", "Choose a validated .zip package", chooseSkin)
                note(page, "Skin packages are data-only: manifest.json plus an optional background.png. Invalid imports leave the current skin unchanged.")
            }
            "Audio Effect" -> {
                val selected = org.ttpod.recreation.playback.EffectPreset.from(prefs.getString("effectPreset", null))
                row(page, "Preset", if (selected == org.ttpod.recreation.playback.EffectPreset.OFF) "Original audio" else selected.name.replace('_', '-').lowercase().replaceFirstChar { it.uppercase() })
                row(page, "Original audio", if (selected == org.ttpod.recreation.playback.EffectPreset.OFF) "Selected" else "Equalizer off") {
                    prefs.edit().putString("effectPreset", org.ttpod.recreation.playback.EffectPreset.OFF.name).apply(); render()
                }
                val presets = listOf("Rock", "Pop", "Dance", "Hip-Hop", "Classic", "Bass", "Vocal", "Concert")
                presets.chunked(3).forEach { names ->
                    val line = LinearLayout(activity)
                    repeat(3) { index ->
                        val tile = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
                        if (index < names.size) {
                            tile.addView(ImageView(activity).apply { setImageResource(if (names[index] == "Bass" || names[index] == "Dance") R.drawable.ic_classic_effect else R.drawable.ic_classic_music); setColorFilter(blue); setPadding(dp(17), dp(17), dp(17), dp(17)); background = android.graphics.drawable.GradientDrawable().apply { shape = android.graphics.drawable.GradientDrawable.OVAL; setColor(0xffe2e8ec.toInt()) } }, LinearLayout.LayoutParams(dp(72), dp(72)))
                            tile.addView(label(names[index], 18f, blue).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, dp(42)))
                            val preset = org.ttpod.recreation.playback.EffectPreset.from(names[index].uppercase().replace('-', '_'))
                            tile.alpha = if (preset == selected) 1f else .72f
                            tile.contentDescription = "${names[index]} equalizer preset${if (preset == selected) ", selected" else ""}"
                            tile.isFocusable = true; tile.setOnClickListener { prefs.edit().putString("effectPreset", preset.name).apply(); render() }
                        }
                        line.addView(tile, LinearLayout.LayoutParams(0, dp(128), 1f))
                    }
                    page.addView(line)
                }
                note(page, "Ten software equalizer bands process this player's PCM. Presets use conservative headroom; custom band editing is not implemented yet.")
            }
            "Sleep Mode" -> {
                val deadline = prefs.getLong("sleepDeadline", 0)
                page.addView(label(if (deadline > System.currentTimeMillis()) "Playback pauses at " + java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(deadline)) else "After the timer, playback will pause.", 15f, 0xff888888.toInt()).apply { gravity = Gravity.CENTER; setBackgroundColor(0xfff2f2f2.toInt()) }, LinearLayout.LayoutParams(-1, dp(52)))
                val selected = if (deadline > System.currentTimeMillis()) prefs.getInt("sleepMinutes", -1) else 0
                listOf(0, 10, 20, 30, 60, 90, -1).forEach { minutes ->
                    val option = RadioButton(activity).apply { text = when(minutes) { 0 -> "Close"; -1 -> "Custom"; else -> "$minutes minutes" }; textSize = 18f; setTextColor(ink); layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), 0, dp(28), 0); buttonTintList = android.content.res.ColorStateList.valueOf(blue); isChecked = selected == minutes
                        setOnClickListener { when(minutes) { -1 -> open("Custom timer"); 0 -> { prefs.edit().remove("sleepDeadline").remove("sleepMinutes").apply(); render() }; else -> setTimer(minutes) } }
                    }
                    page.addView(option, LinearLayout.LayoutParams(-1, dp(44)))
                    page.addView(View(activity).apply { setBackgroundColor(0xffeeeeee.toInt()) }, LinearLayout.LayoutParams(-1, dp(1)))
                }
            }
            "Custom timer" -> {
                note(page, "Pause playback after 1–720 minutes.")
                val input = EditText(activity).apply { hint = "Minutes"; inputType = android.text.InputType.TYPE_CLASS_NUMBER; setSingleLine(); setPadding(dp(18), 0, dp(18), 0) }
                page.addView(input, LinearLayout.LayoutParams(-1, dp(56)))
                row(page, "Start timer") { val minutes = input.text.toString().toIntOrNull(); if (minutes == null || minutes !in 1..720) input.error = "Enter 1–720 minutes" else { prefs.edit().putLong("sleepDeadline", System.currentTimeMillis() + minutes * 60000L).putInt("sleepMinutes", -1).apply(); back() } }
            }
            "Settings" -> {
                row(page, "Library", "Scan or import local music") { open("Manage music") }
                row(page, "Background & Skin", "Classic blue") { open("Background & Skin") }
                row(page, "Audio Effect", "Original audio") { open("Audio Effect") }
                row(page, "Sleep Mode") { open("Sleep Mode") }
                row(page, "Export boomtrack data", "Playlists, favorites and player settings", exportData)
                row(page, "Import boomtrack data", "Restore a boomtrack JSON backup", importData)
                row(page, "About", "boomtrack") { open("About") }
            }
            "About" -> { row(page, "boomtrack", "Private player inspired by the TTPod 8.4.1 interface"); note(page, "Local playback and saved lyrics work offline. Online features use independent providers.") }
            "Track information" -> {
                val c = controller(); val uri = c?.currentMediaItem?.mediaMetadata?.extras?.getString("trackUri") ?: c?.currentMediaItem?.localConfiguration?.uri?.toString()
                val track = tracks().firstOrNull { it.uri == uri }
                if (track == null) note(page, "Choose a song to view its information.") else {
                    row(page, "Title", track.title); row(page, "Artist", track.artist); row(page, "Duration", "%02d:%02d".format(track.duration / 60000, track.duration / 1000 % 60)); row(page, "Folder", track.folder); note(page, track.uri)
                }
            }
            else -> if (route.startsWith("Playlist:")) {
                val id = route.substringAfter(':')
                val playlist = playlists.firstOrNull { it.playlist.id == id }?.playlist
                if (playlist == null) note(page, "This playlist no longer exists.") else {
                    row(page, "Rename playlist", playlist.name) { renamePlaylist(playlist) }
                    row(page, "Delete playlist", "Songs stay in your library") {
                        android.app.AlertDialog.Builder(activity).setTitle("Delete ${playlist.name}?")
                            .setMessage("This removes the playlist only.")
                            .setNegativeButton("Cancel", null).setPositiveButton("Delete") { _, _ ->
                                activity.lifecycleScope.launch { dao.deletePlaylist(id); back() }
                            }.show()
                    }
                    playlistJob?.cancel()
                    playlistJob = activity.lifecycleScope.launch { dao.observePlaylistEntries(id).collectLatest { playlistEntries = it; if (stack.lastOrNull() == route) renderPlaylistSongs(page, id, it) } }
                    renderPlaylistSongs(page, id, playlistEntries)
                }
            }
        }
    }
    private fun renderPlaylistSongs(page: LinearLayout, playlistId: String, entries: List<PlaylistEntry>) {
        while (page.childCount > 4) page.removeViewAt(page.childCount - 1)
        val byUri = tracks().associateBy { it.uri }
        val pairs = entries.mapNotNull { entry -> byUri[entry.trackUri]?.let { entry to it } }
        if (pairs.isEmpty()) { note(page, "This playlist is empty. Add songs from the song menu."); return }
        row(page, "Play all", "${pairs.size} songs") { play(pairs.map { it.second }, 0) }
        val list = ListView(activity).apply { divider = android.graphics.drawable.ColorDrawable(0xffeeeeee.toInt()); dividerHeight = dp(1) }
        list.adapter = object : ArrayAdapter<Pair<PlaylistEntry, Track>>(activity, R.layout.song_row, R.id.songTitle, pairs) {
            override fun getView(position: Int, convertView: View?, container: ViewGroup): View = super.getView(position, convertView, container).apply {
                val (entry, track) = pairs[position]
                findViewById<TextView>(R.id.songNumber).text = "${position + 1}"
                findViewById<TextView>(R.id.songTitle).text = track.title
                findViewById<TextView>(R.id.songArtist).text = if (track.available) track.artist else "File unavailable • ${track.artist}"
                alpha = if (track.available) 1f else .55f
                findViewById<View>(R.id.songMore).visibility = View.VISIBLE
                findViewById<View>(R.id.songMore).setOnClickListener {
                    val choices = buildList { if (position > 0) add("Move up"); if (position < pairs.lastIndex) add("Move down"); add("Remove from playlist") }
                    android.app.AlertDialog.Builder(activity).setTitle(track.title).setItems(choices.toTypedArray()) { _, selected ->
                        activity.lifecycleScope.launch { when (choices[selected]) {
                            "Move up" -> dao.movePlaylistEntry(playlistId, entry.position, entry.position - 1)
                            "Move down" -> dao.movePlaylistEntry(playlistId, entry.position, entry.position + 1)
                            else -> dao.deletePlaylistEntry(entry.id)
                        } }
                    }.show()
                }
            }
        }
        list.setOnItemClickListener { _, _, index, _ -> if (pairs[index].second.available) play(pairs.map { it.second }, index) else Toast.makeText(activity, "This file is unavailable.", Toast.LENGTH_LONG).show() }
        page.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
    }
    private fun renamePlaylist(playlist: Playlist) {
        val input = EditText(activity).apply { setText(playlist.name); setSingleLine(); setSelection(text.length); setPadding(dp(16), 0, dp(16), 0) }
        android.app.AlertDialog.Builder(activity).setTitle("Rename playlist").setView(input).setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ ->
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) activity.lifecycleScope.launch { dao.renamePlaylist(playlist.id, name) }
        }.show()
    }
    private fun createPlaylist() {
        val input = EditText(activity).apply { hint = "Playlist name"; setSingleLine(); setPadding(dp(16), 0, dp(16), 0) }
        android.app.AlertDialog.Builder(activity).setTitle("New playlist").setView(input).setNegativeButton("Cancel", null)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) activity.lifecycleScope.launch { dao.createPlaylist(Playlist(UUID.randomUUID().toString(), name, System.currentTimeMillis())) }
            }.show()
    }
    private fun renderSearchResults() {
        val parent = searchResults ?: return; parent.removeAllViews()
        val term = query.trim()
        if (term.isEmpty()) note(parent, "Search your local library") else songList(parent, tracks().filter { it.title.contains(term, true) || it.artist.contains(term, true) }, "No songs match “$term”.")
    }
    private fun setTimer(minutes: Int) { prefs.edit().putLong("sleepDeadline", System.currentTimeMillis() + minutes * 60000L).putInt("sleepMinutes", minutes).apply(); render() }
    private fun songList(parent: LinearLayout, songs: List<Track>, empty: String) {
        if (songs.isEmpty()) { note(parent, empty); return }
        row(parent, "Play all", "${songs.size} songs") { hideKeyboard(); play(songs, 0) }
        val list = ListView(activity).apply { divider = android.graphics.drawable.ColorDrawable(0xffeeeeee.toInt()); dividerHeight = dp(1) }
        list.adapter = object : ArrayAdapter<Track>(activity, R.layout.song_row, R.id.songTitle, songs) {
            override fun getView(position: Int, convertView: View?, container: ViewGroup): View {
                return super.getView(position, convertView, container).apply {
                    findViewById<TextView>(R.id.songNumber).text = "${position + 1}"
                    findViewById<TextView>(R.id.songTitle).text = songs[position].title
                    findViewById<TextView>(R.id.songArtist).text = if (songs[position].available) songs[position].artist else "File unavailable • ${songs[position].artist}"
                    alpha = if (songs[position].available) 1f else .55f
                    findViewById<View>(R.id.songMore).visibility = View.GONE
                }
            }
        }
        list.setOnItemClickListener { _, _, index, _ ->
            if (!songs[index].available) Toast.makeText(activity, "This file is unavailable. Restore it and scan again.", Toast.LENGTH_LONG).show()
            else { hideKeyboard(); play(songs, index) }
        }
        parent.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
    }
    fun drawer() {
        if (drawerView != null) return
        val width = minOf(dp(277), root.width - dp(60))
        val overlay = FrameLayout(activity).apply { setOnClickListener { closeDrawer() } }
        val scroll = ScrollView(activity).apply {
            background = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xff008fff.toInt(), 0xff2896ef.toInt())); isClickable = true
        }
        val panel = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val profile = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), 0, dp(12), 0); setOnClickListener { open("Account") } }
        profile.addView(ImageView(activity).apply { setImageResource(R.drawable.classic_profile) }, LinearLayout.LayoutParams(dp(46), dp(46)))
        profile.addView(label("   Local listener", 20f, Color.WHITE)); panel.addView(profile, LinearLayout.LayoutParams(-1, dp(82)))
        fun item(name: String, icon: Int, action: (() -> Unit)? = null) {
            val line = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(28), 0, dp(16), 0); if (action != null) { isFocusable = true; setOnClickListener { action() } } }
            line.addView(ImageView(activity).apply { setImageResource(icon); setColorFilter(Color.WHITE); alpha = if (action == null) .55f else 1f }, LinearLayout.LayoutParams(dp(26), dp(26)))
            line.addView(label(name, 16f, Color.WHITE).apply { setPadding(dp(15), 0, 0, 0); alpha = if (action == null) .55f else 1f }, LinearLayout.LayoutParams(-1, -1))
            panel.addView(line, LinearLayout.LayoutParams(-1, dp(48)))
        }
        item("Offline listening", R.drawable.ic_classic_music)
        item("Playlists", R.drawable.ic_classic_music) { open("Playlists") }
        item("Background & Skin", R.drawable.ic_classic_skin) { open("Background & Skin") }
        item("Sleep Mode", R.drawable.ic_classic_clock) { open("Sleep Mode") }
        item("DiDi Find Song", R.drawable.ic_classic_search) { open("DiDi Find Song") }
        item("Audio Effect", R.drawable.ic_classic_effect) { open("Audio Effect") }
        item("Music bag", R.drawable.ic_classic_music) { open("Music bag") }
        item("Settings", R.drawable.ic_classic_settings) { open("Settings") }
        panel.addView(View(activity).apply { setBackgroundColor(0x3388ccff) }, LinearLayout.LayoutParams(-1, dp(1)))
        item("Scan Media", R.drawable.ic_classic_search) { open("Scan Media") }
        panel.addView(View(activity).apply { setBackgroundColor(0x3388ccff) }, LinearLayout.LayoutParams(-1, dp(1)))
        item("Exit", R.drawable.ic_classic_exit) { controller()?.pause(); activity.finish() }
        scroll.addView(panel); overlay.addView(scroll, FrameLayout.LayoutParams(width, -1))
        library.translationX = width.toFloat(); library.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        root.addView(overlay, FrameLayout.LayoutParams(-1, -1)); drawerView = overlay
    }
    fun closeDrawer(): Boolean { val view = drawerView ?: return false; root.removeView(view); library.translationX = 0f; library.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO; drawerView = null; return true }
}
