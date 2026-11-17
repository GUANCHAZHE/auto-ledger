package com.cooper.autoledger.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.autoledger.capture.PaymentNotificationService
import com.cooper.autoledger.capture.ScreenBillParser
import com.cooper.autoledger.capture.ScreenCaptureAccessibilityService
import com.cooper.autoledger.data.LedgerRepository
import com.cooper.autoledger.model.BillRecord
import com.cooper.autoledger.model.Category
import com.cooper.autoledger.model.CurrencyUtils
import com.cooper.autoledger.model.ParsedScreenBill
import com.cooper.autoledger.model.PaymentChannel
import com.cooper.autoledger.model.TransactionType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DailyBillGroup(
    val date: LocalDate,
    val dateLabel: String,
    val dayExpenseCents: Long,
    val dayIncomeCents: Long,
    val records: List<BillRecord>
)

data class CategoryStat(
    val category: Category,
    val amountCents: Long,
    val count: Int,
    val ratio: Float
)

data class DailyTrendPoint(
    val date: LocalDate,
    val shortLabel: String,
    val amountCents: Long,
    val isToday: Boolean
)

data class SimulatorPreset(
    val title: String,
    val subtitle: String,
    val packageName: String,
    val nodes: List<String>,
    val isAntiFalsePositiveCase: Boolean = false
)

data class SimulatorUiState(
    val selectedPresetIndex: Int? = 0,
    val packageName: String = "",
    val nodeLinesText: String = "",
    val hasEvaluated: Boolean = false,
    val parsedPreview: ParsedScreenBill? = null,
    val feedbackMessage: String? = null,
    val feedbackIsWarning: Boolean = false
)

data class ServicePermissionState(
    val accessibilityEnabled: Boolean = false,
    val notificationEnabled: Boolean = false,
    val postNotificationsGranted: Boolean = true
)

data class LedgerUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val monthLabel: String = formatMonthLabel(YearMonth.now()),
    val isCurrentMonth: Boolean = true,
    val monthRecords: List<BillRecord> = emptyList(),
    val totalAllTimeCount: Int = 0,
    val dailyGroups: List<DailyBillGroup> = emptyList(),
    val monthlyExpenseCents: Long = 0L,
    val monthlyIncomeCents: Long = 0L,
    val monthlyBalanceCents: Long = 0L,
    val autoCapturedCount: Int = 0,
    val expenseCategoryStats: List<CategoryStat> = emptyList(),
    val incomeCategoryStats: List<CategoryStat> = emptyList(),
    val last7DaysExpenseTrend: List<DailyTrendPoint> = emptyList(),
    val last7DaysIncomeTrend: List<DailyTrendPoint> = emptyList(),
    val autoCaptureEnabled: Boolean = true,
    val accessibilityEnabled: Boolean = false,
    val notificationEnabled: Boolean = false,
    val postNotificationsGranted: Boolean = true
)

private fun formatMonthLabel(month: YearMonth): String =
    "${month.year}年${month.monthValue}月"

