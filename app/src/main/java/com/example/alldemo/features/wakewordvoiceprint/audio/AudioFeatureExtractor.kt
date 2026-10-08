package com.example.alldemo.features.wakewordvoiceprint.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Extracts Mel-Frequency Cepstral Coefficients (MFCC) and fixed-length
 * Feature Embedding Vectors from 16kHz 16-bit PCM audio signals.
 */
class AudioFeatureExtractor(
    private val sampleRate: Int = 16000,
    private val frameSize: Int = 400,      // 25ms at 16kHz
    private val frameStride: Int = 160,    // 10ms at 16kHz
    private val numMelFilters: Int = 26,
    private val numMfcc: Int = 13,
    private val fftSize: Int = 512
) {

    private val hammingWindow = FloatArray(frameSize) { i ->
        (0.54 - 0.46 * cos(2.0 * PI * i / (frameSize - 1))).toFloat()
    }

    private val melFilterBank: Array<FloatArray> = createMelFilterBank()

    /**
     * Extracts frame-by-frame MFCC matrix from raw short PCM data.
     * Each frame produces [numMfcc] coefficients.
     */
    fun extractMfcc(pcm: ShortArray): Array<FloatArray> {
        if (pcm.size < frameSize) return arrayOf()

        // 1. Convert to Float and apply pre-emphasis filter (y[t] = x[t] - 0.97 * x[t-1])
        val floatPcm = FloatArray(pcm.size)
        floatPcm[0] = pcm[0].toFloat()
        for (i in 1 until pcm.size) {
            floatPcm[i] = pcm[i] - 0.97f * pcm[i - 1]
        }

        // 2. Framing
        val numFrames = (floatPcm.size - frameSize) / frameStride + 1
        if (numFrames <= 0) return arrayOf()

        val mfccMatrix = Array(numFrames) { FloatArray(numMfcc) }

        for (f in 0 until numFrames) {
            val startIdx = f * frameStride

            // 3. Apply Hamming Window & zero-pad to FFT size
            val real = DoubleArray(fftSize)
            val imag = DoubleArray(fftSize)
            for (i in 0 until frameSize) {
                real[i] = (floatPcm[startIdx + i] * hammingWindow[i]).toDouble()
            }

            // 4. Compute FFT
            fft(real, imag)

            // 5. Compute Power Spectrum
            val halfFft = fftSize / 2 + 1
            val powerSpectrum = FloatArray(halfFft)
            for (i in 0 until halfFft) {
                powerSpectrum[i] = ((real[i] * real[i] + imag[i] * imag[i]) / fftSize).toFloat()
            }

            // 6. Apply Mel Filterbank
            val melEnergies = FloatArray(numMelFilters)
            for (m in 0 until numMelFilters) {
                var energy = 0f
                val filter = melFilterBank[m]
                for (k in 0 until halfFft) {
                    energy += powerSpectrum[k] * filter[k]
                }
                // Log energy (with epsilon guard)
                melEnergies[m] = log10(maxOf(energy, 1e-10f))
            }

            // 7. Apply Discrete Cosine Transform (DCT-II)
            for (i in 0 until numMfcc) {
                var sum = 0.0
                for (m in 0 until numMelFilters) {
                    sum += melEnergies[m] * cos(PI * i * (m + 0.5) / numMelFilters)
                }
                mfccMatrix[f][i] = sum.toFloat()
            }
        }

        return mfccMatrix
    }

    /**
     * Extracts a fixed-length Feature Embedding Vector (26 dimensions: 13 means + 13 std devs)
     * from PCM signal to represent voiceprint or keyword signature.
     */
    fun extractFeatureVector(pcm: ShortArray): FloatArray {
        val mfcc = extractMfcc(pcm)
        if (mfcc.isEmpty()) return FloatArray(numMfcc * 2)

        val numFrames = mfcc.size
        val means = FloatArray(numMfcc)
        val stds = FloatArray(numMfcc)

        // Calculate raw means
        for (i in 0 until numMfcc) {
            var sum = 0f
            for (f in 0 until numFrames) {
                sum += mfcc[f][i]
            }
            means[i] = sum / numFrames
        }

        // Calculate raw standard deviations
        for (i in 0 until numMfcc) {
            var sumVariance = 0f
            for (f in 0 until numFrames) {
                val diff = mfcc[f][i] - means[i]
                sumVariance += diff * diff
            }
            stds[i] = sqrt(sumVariance / numFrames)
        }

        // 1. Zero-out c0 (index 0) mean and standard deviation to eliminate total log-energy DC bias & volume scaling
        means[0] = 0f
        stds[0] = 0f

        // 2. Apply Cepstral Mean Normalization across c1..c12 coefficients to subtract static channel/room acoustics
        var sumCoeffs = 0f
        for (i in 1 until numMfcc) {
            sumCoeffs += means[i]
        }
        val avgCoeff = sumCoeffs / (numMfcc - 1)
        for (i in 1 until numMfcc) {
            means[i] -= avgCoeff
        }

        // Combine into fixed 26-dim vector
        val rawVector = FloatArray(numMfcc * 2)
        System.arraycopy(means, 0, rawVector, 0, numMfcc)
        System.arraycopy(stds, 0, rawVector, numMfcc, numMfcc)

        return normalizeL2(rawVector)
    }

    /**
     * Applies L2 Normalization (v_norm = v / ||v||_2) to bring vector length to 1.0.
     */
    fun normalizeL2(vector: FloatArray): FloatArray {
        if (vector.isEmpty()) return vector
        var sumSquares = 0.0
        for (v in vector) {
            sumSquares += (v * v).toDouble()
        }
        val norm = sqrt(sumSquares).toFloat()
        if (norm <= 1e-10f) return vector
        val normalized = FloatArray(vector.size)
        for (i in vector.indices) {
            normalized[i] = vector[i] / norm
        }
        return normalized
    }

    /**
     * Calculates the proportion of signal energy concentrated within the human speech
     * frequency band (300Hz to 3400Hz).
     */
    fun calculateSpeechFrequencyRatio(pcm: ShortArray): Float {
        if (pcm.size < frameSize) return 0f

        val floatPcm = FloatArray(pcm.size)
        floatPcm[0] = pcm[0].toFloat()
        for (i in 1 until pcm.size) {
            floatPcm[i] = pcm[i] - 0.97f * pcm[i - 1]
        }

        val numFrames = (floatPcm.size - frameSize) / frameStride + 1
        if (numFrames <= 0) return 0f

        val halfFft = fftSize / 2 + 1
        val freqPerBin = sampleRate.toDouble() / fftSize
        val minBin = (300.0 / freqPerBin).toInt().coerceIn(0, halfFft - 1)
        val maxBin = (3400.0 / freqPerBin).toInt().coerceIn(minBin, halfFft - 1)

        var totalPowerSum = 0.0
        var speechPowerSum = 0.0

        for (f in 0 until numFrames) {
            val startIdx = f * frameStride
            val real = DoubleArray(fftSize)
            val imag = DoubleArray(fftSize)
            for (i in 0 until frameSize) {
                real[i] = (floatPcm[startIdx + i] * hammingWindow[i]).toDouble()
            }

            fft(real, imag)

            for (k in 0 until halfFft) {
                val power = (real[k] * real[k] + imag[k] * imag[k]) / fftSize
                totalPowerSum += power
                if (k in minBin..maxBin) {
                    speechPowerSum += power
                }
            }
        }

        if (totalPowerSum <= 1e-10) return 0f
        return (speechPowerSum / totalPowerSum).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Creates triangular Mel filterbank matrix of size [numMelFilters] x [fftSize/2 + 1].
     */
    private fun createMelFilterBank(): Array<FloatArray> {
        val halfFft = fftSize / 2 + 1
        val filterBank = Array(numMelFilters) { FloatArray(halfFft) }

        val lowMel = hzToMel(300.0)
        val highMel = hzToMel(sampleRate / 2.0)
        val melStep = (highMel - lowMel) / (numMelFilters + 1)

        val pointsHz = DoubleArray(numMelFilters + 2)
        for (i in pointsHz.indices) {
            pointsHz[i] = melToHz(lowMel + i * melStep)
        }

        val binIndices = IntArray(numMelFilters + 2) { i ->
            val bin = ((fftSize + 1) * pointsHz[i] / sampleRate).toInt()
            bin.coerceIn(0, halfFft - 1)
        }

        for (m in 1..numMelFilters) {
            val fMinus = binIndices[m - 1]
            val fM = binIndices[m]
            val fPlus = binIndices[m + 1]

            for (k in fMinus until fM) {
                filterBank[m - 1][k] = if (fM != fMinus) (k - fMinus).toFloat() / (fM - fMinus) else 0f
            }
            for (k in fM until fPlus) {
                filterBank[m - 1][k] = if (fPlus != fM) (fPlus - k).toFloat() / (fPlus - fM) else 0f
            }
        }

        return filterBank
    }

    private fun hzToMel(hz: Double): Double = 2595.0 * log10(1.0 + hz / 700.0)
    private fun melToHz(mel: Double): Double = 700.0 * (10.0.pow(mel / 2595.0) - 1.0)

    /**
     * In-place Radix-2 Cooley-Tukey Fast Fourier Transform (FFT).
     */
    private fun fft(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = real[i]; real[i] = real[j]; real[j] = tempR
                val tempI = imag[i]; imag[i] = imag[j]; imag[j] = tempI
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        var len = 2
        while (len <= n) {
            val halfLen = len shr 1
            val angle = -2.0 * PI / len
            val wStepR = cos(angle)
            val wStepI = sin(angle)

            var i = 0
            while (i < n) {
                var wR = 1.0
                var wI = 0.0
                for (k in 0 until halfLen) {
                    val uR = real[i + k]
                    val uI = imag[i + k]
                    val vR = real[i + k + halfLen] * wR - imag[i + k + halfLen] * wI
                    val vI = real[i + k + halfLen] * wI + imag[i + k + halfLen] * wR

                    real[i + k] = uR + vR
                    imag[i + k] = uI + vI
                    real[i + k + halfLen] = uR - vR
                    imag[i + k + halfLen] = uI - vI

                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += len
            }
            len = len shl 1
        }
    }
}
