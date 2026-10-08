package com.example.alldemo.ui

import com.example.alldemo.model.DemoCategory
import com.example.alldemo.model.DemoItem

data class DemoLauncherUiState(
    val searchQuery: String = "",
    val selectedCategory: DemoCategory = DemoCategory.ALL,
    val categories: List<DemoCategory> = DemoCategory.entries,
    val demoItems: List<DemoItem> = emptyList(),
    val favoriteItemIds: Set<String> = emptySet(),
    val isLoading: Boolean = false
)
