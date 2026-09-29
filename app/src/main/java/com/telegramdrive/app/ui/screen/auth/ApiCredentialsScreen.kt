package com.telegramdrive.app.ui.screen.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.telegramdrive.app.R
import com.telegramdrive.app.ui.components.GlassPanelTokens
import com.telegramdrive.app.ui.components.GlassPrimaryButton
import com.telegramdrive.app.ui.components.GlassTextField
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.utils.rememberAppHaptics

@Composable
fun ApiCredentialsScreen(
    onSubmit: (String, String) -> Unit
) {
    val haptics = rememberAppHaptics()
    var apiId by remember { mutableStateOf("") }
    var apiHash by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(isSubmitting) {
        // Let the user retry if the credentials were rejected and we stayed on this screen
        if (isSubmitting) {
            kotlinx.coroutines.delay(8000)
            isSubmitting = false
        }
    }

    val submit = {
        val cleanId = apiId.trim()
        val cleanHash = apiHash.trim()
        if (isSubmitting) {
            // ignore double taps
        } else if (cleanId.isBlank() || cleanHash.isBlank()) {
            haptics.error()
            errorMessage = "Please enter both API ID and API Hash"
        } else if (cleanId.toIntOrNull() == null || cleanId.toIntOrNull()!! <= 0) {
            haptics.error()
            errorMessage = "API ID must be a valid positive number"
        } else {
            haptics.click()
            errorMessage = null
            isSubmitting = true
            onSubmit(cleanId, cleanHash)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent,
            shadowElevation = 8.dp,
            modifier = Modifier.size(96.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_keepme_logo),
                contentDescription = "KeepMe Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Welcome to KeepMe",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvas
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your unlimited personal cloud vault, powered by Telegram.",
            style = MaterialTheme.typography.bodyMedium,
            color = TDriveColors.OnCanvasMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        GlassTextField(
            value = apiId,
            onValueChange = {
                apiId = it.filter { char -> char.isDigit() }
                errorMessage = null
                isSubmitting = false
            },
            placeholder = "API ID",
            isError = errorMessage != null && apiId.isBlank(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
        )
        Spacer(modifier = Modifier.height(16.dp))
        GlassTextField(
            value = apiHash,
            onValueChange = {
                apiHash = it.trim()
                errorMessage = null
                isSubmitting = false
            },
            placeholder = "API Hash",
            isError = errorMessage != null && apiHash.isBlank(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() })
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage ?: "",
                color = TDriveColors.Error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        GlassPrimaryButton(
            text = "Continue",
            onClick = { submit() },
            loading = isSubmitting,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
