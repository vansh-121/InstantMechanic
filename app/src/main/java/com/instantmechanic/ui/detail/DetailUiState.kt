package com.instantmechanic.ui.detail

import com.instantmechanic.core.result.DataError
import com.instantmechanic.domain.model.MechanicDetail

/** The Detail screen is either loading, showing a garage, or explaining why it can't. */
sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(val mechanic: MechanicDetail) : DetailUiState
    data class Error(val error: DataError) : DetailUiState
}
