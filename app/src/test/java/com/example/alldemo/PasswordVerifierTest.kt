package com.example.alldemo

import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.util.PasswordVerifier
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordVerifierTest {

    @Test
    fun verifyPassword_returnsTrue_whenExactMatch() {
        assertTrue(PasswordVerifier.verifyPassword("mở khóa", "mở khóa"))
    }

    @Test
    fun verifyPassword_returnsTrue_whenTranscriptContainsPassword() {
        assertTrue(PasswordVerifier.verifyPassword("Tôi muốn mở khóa ngay lập tức", "mở khóa"))
    }

    @Test
    fun verifyPassword_returnsFalse_whenBlank() {
        assertFalse(PasswordVerifier.verifyPassword("", "mở khóa"))
        assertFalse(PasswordVerifier.verifyPassword("mở khóa", ""))
    }

    @Test
    fun verifyPassword_returnsFalse_whenNoMatch() {
        assertFalse(PasswordVerifier.verifyPassword("xin chào bạn", "mở khóa"))
    }
}
