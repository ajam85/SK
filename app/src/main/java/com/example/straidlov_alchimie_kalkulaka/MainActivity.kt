package com.example.straidlov_alchimie_kalkulaka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.straidlov_alchimie_kalkulaka.ui.screens.GridScreen
import com.example.straidlov_alchimie_kalkulaka.ui.screens.HistoryScreen
import com.example.straidlov_alchimie_kalkulaka.ui.screens.SettingsScreen
import com.example.straidlov_alchimie_kalkulaka.ui.screens.StatsScreen
import com.example.straidlov_alchimie_kalkulaka.ui.theme.Strašidlov_alchimie_kalkulačkaTheme
import com.example.straidlov_alchimie_kalkulaka.viewmodel.AppTab
import com.example.straidlov_alchimie_kalkulaka.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Strašidlov_alchimie_kalkulačkaTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: MainViewModel = viewModel()
) {
    val activeTab by viewModel.activeTab.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (activeTab) {
                            AppTab.GRID -> "Strašidlov - Kalkulačka"
                            AppTab.HISTORY -> "Historie kombinací"
                            AppTab.STATS -> "Statistiky"
                            AppTab.SETTINGS -> "Nastavení"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF9333EA),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeTab == AppTab.GRID,
                    onClick = { viewModel.selectTab(AppTab.GRID) },
                    icon = { Icon(Icons.Default.GridOn, contentDescription = "Mřížka") },
                    label = { Text("Mřížka") }
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.HISTORY,
                    onClick = { viewModel.selectTab(AppTab.HISTORY) },
                    icon = { Icon(Icons.Default.History, contentDescription = "Historie") },
                    label = { Text("Historie") }
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.STATS,
                    onClick = { viewModel.selectTab(AppTab.STATS) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Statistiky") },
                    label = { Text("Statistiky") }
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.SETTINGS,
                    onClick = { viewModel.selectTab(AppTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Nastavení") },
                    label = { Text("Nastavení") }
                )
            }
        }
    ) { innerPadding ->
        when (activeTab) {
            AppTab.GRID -> GridScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.HISTORY -> HistoryScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.STATS -> StatsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.SETTINGS -> SettingsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
