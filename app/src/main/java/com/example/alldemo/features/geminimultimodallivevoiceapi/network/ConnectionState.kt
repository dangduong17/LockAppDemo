package com.example.alldemo.features.geminimultimodallivevoiceapi.network

sealed interface ConnectionState {
    object Disconnected : ConnectionState
    object Connecting : ConnectionState
    object Connected : ConnectionState
    data class Error(val message: String, val cause: Throwable? = null) : ConnectionState
}
