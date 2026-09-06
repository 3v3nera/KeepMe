package com.telegramdrive.app.ui.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.telegramdrive.app.R
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
            color = TDriveColors.OnSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your unlimited personal cloud vault, powered by Telegram.",
            style = MaterialTheme.typography.bodyMedium,
            color = TDriveColors.OnSurfaceDim,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = apiId,
            onValueChange = { 
                apiId = it.filter { char -> char.isDigit() }
                errorMessage = null
                isSubmitting = false
            },
            label = { Text("API ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TDriveColors.Primary,
                unfocusedBorderColor = TDriveColors.Outline,
                focusedContainerColor = TDriveColors.SurfaceElevated,
                unfocusedContainerColor = TDriveColors.SurfaceElevated,
                focusedTextColor = TDriveColors.OnSurface,
                unfocusedTextColor = TDriveColors.OnSurface,
                focusedLabelColor = TDriveColors.Primary,
                unfocusedLabelColor = TDriveColors.OnSurfaceDim
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = apiHash,
            onValueChange = { 
                apiHash = it.trim()
                errorMessage = null
                isSubmitting = false
            },
            label = { Text("API Hash") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TDriveColors.Primary,
                unfocusedBorderColor = TDriveColors.Outline,
                focusedContainerColor = TDriveColors.SurfaceElevated,
                unfocusedContainerColor = TDriveColors.SurfaceElevated,
                focusedTextColor = TDriveColors.OnSurface,
                unfocusedTextColor = TDriveColors.OnSurface,
                focusedLabelColor = TDriveColors.Primary,
                unfocusedLabelColor = TDriveColors.OnSurfaceDim
            )
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage ?: "",
                color = TDriveColors.Error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val cleanId = apiId.trim()
                val cleanHash = apiHash.trim()
                if (cleanId.isBlank() || cleanHash.isBlank()) {
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
            },
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TDriveColors.Primary,
                disabledContainerColor = TDriveColors.Primary.copy(alpha = 0.6f)
            )
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    text = "Continue",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}
