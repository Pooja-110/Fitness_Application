package com.biowellness.app.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.biowellness.app.data.models.AudioItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * AudioPlayer
 *
 * Generates audio in real time using Android AudioTrack.
 *
 * No audio files are required.
 *
 * Supported generated sounds:
 * - White noise
 * - Pink noise
 * - Brown noise
 * - Grey noise
 * - Rain
 * - Forest
 * - Ocean
 * - Birds
 * - Tibetan-style tone
 * - Delta binaural
 * - Theta binaural
 * - Alpha binaural
 * - Beta binaural
 * - Gamma binaural
 * - Numeric/frequency tones
 */
class AudioPlayer(context: Context) {

    // Context is kept for API compatibility.
    // AudioTrack itself does not require the Context here.
    private val appContext = context.applicationContext

    private var track: AudioTrack? = null
    private var job: Job? = null

    private val scope =
        CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val sampleRate = 44_100

    /**
     * Currently playing item's name.
     */
    var currentName: String? = null
        private set

    /**
     * Current volume.
     *
     * IMPORTANT:
     * This is private so Kotlin does not expose a public
     * JVM setVolume(Float) method.
     *
     * That prevents the Platform declaration clash.
     */
    private var volume: Float = 0.7f


    // ============================================================
    // PLAY
    // ============================================================

