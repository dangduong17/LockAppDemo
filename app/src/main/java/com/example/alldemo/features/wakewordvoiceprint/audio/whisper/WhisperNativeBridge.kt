package com.example.alldemo.features.wakewordvoiceprint.audio.whisper

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Low-level Native Bridge to libwhisper.so.
 */
object WhisperNativeBridge {

    @Volatile
    private var isNativeLoadedInternal = false

    @Volatile
    private var isNativeDisabledInternal = true // Native disabled by default for 100% crash resilience

    private var nativeLoadErrorMessageInternal: String? = "Thư viện Native C++ được vô hiệu hóa để đảm bảo an toàn 100% (Pure Kotlin Engine)"

    val isNativeLoaded: Boolean
        get() = isNativeLoadedInternal && !isNativeDisabledInternal

    val nativeLoadErrorMessage: String?
        get() = nativeLoadErrorMessageInternal

    private val nativeLock = ReentrantLock()
    private val asyncMutex = Mutex()

    init {
        disableNativeEngine("Sử dụng 100% Pure Kotlin Audio Engine để phòng chống crash Native JNI")
    }

    @Synchronized
    fun loadNativeLibrary(): Boolean {
        if (isNativeDisabledInternal) return false
        if (isNativeLoadedInternal) return true

        return try {
            disableNativeEngine("Disabled natively to prevent C++ crashes")
            false
        } catch (e: UnsatisfiedLinkError) {
            disableNativeEngine("Không thể load libwhisper.so (UnsatisfiedLinkError): ${e.message}")
            false
        } catch (e: LinkageError) {
            disableNativeEngine("Không thể link libwhisper.so (LinkageError): ${e.message}")
            false
        } catch (e: Throwable) {
            disableNativeEngine("Lỗi khi nạp thư viện native: ${e.message}")
            false
        }
    }

    fun disableNativeEngine(reason: String) {
        isNativeDisabledInternal = true
        isNativeLoadedInternal = false
        nativeLoadErrorMessageInternal = reason
    }

    @JvmStatic
    private fun nativeInitFromFile(modelPath: String): Long = 0L

    @JvmStatic
    private fun nativeInitFromBuffer(buffer: ByteArray): Long = 0L

    @JvmStatic
    private fun nativeFullInference(
        contextPtr: Long,
        pcmData: FloatArray,
        numSamples: Int
    ): String = ""

    @JvmStatic
    private fun nativeCalculateKeywordScore(
        contextPtr: Long,
        pcmData: FloatArray,
        numSamples: Int,
        targetKeyword: String
    ): Float = 0f

    @JvmStatic
    private fun nativeFreeContext(contextPtr: Long) {}

    @JvmStatic
    private fun nativeIs16KbPageAligned(): Boolean = false

    fun initFromFile(modelPath: String): WhisperContext? {
        if (!isNativeLoaded) return null

        val validation = ModelValidator.validateModelFile(modelPath)
        if (!validation.isValid) {
            return null
        }

        return nativeLock.withLock {
            try {
                val ptr = nativeInitFromFile(modelPath)
                if (ptr != 0L) {
                    WhisperContext(ptr = ptr, modelPath = modelPath)
                } else {
                    null
                }
            } catch (e: UnsatisfiedLinkError) {
                disableNativeEngine("UnsatisfiedLinkError in nativeInitFromFile: ${e.message}")
                null
            } catch (e: LinkageError) {
                disableNativeEngine("LinkageError in nativeInitFromFile: ${e.message}")
                null
            } catch (e: Throwable) {
                disableNativeEngine("Exception in nativeInitFromFile: ${e.message}")
                null
            }
        }
    }

    fun initFromBuffer(buffer: ByteArray): WhisperContext? {
        if (!isNativeLoaded) return null

        val validation = ModelValidator.validateModelBuffer(buffer)
        if (!validation.isValid) {
            return null
        }

        return nativeLock.withLock {
            try {
                val ptr = nativeInitFromBuffer(buffer)
                if (ptr != 0L) {
                    WhisperContext(ptr = ptr, modelPath = "<buffer>")
                } else {
                    null
                }
            } catch (e: UnsatisfiedLinkError) {
                disableNativeEngine("UnsatisfiedLinkError in nativeInitFromBuffer: ${e.message}")
                null
            } catch (e: LinkageError) {
                disableNativeEngine("LinkageError in nativeInitFromBuffer: ${e.message}")
                null
            } catch (e: Throwable) {
                disableNativeEngine("Exception in nativeInitFromBuffer: ${e.message}")
                null
            }
        }
    }

    suspend fun fullInferenceAsync(context: WhisperContext, pcmData: FloatArray): String {
        if (!isNativeLoaded || !context.isValid || pcmData.isEmpty()) {
            return ""
        }

        return asyncMutex.withLock {
            try {
                nativeFullInference(context.ptr, pcmData, pcmData.size)
            } catch (e: UnsatisfiedLinkError) {
                disableNativeEngine("UnsatisfiedLinkError in nativeFullInference: ${e.message}")
                ""
            } catch (e: LinkageError) {
                disableNativeEngine("LinkageError in nativeFullInference: ${e.message}")
                ""
            } catch (e: Throwable) {
                disableNativeEngine("Exception in nativeFullInference: ${e.message}")
                ""
            }
        }
    }

    fun processAudio(context: WhisperContext?, pcmData: FloatArray): String {
        if (!isNativeLoaded || context == null || !context.isValid || context.ptr == 0L || pcmData.isEmpty()) {
            return ""
        }

        return nativeLock.withLock {
            try {
                nativeFullInference(context.ptr, pcmData, pcmData.size)
            } catch (e: UnsatisfiedLinkError) {
                disableNativeEngine("UnsatisfiedLinkError in processAudio: ${e.message}")
                ""
            } catch (e: LinkageError) {
                disableNativeEngine("LinkageError in processAudio: ${e.message}")
                ""
            } catch (e: Throwable) {
                disableNativeEngine("Exception in processAudio: ${e.message}")
                ""
            }
        }
    }

    fun calculateKeywordScore(
        context: WhisperContext,
        pcmData: FloatArray,
        targetKeyword: String
    ): Float {
        if (!isNativeLoaded || !context.isValid || context.ptr == 0L || pcmData.isEmpty()) {
            return 0f
        }

        return nativeLock.withLock {
            try {
                val score = nativeCalculateKeywordScore(
                    context.ptr,
                    pcmData,
                    pcmData.size,
                    targetKeyword
                )
                score.coerceIn(0f, 1f)
            } catch (e: UnsatisfiedLinkError) {
                disableNativeEngine("UnsatisfiedLinkError in nativeCalculateKeywordScore: ${e.message}")
                0f
            } catch (e: LinkageError) {
                disableNativeEngine("LinkageError in nativeCalculateKeywordScore: ${e.message}")
                0f
            } catch (e: Throwable) {
                disableNativeEngine("Exception in nativeCalculateKeywordScore: ${e.message}")
                0f
            }
        }
    }

    fun freeContext(contextPtr: Long) {
        if (contextPtr == 0L) return

        nativeLock.withLock {
            try {
                nativeFreeContext(contextPtr)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun is16KbPageAligned(): Boolean {
        if (!isNativeLoaded) return false
        return try {
            nativeIs16KbPageAligned()
        } catch (_: Throwable) {
            false
        }
    }
}
