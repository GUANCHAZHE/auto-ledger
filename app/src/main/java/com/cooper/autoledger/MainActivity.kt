package com.cooper.autoledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cooper.autoledger.ui.LedgerViewModel
import com.cooper.autoledger.ui.components.NavTabIcons
import com.cooper.autoledger.ui.screens.HomeScreen
import com.cooper.autoledger.ui.screens.SettingsScreen
import com.cooper.autoledger.ui.screens.SimulatorScreen
import com.cooper.autoledger.ui.screens.StatsScreen
import com.cooper.autoledger.ui.theme.AutoLedgerTheme

private enum class AppTab(
    val navLabel: String,
    val topBarTitle: String,
    val icon: ImageVector
) {
    HOME("账本明细", "自动记账", NavTabIcons.Ledger),
    STATS("统计分析", "收支统计分析", NavTabIcons.Stats),
    SIMULATOR("识别测试", "屏幕识别测试台", NavTabIcons.Simulator),
    SETTINGS("设置权限", "设置与系统权限", NavTabIcons.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoLedgerTheme {
                AutoLedgerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoLedgerApp(
    ledgerViewModel: LedgerViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, ledgerViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                ledgerViewModel.refreshOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val uiState by ledgerViewModel.uiState.collectAsState()
    val simulatorState by ledgerViewModel.simulatorState.collectAsState()

    var currentTabName by rememberSaveable { mutableStateOf(AppTab.HOME.name) }
    val currentTab = AppTab.valueOf(currentTabName)
    var showAddBillDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentTab.topBarTitle,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTabName = tab.name },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.navLabel
                            )
                        },
                        label = { Text(tab.navLabel) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab == AppTab.HOME) {
                FloatingActionButton(
                    onClick = { showAddBillDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "手动记一笔"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.HOME -> {
                    HomeScreen(
                        uiState = uiState,
                        showAddDialog = showAddBillDialog,
                        onRequestAddDialog = { showAddBillDialog = true },
                        onDismissAddDialog = { showAddBillDialog = false },
                        onPreviousMonth = ledgerViewModel::previousMonth,
                        onNextMonth = ledgerViewModel::nextMonth,
                        onJumpToCurrentMonth = ledgerViewModel::jumpToCurrentMonth,
                        onEnableAutoCapture = { ledgerViewModel.setAutoCaptureEnabled(true) },
                        onNavigateToSimulator = { currentTabName = AppTab.SIMULATOR.name },
                        onAddManualBill = ledgerViewModel::addManualBill,
                        onUpdateBill = ledgerViewModel::updateBill,
                        onDeleteBill = ledgerViewModel::deleteBill
                    )
                }
                AppTab.STATS -> {
                    StatsScreen(
                        uiState = uiState,
                        onPreviousMonth = ledgerViewModel::previousMonth,
                        onNextMonth = ledgerViewModel::nextMonth,
                        onJumpToCurrentMonth = ledgerViewModel::jumpToCurrentMonth
                    )
                }
                AppTab.SIMULATOR -> {
                    SimulatorScreen(
                        state = simulatorState,
                        presets = ledgerViewModel.simulatorPresets,
                        onSelectPreset = ledgerViewModel::selectSimulatorPreset,
                        onUpdatePackage = ledgerViewModel::updateSimulatorPackage,
                        onUpdateNodesText = ledgerViewModel::updateSimulatorNodesText,
                        onParseScreen = { ledgerViewModel.parseSimulatorScreen() },
                        onSimulateAutoInsert = ledgerViewModel::simulateAutoInsert,
                        onNavigateToHome = { currentTabName = AppTab.HOME.name }
                    )
                }
                AppTab.SETTINGS -> {
                    SettingsScreen(
                        uiState = uiState,
                        onToggleAutoCapture = ledgerViewModel::setAutoCaptureEnabled,
                        onRefreshPermissions = ledgerViewModel::refreshOnResume,
                        onExportCsv = ledgerViewModel::exportCsvAsync
                    )
                }
            }
        }
    }
}
