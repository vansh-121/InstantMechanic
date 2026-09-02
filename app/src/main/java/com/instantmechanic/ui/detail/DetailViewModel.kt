package com.instantmechanic.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instantmechanic.core.result.AppResult
import com.instantmechanic.domain.repository.MechanicRepository
import com.instantmechanic.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Loads one garage's full profile.
 *
 * The id arrives via [SavedStateHandle] rather than being passed into a factory, which means it
 * survives process death for free: Android can recreate this ViewModel after the app is evicted
 * and it still knows which garage to fetch.
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: MechanicRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val mechanicId: String = checkNotNull(savedStateHandle[Routes.ARG_MECHANIC_ID]) {
        "DetailViewModel requires a '${Routes.ARG_MECHANIC_ID}' navigation argument"
    }

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _uiState.value = DetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = repository.getMechanic(mechanicId)) {
                is AppResult.Success -> DetailUiState.Success(result.data)
                is AppResult.Failure -> DetailUiState.Error(result.error)
            }
        }
    }
}
