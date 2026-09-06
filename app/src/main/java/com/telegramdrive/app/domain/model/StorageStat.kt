package com.telegramdrive.app.domain.model

data class StorageStat(
    val fileType: FileType,
    val totalSize: Long,
    val fileCount: Int
)
