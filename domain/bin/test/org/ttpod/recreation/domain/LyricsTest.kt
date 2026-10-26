package org.ttpod.recreation.domain

import org.junit.Assert.*
import org.junit.Test

class LyricsTest {
    private fun parse(s: String) = LrcParser.parse(s.toByteArray())
    @Test fun importedOffsetAndUserDelayHaveDistinctSigns() {
        val plus = parse("[offset:+500]\n[00:10.00]hello")
        assertEquals(9500L, plus.lines.single().timeMs)
        assertEquals(-1, plus.activeIndex(9499))
        assertEquals(0, plus.activeIndex(9500))
        assertEquals(-1, plus.activeIndex(9999, 500))
        assertEquals(0, plus.activeIndex(10000, 500))
        assertEquals(10500L, parse("[offset:-500]\n[00:10]hello").lines.single().timeMs)
        val noOffset = parse("[00:10]hello")
        assertEquals(-1, noOffset.activeIndex(10499, 500))
        assertEquals(0, noOffset.activeIndex(10500, 500))
    }
    @Test fun multipleTimestampsUnicodeAndMalformedLines() {
        val l = parse("[ar:Example]\r\n[00:02.1][00:04.123]你好\r\n[00:99]bad\r\n[00:01]first")
        assertEquals(listOf(1000L, 2100L, 4123L), l.lines.map { it.timeMs })
        assertEquals(1, l.skippedLines)
        assertEquals(2, l.activeIndex(5000)); assertEquals(0, l.activeIndex(1000))
        assertEquals("你好", l.lines.last().text)
    }
    @Test fun negativeEffectiveCueIsActiveAtZero() {
        assertEquals(0, parse("[offset:500]\n[00:00.10]start").activeIndex(0))
    }
    @Test fun bomAndPlainLyrics() {
        val bytes = "\uFEFF[00:01]café".toByteArray(Charsets.UTF_16LE)
        assertEquals("café", LrcParser.parse(bytes).lines.single().text)
        assertEquals("hello\nworld", parse("hello\nworld").plainText)
    }
    @Test(expected = IllegalArgumentException::class) fun oversizedRejected() {
        LrcParser.parse(ByteArray(LrcParser.MAX_BYTES + 1))
    }
    @Test(expected = IllegalArgumentException::class) fun noReadableContentRejected() { parse("[ar:a]\n[99:99]bad") }
    @Test fun previousAndSeekBoundaries() {
        assertEquals(1 to 0L, QueueRules.previous(2, 3000))
        assertEquals(2 to 0L, QueueRules.previous(2, 3001))
        assertEquals(0 to 0L, QueueRules.previous(0, 0))
        assertEquals(100L, QueueRules.seek(900, 100))
        assertEquals(0L, QueueRules.seek(-1, 100))
    }
}
