package org.ttpod.recreation

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.ttpod.recreation.domain.LrcParser

/** Explicit live-provider gate. Kept outside the deterministic device runner. */
@RunWith(AndroidJUnit4::class)
class ProviderIntegrationTest {
    @Test fun documentedLrclibRecordReturnsParseableLyrics() = runBlocking {
        val track = Track("provider-test", "content://provider-test", "I Want to Live", "Borislav Slavov", "", 233_000, "test")
        val result = LrclibLyricsProvider().find(track) ?: error("LRCLIB documented record was not found")
        assertTrue(result.providerId.startsWith("lrclib:"))
        val parsed = LrcParser.parse(result.bytes)
        assertTrue(parsed.lines.isNotEmpty() || parsed.plainText.isNotBlank())
    }

    @Test fun fallbackContinuesAfterProviderFailure() = runBlocking {
        val expected = RemoteLyric("second", "Title", "Artist", "words".toByteArray())
        val provider = FallbackLyricsProvider(listOf(
            object : LyricsProvider { override suspend fun find(track: Track): RemoteLyric? = error("busy") },
            object : LyricsProvider { override suspend fun find(track: Track): RemoteLyric = expected }
        ))
        assertEquals(expected, provider.find(Track("test", "content://test", "Title", "Artist", "", 1, "test")))
    }

    @Test fun lyricsOvhReturnsParseablePlainLyrics() = runBlocking {
        val track = Track("provider-test-2", "content://provider-test-2", "Shake It Out", "Florence + The Machine", "", 278_000, "test")
        val result = LyricsOvhProvider().find(track) ?: error("lyrics.ovh record was not found")
        assertEquals("lyrics.ovh", result.providerId)
        assertTrue(LrcParser.parse(result.bytes).plainText.isNotBlank())
    }
}
