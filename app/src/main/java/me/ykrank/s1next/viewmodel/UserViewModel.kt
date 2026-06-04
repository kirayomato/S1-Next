package me.ykrank.s1next.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.pref.AppDataPreferencesManager

class UserViewModel(appDataPref: AppDataPreferencesManager) {

    private val _headerUiState = MutableStateFlow(UserHeaderUiState())
    val headerUiState: StateFlow<UserHeaderUiState> = _headerUiState.asStateFlow()

    val user: User = ObservableUser(appDataPref, ::refreshHeaderUiState)

    val isSigned: Boolean
        get() = user.isSigned

    val isAuthor: Boolean
        get() = user.uid == "223963"

    init {
        refreshHeaderUiState()
    }

    private fun refreshHeaderUiState() {
        _headerUiState.value = UserHeaderUiState(
            isLogged = user.isLogged,
            name = user.name,
            isSigned = user.isSigned,
        )
    }

    data class UserHeaderUiState(
        val isLogged: Boolean = false,
        val name: String? = null,
        val isSigned: Boolean = false,
    )

    private class ObservableUser(
        appDataPref: AppDataPreferencesManager,
        private val onChanged: () -> Unit,
    ) : User(appDataPref) {

        override var isLogged: Boolean
            get() = super.isLogged
            set(logged) {
                super.isLogged = logged
                onChanged()
            }

        override var isSigned: Boolean
            get() = super.isSigned
            set(b) {
                super.isSigned = b
                onChanged()
            }
    }
}
