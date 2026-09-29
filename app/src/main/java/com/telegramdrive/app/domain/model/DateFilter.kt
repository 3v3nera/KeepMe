package com.telegramdrive.app.domain.model

sealed interface DateFilter {
    val displayName: String

    data class Month(
        val year: Int,
        val month: Int, // 0-indexed: 0 = January, 11 = December
        override val displayName: String
    ) : DateFilter

    data class SpecificDate(
        val startOfDayMillis: Long,
        val endOfDayMillis: Long,
        override val displayName: String
    ) : DateFilter
}
