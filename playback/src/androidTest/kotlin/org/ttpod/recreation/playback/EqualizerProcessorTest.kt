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
import kotlin.math.PI
import kotlin.math.sin

@UnstableApi
@RunWith(AndroidJUnit4::class)
class EqualizerProcessorTest {
    private fun tone(frequency: Double, frames: Int = 4096): ShortArray = ShortArray(frames * 2) { index ->
        (sin(2.0 * PI * frequency * (index / 2) / 48_000.0) * 12_000).toInt().toShort()
    }
    private fun process(processor: EqualizerProcessor, samples: ShortArray): ShortArray {
        val input = ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.nativeOrder())
        samples.forEach(input::putShort); input.flip(); processor.queueInput(input)
        val output = processor.output.order(ByteOrder.nativeOrder())
        return ShortArray(output.remaining() / 2) { output.short }
    }

    @Test fun offIsBitExactAndPresetRemainsFiniteAcrossFlush() {
        val processor = EqualizerProcessor()
        processor.configure(AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT)); processor.flush()
        val input = tone(1000.0)
        assertArrayEquals(input, process(processor, input))

        processor.setPreset(EffectPreset.BASS)
        val changed = process(processor, input)
        assertEquals(input.size, changed.size)
        assertFalse("Enabled EQ must change PCM", input.contentEquals(changed))
        processor.flush()
        assertEquals(input.size, process(processor, input).size)
    }

    @Test fun monoAndStereoRateChangesAreAccepted() {
        val processor = EqualizerProcessor()
        for ((rate, channels) in listOf(44_100 to 1, 48_000 to 2)) {
            assertEquals(AudioFormat(rate, channels, C.ENCODING_PCM_16BIT), processor.configure(AudioFormat(rate, channels, C.ENCODING_PCM_16BIT)))
            processor.flush()
        }
    }
}
