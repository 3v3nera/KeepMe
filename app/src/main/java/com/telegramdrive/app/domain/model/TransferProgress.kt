package com.telegramdrive.app.domain.model

data class TransferProgress(
    val fileId: Int,
    val fileName: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val isUpload: Boolean,    // true = upload, false = download
    val state: TransferState  // Queued, InProgress, Paused, Completed, Failed
) {
    val percentage: Float get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
    val speedBytesPerSec: Long = 0 // Calculated from delta
    val etaSeconds: Long = 0       // Calculated from speed
}

enum class TransferState { QUEUED, IN_PROGRESS, PAUSED, COMPLETED, FAILED }
