package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }

    // SAF Launchers for Backups
    private val exportJsonLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val data = viewModel.getBackupJson()
                    outputStream.write(data.toByteArray())
                    Toast.makeText(this, "JSON Backup Exported Successfully!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to write backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val exportCsvLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val data = viewModel.getBackupCsv()
                    outputStream.write(data.toByteArray())
                    Toast.makeText(this, "CSV Report Exported Successfully!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to write report: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val importJsonLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val content = inputStream.bufferedReader().use { it.readText() }
                    val success = viewModel.restoreBackupFromJson(content)
                    if (success) {
                        Toast.makeText(this, "Backup Restored Successfully!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Failed to parse backup JSON.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to read file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    viewModel.uiMessage.collectLatest { message ->
                        scope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        val isMainScreen = when (currentScreen) {
                            is AppScreen.Welcome,
                            is AppScreen.Dashboard,
                            is AppScreen.Reports,
                            is AppScreen.FertilizerTypes,
                            is AppScreen.FarmersDetail -> true
                            else -> false
                        }
                        if (isMainScreen) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.height(76.dp)
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen is AppScreen.Welcome,
                                    onClick = { viewModel.navigateTo(AppScreen.Welcome) },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home") }
                                )
                                NavigationBarItem(
                                    selected = currentScreen is AppScreen.Dashboard,
                                    onClick = { viewModel.navigateTo(AppScreen.Dashboard) },
                                    icon = { Icon(Icons.Default.Info, contentDescription = "Stats") },
                                    label = { Text("Stats") }
                                )
                                NavigationBarItem(
                                    selected = currentScreen is AppScreen.Reports,
                                    onClick = { viewModel.navigateTo(AppScreen.Reports) },
                                    icon = { Icon(Icons.Default.List, contentDescription = "Reports") },
                                    label = { Text("Reports") }
                                )
                                NavigationBarItem(
                                    selected = currentScreen is AppScreen.FarmersDetail,
                                    onClick = { viewModel.navigateTo(AppScreen.FarmersDetail) },
                                    icon = { Icon(Icons.Default.Person, contentDescription = "Farmers") },
                                    label = { Text("Farmers") }
                                )
                                NavigationBarItem(
                                    selected = currentScreen is AppScreen.FertilizerTypes,
                                    onClick = { viewModel.navigateTo(AppScreen.FertilizerTypes) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Setup") },
                                    label = { Text("Setup") }
                                )
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (val screen = currentScreen) {
                            is AppScreen.Welcome -> {
                                WelcomeScreen(
                                    viewModel = viewModel,
                                    onNavigate = { viewModel.navigateTo(it) }
                                )
                            }
                            is AppScreen.AddSale -> {
                                AddEditSaleScreen(
                                    viewModel = viewModel,
                                    saleToEdit = null,
                                    onBack = { viewModel.navigateTo(AppScreen.Welcome) }
                                )
                            }
                            is AppScreen.EditSale -> {
                                AddEditSaleScreen(
                                    viewModel = viewModel,
                                    saleToEdit = screen.sale,
                                    onBack = { viewModel.navigateTo(AppScreen.Reports) }
                                )
                            }
                            is AppScreen.Dashboard -> {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateTo(AppScreen.Welcome) },
                                    onEditSale = { viewModel.navigateTo(AppScreen.EditSale(it)) }
                                )
                            }
                            is AppScreen.FertilizerTypes -> {
                                FertilizerTypesScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateTo(AppScreen.Welcome) }
                                )
                            }
                            is AppScreen.IncentiveCalculator -> {
                                IncentiveCalculatorScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateTo(AppScreen.Welcome) }
                                )
                            }
                            is AppScreen.Reports -> {
                                ReportsScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateTo(AppScreen.Welcome) },
                                    onEditSale = { viewModel.navigateTo(AppScreen.EditSale(it)) },
                                    onExportJson = { exportJsonLauncher.launch("fertilizer_daily_sales_backup.json") },
                                    onExportCsv = { exportCsvLauncher.launch("fertilizer_daily_sales_report.csv") },
                                    onImportJson = { importJsonLauncher.launch(arrayOf("application/json")) }
                                )
                            }
                            is AppScreen.FarmersDetail -> {
                                FarmersDetailScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateTo(AppScreen.Welcome) },
                                    onAddEditFarmer = { viewModel.navigateTo(AppScreen.AddEditFarmer(it)) }
                                )
                            }
                            is AppScreen.AddEditFarmer -> {
                                AddEditFarmerScreen(
                                    viewModel = viewModel,
                                    farmerToEdit = screen.farmer,
                                    onBack = { viewModel.navigateTo(AppScreen.FarmersDetail) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
