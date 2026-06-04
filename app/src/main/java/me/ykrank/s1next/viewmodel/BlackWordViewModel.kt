package me.ykrank.s1next.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.ykrank.s1next.data.db.dbmodel.BlackWord

class BlackWordViewModel {
    private val _uiState = MutableStateFlow(BlackWordUiState())
    val uiState: StateFlow<BlackWordUiState> = _uiState.asStateFlow()

    fun setBlackWord(blackWord: BlackWord?) {
        _uiState.update { it.copy(blackWord = blackWord) }
    }

    fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(loading = loading) }
    }

    fun setMessage(message: String?) {
        _uiState.update { it.copy(message = message) }
    }

    data class BlackWordUiState(
        val blackWord: BlackWord? = null,
        val loading: Boolean = false,
        val message: String? = null,
    ) {
        val floatVisible: Boolean
            get() = loading || !message.isNullOrEmpty()
    }
}
