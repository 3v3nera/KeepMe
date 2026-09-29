package com.telegramdrive.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telegramdrive.app.domain.model.DateFilter
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.utils.rememberAppHaptics
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
private fun FilterOptionRow(
    selected: Boolean,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    subtitle: String? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(GlassPanelTokens.InnerRadius))
            .background(if (selected) Color.White.copy(alpha = 0.20f) else GlassPanelTokens.PillFill)
            .then(
                if (selected) Modifier.border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(GlassPanelTokens.InnerRadius))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = GlassPanelTokens.TextPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = GlassPanelTokens.TextSecondary
                )
            }
        }
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateMonthFilterSheet(
    activeFilter: DateFilter?,
    availableMonths: List<DateFilter.Month>,
    onSelectFilter: (DateFilter?) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = rememberAppHaptics()
    var showDatePickerDialog by remember { mutableStateOf(false) }

    GlassSheetDialog(
        title = "Filter by Date & Month",
        subtitle = "View files from a specific month or date",
        doneText = "Close",
        onDismiss = onDismiss
    ) {
        FilterOptionRow(
            selected = activeFilter == null,
            icon = Icons.Rounded.CalendarMonth,
            title = "All Dates (No Filter)",
            onClick = {
                haptics.click()
                onSelectFilter(null)
                onDismiss()
            }
        )

        if (availableMonths.isNotEmpty()) {
            GlassSectionHeader("Months in this folder")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                availableMonths.chunked(2).forEach { rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowMonths.forEach { monthOption ->
                            val isSelected = activeFilter is DateFilter.Month &&
                                    activeFilter.year == monthOption.year &&
                                    activeFilter.month == monthOption.month
                            FilterOptionRow(
                                selected = isSelected,
                                title = monthOption.displayName,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics.click()
                                    onSelectFilter(monthOption)
                                    onDismiss()
                                }
                            )
                        }
                        if (rowMonths.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        GlassSectionHeader("Pick a specific day")
        FilterOptionRow(
            selected = activeFilter is DateFilter.SpecificDate,
            icon = Icons.Rounded.DateRange,
            title = if (activeFilter is DateFilter.SpecificDate) activeFilter.displayName else "Choose a date from calendar",
            subtitle = if (activeFilter is DateFilter.SpecificDate) "Filtering by exact date" else null,
            onClick = {
                haptics.click()
                showDatePickerDialog = true
            }
        )
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )

        GlassSheetDialog(
            title = "Pick a day",
            doneText = "Cancel",
            onDismiss = { showDatePickerDialog = false },
            actions = {
                GlassPrimaryButton(
                    text = "Apply Filter",
                    onClick = {
                        haptics.click()
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = selectedMillis
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val startOfDay = cal.timeInMillis
                            cal.set(Calendar.HOUR_OF_DAY, 23)
                            cal.set(Calendar.MINUTE, 59)
                            cal.set(Calendar.SECOND, 59)
                            cal.set(Calendar.MILLISECOND, 999)
                            val endOfDay = cal.timeInMillis

                            val formattedName = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedMillis))
                            onSelectFilter(
                                DateFilter.SpecificDate(
                                    startOfDayMillis = startOfDay,
                                    endOfDayMillis = endOfDay,
                                    displayName = formattedName
                                )
                            )
                        }
                        showDatePickerDialog = false
                        onDismiss()
                    }
                )
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    headlineContentColor = Color.White,
                    weekdayContentColor = GlassPanelTokens.TextSecondary,
                    subheadContentColor = GlassPanelTokens.TextSecondary,
                    navigationContentColor = Color.White,
                    yearContentColor = Color.White,
                    currentYearContentColor = Color.White,
                    selectedYearContainerColor = Color.White,
                    selectedYearContentColor = Color(0xFF0B0F1A),
                    dayContentColor = Color.White,
                    disabledDayContentColor = Color.White.copy(alpha = 0.3f),
                    selectedDayContainerColor = Color.White,
                    selectedDayContentColor = Color(0xFF0B0F1A),
                    todayContentColor = Color.White,
                    todayDateBorderColor = Color.White.copy(alpha = 0.6f)
                )
            )
        }
    }
}