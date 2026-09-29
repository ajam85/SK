package com.example.straidlov_alchimie_kalkulaka.model

import androidx.compose.ui.graphics.Color

data class StateConfig(
    val state0Name: String = "Stav 0",
    val state1Name: String = "Stav 1",
    val state2Name: String = "Stav 2",
    val state3Name: String = "Stav 3",
    val defaultAuthor: String = "",
    val state0ColorHex: Long = 0xFF3B82F6, // Soft Blue
    val state1ColorHex: Long = 0xFF10B981, // Emerald Green
    val state2ColorHex: Long = 0xFFF59E0B, // Amber
    val state3ColorHex: Long = 0xFFEF4444  // Red
) {
    fun getColor(state: Int): Color {
        return when (state.coerceIn(0, 3)) {
            0 -> Color(state0ColorHex)
            1 -> Color(state1ColorHex)
            2 -> Color(state2ColorHex)
            else -> Color(state3ColorHex)
        }
    }

    fun getName(state: Int): String {
        return when (state.coerceIn(0, 3)) {
            0 -> state0Name
            1 -> state1Name
            2 -> state2Name
            else -> state3Name
        }
    }
}
