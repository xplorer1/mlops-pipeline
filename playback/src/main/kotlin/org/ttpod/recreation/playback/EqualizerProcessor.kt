package org.ttpod.recreation.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

enum class EffectPreset(val gains: FloatArray) {
    OFF(FloatArray(10)),
    ROCK(floatArrayOf(4f, 3f, 1f, -1f, -2f, 0f, 2f, 3f, 4f, 4f)),
    POP(floatArrayOf(-1f, 1f, 3f, 4f, 2f, -1f, -1f, 1f, 2f, 3f)),
    DANCE(floatArrayOf(5f, 4f, 1f, 0f, 0f, -2f, -1f, 2f, 4f, 5f)),
    HIP_HOP(floatArrayOf(5f, 4f, 1f, 2f, -1f, -1f, 1f, 0f, 2f, 3f)),
    CLASSIC(floatArrayOf(4f, 3f, 2f, 1f, -1f, -1f, 0f, 2f, 3f, 4f)),
    BASS(floatArrayOf(6f, 5f, 3f, 1f, 0f, -1f, -2f, -2f, -1f, 0f)),
    VOCAL(floatArrayOf(-2f, -2f, -1f, 1f, 4f, 5f, 4f, 2f, 0f, -1f)),
    CONCERT(floatArrayOf(3f, 2f, 0f, 1f, 3f, 3f, 1f, 0f, 2f, 3f));

    companion object { fun from(value: String?) = entries.firstOrNull { it.name == value } ?: OFF }
}

/** Lock-free, single-producer waveform buffer read by the visible player page. */
object WaveformBus {
    private val samples = FloatArray(512)
    private val cursor = AtomicInteger()
    fun write(value: Float) { val index = cursor.getAndIncrement(); samples[index and (samples.size - 1)] = value }
    fun snapshot(): FloatArray {
        val end = cursor.get(); return FloatArray(samples.size) { samples[(end + it) and (samples.size - 1)] }
    }
}

@UnstableApi
class EqualizerProcessor : BaseAudioProcessor() {
    @Volatile private var requested = EffectPreset.OFF
    private var active = EffectPreset.OFF
    private var sampleRate = 0
    private var channels = 0
    private var filters = emptyArray<Biquad>()
    private var waveformDivider = 0

    fun setPreset(preset: EffectPreset) { requested = preset }

    override fun onConfigure(input: AudioFormat): AudioFormat {
        if (input.encoding != C.ENCODING_PCM_16BIT || input.channelCount !in 1..8) return AudioFormat.NOT_SET
        sampleRate = input.sampleRate; channels = input.channelCount
        rebuild(requested)
        return input
    }

    override fun onFlush() { filters.forEach { it.reset() }; waveformDivider = 0 }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        if (active != requested) rebuild(requested)
        val output = replaceOutputBuffer(inputBuffer.remaining()).order(inputBuffer.order())
        val enabled = active != EffectPreset.OFF
        var channel = 0
        while (inputBuffer.remaining() >= 2) {
            val raw = inputBuffer.short.toFloat()
            var value = if (enabled) raw * 0.5f else raw
            if (enabled) for (band in 0 until 10) value = filters[band * channels + channel].process(value)
            output.putShort(value.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat()).toInt().toShort())
            if (channel == 0 && waveformDivider++ and 7 == 0) WaveformBus.write(value / 32768f)
            channel = (channel + 1) % channels
        }
        output.flip()
    }

    private fun rebuild(preset: EffectPreset) {
        active = preset
        val frequencies = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
        filters = Array(10 * channels) { index -> Biquad.peaking(sampleRate.toFloat(), frequencies[index / channels].coerceAtMost(sampleRate * .45f), preset.gains[index / channels]) }
    }

    private class Biquad(private val b0: Float, private val b1: Float, private val b2: Float, private val a1: Float, private val a2: Float) {
        private var x1 = 0f; private var x2 = 0f; private var y1 = 0f; private var y2 = 0f
        fun process(x: Float): Float { val y = b0*x + b1*x1 + b2*x2 - a1*y1 - a2*y2; x2=x1; x1=x; y2=y1; y1=y; return y }
        fun reset() { x1=0f; x2=0f; y1=0f; y2=0f }
        companion object {
            fun peaking(rate: Float, frequency: Float, gainDb: Float): Biquad {
                val a = 10.0.pow(gainDb / 40.0).toFloat(); val w = (2f * PI.toFloat() * frequency / rate)
                val alpha = sin(w) / 2f; val c = cos(w); val a0 = 1f + alpha/a
                return Biquad((1f+alpha*a)/a0, (-2f*c)/a0, (1f-alpha*a)/a0, (-2f*c)/a0, (1f-alpha/a)/a0)
            }
        }
    }
}
