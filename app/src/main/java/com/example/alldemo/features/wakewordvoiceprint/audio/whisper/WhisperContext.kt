package com.example.alldemo.features.wakewordvoiceprint.audio.whisper

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Kotlin AutoCloseable wrapper holding a handle to native C++ whisper_context pointer.
 */
class WhisperContext(
    val ptr: Long,
    val modelPath: String = ""
) : AutoCloseable {

    private val isClosed = AtomicBoolean(false)

    val isValid: Boolean
        get() = ptr != 0L && !isClosed.get()

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            if (ptr != 0L) {
                try {
                    WhisperNativeBridge.freeContext(ptr)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    protected fun finalize() {
        if (!isClosed.get()) {
            close()
        }
    }
}
