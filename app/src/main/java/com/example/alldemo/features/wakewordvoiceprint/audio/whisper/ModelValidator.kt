package com.example.alldemo.features.wakewordvoiceprint.audio.whisper

import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Result of model file header and path validation.
 */
data class ModelValidationResult(
    val isValid: Boolean,
    val reason: String,
    val fileSizeBytes: Long = 0L,
    val magicHeader: String = ""
)

/**
 * Validates GGML/GGUF model files before passing file paths or byte streams
 * to Native JNI functions to prevent SIGSEGV / native crashes.
 */
object ModelValidator {

    private const val MIN_MODEL_SIZE_BYTES = 100 * 1024L // 100 KB minimum header limit

    /**
     * Validates file path, existence, file size, and magic header bytes.
     */
    fun validateModelFile(path: String?): ModelValidationResult {
        if (path.isNullOrBlank()) {
            return ModelValidationResult(
                isValid = false,
                reason = "Đường dẫn file model bị NULL hoặc rỗng"
            )
        }

        val file = File(path)
        if (!file.exists()) {
            return ModelValidationResult(
                isValid = false,
                reason = "File model không tồn tại tại đường dẫn: $path"
            )
        }

        if (!file.isFile || !file.canRead()) {
            return ModelValidationResult(
                isValid = false,
                reason = "Không thể đọc file model tại đường dẫn: $path"
            )
        }

        val fileSize = file.length()
        if (fileSize < MIN_MODEL_SIZE_BYTES) {
            return ModelValidationResult(
                isValid = false,
                reason = "Kích thước file model quá nhỏ ($fileSize bytes). Cần ít nhất $MIN_MODEL_SIZE_BYTES bytes.",
                fileSizeBytes = fileSize
            )
        }

        return try {
            FileInputStream(file).use { stream ->
                validateInputStreamHeader(stream, fileSize)
            }
        } catch (e: Exception) {
            ModelValidationResult(
                isValid = false,
                reason = "Lỗi khi đọc file model: ${e.localizedMessage}",
                fileSizeBytes = fileSize
            )
        }
    }

    /**
     * Validates model byte array buffer.
     */
    fun validateModelBuffer(buffer: ByteArray?): ModelValidationResult {
        if (buffer == null || buffer.isEmpty()) {
            return ModelValidationResult(
                isValid = false,
                reason = "Buffer model bị NULL hoặc rỗng"
            )
        }

        if (buffer.size < MIN_MODEL_SIZE_BYTES) {
            return ModelValidationResult(
                isValid = false,
                reason = "Buffer model quá nhỏ (${buffer.size} bytes). Cần tối thiểu $MIN_MODEL_SIZE_BYTES bytes.",
                fileSizeBytes = buffer.size.toLong()
            )
        }

        val magicStr = String(buffer, 0, 4, Charsets.US_ASCII)

        val isMagicValid = isSupportedMagicHeader(magicStr)

        return if (isMagicValid) {
            ModelValidationResult(
                isValid = true,
                reason = "Header buffer hợp lệ ($magicStr)",
                fileSizeBytes = buffer.size.toLong(),
                magicHeader = magicStr
            )
        } else {
            ModelValidationResult(
                isValid = false,
                reason = "Định dạng magic header không hỗ trợ: '$magicStr'",
                fileSizeBytes = buffer.size.toLong(),
                magicHeader = magicStr
            )
        }
    }

    private fun validateInputStreamHeader(stream: InputStream, fileSize: Long): ModelValidationResult {
        val headerBytes = ByteArray(4)
        val readCount = stream.read(headerBytes, 0, 4)

        if (readCount < 4) {
            return ModelValidationResult(
                isValid = false,
                reason = "Không thể đọc 4 bytes magic header của file model",
                fileSizeBytes = fileSize
            )
        }

        val magicStr = String(headerBytes, Charsets.US_ASCII)

        val isMagicValid = isSupportedMagicHeader(magicStr)

        return if (isMagicValid) {
            ModelValidationResult(
                isValid = true,
                reason = "Header file hợp lệ ($magicStr)",
                fileSizeBytes = fileSize,
                magicHeader = magicStr
            )
        } else {
            ModelValidationResult(
                isValid = false,
                reason = "Định dạng magic header không hợp lệ: '$magicStr'",
                fileSizeBytes = fileSize,
                magicHeader = magicStr
            )
        }
    }

    private fun isSupportedMagicHeader(magicStr: String): Boolean {
        val lower = magicStr.lowercase()
        return lower.startsWith("ggml") ||
                lower.startsWith("ggjt") ||
                lower.startsWith("gguf") ||
                lower.startsWith("ggmf") ||
                lower.startsWith("whis")
    }
}
