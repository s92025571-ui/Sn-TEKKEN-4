package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class SoundCue {
    UI_CLICK,
    PUNCH_HIT,
    KICK_HIT,
    HEAVY_SLAM,
    BLOCK_CLANG,
    DODGE_WHOOSH,
    TIGER_ROAR,
    BUFFALO_BELLOW,
    DRAGON_ROAR,
    FIRE_BREATH,
    ROUND_ANNOUNCE,
    KO_IMPACT,
    VICTORY_FANFARE
}

enum class MusicTrackMode {
    NONE,
    MENU,
    COMBAT,
    BOSS_COMBAT
}

class ArcadeSoundEngine(context: Context) {

    private val appContext = context.applicationContext
    @Volatile var sfxEnabled: Boolean = true
    @Volatile var musicEnabled: Boolean = true
    @Volatile private var activeMusicMode: MusicTrackMode = MusicTrackMode.NONE
    @Volatile private var released: Boolean = false

    private val sfxExecutor = Executors.newFixedThreadPool(3)
    private var musicThread: Thread? = null
    private var musicTrack: AudioTrack? = null

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Throwable) {
        null
    }

    fun playCue(cue: SoundCue) {
        if (released) return
        if (sfxEnabled) {
            triggerHaptic(cue)
            sfxExecutor.execute {
                try {
                    val pcm = synthesizeCue(cue)
                    playOneShotPcm(pcm)
                } catch (_: Throwable) {
                    // Safe fallback on headless or audio-restricted devices
                }
            }
        }
    }

    fun setMusicMode(mode: MusicTrackMode) {
        if (released) return
        if (activeMusicMode == mode && musicThread?.isAlive == true) return
        activeMusicMode = mode
        restartMusicLoop()
    }

    fun refreshMusicState(enabled: Boolean) {
        musicEnabled = enabled
        restartMusicLoop()
    }

    private fun restartMusicLoop() {
        stopMusicInternal()
        if (!musicEnabled || activeMusicMode == MusicTrackMode.NONE || released) return

        val targetMode = activeMusicMode
        val thread = Thread({
            try {
                val sampleRate = SAMPLE_RATE
                val bufferSamples = sampleRate / 5 // 200ms chunks
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBuf <= 0) return@Thread

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBuf, bufferSamples * 2))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                musicTrack = track
                track.play()

                val chunk = ShortArray(bufferSamples)
                var globalSampleIndex = 0L

                while (!Thread.currentThread().isInterrupted &&
                    musicEnabled &&
                    activeMusicMode == targetMode &&
                    !released
                ) {
                    renderMusicChunk(chunk, globalSampleIndex, sampleRate, targetMode)
                    globalSampleIndex += bufferSamples
                    track.write(chunk, 0, chunk.size)
                }
                track.stop()
                track.release()
            } catch (_: Throwable) {
                // Ignore audio hardware unavailability in tests
            }
        }, "SnTekkenMusicThread")
        thread.isDaemon = true
        musicThread = thread
        thread.start()
    }

    private fun stopMusicInternal() {
        musicThread?.interrupt()
        musicThread = null
        try {
            musicTrack?.pause()
            musicTrack?.flush()
            musicTrack?.release()
        } catch (_: Throwable) {
        }
        musicTrack = null
    }

    fun release() {
        released = true
        stopMusicInternal()
        sfxExecutor.shutdownNow()
    }

    private fun triggerHaptic(cue: SoundCue) {
        if (!sfxEnabled) return
        val durationMs = when (cue) {
            SoundCue.PUNCH_HIT -> 16L
            SoundCue.KICK_HIT -> 28L
            SoundCue.HEAVY_SLAM, SoundCue.KO_IMPACT -> 55L
            SoundCue.BLOCK_CLANG -> 14L
            SoundCue.DRAGON_ROAR -> 65L
            else -> 0L
        }
        if (durationMs <= 0L) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Throwable) {
        }
    }

    private fun synthesizeCue(cue: SoundCue): ShortArray {
        val sr = SAMPLE_RATE
        return when (cue) {
            SoundCue.UI_CLICK -> generateSamples(sr, 0.07f) { t, _ ->
                val env = exp(-t * 48.0)
                (sin(2.0 * PI * 680.0 * t) * 0.6 + sin(2.0 * PI * 1020.0 * t) * 0.4) * env * 0.45
            }
            SoundCue.PUNCH_HIT -> generateSamples(sr, 0.14f) { t, rng ->
                val punchFreq = 190.0 * exp(-t * 18.0) + 55.0
                val body = sin(2.0 * PI * punchFreq * t) * exp(-t * 19.0)
                val snap = (rng.nextDouble() * 2.0 - 1.0) * exp(-t * 42.0) * 0.65
                (body * 0.8 + snap) * 0.85
            }
            SoundCue.KICK_HIT -> generateSamples(sr, 0.22f) { t, rng ->
                val subFreq = 145.0 * exp(-t * 14.0) + 42.0
                val thud = sin(2.0 * PI * subFreq * t) * exp(-t * 12.0)
                val crack = (rng.nextDouble() * 2.0 - 1.0) * exp(-t * 28.0) * 0.55
                (thud * 0.9 + crack) * 0.92
            }
            SoundCue.HEAVY_SLAM -> generateSamples(sr, 0.38f) { t, rng ->
                val sub = sin(2.0 * PI * (110.0 * exp(-t * 9.0) + 34.0) * t) * exp(-t * 6.5)
                val rumble = (rng.nextDouble() * 2.0 - 1.0) * exp(-t * 9.0) * 0.5
                (sub + rumble) * 0.95
            }
            SoundCue.BLOCK_CLANG -> generateSamples(sr, 0.16f) { t, rng ->
                val metal1 = sin(2.0 * PI * 890.0 * t)
                val metal2 = sin(2.0 * PI * 1340.0 * t)
                val noise = (rng.nextDouble() * 2.0 - 1.0) * exp(-t * 36.0)
                ((metal1 * 0.45 + metal2 * 0.35) * exp(-t * 20.0) + noise * 0.3) * 0.72
            }
            SoundCue.DODGE_WHOOSH -> generateSamples(sr, 0.18f) { t, rng ->
                val env = sin((t / 0.18).coerceIn(0.0, 1.0) * PI)
                val sweep = sin(2.0 * PI * (240.0 + t * 600.0) * t) * 0.25
                val air = (rng.nextDouble() * 2.0 - 1.0) * 0.45
                (sweep + air) * env * 0.55
            }
            SoundCue.TIGER_ROAR -> generateSamples(sr, 0.46f) { t, rng ->
                val env = sin((t / 0.46).coerceIn(0.0, 1.0) * PI)
                val growlFreq = 165.0 - t * 65.0 + sin(2.0 * PI * 28.0 * t) * 14.0
                val saw = ((growlFreq * t) % 1.0) * 2.0 - 1.0
                val breath = (rng.nextDouble() * 2.0 - 1.0) * 0.45
                (saw * 0.6 + breath) * env * 0.8
            }
            SoundCue.BUFFALO_BELLOW -> generateSamples(sr, 0.52f) { t, rng ->
                val env = sin((t / 0.52).coerceIn(0.0, 1.0) * PI)
                val hornFreq = 92.0 - t * 22.0 + sin(2.0 * PI * 19.0 * t) * 8.0
                val wave = sin(2.0 * PI * hornFreq * t) + 0.5 * sin(4.0 * PI * hornFreq * t)
                val snort = (rng.nextDouble() * 2.0 - 1.0) * 0.3
                (wave * 0.55 + snort) * env * 0.85
            }
            SoundCue.DRAGON_ROAR -> generateSamples(sr, 0.85f) { t, rng ->
                val env = sin((t / 0.85).coerceIn(0.0, 1.0) * PI)
                val f1 = 78.0 - t * 24.0 + sin(2.0 * PI * 23.0 * t) * 12.0
                val f2 = 154.0 - t * 40.0
                val roar = sin(2.0 * PI * f1 * t) * 0.6 + sin(2.0 * PI * f2 * t) * 0.35
                val fire = (rng.nextDouble() * 2.0 - 1.0) * 0.5
                (roar + fire) * env * 0.92
            }
            SoundCue.FIRE_BREATH -> generateSamples(sr, 0.65f) { t, rng ->
                val env = sin((t / 0.65).coerceIn(0.0, 1.0) * PI)
                val jet = (rng.nextDouble() * 2.0 - 1.0) * 0.65
                val core = sin(2.0 * PI * (130.0 + sin(t * 35.0) * 25.0) * t) * 0.35
                (jet + core) * env * 0.82
            }
            SoundCue.ROUND_ANNOUNCE -> generateSamples(sr, 0.36f) { t, _ ->
                val f = if (t < 0.16) 330.0 else 440.0
                val env = exp(-(t % 0.18) * 9.0)
                (sin(2.0 * PI * f * t) + 0.4 * sin(4.0 * PI * f * t)) * env * 0.5
            }
            SoundCue.KO_IMPACT -> generateSamples(sr, 0.68f) { t, rng ->
                val boom = sin(2.0 * PI * (120.0 * exp(-t * 5.0) + 32.0) * t) * exp(-t * 3.8)
                val gong = sin(2.0 * PI * 220.0 * t) * exp(-t * 3.2) * 0.45
                val burst = (rng.nextDouble() * 2.0 - 1.0) * exp(-t * 11.0) * 0.45
                (boom + gong + burst) * 0.92
            }
            SoundCue.VICTORY_FANFARE -> generateSamples(sr, 0.75f) { t, _ ->
                val freq = when {
                    t < 0.18 -> 293.66 // D4
                    t < 0.36 -> 369.99 // F#4
                    t < 0.54 -> 440.00 // A4
                    else -> 587.33     // D5
                }
                val localT = t % 0.18
                val env = exp(-localT * 5.5)
                (sin(2.0 * PI * freq * t) * 0.65 + sin(4.0 * PI * freq * t) * 0.25) * env * 0.55
            }
        }
    }

    private fun generateSamples(
        sampleRate: Int,
        durationSec: Float,
        generator: (Double, Random) -> Double
    ): ShortArray {
        val total = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(total)
        val rng = Random(42)
        for (i in 0 until total) {
            val t = i.toDouble() / sampleRate.toDouble()
            val sample = generator(t, rng).coerceIn(-1.0, 1.0)
            out[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun playOneShotPcm(pcm: ShortArray) {
        val byteSize = pcm.size * 2
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) return
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf, byteSize))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcm, 0, pcm.size)
        track.play()
        val sleepMs = ((pcm.size * 1000L) / SAMPLE_RATE) + 45L
        try {
            Thread.sleep(sleepMs)
        } catch (_: InterruptedException) {
        } finally {
            try {
                track.stop()
                track.release()
            } catch (_: Throwable) {
            }
        }
    }

    private fun renderMusicChunk(
        buffer: ShortArray,
        startSample: Long,
        sampleRate: Int,
        mode: MusicTrackMode
    ) {
        val bpm = when (mode) {
            MusicTrackMode.MENU -> 92.0
            MusicTrackMode.COMBAT -> 128.0
            MusicTrackMode.BOSS_COMBAT -> 144.0
            MusicTrackMode.NONE -> 100.0
        }
        val stepDuration = 60.0 / bpm / 2.0 // eighth notes
        val bassScale = when (mode) {
            MusicTrackMode.MENU -> doubleArrayOf(73.42, 73.42, 87.31, 82.41, 73.42, 98.00, 87.31, 65.41)
            MusicTrackMode.COMBAT -> doubleArrayOf(82.41, 82.41, 98.00, 110.00, 82.41, 123.47, 110.00, 98.00)
            MusicTrackMode.BOSS_COMBAT -> doubleArrayOf(65.41, 69.30, 65.41, 77.78, 65.41, 87.31, 82.41, 69.30)
            MusicTrackMode.NONE -> doubleArrayOf(73.42)
        }

        for (i in buffer.indices) {
            val t = (startSample + i).toDouble() / sampleRate.toDouble()
            val stepIndex = (t / stepDuration).toInt()
            val stepTime = t % stepDuration
            val bassFreq = bassScale[stepIndex % bassScale.size]

            // Taiko / industrial arena kick on beats 0, 3, 4, 6
            val modStep = stepIndex % 8
            val isKick = modStep == 0 || modStep == 3 || modStep == 4 || (mode != MusicTrackMode.MENU && modStep == 6)
            val kick = if (isKick) {
                sin(2.0 * PI * (95.0 * exp(-stepTime * 22.0) + 42.0) * stepTime) * exp(-stepTime * 11.0) * 0.55
            } else {
                0.0
            }

            // Dark synth bass pulse
            val bassEnv = exp(-stepTime * 5.5)
            val bass = (sin(2.0 * PI * bassFreq * t) * 0.65 + sin(4.0 * PI * bassFreq * t) * 0.35) * bassEnv * 0.26

            // Atmospheric pad harmonic
            val padFreq = bassScale[0] * 2.0
            val pad = sin(2.0 * PI * padFreq * t) * 0.08

            val mix = (kick + bass + pad).coerceIn(-0.85, 0.85) * 0.42
            buffer[i] = (mix * Short.MAX_VALUE).toInt().toShort()
        }
    }

    companion object {
        private const val SAMPLE_RATE = 22050
    }
}
