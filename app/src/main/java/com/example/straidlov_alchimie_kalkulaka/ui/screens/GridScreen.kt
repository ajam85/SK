package com.example.straidlov_alchimie_kalkulaka.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.straidlov_alchimie_kalkulaka.model.CombinationStatus
import com.example.straidlov_alchimie_kalkulaka.model.GridUtils
import com.example.straidlov_alchimie_kalkulaka.ui.components.Grid3x3
import com.example.straidlov_alchimie_kalkulaka.ui.components.StatusBadge
import com.example.straidlov_alchimie_kalkulaka.viewmodel.MainViewModel

@Composable
fun GridScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentGrid by viewModel.currentGrid.collectAsState()
    val currentId by viewModel.currentId.collectAsState()
    val currentCombo by viewModel.currentCombination.collectAsState()
    val currentRelicName by viewModel.currentRelicName.collectAsState()
    val stateConfig by viewModel.stateConfig.collectAsState()
    val isHistoryPreview by viewModel.isHistoryPreview.collectAsState()

    val scrollState = rememberScrollState()
    var showRelicDialog by remember { mutableStateOf(false) }
    var relicName by remember { mutableStateOf("") }
    var showSavedDialog by remember { mutableStateOf(false) }
    var savedStatus by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isHistoryPreview) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Relikvie: ${currentCombo.note.ifBlank { "—" }}",
                        modifier = Modifier.padding(start = 4.dp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { viewModel.editCurrentCombination() }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Upravit vzor"
                        )
                    }
                }

                Grid3x3(
                    grid = currentGrid,
                    stateConfig = stateConfig,
                    onCellClick = {},
                    onCellStateSelect = { _, _ -> },
                    readOnly = true
                )
            }
        } else {
            // Header Card with ID & Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pořadí ${currentId + 1}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "z ${GridUtils.TOTAL_COMBINATIONS} kombinací",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    StatusBadge(status = currentCombo.status)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3x3 Grid Component
            Grid3x3(
                grid = currentGrid,
                stateConfig = stateConfig,
                onCellClick = { viewModel.cycleCell(it) },
                onCellStateSelect = { index, state -> viewModel.setCell(index, state) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status Marking Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.markStatus(CombinationStatus.NONFUNCTIONAL) {
                            savedStatus = "Nefunkční"
                            showSavedDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Nefunkční", fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        relicName = currentRelicName
                        showRelicDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Funkční", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Generators & Next Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.loadNextUntested() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.padding(2.dp))
                    Text("Další netestovaná", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.loadRandomUntested() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.padding(2.dp))
                    Text("Náhodná", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

    }

    if (!isHistoryPreview && showRelicDialog) {
        AlertDialog(
            onDismissRequest = { showRelicDialog = false },
            title = { Text("Název relikvie") },
            text = {
                OutlinedTextField(
                    value = relicName,
                    onValueChange = { relicName = it },
                    label = { Text("Relikvie") },
                    placeholder = { Text("Zadejte název nalezené relikvie") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRelicDialog = false
                        viewModel.markStatus(CombinationStatus.FUNCTIONAL, relicName.trim()) {
                            savedStatus = "Funkční"
                            showSavedDialog = true
                        }
                    },
                    enabled = relicName.isNotBlank()
                ) {
                    Text("Zapsat výsledek")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRelicDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }

    if (showSavedDialog) {
        AlertDialog(
            onDismissRequest = { showSavedDialog = false },
            title = { Text("Výsledek zapsán") },
            text = { Text("Kombinace byla uložena jako $savedStatus a zařazena do synchronizace.") },
            confirmButton = {
                TextButton(onClick = { showSavedDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
