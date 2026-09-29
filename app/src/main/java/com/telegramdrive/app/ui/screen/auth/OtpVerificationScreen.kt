package com.telegramdrive.app.ui.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.telegramdrive.app.ui.components.GlassPanelTokens
import com.telegramdrive.app.ui.components.GlassPrimaryButton
import com.telegramdrive.app.ui.components.GlassTextField
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.utils.rememberAppHaptics

@Composable
fun OtpVerificationScreen(
    onSubmit: (String) -> Unit
) {
    val haptics = rememberAppHaptics()
    var code by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    // If the server bounces us back (wrong code/password) the same screen stays up,
    // so release the loading state after a few seconds and let the user retry
    LaunchedEffect(submitted) {
        if (submitted) {
            kotlinx.coroutines.delay(8000)
            submitted = false
        }
    }

    val submit = {
        if (code.isNotBlank() && !submitted) {
            haptics.click()
            submitted = true
            onSubmit(code)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Check your Telegram",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvas
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We've sent the code to the Telegram app on your other device.",
            style = MaterialTheme.typography.bodyMedium,
            color = TDriveColors.OnCanvasMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        GlassTextField(
            value = code,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() }
                if (filtered.length > code.length) {
                    haptics.tick()
                }
                code = filtered
                submitted = false
            },
            placeholder = "Code",
            textAlign = TextAlign.Center,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() })
        )
        Spacer(modifier = Modifier.height(32.dp))

        GlassPrimaryButton(
            text = "Verify",
            onClick = { submit() },
            loading = submitted,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
