package com.example.straidlov_alchimie_kalkulaka.model

data class GridCombination(
    val id: Int,
    val status: CombinationStatus = CombinationStatus.UNTESTED,
    val note: String = "",
    val author: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = ""
)