class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LedgerRepository = LedgerRepository.getInstance(application)

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _permissions = MutableStateFlow(readServicePermissions())

    val simulatorPresets: List<SimulatorPreset> = listOf(
        SimulatorPreset(
            title = "微信支付成功页",
            subtitle = "瑞幸咖啡 ¥18.50 · 餐饮自动识别",
            packageName = "com.tencent.mm",
            nodes = listOf(
                "支付成功",
                "瑞幸咖啡(科技园店)",
                "￥18.50",
                "支付方式",
                "招商银行储蓄卡(8812)"
            )
        ),
        SimulatorPreset(
            title = "支付宝账单页",
            subtitle = "美团外卖 -36.80 · 账单详情页识别",
            packageName = "com.eg.android.AlipayGphone",
            nodes = listOf(
                "付款成功",
                "-36.80",
                "收款方全称",
                "美团外卖",
                "付款方式",
                "花呗"
            )
        ),
        SimulatorPreset(
            title = "退款/收款入账页",
            subtitle = "京东商城 +89.00 · 收入/退款识别",
            packageName = "com.tencent.mm",
            nodes = listOf(
                "退款到账",
                "+89.00",
                "京东商城",
                "已退回零钱"
            )
        ),
        SimulatorPreset(
            title = "微信聊天防误触测试",
            subtitle = "普通对话含金额 · 验证安全拦截不记账",
            packageName = "com.tencent.mm",
            nodes = listOf(
                "张三",
                "今天午饭花了 25.00 元",
                "发消息"
            ),
            isAntiFalsePositiveCase = true
        )
    )

    private val _simulatorState = MutableStateFlow(buildInitialSimulatorState())
    val simulatorState: StateFlow<SimulatorUiState> = _simulatorState.asStateFlow()

    val uiState: StateFlow<LedgerUiState> = combine(
        repository.records,
        repository.autoCaptureEnabled,
        _selectedMonth,
        _permissions
    ) { allRecords, autoEnabled, month, perms ->
        buildLedgerUiState(
            allRecords = allRecords,
            autoCaptureEnabled = autoEnabled,
            selectedMonth = month,
            perms = perms
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = buildLedgerUiState(
            allRecords = repository.records.value,
            autoCaptureEnabled = repository.autoCaptureEnabled.value,
            selectedMonth = _selectedMonth.value,
            perms = _permissions.value
        )
    )

    private fun buildInitialSimulatorState(): SimulatorUiState {
        val defaultPreset = simulatorPresets.first()
        val preview = runCatching {
            ScreenBillParser.parseScreenNodes(defaultPreset.packageName, defaultPreset.nodes)
        }.getOrNull()
        return SimulatorUiState(
            selectedPresetIndex = 0,
            packageName = defaultPreset.packageName,
            nodeLinesText = defaultPreset.nodes.joinToString("\n"),
            hasEvaluated = true,
            parsedPreview = preview
        )
    }

    private fun readServicePermissions(): ServicePermissionState {
        val app = getApplication<Application>()
        val accessibility = runCatching {
            ScreenCaptureAccessibilityService.isServiceEnabled(app)
        }.getOrDefault(false)
        val notification = runCatching {
            PaymentNotificationService.isNotificationAccessEnabled(app)
        }.getOrDefault(false)
        val postNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                app,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return ServicePermissionState(
            accessibilityEnabled = accessibility,
            notificationEnabled = notification,
            postNotificationsGranted = postNotif
        )
    }

    fun refreshOnResume() {
        _permissions.value = readServicePermissions()
        viewModelScope.launch(Dispatchers.IO) {
            repository.refresh()
        }
    }

    fun refreshPermissions() {
        _permissions.value = readServicePermissions()
    }

    fun previousMonth() {
        _selectedMonth.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        _selectedMonth.update { it.plusMonths(1) }
    }

    fun jumpToCurrentMonth() {
        _selectedMonth.value = YearMonth.now()
    }

    fun setAutoCaptureEnabled(enabled: Boolean) {
        repository.setAutoCaptureEnabled(enabled)
    }

    fun addManualBill(
        amountCents: Long,
        type: TransactionType,
        category: Category,
        channel: PaymentChannel,
        merchant: String,
        note: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val record = BillRecord(
                amountCents = amountCents,
                type = type,
                category = category,
                merchant = merchant.ifBlank { category.displayName },
                note = note.trim(),
                channel = channel,
                sourcePkg = "com.cooper.autoledger",
                timestampMs = System.currentTimeMillis(),
                autoCaptured = false
            )
            repository.insertRecord(record)
            _selectedMonth.value = YearMonth.now()
        }
    }

    fun updateBill(
        existing: BillRecord,
        amountCents: Long,
        type: TransactionType,
        category: Category,
        channel: PaymentChannel,
        merchant: String,
        note: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = existing.copy(
                amountCents = amountCents,
                type = type,
                category = category,
                channel = channel,
                merchant = merchant.ifBlank { category.displayName },
                note = note.trim()
            )
            repository.updateRecord(updated)
        }
    }

    fun deleteBill(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRecord(id)
        }
    }

    fun selectSimulatorPreset(index: Int) {
        val preset = simulatorPresets.getOrNull(index) ?: return
        val parsed = runCatching {
            ScreenBillParser.parseScreenNodes(preset.packageName, preset.nodes)
        }.getOrNull()
        _simulatorState.value = SimulatorUiState(
            selectedPresetIndex = index,
            packageName = preset.packageName,
            nodeLinesText = preset.nodes.joinToString("\n"),
            hasEvaluated = true,
            parsedPreview = parsed,
            feedbackMessage = null,
            feedbackIsWarning = false
        )
    }

    fun updateSimulatorPackage(packageName: String) {
        _simulatorState.update {
            it.copy(
                selectedPresetIndex = null,
                packageName = packageName,
                feedbackMessage = null
            )
        }
    }

    fun updateSimulatorNodesText(nodeLinesText: String) {
        _simulatorState.update {
            it.copy(
                selectedPresetIndex = null,
                nodeLinesText = nodeLinesText,
                feedbackMessage = null
            )
        }
    }

    fun parseSimulatorScreen(): ParsedScreenBill? {
        val state = _simulatorState.value
        val pkg = state.packageName.trim()
        val nodes = state.nodeLinesText
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val parsed = if (nodes.isEmpty()) {
            null
        } else {
            runCatching {
                ScreenBillParser.parseScreenNodes(pkg, nodes)
            }.getOrNull()
        }

        _simulatorState.update {
            it.copy(
                hasEvaluated = true,
                parsedPreview = parsed,
                feedbackMessage = if (nodes.isEmpty()) {
                    "请至少输入一行屏幕节点文本后再解析"
                } else {
                    null
                },
                feedbackIsWarning = nodes.isEmpty()
            )
        }
        return parsed
    }

    fun simulateAutoInsert() {
        val parsed = parseSimulatorScreen()
        if (parsed == null) {
            _simulatorState.update {
                it.copy(
                    feedbackMessage = "当前屏幕未识别为有效支付页面，防误触引擎已拦截入账。",
                    feedbackIsWarning = true
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val saved = repository.insertFromParsedIfNotDuplicate(parsed)
            withContext(Dispatchers.Main) {
                if (saved != null) {
                    _selectedMonth.value = YearMonth.now()
                    val sign = if (saved.type == TransactionType.EXPENSE) "-" else "+"
                    val yuan = CurrencyUtils.formatYuan(saved.amountCents)
                    _simulatorState.update {
                        it.copy(
                            feedbackMessage = "已成功自动入账：$sign¥$yuan · ${saved.category.displayName}（${saved.merchant}）",
                            feedbackIsWarning = false
                        )
                    }
                } else {
                    _simulatorState.update {
                        it.copy(
                            feedbackMessage = "触发 90 秒智能去重保护：同金额与渠道账单刚刚已入账，避免重复记账！",
                            feedbackIsWarning = true
                        )
                    }
                }
            }
        }
    }

    fun exportCsvAsync(onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val csv = repository.exportCsv()
            withContext(Dispatchers.Main) {
                onResult(csv)
            }
        }
    }

    private fun buildLedgerUiState(
        allRecords: List<BillRecord>,
        autoCaptureEnabled: Boolean,
        selectedMonth: YearMonth,
        perms: ServicePermissionState
    ): LedgerUiState {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        val recordsWithDate = allRecords.map { record ->
            val localDate = Instant.ofEpochMilli(record.timestampMs)
                .atZone(zone)
                .toLocalDate()
            record to localDate
        }

        val monthPairs = recordsWithDate
            .filter { (_, date) -> YearMonth.from(date) == selectedMonth }
            .sortedByDescending { (record, _) -> record.timestampMs }

        val monthRecords = monthPairs.map { it.first }

        var monthlyExpense = 0L
        var monthlyIncome = 0L
        var autoCount = 0

        monthRecords.forEach { r ->
            if (r.type == TransactionType.EXPENSE) {
                monthlyExpense += r.amountCents
            } else {
                monthlyIncome += r.amountCents
            }
            if (r.autoCaptured) {
                autoCount++
            }
        }

        val dailyGroups = monthPairs
            .groupBy { (_, date) -> date }
            .entries
            .sortedByDescending { it.key }
            .map { (date, pairs) ->
                val dayRecords = pairs.map { it.first }.sortedByDescending { it.timestampMs }
                val dayExp = dayRecords
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amountCents }
                val dayInc = dayRecords
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amountCents }
                DailyBillGroup(
                    date = date,
                    dateLabel = formatDayGroupHeader(date, today),
                    dayExpenseCents = dayExp,
                    dayIncomeCents = dayInc,
                    records = dayRecords
                )
            }

        val expenseStats = computeCategoryStats(monthRecords, TransactionType.EXPENSE)
        val incomeStats = computeCategoryStats(monthRecords, TransactionType.INCOME)

        // 7-day trend: if viewing current month, anchor at today; otherwise anchor at end of selectedMonth
        val currentYm = YearMonth.now(zone)
        val anchorDate = if (selectedMonth == currentYm) {
            today
        } else {
            selectedMonth.atEndOfMonth()
        }
        val trendFormatter = DateTimeFormatter.ofPattern("MM/dd")
        val byDateMap = recordsWithDate.groupBy({ it.second }, { it.first })

        val last7Expense = (6 downTo 0).map { offset ->
            val d = anchorDate.minusDays(offset.toLong())
            val cents = byDateMap[d]
                ?.filter { it.type == TransactionType.EXPENSE }
                ?.sumOf { it.amountCents } ?: 0L
            DailyTrendPoint(
                date = d,
                shortLabel = if (d == today) "今日" else d.format(trendFormatter),
                amountCents = cents,
                isToday = d == today
            )
        }

        val last7Income = (6 downTo 0).map { offset ->
            val d = anchorDate.minusDays(offset.toLong())
            val cents = byDateMap[d]
                ?.filter { it.type == TransactionType.INCOME }
                ?.sumOf { it.amountCents } ?: 0L
            DailyTrendPoint(
                date = d,
                shortLabel = if (d == today) "今日" else d.format(trendFormatter),
                amountCents = cents,
                isToday = d == today
            )
        }

        return LedgerUiState(
            selectedMonth = selectedMonth,
            monthLabel = formatMonthLabel(selectedMonth),
            isCurrentMonth = selectedMonth == currentYm,
            monthRecords = monthRecords,
            totalAllTimeCount = allRecords.size,
            dailyGroups = dailyGroups,
            monthlyExpenseCents = monthlyExpense,
            monthlyIncomeCents = monthlyIncome,
            monthlyBalanceCents = monthlyIncome - monthlyExpense,
            autoCapturedCount = autoCount,
            expenseCategoryStats = expenseStats,
            incomeCategoryStats = incomeStats,
            last7DaysExpenseTrend = last7Expense,
            last7DaysIncomeTrend = last7Income,
            autoCaptureEnabled = autoCaptureEnabled,
            accessibilityEnabled = perms.accessibilityEnabled,
            notificationEnabled = perms.notificationEnabled,
            postNotificationsGranted = perms.postNotificationsGranted
        )
    }

    private fun computeCategoryStats(
        records: List<BillRecord>,
        type: TransactionType
    ): List<CategoryStat> {
        val filtered = records.filter { it.type == type }
        val total = filtered.sumOf { it.amountCents }
        if (total <= 0L) return emptyList()

        return filtered
            .groupBy { it.category }
            .map { (category, list) ->
                val sum = list.sumOf { it.amountCents }
                CategoryStat(
                    category = category,
                    amountCents = sum,
                    count = list.size,
                    ratio = (sum.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
                )
            }
            .sortedByDescending { it.amountCents }
    }

    private fun formatDayGroupHeader(date: LocalDate, today: LocalDate): String {
        val weekStr = when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "周一"
            DayOfWeek.TUESDAY -> "周二"
            DayOfWeek.WEDNESDAY -> "周三"
            DayOfWeek.THURSDAY -> "周四"
            DayOfWeek.FRIDAY -> "周五"
            DayOfWeek.SATURDAY -> "周六"
            DayOfWeek.SUNDAY -> "周日"
        }
        val relativeSuffix = when (date) {
            today -> " · 今天"
            today.minusDays(1) -> " · 昨天"
            else -> ""
        }
        return "${date.monthValue}月${date.dayOfMonth}日 $weekStr$relativeSuffix"
    }
}
