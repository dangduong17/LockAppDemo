package com.example.alldemo.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class DemoItem(
    val id: String,
    val title: String,
    val description: String,
    val category: DemoCategory,
    val icon: ImageVector,
    val color: Color,
    val destination: String,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isActivityDestination: Boolean = false
)
