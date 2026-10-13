package com.example.alldemo.features.speechbraintest.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object WavUtils {

    fun pcmToWav(
        pcmBytes: ByteArray,
        sampleRate: Int = 16000,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ): ByteArray {
        val totalAudioLen = pcmBytes.size.toLong()
        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()
        val blockAlign = (channels * bitsPerSample / 8)

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(totalDataLen.toInt())
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))

        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)
        buffer.putShort(1.toShort())
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate.toInt())
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(bitsPerSample.toShort())

        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(totalAudioLen.toInt())

        val wavBytes = ByteArray(44 + pcmBytes.size)
        System.arraycopy(header, 0, wavBytes, 0, 44)
        System.arraycopy(pcmBytes, 0, wavBytes, 44, pcmBytes.size)

        return wavBytes
    }

    fun calculateRmsAmplitude(buffer: ShortArray, readSize: Int): Float {
        if (readSize <= 0) return 0f
        var sum = 0.0
        for (i in 0 until readSize) {
            val sample = buffer[i].toDouble()
            sum += sample * sample
        }
        val rms = sqrt(sum / readSize)
        val normalized = (rms / 32767.0).toFloat()
        return min(1.0f, max(0.0f, normalized))
    }

    fun calculatePeakAmplitude(buffer: ShortArray, readSize: Int): Float {
        if (readSize <= 0) return 0f
        var maxPeak = 0
        for (i in 0 until readSize) {
            val absVal = kotlin.math.abs(buffer[i].toInt())
            if (absVal > maxPeak) {
                maxPeak = absVal
            }
        }
        val normalized = maxPeak.toFloat() / 32767.0f
        return min(1.0f, max(0.0f, normalized))
    }

    fun shortArrayToByteArray(shorts: ShortArray, readSize: Int): ByteArray {
        val bytes = ByteArray(readSize * 2)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until readSize) {
            buffer.putShort(shorts[i])
        }
        return bytes
    }
}
