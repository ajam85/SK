package com.example.straidlov_alchimie_kalkulaka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.straidlov_alchimie_kalkulaka.model.StateConfig

@Composable
fun MiniGrid3x3(
    grid: IntArray,
    stateConfig: StateConfig,
    modifier: Modifier = Modifier,
    cellSize: Dp = 14.dp,
    spacing: Dp = 3.dp
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        for (row in 0 until 3) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    val state = grid.getOrElse(index) { 0 }
                    val color = stateConfig.getColor(state)

                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                }
            }
        }
    }
}
