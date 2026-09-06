package com.telegramdrive.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.shimmer

@Composable
fun ShimmerFileCard(modifier: Modifier = Modifier, aspectRatio: Float? = null) {
    val finalRatio = remember(aspectRatio) {
        aspectRatio ?: listOf(1f, 4f / 3f, 3f / 4f, 16f / 9f, 9f / 16f).random()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shimmer(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(finalRatio)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}
