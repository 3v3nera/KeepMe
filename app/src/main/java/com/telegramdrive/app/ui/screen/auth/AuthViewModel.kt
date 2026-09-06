package com.telegramdrive.app.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegramdrive.app.data.datastore.SettingsDataStore
import com.telegramdrive.app.domain.model.AuthState
import com.telegramdrive.app.engine.TelegramManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val telegramManager: TelegramManager,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        telegramManager.authState.onEach { state ->
            if (state == null) return@onEach
            _authState.value = mapAuthorizationState(state)
        }.launchIn(viewModelScope)
        
        telegramManager.errorFlow.onEach { errorMsg ->
            if (errorMsg.contains("Unexpected setTdlibParameters", ignoreCase = true)) {
                // TDLib client was out of sync with an old/stale session.
                // Reset storage and recreate client cleanly.
                _authState.value = AuthState.Idle
                telegramManager.resetAndRestart(clearStorage = true)
                return@onEach
            }
            _authState.value = AuthState.Error(errorMsg)
        }.launchIn(viewModelScope)
    }

    private fun mapAuthorizationState(state: TdApi.AuthorizationState): AuthState {
        return when (state.constructor) {
            TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                // If we don't have credentials in store, we ask the user.
                // Otherwise TDLib will consume them automatically.
                if (!settingsDataStore.hasApiCredentials()) {
                    AuthState.WaitApiCredentials
                } else {
                    AuthState.Idle
                }
            }
            TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> AuthState.WaitPhoneNumber
            TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> AuthState.WaitOtpCode
            TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR -> {
                val hint = (state as TdApi.AuthorizationStateWaitPassword).passwordHint ?: ""
                AuthState.WaitPassword(hint)
            }
            TdApi.AuthorizationStateReady.CONSTRUCTOR -> AuthState.Ready
            TdApi.AuthorizationStateWaitRegistration.CONSTRUCTOR -> AuthState.Error("Registration not supported. Please use an existing Telegram account.")
            TdApi.AuthorizationStateClosed.CONSTRUCTOR -> AuthState.Idle
            else -> AuthState.Idle // Mapping others to Idle or Error depending on needs
        }
    }

    fun submitApiCredentials(apiId: String, apiHash: String) {
        val id = apiId.trim().toIntOrNull()
        val hash = apiHash.trim()
        if (id != null && id > 0 && hash.isNotBlank()) {
            settingsDataStore.saveApiCredentials(id, hash)
            val currentState = telegramManager.authState.value
            if (currentState?.constructor == TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR) {
                _authState.value = AuthState.WaitPhoneNumber
            } else {
                _authState.value = AuthState.Idle
                telegramManager.sendTdlibParameters(id, hash)
            }
        } else {
            _authState.value = AuthState.Error("Invalid API ID or Hash. Please check your credentials from my.telegram.org")
        }
    }

    fun resetAuth() {
        settingsDataStore.clearApiCredentials()
        _authState.value = AuthState.Idle
        telegramManager.resetAndRestart(clearStorage = true)
    }

    fun submitPhoneNumber(phoneNumber: String) {
        telegramManager.sendPhoneNumber(phoneNumber)
    }

    fun submitOtpCode(code: String) {
        telegramManager.sendVerificationCode(code)
    }

    fun submitPassword(password: String) {
        telegramManager.sendPassword(password)
    }
}
