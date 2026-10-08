package com.example.alldemo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alldemo.data.FeatureRegistryRepository
import com.example.alldemo.data.FeatureRegistryRepositoryImpl
import com.example.alldemo.model.DemoCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class DemoLauncherViewModel(
    private val repository: FeatureRegistryRepository = FeatureRegistryRepositoryImpl()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(DemoCategory.ALL)
    val selectedCategory: StateFlow<DemoCategory> = _selectedCategory.asStateFlow()

    private val _favoriteItemIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteItemIds: StateFlow<Set<String>> = _favoriteItemIds.asStateFlow()

    val uiState: StateFlow<DemoLauncherUiState> = combine(
        _searchQuery,
        _selectedCategory,
        _favoriteItemIds,
        repository.getDemoItems()
    ) { query, category, favorites, items ->
        val filtered = items.filter { item ->
            val matchesCategory = (category == DemoCategory.ALL) || (item.category == category)
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true) ||
                    item.tags.any { tag -> tag.contains(query, ignoreCase = true) }

            matchesCategory && matchesQuery
        }.map { item ->
            item.copy(isFavorite = favorites.contains(item.id))
        }

        DemoLauncherUiState(
            searchQuery = query,
            selectedCategory = category,
            categories = repository.getCategories(),
            demoItems = filtered,
            favoriteItemIds = favorites,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DemoLauncherUiState(isLoading = true)
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onCategorySelected(category: DemoCategory) {
        _selectedCategory.value = category
    }

    fun toggleFavorite(itemId: String) {
        _favoriteItemIds.update { current ->
            if (current.contains(itemId)) {
                current - itemId
            } else {
                current + itemId
            }
        }
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }
}
