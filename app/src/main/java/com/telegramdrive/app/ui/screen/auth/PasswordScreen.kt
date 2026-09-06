package com.telegramdrive.app.ui.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.utils.rememberAppHaptics

@Composable
fun PasswordScreen(
    hint: String,
    onSubmit: (String) -> Unit
) {
    val haptics = rememberAppHaptics()
    var password by remember { mutableStateOf("") }

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
            color = TDriveColors.OnSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your account is protected with an additional password.",
            style = MaterialTheme.typography.bodyMedium,
            color = TDriveColors.OnSurfaceDim
        )
        if (hint.isNotBlank()) {
            Text(
                text = "Hint: $hint",
                style = MaterialTheme.typography.bodySmall,
                color = TDriveColors.Primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
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
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (password.isNotBlank()) {
                    haptics.click()
                    onSubmit(password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TDriveColors.Primary,
                disabledContainerColor = TDriveColors.SurfaceElevated
            )
        ) {
            Text(
                "Sign In",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}
