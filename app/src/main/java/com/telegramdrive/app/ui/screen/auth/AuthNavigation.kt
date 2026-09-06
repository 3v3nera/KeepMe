package com.telegramdrive.app.ui.screen.auth

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.telegramdrive.app.domain.model.AuthState
import com.telegramdrive.app.ui.theme.TDriveMotion
import com.telegramdrive.app.ui.utils.rememberAppHaptics

@Composable
fun AuthHost(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthSuccess: () -> Unit
) {
    val haptics = rememberAppHaptics()
    val authState by viewModel.authState.collectAsState()

    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxSize(),
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        AnimatedContent(
            targetState = authState,
            transitionSpec = {
                TDriveMotion.screenEnter togetherWith TDriveMotion.screenExit
            },
            label = "AuthTransitions"
        ) { state ->
            when (state) {
                is AuthState.Idle -> {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = com.telegramdrive.app.ui.theme.TDriveColors.Primary
                        )
                    }
                }
                is AuthState.WaitApiCredentials -> {
                    ApiCredentialsScreen(
                        onSubmit = { id, hash -> viewModel.submitApiCredentials(id, hash) }
                    )
                }
                is AuthState.WaitPhoneNumber -> {
                    PhoneInputScreen(
                        onSubmit = { phone -> viewModel.submitPhoneNumber(phone) }
                    )
                }
                is AuthState.WaitOtpCode -> {
                    OtpVerificationScreen(
                        onSubmit = { code -> viewModel.submitOtpCode(code) }
                    )
                }
                is AuthState.WaitPassword -> {
                    PasswordScreen(
                        hint = state.hint,
                        onSubmit = { pwd -> viewModel.submitPassword(pwd) }
                    )
                }
                is AuthState.Ready -> {
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        haptics.success()
                        onAuthSuccess()
                    }
                }
                is AuthState.Error -> {
                    androidx.compose.runtime.LaunchedEffect(state.message) {
                        haptics.error()
                    }
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        androidx.compose.material3.Text(
                            text = "Authentication Error",
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            ),
                            color = com.telegramdrive.app.ui.theme.TDriveColors.Error
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                        androidx.compose.material3.Text(
                            text = state.message,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = com.telegramdrive.app.ui.theme.TDriveColors.OnSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(28.dp))
                        androidx.compose.material3.Button(
                            onClick = {
                                haptics.click()
                                viewModel.resetAuth()
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = com.telegramdrive.app.ui.theme.TDriveColors.Primary
                            )
                        ) {
                            androidx.compose.material3.Text("Try Again", color = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                }
            }
        }
    }
}
