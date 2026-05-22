package com.example.anhnn_layr.presentation.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anhnn_layr.domain.usecases.RemoveBackgroundUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RembgUiState {
    data object Idle : RembgUiState
    data object Loading : RembgUiState
    data class Success(val pngBytes: ByteArray) : RembgUiState {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Success && pngBytes.contentEquals(other.pngBytes))
        override fun hashCode(): Int = pngBytes.contentHashCode()
    }
    data class Error(val message: String) : RembgUiState
}

@HiltViewModel
class RembgViewModel @Inject constructor(
    private val removeBackground: RemoveBackgroundUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<RembgUiState>(RembgUiState.Idle)
    val state: StateFlow<RembgUiState> = _state.asStateFlow()

    fun remove(uri: Uri, model: String = "u2net") {
        _state.value = RembgUiState.Loading
        viewModelScope.launch {
            runCatching { removeBackground(uri, model = model) }
                .onSuccess { _state.value = RembgUiState.Success(it) }
                .onFailure { _state.value = RembgUiState.Error(it.message ?: "Lỗi không xác định") }
        }
    }
}
