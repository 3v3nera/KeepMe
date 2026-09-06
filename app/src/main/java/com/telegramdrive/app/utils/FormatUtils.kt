package com.telegramdrive.app.utils

fun formatRelativeTime(timestamp: Long): String {
    return FileDateUtils.formatRelativeTime(timestamp)
}
