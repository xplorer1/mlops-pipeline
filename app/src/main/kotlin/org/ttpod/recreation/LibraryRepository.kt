package org.ttpod.recreation

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ttpod.recreation.domain.LrcParser

class LibraryRepository(private val context: Context, val dao: LibraryDao) {
    data class FolderImportResult(val imported: Int, val skipped: Int)

    suspend fun scan(): Int = withContext(Dispatchers.IO) {
        val rows = mutableListOf<Track>()
        val volumes = if (Build.VERSION.SDK_INT >= 29) MediaStore.getExternalVolumeNames(context) else setOf("external")
        volumes.forEach { volume ->
            val collection = MediaStore.Audio.Media.getContentUri(volume)
            val path = if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA
            val columns = arrayOf("_id", "title", "artist", "duration", path)
            context.contentResolver.query(collection, columns, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val location = c.getString(4).orEmpty()
                    val uri = ContentUris.withAppendedId(collection, id)
                    val indexedDuration = c.getLong(3)
                    // Android 16 can publish an audio row before (or without) filling duration.
                    // Verify the file itself so a valid song is not silently hidden forever.
                    val duration = if (indexedDuration > 0) indexedDuration else readDuration(uri)
                    if (duration <= 0) continue
                    rows.add(Track("media:$volume:$id", uri.toString(),
                        c.getString(1).orEmpty().ifBlank { "Untitled" }, c.getString(2).orEmpty().replace("<unknown>", "Unknown artist"),
                        if (Build.VERSION.SDK_INT >= 29) location else location.substringBeforeLast('/', "Music"), duration, "mediastore"))
                }
            }
        }
        // Reconcile only after every accessible-volume query completed. Organization
        // survives: stale rows are marked unavailable instead of being deleted.
        dao.reconcileSource("mediastore", rows)
        rows.size
    }

    suspend fun importAudio(uri: Uri, folder: String = "Selected files", source: String = "saf") = withContext(Dispatchers.IO) {
        var name = "Imported audio"
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) name = it.getString(0)
        }
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: name
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown artist"
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            require(duration > 0) { "This file has no readable audio duration." }
            dao.saveTracks(listOf(Track("document:$uri", uri.toString(), title, artist, folder, duration, source)))
        } finally { retriever.release() }
    }

    /** Replace the location of a retained missing track while preserving its identity and organization. */
    suspend fun relinkAudio(oldUri: String, uri: Uri) = withContext(Dispatchers.IO) {
        val replacement = readTrack(uri, displayName(uri), "Relinked files", "saf")
            ?: error("The selected file is not readable audio.")
        dao.relinkTrack(oldUri, replacement)
    }

    private fun displayName(uri: Uri): String =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }?.takeIf { it.isNotBlank() } ?: "Relinked audio"

    /** Index audio below a user-granted tree without copying the media bytes. */
    suspend fun importAudioTree(treeUri: Uri): FolderImportResult = withContext(Dispatchers.IO) {
        val source = "tree:$treeUri"
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val queue = ArrayDeque<Pair<String, String>>()
        queue.add(rootId to treeDisplayName(treeUri, rootId))
        val tracks = mutableListOf<Track>()
        var visited = 0
        var skipped = 0
        while (queue.isNotEmpty() && visited < MAX_TREE_ENTRIES) {
            val (parentId, folderName) = queue.removeFirst()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            context.contentResolver.query(children, arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ), null, null, null)?.use { cursor ->
                while (cursor.moveToNext() && visited++ < MAX_TREE_ENTRIES) {
                    val id = cursor.getString(0)
                    val name = cursor.getString(1).orEmpty()
                    val type = cursor.getString(2).orEmpty()
                    if (type == DocumentsContract.Document.MIME_TYPE_DIR) {
                        queue.add(id to if (folderName.isBlank()) name else "$folderName/$name")
                    } else if (type.startsWith("audio/") || AUDIO_EXTENSIONS.any { name.endsWith(it, true) }) {
                        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                        readTrack(uri, name, folderName, source)?.let(tracks::add) ?: skipped++
                    }
                }
            } ?: run { skipped++ }
        }
        if (visited >= MAX_TREE_ENTRIES) skipped++
        dao.reconcileSource(source, tracks)
        FolderImportResult(tracks.size, skipped)
    }

    private fun treeDisplayName(treeUri: Uri, rootId: String): String {
        val root = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId)
        return context.contentResolver.query(root, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0).orEmpty() else "Music folder"
        }.orEmpty().ifBlank { "Music folder" }
    }

    private fun readTrack(uri: Uri, name: String, folder: String, source: String): Track? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: return null
            if (duration <= 0) return null
            Track(
                "document:$uri", uri.toString(),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.ifBlank { null } ?: name.substringBeforeLast('.', name),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.ifBlank { null } ?: "Unknown artist",
                folder.ifBlank { "Music folder" }, duration, source
            )
        } catch (_: RuntimeException) { null }
        finally { retriever.release() }
    }

    private fun readDuration(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
        } catch (_: RuntimeException) { 0 }
        finally { retriever.release() }
    }

    suspend fun importLyrics(trackUri: String, uri: Uri): Int = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer); if (count < 0) break
                require(out.size() + count <= LrcParser.MAX_BYTES) { "Lyrics exceed the 1 MiB limit." }
                out.write(buffer, 0, count)
            }
            out.toByteArray()
        } ?: error("The lyric file could not be opened.")
        val parsed = LrcParser.parse(bytes) // Validate before replacing the previous selection.
        dao.saveLyric(SavedLyric(trackUri, bytes))
        parsed.skippedLines
    }

    suspend fun importSidecarLyrics(track: Track, treeUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val audioName = context.contentResolver.query(Uri.parse(track.uri), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
        val candidates = setOfNotNull(
            audioName?.substringBeforeLast('.', audioName)?.plus(".lrc"),
            "${track.artist} - ${track.title}.lrc",
            "${track.title}.lrc"
        ).map { it.lowercase() }.toSet()
        val queue = ArrayDeque<String>()
        queue.add(DocumentsContract.getTreeDocumentId(treeUri))
        var visited = 0
        var match: Uri? = null
        while (queue.isNotEmpty() && visited < 10_000 && match == null) {
            val parent = queue.removeFirst()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parent)
            context.contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use { cursor ->
                while (cursor.moveToNext() && visited++ < 10_000) {
                    val id = cursor.getString(0); val name = cursor.getString(1).orEmpty(); val type = cursor.getString(2)
                    if (type == DocumentsContract.Document.MIME_TYPE_DIR) queue.add(id)
                    else if (name.lowercase() in candidates) { match = DocumentsContract.buildDocumentUriUsingTree(treeUri, id); break }
                }
            }
        }
        val lyricUri = match ?: return@withContext false
        importLyrics(track.uri, lyricUri)
        true
    }

    private companion object {
        const val MAX_TREE_ENTRIES = 10_000
        val AUDIO_EXTENSIONS = setOf(".mp3", ".m4a", ".aac", ".ogg", ".opus", ".flac", ".wav", ".wma")
    }
}
