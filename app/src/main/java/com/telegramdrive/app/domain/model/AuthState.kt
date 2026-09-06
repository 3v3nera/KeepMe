package com.telegramdrive.app.domain.model

sealed class AuthState {
    data object Idle : AuthState()
    data object WaitApiCredentials : AuthState()
    data object WaitPhoneNumber : AuthState()
    data object WaitOtpCode : AuthState()
    data class WaitPassword(val hint: String) : AuthState()
    data object Ready : AuthState()
    data class Error(val message: String) : AuthState()
}
