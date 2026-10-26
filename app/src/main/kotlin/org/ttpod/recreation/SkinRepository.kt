package org.ttpod.recreation

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream

data class InstalledSkin(val id: String, val name: String, val topColor: Int, val bottomColor: Int, val background: File?)

/** Imports a constrained data-only skin package. Packages never contain executable content. */
class SkinRepository(private val context: Context) {
    private val root get() = File(context.filesDir, "skins").apply { mkdirs() }

    fun installed(): List<InstalledSkin> = root.listFiles().orEmpty().filter { it.isDirectory && !it.name.startsWith(".staging-") }
        .mapNotNull { readSkin(it) }.sortedBy { it.name.lowercase(Locale.ROOT) }

    fun find(id: String): InstalledSkin? = readSkin(File(root, id))

    fun import(uri: Uri): InstalledSkin {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            require(descriptor.length < 0 || descriptor.length <= 10L * 1024 * 1024) { "Skin package exceeds 10 MiB." }
        }
        val staging = File(root, ".staging-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            var expanded = 0L
            var count = 0
            val names = mutableSetOf<String>()
            context.contentResolver.openInputStream(uri)?.use { raw ->
                ZipInputStream(raw.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        require(!entry.isDirectory) { "Skin folders are not supported." }
                        val name = entry.name.replace('\\', '/')
                        val normalized = name.lowercase(Locale.ROOT)
                        require(name == "manifest.json" || name == "background.png") { "Unsupported skin entry: $name" }
                        require('/' !in name && name != "." && name != "..") { "Unsafe skin path." }
                        require(names.add(normalized)) { "Duplicate skin entry: $name" }
                        require(++count <= 256) { "Skin contains too many files." }
                        val bytes = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            expanded += read
                            require(expanded <= 40L * 1024 * 1024) { "Expanded skin exceeds 40 MiB." }
                            bytes.write(buffer, 0, read)
                        }
                        File(staging, name).writeBytes(bytes.toByteArray())
                    }
                }
            } ?: error("The skin package could not be opened.")
            require("manifest.json" in names) { "Skin manifest is missing." }
            val skin = readSkin(staging) ?: error("Skin manifest is invalid.")
            skin.background?.let { image ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(image.path, options)
                require(options.outWidth in 1..4096 && options.outHeight in 1..4096 && options.outWidth.toLong() * options.outHeight <= 24_000_000) { "Skin background dimensions are invalid." }
            }
            val destination = File(root, skin.id)
            require(!destination.exists()) { "A skin with this ID is already installed." }
            require(staging.renameTo(destination)) { "Skin could not be activated." }
            return readSkin(destination) ?: error("Skin activation failed.")
        } finally {
            if (staging.exists()) staging.deleteRecursively()
        }
    }

    private fun readSkin(folder: File): InstalledSkin? = runCatching {
        val manifestFile = File(folder, "manifest.json")
        require(manifestFile.length() in 1..65_536)
        val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
        require(manifest.getInt("version") == 1)
        val id = manifest.getString("id")
        require(id.matches(Regex("[a-z0-9][a-z0-9._-]{2,63}")))
        val name = manifest.getString("name").trim()
        require(name.length in 1..48)
        val top = android.graphics.Color.parseColor(manifest.getString("topColor"))
        val bottom = android.graphics.Color.parseColor(manifest.getString("bottomColor"))
        val background = File(folder, "background.png").takeIf { it.isFile }
        InstalledSkin(id, name, top, bottom, background)
    }.getOrNull()
}
