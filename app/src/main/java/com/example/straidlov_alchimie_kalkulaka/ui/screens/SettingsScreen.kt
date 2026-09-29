package com.example.straidlov_alchimie_kalkulaka.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.straidlov_alchimie_kalkulaka.viewmodel.MainViewModel
import com.example.straidlov_alchimie_kalkulaka.data.FirebaseSyncState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val stateConfig by viewModel.stateConfig.collectAsState()
    val firebaseSyncState by viewModel.firebaseSyncState.collectAsState()
    val firebaseSyncError by viewModel.firebaseSyncError.collectAsState()
    val firebaseSyncLog by viewModel.firebaseSyncLog.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val logDateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()) }

    var name0 by remember(stateConfig) { mutableStateOf(stateConfig.state0Name) }
    var name1 by remember(stateConfig) { mutableStateOf(stateConfig.state1Name) }
    var name2 by remember(stateConfig) { mutableStateOf(stateConfig.state2Name) }
    var name3 by remember(stateConfig) { mutableStateOf(stateConfig.state3Name) }
    var defaultAuthor by remember(stateConfig) { mutableStateOf(stateConfig.defaultAuthor) }
    var showSyncLogDialog by remember { mutableStateOf(false) }

    var importExportText by remember { mutableStateOf("") }
    val xmlPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val xml = uri?.let {
            context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }
        }
        if (xml.isNullOrBlank()) {
            Toast.makeText(context, "XML soubor se nepodařilo načíst", Toast.LENGTH_LONG).show()
        } else {
            viewModel.importXml(xml) { success ->
                val message = if (success) {
                    "XML bylo importováno a čeká na synchronizaci"
                } else {
                    "Chyba: XML neobsahuje rozpoznatelné kombinace"
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Section: Názvy stavů políček
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Názvy stavů políček",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Přizpůsobte si názvy pro 4 možné stavy (0–3) v každém políčku mřížky.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name0,
                    onValueChange = { name0 = it },
                    label = { Text("Stav 0 (Výchozí)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name1,
                    onValueChange = { name1 = it },
                    label = { Text("Stav 1") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name2,
                    onValueChange = { name2 = it },
                    label = { Text("Stav 2") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name3,
                    onValueChange = { name3 = it },
                    label = { Text("Stav 3") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = defaultAuthor,
                    onValueChange = { defaultAuthor = it },
                    label = { Text("Jméno objevitele / ověřitele") },
                    placeholder = { Text("Jméno se použije při zápisu výsledku") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val updated = stateConfig.copy(
                            state0Name = name0.ifBlank { "Stav 0" },
                            state1Name = name1.ifBlank { "Stav 1" },
                            state2Name = name2.ifBlank { "Stav 2" },
                            state3Name = name3.ifBlank { "Stav 3" },
                            defaultAuthor = defaultAuthor.trim()
                        )
                        viewModel.updateStateConfig(updated)
                        Toast.makeText(context, "Nastavení uloženo", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Uložit nastavení")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                viewModel.forceSync()
                Toast.makeText(context, "Synchronizace byla vynucena", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Vynutit synchronizaci")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when (firebaseSyncState) {
                FirebaseSyncState.LOCAL_ONLY -> "Synchronizace: pouze lokální režim"
                FirebaseSyncState.CONNECTING -> "Synchronizace: připojování…"
                FirebaseSyncState.CONNECTED -> "Synchronizace: připojeno"
                FirebaseSyncState.ERROR -> "Synchronizace: chyba"
            },
            color = when (firebaseSyncState) {
                FirebaseSyncState.ERROR -> MaterialTheme.colorScheme.error
                FirebaseSyncState.CONNECTED -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontSize = 12.sp
        )

        if (firebaseSyncState == FirebaseSyncState.ERROR && !firebaseSyncError.isNullOrBlank()) {
            Text(
                text = firebaseSyncError.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { showSyncLogDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Zobrazit log synchronizace")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Záloha a Import
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Záloha a Import dat",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "JSON slouží jako záloha. XML se načítá podle sloupců tabulky: kód, pořadí, úspěšnost, název relikvie a objevitel.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            val json = viewModel.exportJson()
                            importExportText = json
                            Toast.makeText(context, "Záloha vygenerována do pole níže", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exportovat", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = {
                            importExportText = ""
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Vyčistit pole", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        xmlPicker.launch(arrayOf("text/xml", "application/xml", "*/*"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Importovat XML podle tabulky")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = importExportText,
                    onValueChange = { importExportText = it },
                    label = { Text("JSON Kód zálohy") },
                    placeholder = { Text("Sem vložte kód zálohy pro import...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5
                )

                if (importExportText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.importJson(importExportText) { success ->
                                if (success) {
                                    Toast.makeText(context, "Data byla úspěšně importována!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Chyba: Neplatný formát zálohy JSON", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Provést import")
                    }
                }
            }
        }

    }

    if (showSyncLogDialog) {
        AlertDialog(
            onDismissRequest = { showSyncLogDialog = false },
            title = { Text("Log synchronizace databáze") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (firebaseSyncLog.isEmpty()) {
                        Text(
                            text = "Zatím zde nejsou žádné záznamy.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        firebaseSyncLog.takeLast(30).asReversed().forEach { entry ->
                            val target = entry.combinationId?.let { "Pořadí ${it + 1}" } ?: "Synchronizace"
                            Column(modifier = Modifier.padding(vertical = 3.dp)) {
                                Text(
                                    text = "${logDateFormat.format(Date(entry.timestamp))} • $target • ${if (entry.success) "OK" else "CHYBA"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (entry.success) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                                Text(
                                    text = entry.message,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSyncLogDialog = false }) {
                    Text("Zavřít")
                }
            }
        )
    }
}
