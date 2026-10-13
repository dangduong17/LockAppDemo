package com.example.alldemo.features.speechbraintest.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {

    @Serializable
    data object Verification : Screen

    @Serializable
    data object Config : Screen

    @Serializable
    data object History : Screen
}
