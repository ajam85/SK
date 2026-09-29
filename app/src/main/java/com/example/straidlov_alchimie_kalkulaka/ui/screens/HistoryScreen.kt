package com.example.straidlov_alchimie_kalkulaka.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.straidlov_alchimie_kalkulaka.model.CombinationStatus
import com.example.straidlov_alchimie_kalkulaka.model.GridUtils
import com.example.straidlov_alchimie_kalkulaka.model.StateConfig
import com.example.straidlov_alchimie_kalkulaka.ui.components.MiniGrid3x3
import com.example.straidlov_alchimie_kalkulaka.ui.components.StatusBadge
import com.example.straidlov_alchimie_kalkulaka.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val historyItems by viewModel.historyItems.collectAsState()
    val selectedFilter by viewModel.historyFilterStatus.collectAsState()
    val searchQuery by viewModel.historySearchQuery.collectAsState()
    val stateConfig by viewModel.stateConfig.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setHistorySearchQuery(it) },
            placeholder = { Text("Hledat pořadí, objevitele, relikvii nebo vzor...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setHistorySearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Vymazat hledání")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { viewModel.setHistoryFilter(null) },
                    label = { Text("Vše uložené") }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == CombinationStatus.FUNCTIONAL,
                    onClick = { viewModel.setHistoryFilter(CombinationStatus.FUNCTIONAL) },
                    label = { Text("Funkční") }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == CombinationStatus.NONFUNCTIONAL,
                    onClick = { viewModel.setHistoryFilter(CombinationStatus.NONFUNCTIONAL) },
                    label = { Text("Nefunkční") }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        GridLegend(stateConfig = stateConfig)

        Spacer(modifier = Modifier.height(8.dp))

        // List of entries
        if (historyItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank() || selectedFilter != null)
                        "Žádná kombinace neodpovídá filtru."
                    else
                        "Zatím nemáte žádné uložené vyzkoušené kombinace.\nOznačte kombinace na kartě 'Mřížka'.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        } else {
            val dateFormat = rememberDateFormat()

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(historyItems, key = { it.id }) { combo ->
                    val decodedGrid = GridUtils.decodeIndex(combo.id)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.openHistoryPreview(combo.id)
                            },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mini grid preview
                            MiniGrid3x3(
                                grid = decodedGrid,
                                stateConfig = stateConfig,
                                cellSize = 16.dp,
                                spacing = 3.dp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (combo.status != CombinationStatus.FUNCTIONAL) {
                                        Text(
                                            text = "Pořadí ${combo.id + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                    Text(
                                        text = formatHistoryDate(dateFormat, combo.updatedAt),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                if (combo.status == CombinationStatus.FUNCTIONAL) {
                                    Text(
                                        text = "Relikvie: ${combo.note.ifBlank { "—" }}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Text(
                                        text = "Pořadí ${combo.id + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }

                                Text(
                                    text = "Vzor: ${GridUtils.formatPatternString(decodedGrid)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (combo.status != CombinationStatus.FUNCTIONAL && combo.note.isNotBlank()) {
                                    Text(
                                        text = "Relikvie: ${combo.note}",
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                if (combo.author.isNotBlank()) {
                                    Text(
                                        text = "Objevitel / ověřitel: ${combo.author}",
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                StatusBadge(status = combo.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridLegend(stateConfig: StateConfig) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Text(
                text = "Legenda:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        for (state in 0..3) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(stateConfig.getColor(state), RoundedCornerShape(4.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    Text(
                        text = state.toString(),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberDateFormat(): SimpleDateFormat {
    return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
}

private fun formatHistoryDate(dateFormat: SimpleDateFormat, timestamp: Long): String {
    return if (timestamp > 0L) {
        dateFormat.format(Date(timestamp))
    } else {
        "Čas neuveden"
    }
}
