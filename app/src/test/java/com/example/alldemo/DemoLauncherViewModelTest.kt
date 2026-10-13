package com.example.alldemo

import com.example.alldemo.data.FeatureRegistryRepositoryImpl
import com.example.alldemo.model.DemoCategory
import com.example.alldemo.ui.DemoLauncherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DemoLauncherViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: DemoLauncherViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = DemoLauncherViewModel(FeatureRegistryRepositoryImpl())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_containsAllDemosAndCategories() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertEquals(DemoCategory.ALL, state.selectedCategory)
        assertEquals("", state.searchQuery)
        assertEquals(14, state.demoItems.size)
        assertEquals(DemoCategory.entries.size, state.categories.size)
    }

    @Test
    fun filterBySearchQuery_filtersByTitleAndTag() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onSearchQueryChanged("SherpaOnnx")

        val state = viewModel.uiState.value
        assertEquals("SherpaOnnx", state.searchQuery)
        assertEquals(1, state.demoItems.size)
        assertEquals("voicespeech_demo", state.demoItems.first().id)
    }

    @Test
    fun filterByCategory_returnsOnlySelectedCategoryDemos() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onCategorySelected(DemoCategory.SETTINGS_CONFIG)

        val state = viewModel.uiState.value
        assertEquals(DemoCategory.SETTINGS_CONFIG, state.selectedCategory)
        assertEquals(1, state.demoItems.size)
        assertEquals("locktest_demo", state.demoItems.first().id)
    }

    @Test
    fun toggleFavorite_updatesFavoriteStatus() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.toggleFavorite("lockscreen_demo")

        val state = viewModel.uiState.value
        assertTrue(state.favoriteItemIds.contains("lockscreen_demo"))
        val item = state.demoItems.first { it.id == "lockscreen_demo" }
        assertTrue(item.isFavorite)

        viewModel.toggleFavorite("lockscreen_demo")

        val stateAfterToggle = viewModel.uiState.value
        assertFalse(stateAfterToggle.favoriteItemIds.contains("lockscreen_demo"))
    }

    @Test
    fun clearSearchQuery_resetsQueryToEmpty() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onSearchQueryChanged("Lock")
        assertEquals(2, viewModel.uiState.value.demoItems.size)

        viewModel.clearSearchQuery()
        assertEquals("", viewModel.uiState.value.searchQuery)
        assertEquals(14, viewModel.uiState.value.demoItems.size)
    }
}
