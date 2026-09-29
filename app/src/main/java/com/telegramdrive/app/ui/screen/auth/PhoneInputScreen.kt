package com.telegramdrive.app.ui.screen.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.utils.rememberAppHaptics
import com.togitech.ccp.data.CountryData
import com.togitech.ccp.data.utils.getLibCountries

@Composable
fun PhoneInputScreen(
    onSubmit: (String) -> Unit
) {
    val haptics = rememberAppHaptics()
    val allCountries = remember { getLibCountries }

    // Default to India (country code "in", phone code "+91")
    val defaultCountry = remember(allCountries) {
        allCountries.firstOrNull { it.countryCode.equals("in", ignoreCase = true) }
            ?: allCountries.firstOrNull { it.countryPhoneCode == "+91" }
            ?: allCountries.first()
    }

    var selectedCountry by remember { mutableStateOf(defaultCountry) }
    var phoneNumber by remember { mutableStateOf("") }
    var showCountryDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Your Phone Number",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = TDriveColors.OnCanvas
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Please confirm your country code and enter your phone number.",
            style = MaterialTheme.typography.bodyMedium,
            color = TDriveColors.OnCanvasMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Country Selector Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.CircleShape)
                .clickable {
                    haptics.click()
                    showCountryDialog = true
                },
            shape = androidx.compose.foundation.shape.CircleShape,
            color = TDriveColors.FillSoft
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = countryCodeToEmoji(selectedCountry.countryCode),
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = selectedCountry.cNames,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = TDriveColors.OnCanvas,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = selectedCountry.countryPhoneCode,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TDriveColors.OnCanvas
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = "Select Country",
                    tint = TDriveColors.OnCanvasMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Phone Number Input
        com.telegramdrive.app.ui.components.GlassTextField(
            value = phoneNumber,
            onValueChange = { input ->
                phoneNumber = input.filter { it.isDigit() }
                errorMessage = null
            },
            placeholder = "Phone number",
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leading = {
                Text(
                    text = selectedCountry.countryPhoneCode,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = TDriveColors.OnCanvas
                )
            }
        )
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = errorMessage ?: "",
                color = TDriveColors.Error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        var isSubmitting by remember { mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(isSubmitting) {
            if (isSubmitting) {
                kotlinx.coroutines.delay(8000)
                isSubmitting = false
            }
        }

        com.telegramdrive.app.ui.components.GlassPrimaryButton(
            text = "Next",
            loading = isSubmitting,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (isSubmitting) return@GlassPrimaryButton
                val cleanDigits = phoneNumber.trim().trimStart('0')
                if (cleanDigits.length < 5) {
                    haptics.error()
                    errorMessage = "Please enter a valid phone number"
                } else {
                    haptics.click()
                    errorMessage = null
                    isSubmitting = true
                    val fullNumber = "${selectedCountry.countryPhoneCode}$cleanDigits"
                    onSubmit(fullNumber)
                }
            }
        )
    }
    // Country Search & Selection Dialog
    if (showCountryDialog) {
        CountryPickerDialog(
            countries = allCountries,
            selectedCountry = selectedCountry,
            onCountrySelected = {
                selectedCountry = it
                showCountryDialog = false
                haptics.click()
            },
            onDismiss = { showCountryDialog = false }
        )
    }
}

@Composable
private fun CountryPickerDialog(
    countries: List<CountryData>,
    selectedCountry: CountryData,
    onCountrySelected: (CountryData) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(searchQuery, countries) {
        if (searchQuery.isBlank()) {
            countries
        } else {
            val query = searchQuery.trim().lowercase()
            countries.filter { country ->
                country.cNames.lowercase().contains(query) ||
                country.countryPhoneCode.lowercase().contains(query) ||
                country.countryCode.lowercase().contains(query)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = androidx.compose.ui.graphics.Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, TDriveColors.LineSoft)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Country",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TDriveColors.OnCanvas
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = TDriveColors.OnCanvasMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search country or dial code...",
                            color = TDriveColors.OnCanvasMuted.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = TDriveColors.OnCanvasMuted
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = TDriveColors.OnCanvasMuted
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TDriveColors.PrimaryVariant,
                        unfocusedBorderColor = TDriveColors.LineSoft,
                        focusedContainerColor = TDriveColors.FillSoft,
                        unfocusedContainerColor = TDriveColors.FillSoft,
                        focusedTextColor = TDriveColors.OnCanvas,
                        unfocusedTextColor = TDriveColors.OnCanvas
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Country List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredCountries, key = { it.countryCode }) { country ->
                        val isSelected = country.countryCode == selectedCountry.countryCode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) TDriveColors.PrimaryVariant.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .clickable { onCountrySelected(country) }
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = countryCodeToEmoji(country.countryCode),
                                fontSize = 22.sp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = country.cNames,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) TDriveColors.PrimaryVariant else TDriveColors.OnCanvas,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = country.countryPhoneCode,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) TDriveColors.PrimaryVariant else TDriveColors.OnCanvasMuted
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = TDriveColors.PrimaryVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun countryCodeToEmoji(countryCode: String): String {
    if (countryCode.length != 2) return "🌐"
    return try {
        val upper = countryCode.uppercase()
        val firstChar = Character.codePointAt(upper, 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(upper, 1) - 0x41 + 0x1F1E6
        String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    } catch (e: Exception) {
        "🌐"
    }
}
