package org.ttpod.recreation.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong

/** M1 feasibility probe: exact PCM passthrough, no allocation per sample or disk I/O. */
@UnstableApi
class PcmProbe : BaseAudioProcessor() {
    val bytesProcessed = AtomicLong(0)
    override fun onConfigure(input: AudioFormat): AudioFormat =
        if (input.encoding == C.ENCODING_PCM_16BIT) input else AudioFormat.NOT_SET
    override fun queueInput(inputBuffer: ByteBuffer) {
        val count = inputBuffer.remaining()
        if (count == 0) return
        val output = replaceOutputBuffer(count)
        output.put(inputBuffer).flip()
        bytesProcessed.addAndGet(count.toLong())
    }
}
