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
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun PasswordScreen(
    hint: String,
    onSubmit: (String) -> Unit
) {
    val haptics = rememberAppHaptics()
    var password by remember { mutableStateOf("") }
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
        if (password.isNotBlank() && !submitted) {
            haptics.click()
            submitted = true
            onSubmit(password)
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
            text = "Enter Password",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvas
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your account is protected with an additional password.",
            style = MaterialTheme.typography.bodyMedium,
            color = TDriveColors.OnCanvasMuted,
            textAlign = TextAlign.Center
        )
        if (hint.isNotBlank()) {
            Text(
                text = "Hint: $hint",
                style = MaterialTheme.typography.bodySmall,
                color = TDriveColors.OnCanvas,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        GlassTextField(
            value = password,
            onValueChange = { password = it; submitted = false },
            placeholder = "Password",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() })
        )
        Spacer(modifier = Modifier.height(32.dp))

        GlassPrimaryButton(
            text = "Sign In",
            onClick = { submit() },
            loading = submitted,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
