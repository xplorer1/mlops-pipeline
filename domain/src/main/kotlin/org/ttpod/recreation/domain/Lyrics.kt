package org.ttpod.recreation.domain

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

data class LyricLine(val timeMs: Long, val text: String)
data class Lyrics(val lines: List<LyricLine>, val plainText: String, val skippedLines: Int) {
    fun activeIndex(positionMs: Long, userDelayMs: Long = 0): Int {
        val time = positionMs - userDelayMs
        var lo = 0; var hi = lines.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= time) lo = mid + 1 else hi = mid
        }
        return lo - 1
    }
}

object LrcParser {
    const val MAX_BYTES = 1024 * 1024
    const val MAX_LINES = 20_000
    private val stamp = Regex("\\[(\\d{1,7}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val offset = Regex("\\[offset:([+-]?\\d+)]", RegexOption.IGNORE_CASE)
    private val metadata = Regex("^\\[(ti|ar|al|by|re|ve|length|offset):.*]$", RegexOption.IGNORE_CASE)

    fun parse(bytes: ByteArray): Lyrics {
        require(bytes.size <= MAX_BYTES) { "Lyrics exceed the 1 MiB limit." }
        val charset = when {
            bytes.size >= 2 && bytes[0] == 0xff.toByte() && bytes[1] == 0xfe.toByte() -> Charsets.UTF_16LE
            bytes.size >= 2 && bytes[0] == 0xfe.toByte() && bytes[1] == 0xff.toByte() -> Charsets.UTF_16BE
            else -> Charsets.UTF_8
        }
        val text = charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF")
        val source = text.lines()
        require(source.size <= MAX_LINES) { "Lyrics exceed the 20,000 line limit." }
        val fileOffset = offset.findAll(text).lastOrNull()?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        require(fileOffset in -86_400_000L..86_400_000L) { "Lyric offset exceeds one day." }
        val result = mutableListOf<LyricLine>(); val plain = mutableListOf<String>(); var skipped = 0
        source.forEach { raw ->
            val line = raw.trim()
            if (line.isBlank() || metadata.matches(line)) return@forEach
            val stamps = stamp.findAll(line).toList()
            if (stamps.isEmpty()) {
                if (line.startsWith("[")) skipped++ else plain.add(line)
            } else {
                val words = line.substring(stamps.last().range.last + 1).trim()
                stamps.forEach { match ->
                    val minutes = match.groupValues[1].toLong()
                    val seconds = match.groupValues[2].toInt()
                    if (seconds > 59) skipped++ else {
                        val fraction = match.groupValues[3].padEnd(3, '0').toLongOrNull() ?: 0L
                        result.add(LyricLine(minutes * 60_000 + seconds * 1000 + fraction - fileOffset, words))
                        require(result.size <= MAX_LINES) { "Too many lyric cues." }
                    }
                }
            }
        }
        require(result.isNotEmpty() || plain.isNotEmpty()) { "No readable lyrics were found." }
        return Lyrics(result.sortedBy { it.timeMs }, plain.joinToString("\n"), skipped)
    }
}