    fun play(item: AudioItem) {

        stop()

        currentName = item.name

        job = scope.launch {

            var audioTrack: AudioTrack? = null

            try {

                val audioAttributes =
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()

                val audioFormat =
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()

                val minBufferSize =
                    AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_FLOAT
                    )

                if (minBufferSize <= 0) {
                    throw IllegalStateException(
                        "Unable to determine AudioTrack buffer size"
                    )
                }

                val bufferSize =
                    maxOf(
                        minBufferSize,
                        sampleRate / 2 * 4
                    )

                audioTrack =
                    AudioTrack(
                        audioAttributes,
                        audioFormat,
                        bufferSize,
                        AudioTrack.MODE_STREAM,
                        AudioManager.AUDIO_SESSION_ID_GENERATE
                    )

                if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
                    throw IllegalStateException(
                        "AudioTrack failed to initialize"
                    )
                }

                track = audioTrack

                // Set volume ONCE.
                audioTrack.setVolume(volume)

                audioTrack.play()

                val generator = generator(item)

                var phase = 0.0

                // 250 ms chunks.
                val framesPerChunk = sampleRate / 4

                while (
                    isActive &&
                    audioTrack.state == AudioTrack.STATE_INITIALIZED
                ) {

                    val buffer =
                        generator(
                            framesPerChunk,
                            phase
                        )

                    phase += framesPerChunk

                    if (!isActive) {
                        break
                    }

                    val written =
                        audioTrack.write(
                            buffer,
                            0,
                            buffer.size,
                            AudioTrack.WRITE_BLOCKING
                        )

                    if (written < 0) {
                        break
                    }
                }

            } catch (_: CancellationException) {

                // Normal when stop() cancels playback.

            } catch (_: Exception) {

                // Prevent audio generation errors from crashing
                // the coroutine/app.

            } finally {

                try {
                    audioTrack?.pause()
                } catch (_: Exception) {
                }

                try {
                    audioTrack?.flush()
                } catch (_: Exception) {
                }

                try {
                    audioTrack?.stop()
                } catch (_: Exception) {
                }

                try {
                    audioTrack?.release()
                } catch (_: Exception) {
                }

                if (track === audioTrack) {
                    track = null
                }
            }
        }
    }


    // ============================================================
    // STOP
    // ============================================================

    fun stop() {

        job?.cancel()
        job = null

        val currentTrack = track
        track = null

        if (currentTrack != null) {

            try {
                currentTrack.pause()
            } catch (_: Exception) {
            }

            try {
                currentTrack.flush()
            } catch (_: Exception) {
            }

            try {
                currentTrack.stop()
            } catch (_: Exception) {
            }

            try {
                currentTrack.release()
            } catch (_: Exception) {
            }
        }

        currentName = null
    }


    // ============================================================
    // VOLUME
    // ============================================================

    /**
     * Changes player volume from 0.0 to 1.0.
     *
     * IMPORTANT:
     * Do NOT call this setVolume(), because a public
     * property named volume would generate the same JVM
     * setVolume(Float) method.
     */
    fun setPlayerVolume(value: Float) {

        volume = value.coerceIn(
            0.0f,
            1.0f
        )

        try {
            track?.setVolume(volume)
        } catch (_: Exception) {
        }
    }


    /**
     * Returns current player volume.
     */
    fun getPlayerVolume(): Float {
        return volume
    }


    // ============================================================
    // RELEASE
    // ============================================================

    fun release() {

        stop()

        scope.cancel()
    }


    // ============================================================
    // GENERATOR FACTORY
    // ============================================================

    private fun generator(
        item: AudioItem
    ): (Int, Double) -> FloatArray {

        val name =
            item.name.lowercase()

        return when {

            "brown" in name ->
                ::brown

            "pink" in name ->
                ::pink

            "white" in name ->
                ::white

            "grey" in name ->
                ::grey

            "gray" in name ->
                ::grey

            "rain" in name ->
                ::rain

            "forest" in name ->
                ::forest

            "ocean" in name ->
                ::ocean

            "bird" in name ->
                ::birds

            "tibetan" in name ->
                ::tibetan

            "delta" in name ->
                { frames, phase ->
                    binaural(
                        frames,
                        phase,
                        100.0,
                        2.0
                    )
                }

            "theta" in name ->
                { frames, phase ->
                    binaural(
                        frames,
                        phase,
                        100.0,
                        6.0
                    )
                }

            "alpha" in name ->
                { frames, phase ->
                    binaural(
                        frames,
                        phase,
                        100.0,
                        10.0
                    )
                }

            "beta" in name ->
                { frames, phase ->
                    binaural(
                        frames,
                        phase,
                        100.0,
                        18.0
                    )
                }

            "gamma" in name ->
                { frames, phase ->
                    binaural(
                        frames,
                        phase,
                        100.0,
                        40.0
                    )
                }

            else -> {

                val frequency =
                    Regex(
                        "[0-9]+\\.?[0-9]*"
                    )
                        .find(item.frequency)
                        ?.value
                        ?.toDoubleOrNull()
                        ?: 528.0

                { frames, phase ->
                    sine(
                        frames,
                        phase,
                        frequency
                    )
                }
            }
        }
    }


    // ============================================================
    // SINE WAVE
    // ============================================================

    private fun sine(
        frames: Int,
        phase: Double,
        hz: Double
    ): FloatArray {

        val buffer =
            FloatArray(frames * 2)

        for (i in 0 until frames) {

            val sample =
                (
                        sin(
                            2.0 *
                                    PI *
                                    hz *
                                    (phase + i) /
                                    sampleRate
                        ) * 0.4
                        ).toFloat()

            // Left channel
            buffer[i * 2] = sample

            // Right channel
            buffer[i * 2 + 1] = sample
        }

        return buffer
    }


    // ============================================================
    // BINAURAL
    // ============================================================

    private fun binaural(
        frames: Int,
        phase: Double,
        base: Double,
        beat: Double
    ): FloatArray {

        val buffer =
            FloatArray(frames * 2)

        for (i in 0 until frames) {

            val time =
                (phase + i) /
                        sampleRate

            val left =
                sin(
                    2.0 *
                            PI *
                            base *
                            time
                ) * 0.35

            val right =
                sin(
                    2.0 *
                            PI *
                            (base + beat) *
                            time
                ) * 0.35

            buffer[i * 2] =
                left.toFloat()

            buffer[i * 2 + 1] =
                right.toFloat()
        }

        return buffer
    }


    // ============================================================
    // TIBETAN STYLE TONE
    // ============================================================

    private fun tibetan(
        frames: Int,
        phase: Double
    ): FloatArray {

        val buffer =
            FloatArray(frames * 2)

        val frequency = 196.0

        for (i in 0 until frames) {

            val time =
                (phase + i) /
                        sampleRate

            val envelope =
                1.0 -
                        exp(
                            -time * 0.3
                        )

            val sample =
                (
                        envelope *
                                (
                                        sin(
                                            2.0 *
                                                    PI *
                                                    frequency *
                                                    time
                                        ) * 0.4 +

                                                sin(
                                                    2.0 *
                                                            PI *
                                                            frequency *
                                                            2.76 *
                                                            time
                                                ) * 0.15 +

                                                sin(
                                                    2.0 *
                                                            PI *
                                                            frequency *
                                                            5.4 *
                                                            time
                                                ) * 0.06
                                        )
                        ).toFloat()

            buffer[i * 2] =
                sample

            buffer[i * 2 + 1] =
                sample
        }

        return buffer
    }


    // ============================================================
    // WHITE NOISE
    // ============================================================

    private fun white(
        frames: Int,
        phase: Double
    ): FloatArray {

        val buffer =
            FloatArray(frames * 2)

        for (i in 0 until frames) {

            val sample =
                (
                        Math.random()
                            .toFloat() *
                                2f -
                                1f
                        ) * 0.3f

            buffer[i * 2] =
                sample

            buffer[i * 2 + 1] =
                sample
        }

        return buffer
    }


    // ============================================================
    // PINK NOISE STATE
    // ============================================================

    private var pink0 = 0f
    private var pink1 = 0f
    private var pink2 = 0f
    private var pink3 = 0f
    private var pink4 = 0f
    private var pink5 = 0f
    private var pink6 = 0f


    // ============================================================
    // PINK NOISE
    // ============================================================

    private fun pink(
        frames: Int,
        phase: Double
    ): FloatArray {

        val buffer =
            FloatArray(frames * 2)

        for (i in 0 until frames) {

            val whiteSample =
                Math.random()
                    .toFloat() *
                        2f -
                        1f

            pink0 =
                0.99886f *
                        pink0 +
                        whiteSample *
                        0.0555179f

            pink1 =
                0.99332f *
                        pink1 +
                        whiteSample *
                        0.0750759f

            pink2 =
                0.96900f *
                        pink2 +
                        whiteSample *
                        0.1538520f

            pink3 =
                0.86650f *
                        pink3 +
                        whiteSample *
                        0.3104856f

            pink4 =
                0.55000f *
                        pink4 +
                        whiteSample *
                        0.5329522f

            pink5 =
                -0.7616f *
                        pink5 -
                        whiteSample *
                        0.0168980f

            val sample =
                (
                        (
                                pink0 +
                                        pink1 +
                                        pink2 +
                                        pink3 +
                                        pink4 +
                                        pink5 +
                                        pink6 +
                                        whiteSample *
                                        0.5362f
                                ) * 0.11f
                        ).coerceIn(
                        -1f,
                        1f
                    )

            pink6 =
                whiteSample *
                        0.115926f

            buffer[i * 2] =
                sample

            buffer[i * 2 + 1] =
                sample
        }

        return buffer
    }


    // ============================================================
    // BROWN NOISE STATE
    // ============================================================

    private var brownPrevious =
        0f


    // ============================================================
    // BROWN NOISE
    // ============================================================

    private fun brown(
        frames: Int,
        phase: Double
    ): FloatArray {

        val buffer =
            FloatArray(frames * 2)

        for (i in 0 until frames) {

            brownPrevious =
                (
                        brownPrevious +
                                (
                                        Math.random()
                                            .toFloat() *
                                                2f -
                                                1f
                                        ) * 0.02f
                        ).coerceIn(
                        -1f,
                        1f
                    )

            val sample =
                (
                        brownPrevious *
                                3.5f
                        )
                    .coerceIn(
                        -1f,
                        1f
                    ) * 0.4f

            buffer[i * 2] =
                sample

            buffer[i * 2 + 1] =
                sample
        }

        return buffer
    }


    // ============================================================
    // GREY NOISE
    // ============================================================

    private fun grey(
        frames: Int,
        phase: Double
    ): FloatArray {

        val buffer =
            pink(
                frames,
                phase
            )

        for (i in buffer.indices) {
            buffer[i] *= 0.85f
        }

        return buffer
    }


    // ============================================================
    // RAIN
    // ============================================================

    private fun rain(
        frames: Int,
        phase: Double
    ): FloatArray {

        val buffer =
            pink(
                frames,
                phase
            )

        for (i in 0 until frames) {

            val envelope =
                (
                        0.7 +
                                0.3 *
                                sin(
                                    2.0 *
                                            PI *
                                            0.7 *
                                            (phase + i) /
                                            sampleRate
                                )
                        ).toFloat()

            buffer[i * 2] =
                (
                        buffer[i * 2] *
                                envelope
                        ).coerceIn(
                        -1f,
                        1f
                    )

            buffer[i * 2 + 1] =
                (
                        buffer[i * 2 + 1] *
                                envelope
                        ).coerceIn(
                        -1f,
                        1f
                    )
        }

        return buffer
    }


    // ============================================================
    // FOREST
    // ============================================================

    private fun forest(
        frames: Int,
        phase: Double
    ): FloatArray {

        val buffer =
            pink(
                frames,
                phase
            )

        for (i in 0 until frames) {

            val time =
                (phase + i) /
                        sampleRate

            val chirp =
                if ((time * 3).toInt() % 7 == 0) {

                    (
                            sin(
                                2.0 *
                                        PI *
                                        800.0 *
                                        time
                            ) * 0.08
                            ).toFloat()

                } else {
                    0f
                }

            buffer[i * 2] =
                (
                        buffer[i * 2] *
                                0.6f +
                                chirp
                        ).coerceIn(
                        -1f,
                        1f
                    )

            buffer[i * 2 + 1] =
                (
                        buffer[i * 2 + 1] *
                                0.6f +
                                chirp
                        ).coerceIn(
                        -1f,
                        1f
                    )
        }

        return buffer
    }


    // ============================================================
    // OCEAN
    // ============================================================

    private fun ocean(
        frames: Int,
        phase: Double
    ): FloatArray {

        val buffer =
            brown(
                frames,
                phase
            )

        for (i in 0 until frames) {

            val envelope =
                (
                        0.4 +
                                0.6 *
                                (
                                        (
                                                sin(
                                                    2.0 *
                                                            PI *
                                                            0.1 *
                                                            (phase + i) /
                                                            sampleRate
                                                ) + 1.0
                                                ) / 2.0
                                        )
                        ).toFloat()

            buffer[i * 2] =
                (
                        buffer[i * 2] *
                                envelope
                        ).coerceIn(
                        -1f,
                        1f
                    )

            buffer[i * 2 + 1] =
                (
                        buffer[i * 2 + 1] *
                                envelope
                        ).coerceIn(
                        -1f,
                        1f
                    )
        }

        return buffer
    }


    // ============================================================
    // BIRDS
    // ============================================================

    private fun birds(
        frames: Int,
        phase: Double
    ): FloatArray {

        val buffer =
            FloatArray(frames * 2)

        for (i in 0 until frames) {

            val time =
                (phase + i) /
                        sampleRate

            val cycle =
                time % 2.0

            val sample =

                if (cycle < 0.15) {

                    (
                            sin(
                                2.0 *
                                        PI *
                                        (
                                                2000.0 +
                                                        1500.0 *
                                                        sin(
                                                            PI *
                                                                    cycle /
                                                                    0.15
                                                        )
                                                ) *
                                        time
                            ) * 0.25
                            ).toFloat()

                } else if (cycle < 0.25) {

                    (
                            sin(
                                2.0 *
                                        PI *
                                        (
                                                3000.0 +
                                                        500.0 *
                                                        sin(
                                                            PI *
                                                                    (cycle - 0.15) /
                                                                    0.1
                                                        )
                                                ) *
                                        time
                            ) * 0.15
                            ).toFloat()

                } else {
                    0f
                }

            buffer[i * 2] =
                sample

            buffer[i * 2 + 1] =
                sample
        }

        return buffer
    }
}