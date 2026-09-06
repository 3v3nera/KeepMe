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
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.utils.rememberAppHaptics

@Composable
fun OtpVerificationScreen(
    onSubmit: (String) -> Unit
) {
    val haptics = rememberAppHaptics()
    var code by remember { mutableStateOf("") }

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
            color = TDriveColors.OnSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We've sent the code to the Telegram app on your other device.",
            style = MaterialTheme.typography.bodyMedium,
            color = TDriveColors.OnSurfaceDim
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() }
                if (filtered.length > code.length) {
                    haptics.tick()
                }
                code = filtered
            },
            label = { Text("Code") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TDriveColors.Primary,
                unfocusedBorderColor = TDriveColors.Outline,
                focusedContainerColor = TDriveColors.SurfaceElevated,
                unfocusedContainerColor = TDriveColors.SurfaceElevated,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = TDriveColors.Primary,
                unfocusedLabelColor = TDriveColors.OnSurfaceDim
            )
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (code.isNotBlank()) {
                    haptics.click()
                    onSubmit(code)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TDriveColors.Primary)
        ) {
            Text("Verify", color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}
