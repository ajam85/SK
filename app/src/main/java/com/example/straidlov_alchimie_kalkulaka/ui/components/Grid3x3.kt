package com.example.straidlov_alchimie_kalkulaka.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.straidlov_alchimie_kalkulaka.model.StateConfig

private val AlchemicalSymbols = listOf(
    "☿", "☉", "☽", "♄", "♃", "♀", "♂", "♁", "🜂", "🜄", "🜁", "🜃"
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Grid3x3(
    grid: IntArray,
    stateConfig: StateConfig,
    onCellClick: (Int) -> Unit,
    onCellStateSelect: (Int, Int) -> Unit,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (row in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    val state = grid.getOrElse(index) { 0 }
                    var menuExpanded by remember { mutableStateOf(false) }

                    val targetBgColor = stateConfig.getColor(state)
                    val isEmpty = state == 0
                    val backgroundColor = if (isEmpty) {
                        Color.Transparent
                    } else {
                        targetBgColor.copy(alpha = 0.55f)
                    }
                    val borderColor = Color(0xFFD946EF)
                    val animatedBgColor by animateColorAsState(
                        targetValue = backgroundColor,
                        animationSpec = tween(durationMillis = 200),
                        label = "CellBgColor"
                    )

                    // Text color based on luminance
                    val textColor = if (isEmpty) {
                        Color(0xFF9333EA)
                    } else if (targetBgColor.luminance() > 0.5f) {
                        Color.Black
                    } else {
                        Color.White
                    }
                    val backgroundSymbol = remember(index) { AlchemicalSymbols.random() }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .shadow(4.dp, shape = RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(animatedBgColor)
                            .border(
                                width = 2.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .combinedClickable(
                                enabled = !readOnly,
                                onClick = { onCellClick(index) },
                                onLongClick = { menuExpanded = true }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = backgroundSymbol,
                            color = textColor.copy(alpha = if (isEmpty) 0.18f else 0.14f),
                            fontSize = 58.sp,
                            fontWeight = FontWeight.Normal
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stateConfig.getName(state),
                                color = textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Context menu on long-click to select state directly
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            for (s in 0..3) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stateConfig.getName(s),
                                            fontWeight = if (s == state) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onCellStateSelect(index, s)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
