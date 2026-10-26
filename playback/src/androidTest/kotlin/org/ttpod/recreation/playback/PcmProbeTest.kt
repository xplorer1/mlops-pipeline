package org.ttpod.recreation.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder

@UnstableApi
@RunWith(AndroidJUnit4::class)
class PcmProbeTest {
    @Test fun emptyBufferDuringDrainDoesNotCopyIntoItself() {
        val probe = PcmProbe()
        probe.configure(AudioFormat(44100, 1, C.ENCODING_PCM_16BIT)); probe.flush()
        probe.queueInput(androidx.media3.common.audio.AudioProcessor.EMPTY_BUFFER)
        assertEquals(0L, probe.bytesProcessed.get())
        assertFalse(probe.output.hasRemaining())
    }
    @Test fun passthroughPreservesPcmAcrossFlushAndRateChange() {
        val probe = PcmProbe()
        for (rate in listOf(44100, 48000)) {
            probe.configure(AudioFormat(rate, 2, C.ENCODING_PCM_16BIT)); probe.flush()
            val input = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder())
            val samples = shortArrayOf(0, 32767, -32768, 1234)
            samples.forEach { input.putShort(it) }; input.flip()
            probe.queueInput(input)
            val output = probe.output.order(ByteOrder.nativeOrder())
            samples.forEach { assertEquals(it, output.short) }
            assertFalse(output.hasRemaining())
        }
        assertEquals(16L, probe.bytesProcessed.get())
        probe.reset()
    }
}
